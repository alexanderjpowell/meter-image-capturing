package com.slotmachine.ocr.mic.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.slotmachine.ocr.mic.R

class NotesBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "NotesBottomSheet"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val editText: EditText = view.findViewById(R.id.notes_edit_text)
    }

}