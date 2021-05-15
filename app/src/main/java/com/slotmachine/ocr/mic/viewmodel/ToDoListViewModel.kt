package com.slotmachine.ocr.mic.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.DocumentSnapshot
import com.slotmachine.ocr.mic.model.ToDoListItem
import com.slotmachine.ocr.mic.repository.ToDoListRepository

class ToDoListViewModel {

    private val mRepository = ToDoListRepository()

    private val _allToDoItems : MutableLiveData<List<ToDoListItem>> = MutableLiveData()

    fun getAllToDoListItems(uid: String) : LiveData<List<ToDoListItem>> {
        mRepository.getAllToDoListItems(uid).get().addOnCompleteListener {
            if (it.isSuccessful && !it.result.documents.isNullOrEmpty()) {
                val tmp = mutableListOf<ToDoListItem>()
                for (doc in it.result.documents) {
                    doc.toObject(ToDoListItem::class.java)?.let { it1 -> tmp.add(it1) }
                }
                _allToDoItems.value = tmp
            }
        }
        return _allToDoItems
    }
}