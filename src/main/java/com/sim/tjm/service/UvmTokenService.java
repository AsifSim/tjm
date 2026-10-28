package com.sim.tjm.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UvmTokenService {
    private final Map<String, String> activeTokens = new ConcurrentHashMap<>();

    public String generateToken(String username) {
        String token = UUID.randomUUID().toString();
        activeTokens.put(token, username);
        return token;
    }

    public boolean isTokenValid(String token) {
        return token != null && activeTokens.containsKey(token);
    }
}