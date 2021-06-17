package com.slotmachine.ocr.mic.model

import com.google.firebase.firestore.DocumentId
import java.io.Serializable

data class ToDoListItem (
    @DocumentId
    var documentId: String? = null,
    val location: String? = null,
    val machineId: String? = null,
    val user: String? = null,
    val description: String? = null,
    val descriptions: List<String>? = null,
    val increments: List<String>? = null,
    val bases: List<String>? = null,
    val fileIndex: Int? = null,
    @field:JvmField
    val isScanned: Boolean? = null,
) : Serializable