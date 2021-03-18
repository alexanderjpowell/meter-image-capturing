package com.slotmachine.ocr.mic

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class ScannedDataRepository {

    private var database = FirebaseFirestore.getInstance()

    fun getUserNames(uid: String) : CollectionReference {
        return database.collection("users").document(uid).collection("displayNames")
    }

    fun getPinCodeForUser(uid: String, name: String) : DocumentReference {
        return database.collection("users").document(uid).collection("displayNames").document(name)
    }

    fun deleteUserName(uid: String, name: String) {
        database.collection("users")
                .document(uid)
                .collection("displayNames")
                .document(name)
                .delete()
    }
}