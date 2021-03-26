package com.slotmachine.ocr.mic;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

public class DraggableRecyclerAdapter extends RecyclerView.Adapter<DraggableRecyclerAdapter.ParentViewHolder> implements ItemMoveCallback.ItemTouchHelperContract {

    private static final int ITEM_MACHINE_ID = 1;
    private static final int ITEM_PROGRESSIVE = 2;
    private static final int ITEM_BUTTONS = 3;

    private ArrayList data;
    StartDragListener startDragListener;

    private List<EditModel> progs = new ArrayList<>();

    public DraggableRecyclerAdapter(ArrayList data, StartDragListener startDragListener) {
        this.data = data;
        this.startDragListener = startDragListener;
        progs.add(new EditModel());
        progs.add(new EditModel());
        progs.add(new EditModel());
        progs.add(new EditModel());
        progs.add(new EditModel());
        progs.add(new EditModel());
        progs.add(new EditModel());
        progs.add(new EditModel());
//        progs.add(new EditModel());
//        progs.add(new EditModel());
//        progs.add(new EditModel());
//        progs.add(new EditModel());
//        progs.add(new EditModel());
//        progs.add(new EditModel());
//        progs.add(new EditModel());
//        progs.add(new EditModel());
//        progs.add(new EditModel());
//        progs.add(new EditModel());
//        progs.add(new EditModel());
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return ITEM_MACHINE_ID;
        } else if (position == data.size() - 1) {
            return ITEM_BUTTONS;
        } else {
            return ITEM_PROGRESSIVE;
        }
    }

    @Override
    public @NotNull ParentViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_PROGRESSIVE) {
            return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_draggable_progressive, parent, false));
        } else if (viewType == ITEM_MACHINE_ID) {
            return new MachineIdViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_machine_id, parent, false));
        } else {
            return new ButtonsViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_buttons, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NotNull ParentViewHolder holder, int position) {
        if (holder instanceof MyViewHolder) {
            ((MyViewHolder)holder).bind();
            ((MyViewHolder) holder).progressiveEditText.setText(progs.get(position).getEditTextValue());
        } else if (holder instanceof ButtonsViewHolder) {
            ((ButtonsViewHolder) holder).button.setOnClickListener(view -> startDragListener.onSubmitButtonClick());
            ((ButtonsViewHolder) holder).button2.setOnClickListener(view -> startDragListener.onSubmitScan());
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public void onRowMoved(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(data, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(data, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    public void setItems(List<String> progressives) {
        for (int i = 0; i < Math.min(6, progressives.size()); i++) {
            if (!progressives.get(i).isEmpty()) {
                Timber.e(progressives.get(i));
                progs.get(i + 1).setEditTextValue(progressives.get(i));
            }
        }
        notifyDataSetChanged();
    }

    public String getMachineId() {
        return progs.get(0).getEditTextValue();
    }

//    @Override
//    public void onRowSelected(MyViewHolder myViewHolder) {
//        //myViewHolder.rowView.setBackgroundColor(Color.GRAY);
//
//    }
//
//    @Override
//    public void onRowClear(MyViewHolder myViewHolder) {
//        //myViewHolder.rowView.setBackgroundColor(Color.WHITE);
//
//    }

    public static class ParentViewHolder extends RecyclerView.ViewHolder {
        public ParentViewHolder(View itemView) {
            super(itemView);
        }
    }

    public class MyViewHolder extends ParentViewHolder {

        View rowView;
        ImageView dragIcon;
        TextInputEditText progressiveEditText;

        public MyViewHolder(View itemView) {
            super(itemView);
            rowView = itemView;
            dragIcon = itemView.findViewById(R.id.drag_icon);
            progressiveEditText = itemView.findViewById(R.id.progressive_edit_text);

            progressiveEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    //progs.get(getAdapterPosition()).setEditTextValue(charSequence.toString());
                    progs.get(getAdapterPosition()).setEditTextValue(progressiveEditText.getText().toString());
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });

        }

        @SuppressLint("ClickableViewAccessibility")
        private void bind() {
            dragIcon.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startDragListener.requestDrag(this);
                }
                return false;
            });

//            progressiveEditText.addTextChangedListener(new TextWatcher() {
//                @Override
//                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                }
//
//                @Override
//                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                    progs.get(getAdapterPosition()).setEditTextValue(charSequence.toString());
//                }
//
//                @Override
//                public void afterTextChanged(Editable editable) {
//                }
//            });
        }
    }

    public static class MachineIdViewHolder extends ParentViewHolder {
        public MachineIdViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static class ButtonsViewHolder extends ParentViewHolder {
        MaterialButton button;
        MaterialButton button2;
        public ButtonsViewHolder(View itemView) {
            super(itemView);
            button = itemView.findViewById(R.id.mButton2);
            button2 = itemView.findViewById(R.id.submit_button);
        }
    }

    interface StartDragListener {
        void requestDrag(RecyclerView.ViewHolder viewHolder);
        void onSubmitButtonClick();
        void onSubmitScan();
    }

    public static class EditModel {

        private String editTextValue;

        public String getEditTextValue() {
            return editTextValue;
        }

        public void setEditTextValue(String editTextValue) {
            this.editTextValue = editTextValue;
        }
    }

}