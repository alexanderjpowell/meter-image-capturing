package com.slotmachine.ocr.mic.model

data class ToDoListItem (
    val location: String? = null,
    val machineId: String? = null,
    val user: String? = null,
    val description: String? = null,
    val descriptions: List<String>? = null,
    val increments: List<String>? = null,
    val bases: List<String>? = null,
    @field:JvmField
    val isScanned: Boolean? = null
)