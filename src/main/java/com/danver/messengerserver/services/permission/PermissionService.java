package com.danver.messengerserver.services.permission;

import com.danver.messengerserver.models.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PermissionService implements IPermissionService<UserDetails, Long> {

    private final IPermissionRepository<UserDetails, Long> permissionRepository;

    @Autowired
    public PermissionService(RedisTemplate<String, ?> redis, IPermissionRepository<UserDetails, Long> permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public boolean isAuthorized(UserDetails principal, Long resourceId, int resourceType, String permission) {
        List<String> permissions = permissionRepository.getPermissions(principal, resourceId, resourceType);
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        return permissions.contains(permission);
    }

    @Override
    public int grantAuthority(UserDetails principal, Long resourceId, int resourceType, String permission) {
        return permissionRepository.addPermission(principal, resourceId, resourceType, permission);
    }

    @Override
    public int grantAuthority(Long user, Long resource, int resourceType, String permission) {
        return permissionRepository.addPermission(user, resource, resourceType, permission);
    }
}
