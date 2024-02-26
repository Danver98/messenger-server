package com.danver.messengerserver.services.permission;

import com.danver.messengerserver.models.User;
import org.springframework.security.core.Authentication;
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

    /**
     * Replaces existing list of permissions with a new one
     *
     * @param principal
     * @param resource
     * @param resourceType
     * @param permissions
     * @return
     */
    default int grantAuthority(U principal, R resource, int resourceType, List<?> permissions) {
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

}
