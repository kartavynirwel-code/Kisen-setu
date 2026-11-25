package com.kisansetu.auth;

public record LoginResponse(String token, String tokenType, long expiresInSeconds, String username, String role) {
}
