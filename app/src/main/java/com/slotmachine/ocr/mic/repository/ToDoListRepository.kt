package com.slotmachine.ocr.mic.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*

class ToDoListRepository {

    private var database = FirebaseFirestore.getInstance()

    fun getLatestUploadCollection(uid: String) : Query {
        return database.collection("toDoFileData")
            .document(uid)
            .collection("files")
            .orderBy("uploadDate", Query.Direction.DESCENDING)
            .limit(1)
    }

    fun getAllUnscannedToDoItems(uid: String, docId: String) : Query {
        return database.collection("toDoFileData")
            .document(uid)
            .collection("files")
            .document(docId)
            .collection("machines")
            .whereEqualTo("isScanned", false)
            .limit(10)
    }

    fun getScannedItem(uid: String, uploadId: String, docId: String) : DocumentReference {
        return database.collection("toDoFileData")
            .document(uid)
            .collection("files")
            .document(uploadId)
            .collection("machines")
            .document(docId)
    }

    fun getLatestDoc(uid: String, uploadId: String) : DocumentReference {
        return database.collection("toDoFileData")
            .document(uid)
            .collection("files")
            .document(uploadId)
    }

    fun getDocsByMachineId(uid: String, docId: String, machineId: String) : Query {
        return database.collection("toDoFileData")
            .document(uid)
            .collection("files")
            .document(docId)
            .collection("machines")
            .whereGreaterThanOrEqualTo("machineId", machineId)
            .whereLessThanOrEqualTo("machineId", machineId + "\uF7FF")
            .whereEqualTo("isScanned", false)
            .orderBy("machineId")
            .limit(10)
    }

    fun batchUpdateScan(parentDoc: DocumentReference, childDoc: DocumentReference) {
        database.runBatch { batch ->
            batch.update(parentDoc, "initializedScanning", false)
            batch.update(childDoc, "isScanned", true)
        }
    }

    ////


//    fun getAllToDoListItems(uid: String) : CollectionReference {
//        return database.collection("newUpload").document(uid).collection("machines")
//    }
//
//    fun getUnscannedItems(uid: String) : Query {
//        return database.collection("newUpload")
//            .document(uid)
//            .collection("machines")
//            .whereNotEqualTo("scanned", true)
//    }
//
//    fun getScannedItems(uid: String) : Query {
//        return database.collection("newUpload")
//            .document(uid)
//            .collection("machines")
//            .whereEqualTo("scanned", true)
//    }
}