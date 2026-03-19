package com.example.airline.controller;

import com.example.airline.model.LoginRequest;
import com.example.airline.model.RegisterRequest;
import com.example.airline.service.SupabaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SupabaseService supabaseService;

    public AuthController(SupabaseService supabaseService) {
        this.supabaseService = supabaseService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Map result = supabaseService.login(request.getEmail(), request.getPassword());

        if (result.containsKey("error")) {
            return ResponseEntity.status(401).body(Map.of(
                    "status",  "failed",
                    "message", result.get("error")
            ));
        }

        Map user = (Map) result.getOrDefault("user", Map.of());
        return ResponseEntity.ok(Map.of(
                "status",       "success",
                "message",      "Login successful",
                "access_token", result.getOrDefault("access_token", ""),
                "user_id",      user.getOrDefault("id", "")
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            Map authResult = supabaseService.register(request.getEmail(), request.getPassword());

            // Check for Supabase error codes
            if (authResult.containsKey("error_code")) {
                String msg = String.valueOf(authResult.getOrDefault("msg", "Registration failed"));
                return ResponseEntity.badRequest().body(Map.of(
                        "status",  "failed",
                        "message", msg
                ));
            }

            if (authResult.containsKey("error")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status",  "failed",
                        "message", String.valueOf(authResult.get("error"))
                ));
            }

            // Get user ID
            String userId = null;
            Object userObj = authResult.get("user");
            if (userObj instanceof Map) {
                Object id = ((Map) userObj).get("id");
                if (id != null) userId = String.valueOf(id);
            }
            if (userId == null) {
                Object id = authResult.get("id");
                if (id != null) userId = String.valueOf(id);
            }

            // Create profile
            if (userId != null && !userId.isEmpty()) {
                supabaseService.createProfile(
                        userId,
                        request.getFullName(),
                        request.getUsername() != null ? request.getUsername() : "",
                        request.getPhone() != null ? request.getPhone() : "",
                        request.getAddress() != null ? request.getAddress() : ""
                );
            }

            // Build response without nulls
            java.util.Map<String, Object> responseMap = new java.util.HashMap<>();
            responseMap.put("status", "success");
            responseMap.put("message", "Registration successful!");
            responseMap.put("user_id", userId != null ? userId : "");
            return ResponseEntity.ok(responseMap);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                    new java.util.HashMap<String, Object>() {{
                        put("status", "failed");
                        put("message", "Server error: " + e.getMessage());
                    }}
            );
        }
    }
}