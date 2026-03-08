package com.danver.messengerserver.models.permission;

public record Permission(long id, long user, long resource, short resourceType, String[] permissions) {
}
