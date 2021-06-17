package com.slotmachine.ocr.mic.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.slotmachine.ocr.mic.fragment.ToDoItemsFragment

class DemoCollectionAdapter(activity: FragmentActivity?) : FragmentStateAdapter(activity!!) {
    override fun createFragment(position: Int): Fragment {
        return ToDoItemsFragment()
    }

    override fun getItemCount() = 2
}