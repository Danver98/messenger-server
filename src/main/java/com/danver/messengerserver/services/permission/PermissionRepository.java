package com.danver.messengerserver.services.permission;

import com.danver.messengerserver.exceptions.CompletableFutureException;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.models.permission.Permission;
import com.danver.messengerserver.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Array;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class PermissionRepository implements IPermissionRepository<UserDetails, Long> {
    private final RedisTemplate<String, ?> redis;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final DataSource dataSource;

    private final RedisTemplate<String, ?> redisTemplate;

    private final static String PERMISSION_KEY = Constants.REDIS_USERS_PERMISSIONS;
    private static final int BATCH_SIZE = 1000;

    @Autowired
    public PermissionRepository(RedisTemplate<String, ?> redis, JdbcTemplate jdbcTemplate,
                                DataSource dataSource,
                                RedisTemplate<String, ?> redisTemplate,
                                NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.redis = redis;
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public List<String> getPermissions(UserDetails principal, Long resourceId, int resourceType) {
        HashOperations<String, String, String> hashOps = redis.opsForHash();
        String key = ((User)principal).getId() + ":" + resourceId + ":" + resourceType;
        List<String> permissions = null;
        try{
            String permissionsStr = hashOps.get(PERMISSION_KEY, key);
            //TODO: if key is absent, get info from DB and then write to Redis?
            if (permissionsStr == null) {
                return null;
            }
            permissions = List.of(permissionsStr.replace("[", "")
                    .replace("]", "")
                    .replace(" ", "")
                    .split(","));
        } catch( RedisConnectionFailureException ex) {
            log.info("Couldn't connect to Redis server");
            permissions = getPermissionsFromDB(((User)principal).getId(), resourceId, resourceType);
        }
        return permissions;
    }

    @Override
    public int addPermission(UserDetails principal, Long resourceId, int resourceType, String permission) {
        CompletableFuture<Integer> redisFuture = CompletableFuture.supplyAsync(() -> addPermissionToRedis(principal, resourceId, resourceType, permission));
        CompletableFuture<Integer> dbFuture = CompletableFuture.supplyAsync(() -> addPermissionToDB(principal, resourceId, resourceType, permission));
        try {
            CompletableFuture.allOf(dbFuture).join();
            if (redisFuture.get() < 0) {
                // TODO: queue task to do later
            }
        } catch (CompletionException | ExecutionException | InterruptedException e) {
            throw new CompletableFutureException(e);
        }
        return 0;
    }

    @Override
    public int addPermission(Long user, Long resource, int resourceType, String permission) {
        return addPermission(User.builder().id(user).build(), resource, resourceType, permission);
    }

    @Override
    public int addPermission(List<Long> users, Long resource, int resourceType, String permission) {
        List<UserDetails> principals = users.stream()
                .map(userId -> User.builder().id(userId).build())
                .collect(Collectors.toList());
        return addPermissionsBatch(principals, resource, resourceType, Collections.singletonList(permission));
    }

    @Override
    public int addPermission(long[] userIds, Long chatId, int resourceType, String permission) {
        // Convert to UserDetails list for batch operations
        List<UserDetails> principals = Arrays.stream(userIds)
                .mapToObj(userId -> User.builder().id(userId).build())
                .collect(Collectors.toList());
        return addPermissionsBatch(principals, chatId, resourceType, Collections.singletonList(permission));
    }

    private List<String> getPermissionsFromDB(Long user, Long resourceId, int resourceType) {
        String permissionsStr = jdbcTemplate.queryForObject("""
            select
                "permissions"::text[]
            from
                "UsersPermissions"
            where
                "user" = ?
                and "resource" is not distinct from ?
                and "resource_type" is not distinct from ?
        """, String.class, user, resourceId, resourceType);
        if (permissionsStr == null) {
            return null;
        }
        return List.of(permissionsStr.replace("{", "")
                .replace("}", "")
                .replace("\"", "")
                .split(","));
    }

    private int addPermissionToDB(UserDetails principal, Long resourceId, int resourceType, String permission) {
        jdbcTemplate.update("""
        INSERT INTO
            "UsersPermissions" ("user", "resource", "resource_type", "permissions")
        VALUES
            (?, ?, ?, ARRAY[?])
        ON CONFLICT ("user", "resource", "resource_type") 
        DO UPDATE SET 
            "permissions" = CASE 
                WHEN ? = ANY(COALESCE("UsersPermissions"."permissions", ARRAY[]::TEXT[])) 
                THEN "UsersPermissions"."permissions"
                ELSE ARRAY_APPEND(COALESCE("UsersPermissions"."permissions", ARRAY[]::TEXT[]), ?)
            END
    """, ((User)principal).getId(), resourceId, resourceType, permission, permission, permission);
        return 0;
    }

    /**
     * Optimized Database batch operation using PostgreSQL UNNEST with array
     */
    @Transactional
    private int addPermissionsToDBBatch(List<UserDetails> principals, Long resourceId, int resourceType, List<String> permissions) {
        if (principals.isEmpty() || permissions.isEmpty()) {
            return 0;
        }
        // Prepare user IDs array
        List<Long> userIds = principals.stream()
                .map(principal -> ((User)principal).getId())
                .collect(Collectors.toList());

        // Use NamedParameterJdbcTemplate for cleaner array handling
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("userIds", userIds.toArray(new Long[0]));
        parameters.addValue("resourceId", resourceId);
        parameters.addValue("resourceType", resourceType);
        parameters.addValue("permissions", permissions.toArray(new String[0]));

        String query = """
                WITH new_permissions AS (
                    SELECT 
                        unnest(:userIds) as user_id,
                        :resourceId as resource_id,
                        :resourceType as resource_type,
                        :permissions as new_perms
                ),
                updated AS (
                    INSERT INTO "UsersPermissions" ("user", "resource", "resource_type", "permissions")
                    SELECT 
                        np.user_id,
                        np.resource_id,
                        np.resource_type,
                        np.new_perms
                    FROM new_permissions np
                    ON CONFLICT ("user", "resource", "resource_type") 
                    DO UPDATE SET 
                        "permissions" = (
                            SELECT ARRAY(
                                SELECT DISTINCT UNNEST(
                                    COALESCE("UsersPermissions"."permissions", ARRAY[]::TEXT[]) || 
                                    EXCLUDED."permissions"
                                )
                            )
                        )
                    RETURNING "user", "permissions"
                )
                SELECT COUNT(*) FROM updated
            """;

        Integer updatedCount = namedParameterJdbcTemplate.queryForObject(query, parameters, Integer.class);
        log.debug("Database batch update: updated {} user records", updatedCount);

        return 0;
    }

    /**
     *
     * @param principal
     * @param resourceId
     * @param resourceType
     * @param permission
     * @return negative value if operation failed, zero or positive number otherwise
     */
    private int addPermissionToRedis(UserDetails principal, Long resourceId, int resourceType, String permission) {
        // TODO: process situation, when data is sent to redis, but machine had stopped before data was put to persistent store
        HashOperations<String, String, String> hashOps = redis.opsForHash();
        String key = ((User)principal).getId() + ":" + resourceId + ":" + resourceType;
        List<String> permissions = this.getPermissions(principal, resourceId, resourceType);
        if (permissions == null) {
            permissions = new ArrayList<>();
        }
        permissions.add(permission);
        try {
            String permissionsStr = "[" + String.join(", ", permissions) + "]";
            hashOps.put(PERMISSION_KEY, key, permissionsStr);
        } catch (RedisConnectionFailureException ex) {
            log.info("Couldn't connect to Redis server: {}", ex.getMessage());
            // TODO: send message to queue to write this later
            return -1;
        }
        return 0;
    }

    /**
     * Optimized Redis batch operation using HashOperations multiGet and putAll
     */
    private int addPermissionsToRedisBatch(List<UserDetails> principals, Long resourceId, int resourceType, List<String> permissions) {
        if (principals.isEmpty() || permissions.isEmpty()) {
            return 0;
        }
        HashOperations<String, String, Set<String>> hashOps = redis.opsForHash();
        // Prepare keys
        List<String> keys = principals.stream()
                .map(principal -> ((User)principal).getId() + ":" + resourceId + ":" + resourceType)
                .collect(Collectors.toList());
        try {
            // Batch get existing permissions
            List<Set<String>> existingPermissionsList = hashOps.multiGet(PERMISSION_KEY, keys);
            // Prepare updates
            Map<String, Set<String>> updates = new HashMap<>();
            int updatedCount = 0;

            for (int i = 0; i < principals.size(); i++) {
                String key = keys.get(i);
                Set<String> allPermissions = existingPermissionsList.get(i);

                if (allPermissions == null) {
                    allPermissions = new HashSet<>();
                }

                // Check if any new permission is actually added
                int beforeSize = allPermissions.size();
                allPermissions.addAll(permissions);
                int afterSize = allPermissions.size();

                if (afterSize > beforeSize) {
                    updates.put(key, allPermissions);
                    updatedCount += (afterSize - beforeSize);
                }
            }

            // Batch update only if there are changes
            if (!updates.isEmpty()) {
                hashOps.putAll(PERMISSION_KEY, updates);
                log.debug("Redis batch update: updated {} keys with {} new permissions", updates.size(), updatedCount);
            }

            return updatedCount;

        } catch (RedisConnectionFailureException ex) {
            log.error("Redis connection failed for batch permission update", ex);
            return -1;
        } catch (Exception ex) {
            log.error("Unexpected error while updating permissions in Redis", ex);
            return -1;
        }
    }

    @Transactional
    public int addPermissionsBatch(List<UserDetails> principals, Long resourceId, int resourceType, List<String> permissions) {
        if (principals == null || principals.isEmpty() || permissions == null || permissions.isEmpty()) {
            log.debug("Empty principals or permissions list");
            return 0;
        }

        long startTime = System.currentTimeMillis();

        // Remove duplicates from permissions
        List<String> distinctPermissions = permissions.stream().distinct().collect(Collectors.toList());

        // Execute both operations in parallel
        CompletableFuture<Integer> dbFuture = CompletableFuture.supplyAsync(() ->
                addPermissionsToDBBatch(principals, resourceId, resourceType, distinctPermissions));
        CompletableFuture<Integer> redisFuture = CompletableFuture.supplyAsync(() ->
                addPermissionsToRedisBatch(principals, resourceId, resourceType, distinctPermissions));

        try {
            CompletableFuture.allOf(dbFuture, redisFuture).join();
            int dbResult = dbFuture.get();
            int redisResult = redisFuture.get();

            long endTime = System.currentTimeMillis();
            log.info("Batch add completed for {} users and {} permissions in {} ms. DB: {}, Redis: {}",
                    principals.size(), distinctPermissions.size(), (endTime - startTime), dbResult, redisResult);

            return (dbResult < 0 || redisResult < 0) ? -1 : 0;
        } catch (Exception e) {
            log.error("Batch add failed", e);
            throw new CompletableFutureException(e);
        }
    }

    @Scheduled(fixedDelay = 180000)
    public void updateRedisPermissions() {
        long startTime = System.currentTimeMillis();
        JdbcTemplate template = new JdbcTemplate(this.dataSource);
        template.setFetchSize(1000);

        // Process in chunks to avoid memory issues
        int offset = 0;
        int totalProcessed = 0;

        while (true) {
            List<Permission> permissions = template.query("""
                select
                    "id",
                    "user",
                    "resource",
                    "resource_type",
                    "permissions"
                from
                    "UsersPermissions"
                order by "id"
                limit ? offset ?
            """, new Object[]{BATCH_SIZE, offset}, (RowMapper<Permission>) (rs, rowNum) -> {
                Array permissionsArray = rs.getArray("permissions");
                return new Permission(
                        rs.getLong("id"),
                        rs.getLong("user"),
                        rs.getLong("resource"),
                        rs.getShort("resource_type"),
                        (String[]) permissionsArray.getArray()
                );
            });

            if (permissions.isEmpty()) {
                break;
            }

            var hashOps = this.redis.opsForHash();
            Map<String, String> data = permissions.stream().collect(Collectors.toMap(
                    (p) -> p.user() + ":" + p.resource() + ":" + p.resourceType(),
                    (p) -> "[" + String.join(", ", p.permissions()) + "]"
            ));

            // Batch update Redis in chunks
            hashOps.putAll(PERMISSION_KEY, data);
            totalProcessed += permissions.size();
            offset += BATCH_SIZE;

            log.debug("Processed {} permissions in current chunk", permissions.size());
        }

        long endTime = System.currentTimeMillis();
        log.info("Redis cache updated with {} permission records in {} ms", totalProcessed, (endTime - startTime));
    }
}
