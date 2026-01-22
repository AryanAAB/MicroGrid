package com.example.backend.controller;

import com.example.backend.service.FirebaseAuthService;
import com.example.backend.service.FirestoreService;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // tighten later
public class UserController {

    private final FirebaseAuthService authService;
    private final FirestoreService firestoreService;

    public UserController(
            FirebaseAuthService authService,
            FirestoreService firestoreService) {
        this.authService = authService;
        this.firestoreService = firestoreService;
    }

    @GetMapping("/dashboard")
    public Map<String, String> dashboard(
            @RequestHeader("Authorization") String authHeader
    ) throws Exception {

        String token = authHeader.replace("Bearer ", "");
        FirebaseToken decoded = authService.verifyToken(token);

        String houseName =
                firestoreService.getHouseName(decoded.getUid());

        return Map.of(
            "email", decoded.getEmail(),
            "message", "Your house name is: " + houseName
        );
    }
}

