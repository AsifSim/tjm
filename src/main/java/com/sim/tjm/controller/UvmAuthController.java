package com.sim.tjm.controller;

import com.sim.tjm.service.UvmTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/uvm/auth")
@CrossOrigin(origins = "*")
public class UvmAuthController {
    private static final Logger logger = LoggerFactory.getLogger(UvmAuthController.class);

    @Autowired
    private UvmTokenService tokenService;

    // In-memory User Registry (POC) - Preloaded with admin account
    private final Map<String, String> userRegistry = new ConcurrentHashMap<>(Map.of("admin", "password"));

    @Value("classpath:uvm-banner.txt")
    private Resource bannerResource;
    private String cachedBanner = "\n=== UVM HEADLESS PROTOCOL INITIALIZED ===\n";

    @PostConstruct
    public void initBanner() {
        try {
            if (bannerResource.exists()) {
                cachedBanner = "\n" + StreamUtils.copyToString(bannerResource.getInputStream(), StandardCharsets.UTF_8) + "\n";
            }
        } catch (Exception ignored) {}
    }

    // --- NEW: Sign Up Endpoint ---
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
        }

        if (userRegistry.containsKey(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Username already exists"));
        }

        userRegistry.put(username, password);
        logger.info("[AUTH_SIGNUP] New user registered: {}", username);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "User registered successfully. You can now sign in."));
    }

    // --- UPDATED: Sign In Endpoint ---
    @PostMapping("/signin")
    public ResponseEntity<?> signIn(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        // Validate Credentials
        if (username == null || !password.equals(userRegistry.get(username))) {
            logger.warn("[AUTH_FAILED] Invalid login attempt for user: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid username or password"));
        }

        // Generate Token and create HTTP-Only Cookie for console tracing
        String token = tokenService.generateToken(username);
        String cookieHeader = String.format("uvm_token=%s; Path=/; HttpOnly; Max-Age=3600", token);

        logger.info("[AUTH_SUCCESS] User signed in: {}", username);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieHeader)
                .body(Map.of(
                        "_banner", cachedBanner,
                        "status", "CONNECTED",
                        "token", token
                ));
    }

    // Add this to UvmAuthController.java

    @GetMapping(value = "/cli-connect", produces = org.springframework.http.MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> cliConnect(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        // 1. Check for Basic Auth
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"UVM Protocol\"")
                    .body("Unauthorized. Please provide credentials.\n");
        }

        // 2. Decode Basic Auth (username:password)
        String base64Credentials = authHeader.substring("Basic ".length()).trim();
        byte[] credDecoded = java.util.Base64.getDecoder().decode(base64Credentials);
        String credentials = new String(credDecoded, StandardCharsets.UTF_8);
        String[] values = credentials.split(":", 2);

        if (values.length != 2) {
            return ResponseEntity.badRequest().body("Invalid basic auth format.\n");
        }

        String username = values[0];
        String password = values[1];

        // 3. Validate against in-memory registry
        if (!password.equals(userRegistry.get(username))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials.\n");
        }

        // 4. Generate Token & Cookie
        String token = tokenService.generateToken(username);
        String cookieHeader = String.format("uvm_token=%s; Path=/; HttpOnly; Max-Age=3600", token);

        logger.info("[AUTH_SUCCESS] CLI User connected: {}", username);

        // 5. Return plain text banner
        String successMessage = cachedBanner + "\n> Successfully connected as '" + username + "'.\n> Session cookie established. System Ready.\n";

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieHeader)
                .body(successMessage);
    }
}