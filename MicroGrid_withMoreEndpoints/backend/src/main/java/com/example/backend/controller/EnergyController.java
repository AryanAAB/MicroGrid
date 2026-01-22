package com.example.backend.controller;

import com.example.backend.dto.EnergyStatusDto;
import com.example.backend.dto.GridPriceDto;
import com.example.backend.dto.UpdatePriceRequestDto;
import com.example.backend.service.FirebaseAuthService;
import com.example.backend.service.FirestoreService;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/energy")
@CrossOrigin(origins = "*")
public class EnergyController {

    private final FirebaseAuthService authService;
    private final FirestoreService firestoreService;

    public EnergyController(FirebaseAuthService authService,
                            FirestoreService firestoreService) {
        this.authService = authService;
        this.firestoreService = firestoreService;
    }

    @GetMapping("/status")
    public EnergyStatusDto getStatus(
            @RequestHeader("Authorization") String authHeader
    ) throws Exception {

        FirebaseToken token = authService.verifyToken(authHeader.replace("Bearer ", ""));
        Map<String, Object> user = firestoreService.getUser(token.getUid());

        return new EnergyStatusDto(
                (String) user.get("status"),
                getDouble(user, "sellingPrice")
        );
    }

    @PostMapping("/price")
    public void updateSellingPrice(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdatePriceRequestDto request
    ) throws Exception {

        FirebaseToken token = authService.verifyToken(authHeader.replace("Bearer ", ""));
        firestoreService.updateUserField(token.getUid(), "sellingPrice", request.sellingPrice());
    }

    @GetMapping("/grid-price")
    public GridPriceDto getGridPrice() {
        // Hardcoded for now (can be moved to Firestore later)
        return new GridPriceDto(5.2);//////////////////////////////////////////////////////////////////////
    }

    private double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? 0.0 : ((Number) value).doubleValue();
    }
}
