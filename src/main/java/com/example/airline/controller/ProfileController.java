package com.example.airline.controller;

import com.example.airline.model.PasswordRequest;
import com.example.airline.model.ProfileRequest;
import com.example.airline.service.SupabaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final SupabaseService supabaseService;

    public ProfileController(SupabaseService supabaseService) {
        this.supabaseService = supabaseService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getProfile(@PathVariable String userId) {
        Object profile = supabaseService.getProfile(userId);
        return ResponseEntity.ok(Map.of("status", "success", "data", profile));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> editProfile(@PathVariable String userId,
                                         @RequestBody ProfileRequest request) {
        try {
            Object result = supabaseService.updateProfile(userId,
                    request.getFullName(), request.getUsername(),
                    request.getPhone(), request.getAddress());

            java.util.HashMap<String, Object> response = new java.util.HashMap<>();
            response.put("status", "success");
            response.put("message", "Profile updated successfully");
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            java.util.HashMap<String, Object> response = new java.util.HashMap<>();
            response.put("status", "failed");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PutMapping("/password/{userId}")
    public ResponseEntity<?> editPassword(@PathVariable String userId,
                                          @RequestBody PasswordRequest request,
                                          @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "").trim();
        supabaseService.updatePassword(token, request.getNewPassword());
        return ResponseEntity.ok(Map.of(
                "status",  "success",
                "message", "Password updated successfully"
        ));
    }

    @PostMapping("/photo/{userId}")
    public ResponseEntity<?> uploadPhoto(@PathVariable String userId,
                                         @RequestParam("file") MultipartFile file) {
        try {
            String contentType = file.getContentType();
            if (contentType == null ||
                    (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status",  "failed",
                        "message", "Only .jpg and .png files are allowed"
                ));
            }
            byte[] bytes = file.getBytes();
            supabaseService.uploadPhoto(userId, bytes, contentType);
            return ResponseEntity.ok(Map.of(
                    "status",    "success",
                    "message",   "Photo uploaded successfully",
                    "file_size", bytes.length + " bytes",
                    "file_type", contentType
            ));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status",  "failed",
                    "message", "Failed to process image"
            ));
        }
    }


}
