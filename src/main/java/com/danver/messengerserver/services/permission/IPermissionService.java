package com.danver.messengerserver.services.permission;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface IPermissionService<U extends UserDetails, R> {

    boolean isAuthorized(U principal, R resource, int resourceType, String permission);

    /**
     * Adds certain permissions to the list of permissions
     *
     * @param principal
     * @param resource
     * @param permission
     * @param resourceType
     * @return
     */
    default int grantAuthority(U principal, R resource, int resourceType, String permission) {
        return 0;
    }

    default int grantAuthority(Long user, R resource, int resourceType, String permission) {
        return 0;
    }

    default int grantAuthority(List<Long> users, Long resource, int resourceType, List<String> permission) {
        return 0;
    }

    default int revokeAuthority(U principal, R resource, int resourceType, String permission) {
        return 0;
    }

    /**
     * Revokes ALL permissions for certain principal relating to resource
     *
     * @param principal
     * @param resource
     * @param resourceType
     * @return
     */
    default int revokeAuthority(U principal, R resource, int resourceType) {
        return 0;
    }

    int grantAuthority(List<Long> userIds, Long resource, int resourceType, String permission);

    int grantAuthority(long[] userIds, long chatId, int resourceType, String permission);
}
