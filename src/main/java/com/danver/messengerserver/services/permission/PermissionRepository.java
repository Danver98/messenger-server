package com.danver.messengerserver.services.permission;

import com.danver.messengerserver.exceptions.CompletableFutureException;
import com.danver.messengerserver.models.User;
import com.danver.messengerserver.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@Slf4j
@Repository
public class PermissionRepository implements IPermissionRepository<User, Long> {
    private final RedisTemplate<String, ?> redis;
    private final JdbcTemplate jdbcTemplate;
    private final static String PERMISSION_KEY = Constants.REDIS_USERS_PERMISSIONS;

    public PermissionRepository(RedisTemplate<String, ?> redis, JdbcTemplate jdbcTemplate) {
        this.redis = redis;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<String> getPermissions(User principal, Long resourceId, int resourceType) {
        HashOperations<String, String, String> hashOps = redis.opsForHash();
        String key = principal.getId() + ":" + resourceId + ":" + resourceType;
        List<String> permissions = null;
        try{
            String permissionsStr = hashOps.get(PERMISSION_KEY, key);
            //TODO: if key is absent, get info from DB and then write to Redis?
            if (permissionsStr == null) {
                return null;
            }
            permissions = List.of(permissionsStr.replace("[", "")
                    .replace("]", "")
                    .split(","));
        } catch( RedisConnectionFailureException ex) {
            log.info("Couldn't connect to Redis server");
            permissions = getPermissionsFromDB(principal, resourceId, resourceType);
        }
        return permissions;
    }

    @Override
    public int addPermission(User principal, Long resourceId, int resourceType, String permission) {
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

    private List<String> getPermissionsFromDB(User principal, Long resourceId, int resourceType) {
        return jdbcTemplate.queryForList("""
            select
                "permissions"
            from
                "user_permissions"
            where
                "userId" = ?
                and "resourceId" = ?
                and "resourceType" = ?
        """, String.class, principal.getId(), resourceId, resourceType);
    }

    private int addPermissionToDB(User principal, Long resourceId, int resourceType, String permission) {
        jdbcTemplate.update("""
            insert into "UsersPermissions" ("user", "resource", "resource_type", "permissions")
                values (?, ?, ?, ?::text[])
            on conflict do update
                set "permissions" = "permissions" || ?
        """, principal, resourceId, resourceType, permission, permission);
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
    private int addPermissionToRedis(User principal, Long resourceId, int resourceType, String permission) {
        HashOperations<String, String, String> hashOps = redis.opsForHash();
        String key = principal.getId() + ":" + resourceId + ":" + resourceType;
        List<String> permissions = this.getPermissions(principal, resourceId, resourceType);
        if (permissions == null) {
            permissions = new ArrayList<>();
        }
        permissions.add(permission);
        try {
            String permissionsStr = "[ " + String.join(", ", permissions) + "]";
            hashOps.put(PERMISSION_KEY, key, permissionsStr);
        } catch (RedisConnectionFailureException ex) {
            log.info("Couldn't connect to Redis server: " + ex.getMessage());
            // TODO: send message to queue to write this later
            return -1;
        }
        return 0;
    }

}
