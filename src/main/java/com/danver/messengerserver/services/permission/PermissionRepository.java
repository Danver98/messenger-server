package com.danver.messengerserver.services.permission;

import com.danver.messengerserver.exceptions.CompletableFutureException;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.models.permission.Permission;
import com.danver.messengerserver.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class PermissionRepository implements IPermissionRepository<UserDetails, Long> {
    private final RedisTemplate<String, ?> redis;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final static String PERMISSION_KEY = Constants.REDIS_USERS_PERMISSIONS;

    public PermissionRepository(RedisTemplate<String, ?> redis, JdbcTemplate jdbcTemplate,
                                DataSource dataSource) {
        this.redis = redis;
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public List<String> getPermissions(UserDetails principal, Long resourceId, int resourceType) {
        HashOperations<String, String, String> hashOps = redis.opsForHash();
        String key = principal.getUsername() + ":" + resourceId + ":" + resourceType;
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
            permissions = getPermissionsFromDB(Long.parseLong(principal.getUsername()), resourceId, resourceType);
        }
        return permissions;
    }

    @Override
    public int addPermission(UserDetails principal, Long resourceId, int resourceType, String permission) {
        CompletableFuture<Integer> redisFuture = CompletableFuture.supplyAsync(() -> addPermissionToRedis(principal, resourceId, resourceType, permission));
        CompletableFuture<Integer> dbFuture = CompletableFuture.supplyAsync(() -> addPermissionToDB(principal, resourceId, resourceType, permission));
        try {
            CompletableFuture.allOf(redisFuture, dbFuture).join();
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
            insert into "UsersPermissions" ("user", "resource", "resource_type", "permissions")
                values (?, ?, ?, ?::text[])
            on conflict do update
                set "permissions" = "permissions" || ?
        """, principal.getUsername(), resourceId, resourceType, permission, permission);
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
        String key = principal.getUsername() + ":" + resourceId + ":" + resourceType;
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

    @Scheduled(fixedDelay = 180000)
    public void updateRedisPermissions() {
        JdbcTemplate template = new JdbcTemplate(this.dataSource);
        template.setFetchSize(1000);
            List<Permission> permissions = template.query("""
            select
                "id",
                "user",
                "resource",
                "resource_type",
                "permissions"
             from
                "UsersPermissions"
        """, (RowMapper) (rs, rowNum) -> {
            Array permissions1 = rs.getArray("permissions");
            return new Permission(
                    rs.getLong("id"),
                    rs.getLong("user"),
                    rs.getLong("resource"),
                    rs.getShort("resource_type"),
                    (String[]) permissions1.getArray()
            );
        });
        var hashOps = this.redis.opsForHash();
        Map<String, String> data = permissions.stream().collect(Collectors.toMap(
                (p) -> String.valueOf(p.user()) + ':' + p.resource() + ':' + String.valueOf(p.resourceType()),
                (p) -> "[" + String.join(", ", p.permissions()) + "]"
        ));
        hashOps.putAll(PERMISSION_KEY, data);
    }
}
