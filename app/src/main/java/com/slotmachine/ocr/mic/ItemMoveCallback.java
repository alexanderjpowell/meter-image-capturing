package com.slotmachine.ocr.mic;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

public class ItemMoveCallback extends ItemTouchHelper.Callback {

    private final ItemTouchHelperContract mAdapter;

    public ItemMoveCallback(ItemTouchHelperContract adapter) {
        mAdapter = adapter;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

    }

    @Override
    public int getMovementFlags(@NotNull RecyclerView recyclerView, RecyclerView.@NotNull ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        return makeMovementFlags(dragFlags, 0);
    }

    @Override
    public boolean onMove(@NotNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        int fromPos = viewHolder.getAdapterPosition();
        int toPos = target.getAdapterPosition();
        Timber.e(Integer.toString(fromPos));
        Timber.e(Integer.toString(toPos));
        Timber.e("----");
//        if (toPos == 5) {
//            //do nothing
//        } else {
//            mAdapter.onRowMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
//        }
        if (toPos <= 6 && toPos >= 1) {
            mAdapter.onRowMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        }
        return true;
    }

//    @Override
//    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
//        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
//            if (viewHolder instanceof DraggableRecyclerAdapter.MyViewHolder) {
//                DraggableRecyclerAdapter.MyViewHolder myViewHolder = (DraggableRecyclerAdapter.MyViewHolder) viewHolder;
//                //mAdapter.onRowSelected(myViewHolder);
//            }
//
//        }
//        super.onSelectedChanged(viewHolder, actionState);
//    }

//    @Override
//    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
//        super.clearView(recyclerView, viewHolder);
//        if (viewHolder instanceof DraggableRecyclerAdapter.MyViewHolder) {
//            DraggableRecyclerAdapter.MyViewHolder myViewHolder=
//                    (DraggableRecyclerAdapter.MyViewHolder) viewHolder;
//            //mAdapter.onRowClear(myViewHolder);
//        }
//    }

    public interface ItemTouchHelperContract {
        void onBindViewHolder(DraggableRecyclerAdapter.ParentViewHolder holder, int position);
        void onRowMoved(int fromPosition, int toPosition);
        //void onRowSelected(DraggableRecyclerAdapter.MyViewHolder myViewHolder);
        //void onRowClear(DraggableRecyclerAdapter.MyViewHolder myViewHolder);
    }

}
