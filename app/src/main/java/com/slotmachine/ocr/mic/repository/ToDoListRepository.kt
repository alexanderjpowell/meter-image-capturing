package com.slotmachine.ocr.mic.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ToDoListRepository {

    private var database = FirebaseFirestore.getInstance()

    fun getAllToDoListItems(uid: String) : CollectionReference {
        return database.collection("newUpload").document(uid).collection("machines")
    }

    fun getUnscannedItems(uid: String) : Query {
        return database.collection("newUpload")
            .document(uid)
            .collection("machines")
            .whereNotEqualTo("scanned", true)
    }

    fun getScannedItems(uid: String) : Query {
        return database.collection("newUpload")
            .document(uid)
            .collection("machines")
            .whereEqualTo("scanned", true)
    }
}