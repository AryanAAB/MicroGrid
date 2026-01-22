package com.example.backend.service;

import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FirestoreService {

    private final Firestore firestore;

    public FirestoreService(Firestore firestore) {
        this.firestore = firestore;
    }

    public Map<String, Object> getUser(String uid) throws Exception {
        return firestore.collection("users")
                .document(uid)
                .get()
                .get()
                .getData();
    }

    public void updateUserField(String uid, String field, Object value) throws Exception {
        firestore.collection("users")
                .document(uid)
                .update(field, value)
                .get();
    }
}
