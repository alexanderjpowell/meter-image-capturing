package com.slotmachine.ocr.mic

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.slotmachine.ocr.mic.DraggableRecyclerAdapter.ParentViewHolder

class ItemMoveCallback(private val mAdapter: ItemTouchHelperContract) : ItemTouchHelper.Callback() {
    override fun isLongPressDragEnabled(): Boolean = false

    override fun isItemViewSwipeEnabled(): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean {
        if (target.bindingAdapterPosition in 1..UserSettings.getNumberOfProgressives(recyclerView.context)) {
            mAdapter.onRowMoved(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        }
        return true
    }
    interface ItemTouchHelperContract {
        fun onBindViewHolder(holder: ParentViewHolder?, position: Int)
        fun onRowMoved(fromPosition: Int, toPosition: Int)
    }
}