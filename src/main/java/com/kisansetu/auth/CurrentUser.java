package com.kisansetu.auth;

import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        Number uid = jwt.getClaim("uid");
        return uid == null ? null : uid.longValue();
    }
}
