package com.slotmachine.ocr.mic.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.slotmachine.ocr.mic.model.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository: AuthRepository = AuthRepository(application)
    private val _userLiveData = MutableLiveData<FirebaseUser>()
    val userLiveData: LiveData<FirebaseUser> = _userLiveData
    //private val _loggedOutLiveData = MutableLiveData<Boolean>()
    //val loggedOutLiveData: LiveData<Boolean> = _loggedOutLiveData

    fun login(email: String?, password: String?) {
        authRepository.login(email, password)
    }

    fun logOut() {
        authRepository.logOut()
    }

    init {
        viewModelScope.launch {
            _userLiveData.value = authRepository.fetchUser()
            //_loggedOutLiveData = authRepository.loggedOutLiveData
        }
    }
}