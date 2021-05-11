package com.slotmachine.ocr.mic

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

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

    fun getLatestScansById(uid: String, machineId: String) : Query {
        return database.collection("users")
                .document(uid)
                .collection("scans")
                .whereEqualTo("machine_id", machineId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
    }

    fun getLastScanByIdInTimeRange(uid: String, machineId: String, rejectDurationMillis: Int) : Query {
        val time = Date(System.currentTimeMillis() - rejectDurationMillis)
        return database.collection("users")
                .document(uid)
                .collection("scans")
                .whereEqualTo("machine_id", machineId)
                .whereGreaterThan("timestamp", time)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
    }
}