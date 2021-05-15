package com.slotmachine.ocr.mic.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.slotmachine.ocr.mic.repository.ScannedDataRepository

class MainActivityViewModel : ViewModel() {

    private val mRepository = ScannedDataRepository()

    private val _prevValues : MutableLiveData<List<String>> = MutableLiveData()
    private val _duplicateScanMachineId: MutableLiveData<String> = MutableLiveData()

    fun getPrevDayValues(uid: String, machineId: String) : LiveData<List<String>> {
        mRepository.getLatestScansById(uid, machineId).get().addOnCompleteListener { result ->
            if (result.isSuccessful) {
                val prevValues: MutableList<String> = mutableListOf()
                if (!result.result.documents.isNullOrEmpty()) {
                    val snapshot = result.result.documents[0]
                    if (snapshot.contains("progressive1")) {
                        prevValues.add(snapshot.get("progressive1") as String)
                    }
                    if (snapshot.contains("progressive2")) {
                        prevValues.add(snapshot.get("progressive2") as String)
                    }
                    if (snapshot.contains("progressive3")) {
                        prevValues.add(snapshot.get("progressive3") as String)
                    }
                    if (snapshot.contains("progressive4")) {
                        prevValues.add(snapshot.get("progressive4") as String)
                    }
                    if (snapshot.contains("progressive5")) {
                        prevValues.add(snapshot.get("progressive5") as String)
                    }
                    if (snapshot.contains("progressive6")) {
                        prevValues.add(snapshot.get("progressive6") as String)
                    }
                    if (snapshot.contains("progressive7")) {
                        prevValues.add(snapshot.get("progressive7") as String)
                    }
                    if (snapshot.contains("progressive8")) {
                        prevValues.add(snapshot.get("progressive8") as String)
                    }
                    if (snapshot.contains("progressive9")) {
                        prevValues.add(snapshot.get("progressive9") as String)
                    }
                    if (snapshot.contains("progressive10")) {
                        prevValues.add(snapshot.get("progressive10") as String)
                    }
                    _prevValues.value = prevValues
                }
            }
        }
        return _prevValues
    }

    fun getDuplicateScan(uid: String, machineId: String, rejectDurationMillis: Int) : LiveData<String> {
        mRepository.getLastScanByIdInTimeRange(uid, machineId, rejectDurationMillis)
                .get().addOnCompleteListener { result ->
                    if (result.isSuccessful) {
                        if (!result.result.documents.isNullOrEmpty()) {
                            val snapshot = result.result.documents[0]
                            _duplicateScanMachineId.value = (snapshot.get("machine_id") as String)
                        } else {
                            _duplicateScanMachineId.value = null
                        }
                    }
                }
        return _duplicateScanMachineId
    }
}