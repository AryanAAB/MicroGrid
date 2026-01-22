package com.example.backend.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

@Service
public class FirestoreService {

    private final Firestore db = FirestoreClient.getFirestore();

    public String getHouseName(String uid) throws Exception {

        DocumentSnapshot doc = db
            .collection("users")
            .document(uid)
            .get()
            .get();

        if (doc.exists()) {
            return doc.getString("houseName");
        }

        return "Unknown House";
    }
}
