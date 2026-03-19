package com.example.airline.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Map;

@Service
public class SupabaseService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.anon-key}")
    private String anonKey;

    @Value("${supabase.service-key}")
    private String serviceKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final RestTemplate patchRestTemplate = createPatchRestTemplate();

    private RestTemplate createPatchRestTemplate() {
        org.springframework.http.client.HttpComponentsClientHttpRequestFactory factory =
                new org.springframework.http.client.HttpComponentsClientHttpRequestFactory();
        return new RestTemplate(factory);
    }

    private HttpHeaders anonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", anonKey);
        return headers;
    }

    private HttpHeaders serviceHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", serviceKey);
        headers.set("Authorization", "Bearer " + serviceKey);
        return headers;
    }

    private java.util.HashMap parseJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, java.util.HashMap.class);
        } catch (Exception e) {
            java.util.HashMap<String, Object> err = new java.util.HashMap<>();
            err.put("error", "Failed to parse response");
            return err;
        }
    }

    public Map login(String email, String password) {
        String url = supabaseUrl + "/auth/v1/token?grant_type=password";
        Map<String, String> body = Map.of("email", email, "password", password);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, anonHeaders()), String.class);
            String responseBody = response.getBody();
            System.out.println("Login response: " + responseBody);
            return parseJson(responseBody);
        } catch (HttpClientErrorException e) {
            System.out.println("Login error: " + e.getResponseBodyAsString());
            java.util.HashMap<String, Object> err = new java.util.HashMap<>();
            err.put("error", "Invalid email or password");
            return err;
        } catch (Exception e) {
            java.util.HashMap<String, Object> err = new java.util.HashMap<>();
            err.put("error", e.getMessage());
            return err;
        }
    }

    public Map register(String email, String password) {
        String url = supabaseUrl + "/auth/v1/signup";
        Map<String, String> body = Map.of("email", email, "password", password);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, anonHeaders()), String.class);
            String responseBody = response.getBody();
            System.out.println("Register response: " + responseBody);
            return parseJson(responseBody);
        } catch (HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            System.out.println("Register error: " + errorBody);
            return parseJson(errorBody);
        } catch (Exception e) {
            java.util.HashMap<String, Object> err = new java.util.HashMap<>();
            err.put("error", e.getMessage());
            return err;
        }
    }

    public void createProfile(String userId, String fullName, String username,
                              String phone, String address) {
        String url = supabaseUrl + "/rest/v1/profiles";
        Map<String, Object> body = Map.of(
                "id",        userId,
                "full_name", fullName  != null ? fullName  : "",
                "username",  username  != null ? username  : "",
                "phone",     phone     != null ? phone     : "",
                "address",   address   != null ? address   : ""
        );
        HttpHeaders headers = serviceHeaders();
        headers.set("Prefer", "return=representation");
        try {
            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
        } catch (HttpClientErrorException e) {
            System.out.println("Profile error: " + e.getResponseBodyAsString());
        }
    }

    public Object getProfile(String userId) {
        String url = supabaseUrl + "/rest/v1/profiles?id=eq." + userId + "&select=*";
        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(serviceHeaders()), Object.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            java.util.HashMap<String, Object> err = new java.util.HashMap<>();
            err.put("error", e.getResponseBodyAsString());
            return err;
        }
    }

    public Object updateProfile(String userId, String fullName, String username,
                                String phone, String address) {
        String url = supabaseUrl + "/rest/v1/profiles?id=eq." + userId;

        java.util.HashMap<String, Object> body = new java.util.HashMap<>();
        body.put("full_name", fullName != null ? fullName : "");
        body.put("username", username != null ? username : "");
        body.put("phone", phone != null ? phone : "");
        body.put("address", address != null ? address : "");
        body.put("updated_at", java.time.Instant.now().toString());

        HttpHeaders headers = serviceHeaders();
        headers.set("Prefer", "return=representation");
        try {
            ResponseEntity<String> response = patchRestTemplate.exchange(
                    url, HttpMethod.PATCH,
                    new HttpEntity<>(body, headers), String.class);
            return parseJson(response.getBody() != null ? response.getBody() : "{}");
        } catch (HttpClientErrorException e) {
            System.out.println("Update profile error: " + e.getResponseBodyAsString());
            java.util.HashMap<String, Object> err = new java.util.HashMap<>();
            err.put("error", e.getResponseBodyAsString());
            return err;
        } catch (Exception e) {
            System.out.println("Update profile exception: " + e.getMessage());
            java.util.HashMap<String, Object> err = new java.util.HashMap<>();
            err.put("error", e.getMessage());
            return err;
        }
    }

    public Object updatePassword(String accessToken, String newPassword) {
        String url = supabaseUrl + "/auth/v1/user";
        Map<String, String> body = Map.of("password", newPassword);
        HttpHeaders headers = anonHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.PUT,
                    new HttpEntity<>(body, headers), Object.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            java.util.HashMap<String, Object> err = new java.util.HashMap<>();
            err.put("error", e.getResponseBodyAsString());
            return err;
        }
    }

    public Object uploadPhoto(String userId, byte[] imageBytes, String contentType) {
        String url = supabaseUrl + "/rest/v1/profiles?id=eq." + userId;
        String base64 = Base64.getEncoder().encodeToString(imageBytes);

        java.util.HashMap<String, Object> body = new java.util.HashMap<>();
        body.put("photo", base64);
        body.put("photo_type", contentType);
        body.put("updated_at", java.time.Instant.now().toString());

        HttpHeaders headers = serviceHeaders();
        headers.set("Prefer", "return=representation");
        try {
            ResponseEntity<String> response = patchRestTemplate.exchange(
                    url, HttpMethod.PATCH,
                    new HttpEntity<>(body, headers), String.class);
            return parseJson(response.getBody() != null ? response.getBody() : "{}");
        } catch (HttpClientErrorException e) {
            System.out.println("Upload photo error: " + e.getResponseBodyAsString());
            java.util.HashMap<String, Object> err = new java.util.HashMap<>();
            err.put("error", e.getResponseBodyAsString());
            return err;
        } catch (Exception e) {
            System.out.println("Upload photo exception: " + e.getMessage());
            java.util.HashMap<String, Object> err = new java.util.HashMap<>();
            err.put("error", e.getMessage());
            return err;
        }
    }
}