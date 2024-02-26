package com.danver.messengerserver.services.permission;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface IPermissionRepository<U extends UserDetails, R> {

    /**
     *
     * @param principal
     * @param resource
     * @param resourceType
     * @return permissions for user for given resource object
     */
    List<String> getPermissions(U principal, R resource, int resourceType);

    /**
     *
     * @param principal
     * @param resource
     * @param resourceType
     * @param permission
     * @return negative value if operation failed, zero or positive number otherwise
     */
    int addPermission(U principal, R resource, int resourceType, String permission);
}
