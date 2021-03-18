package com.slotmachine.ocr.mic.model

data class MachineScanItem(
        val documentId: String,
        val machineId: String,
        val date: String,
        val userName: String,
        val location: String,
        val notes: String,
        val progressives: List<String>,
        val bases: List<String>,
        val increment:List<String>
)
