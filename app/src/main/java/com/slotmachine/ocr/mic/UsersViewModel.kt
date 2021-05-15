package com.slotmachine.ocr.mic

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.slotmachine.ocr.mic.repository.ScannedDataRepository

class UsersViewModel : ViewModel() {

    private var mRepository = ScannedDataRepository()
    private var _users : MutableLiveData<List<String>> = MutableLiveData()
    private var _pin: MutableLiveData<String> = MutableLiveData()

    fun getUserNames(uid: String) : LiveData<List<String>> {
        mRepository.getUserNames(uid).get().addOnCompleteListener { result ->
            if (result.isSuccessful) {
                val savedAddressList: MutableList<String> = mutableListOf()
                result.result.documents.forEach {
                    savedAddressList.add(it.id)
                }
                _users.value = savedAddressList
            }
        }
        return _users
    }

    fun getPinCodeForUser(uid: String, name: String) : LiveData<String> {
        mRepository.getPinCodeForUser(uid, name).get().addOnCompleteListener { result ->
            if (result.isSuccessful) {
                _pin.value = result.result.data?.get("pinCode").toString()
            }
        }
        return _pin
    }

    fun deleteUserName(uid: String, name: String) {
        mRepository.deleteUserName(uid, name)
    }

}