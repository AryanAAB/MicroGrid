package com.example.backend.controller;

import com.example.backend.dto.DashboardStatsDto;
import com.example.backend.dto.UserProfileDto;
import com.example.backend.service.FirebaseAuthService;
import com.example.backend.service.FirestoreService;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    private final FirebaseAuthService authService;
    private final FirestoreService firestoreService;

    public UserController(FirebaseAuthService authService,
                          FirestoreService firestoreService) {
        this.authService = authService;
        this.firestoreService = firestoreService;
    }

    @GetMapping("/profile")
    public UserProfileDto getProfile(
            @RequestHeader("Authorization") String authHeader
    ) throws Exception {

        FirebaseToken token = authService.verifyToken(authHeader.replace("Bearer ", ""));
        Map<String, Object> user = firestoreService.getUser(token.getUid());

        return new UserProfileDto(
                (String) user.get("houseName"),
                (String) user.get("name"),
                (String) user.get("phone"),
                getDouble(user, "latitude"),
                getDouble(user, "longitude")
        );
    }

    @GetMapping("/dashboard/stats")
    public DashboardStatsDto getDashboardStats(
            @RequestHeader("Authorization") String authHeader
    ) throws Exception {

        FirebaseToken token = authService.verifyToken(authHeader.replace("Bearer ", ""));
        Map<String, Object> user = firestoreService.getUser(token.getUid());

        return new DashboardStatsDto(
                getDouble(user, "energySold"),
                getDouble(user, "energyConsumed"),
                getDouble(user, "totalEarnings"),
                getDouble(user, "gridSavings")
        );

    }
        private double getDouble(Map<String, Object> map, String key) {
                Object value = map.get(key);
                return value == null ? 0.0 : ((Number) value).doubleValue();
        }
}



