package com.slotmachine.ocr.mic.viewmodel

import androidx.lifecycle.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.slotmachine.ocr.mic.model.ToDoListItem
import com.slotmachine.ocr.mic.repository.ToDoListRepository
import com.google.firebase.firestore.ktx.toObject
import timber.log.Timber

class ToDoListViewModel : ViewModel() {

    private val mRepository = ToDoListRepository()

    private val _allToDoItems : MutableLiveData<List<ToDoListItem>> = MutableLiveData()
    private val _userHasUploadFile : MutableLiveData<Boolean> = MutableLiveData()
    private val _latestCollectionDoc : MutableLiveData<DocumentSnapshot?> = MutableLiveData()
    private val _toDoItemsById : MutableLiveData<List<ToDoListItem>> = MutableLiveData()

//    private val _documentCursor = MutableLiveData<DocumentSnapshot?>()

//    val latestCollectionId: LiveData<String>
//        get() = _latestCollectionId

//    private val _allDocs : MutableLiveData<List<String>> = MutableLiveData()

//    fun getAllToDoListItems(uid: String) : LiveData<List<ToDoListItem>> {
//        mRepository.getAllToDoListItems(uid).get().addOnCompleteListener {
//            if (it.isSuccessful && !it.result.documents.isNullOrEmpty()) {
//                val tmp = mutableListOf<ToDoListItem>()
//                for (doc in it.result.documents) {
//                    doc.toObject(ToDoListItem::class.java)?.let { it1 -> tmp.add(it1) }
//                }
//                _allToDoItems.value = tmp
//            }
//        }
//        return _allToDoItems
//    }

    fun getAllUnscannedToDoItems(uid: String) : LiveData<List<ToDoListItem>> {
        mRepository.getLatestUploadCollection(uid).get().addOnCompleteListener {
            if (it.isSuccessful && !it.result.documents.isNullOrEmpty() && it.result.documents.size == 1) {
                val id = it.result.documents[0].id
                mRepository.getAllUnscannedToDoItems(uid, id).get().addOnCompleteListener { itt ->
                    if (itt.isSuccessful) {
                        val tmp = mutableListOf<ToDoListItem>()
                        for (doc in itt.result.documents) {
                            doc.toObject<ToDoListItem>()?.let { it1 -> tmp.add(it1) }
                        }
                        _allToDoItems.value = tmp
                    }
                }
            }
        }
        return _allToDoItems
    }

    fun setItemAsScanned(uid: String, docId: String) {
        mRepository.getLatestUploadCollection(uid).get().addOnCompleteListener {
            if (it.isSuccessful && !it.result.documents.isNullOrEmpty() && it.result.documents.size == 1) {
                mRepository.batchUpdateScan(
                    mRepository.getLatestDoc(uid, it.result.documents[0].id),
                    mRepository.getScannedItem(uid, it.result.documents[0].id, docId))
            }
        }
    }

    fun userHasUploadFile(uid: String) : LiveData<Boolean> {
        mRepository.getLatestUploadCollection(uid).get().addOnCompleteListener {
            _userHasUploadFile.value = it.isSuccessful && !it.result.documents.isNullOrEmpty()
        }
        return _userHasUploadFile
    }

    fun getLatestUploadCollectionId(uid: String) : LiveData<DocumentSnapshot?> {
        mRepository.getLatestUploadCollection(uid).get().addOnCompleteListener {
            if (it.isSuccessful) {
                if (it.result.isEmpty || it.result.documents.isNullOrEmpty()) {
                    _latestCollectionDoc.value = null
                } else if (it.result.documents.size == 1) {
                    _latestCollectionDoc.value = it.result.documents[0]//.id
                }
            }
        }
        return _latestCollectionDoc
    }

    fun searchForToDoItemsById(uid: String, docId: String, machineId: String) : LiveData<List<ToDoListItem>> {
        mRepository.getDocsByMachineId(uid, docId, machineId).get().addOnCompleteListener {
            if (it.isSuccessful) {
                val tmp = mutableListOf<ToDoListItem>()
                for (doc in it.result.documents) {
                    doc.toObject<ToDoListItem>()?.let { it1 -> tmp.add(it1) }
                }
                _toDoItemsById.value = tmp
            } else {
                Timber.e(it.exception)
            }
        }
        return _toDoItemsById
    }

//    val toDoItemDocs: LiveData<List<String>> = Transformations.switchMap(_documentCursor) {
//        if (it == null) {
//
//        } else {
//
//        }
//    }
}