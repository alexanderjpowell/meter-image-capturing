package com.slotmachine.ocr.mic;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DraggableRecyclerAdapter extends RecyclerView.Adapter<DraggableRecyclerAdapter.ParentViewHolder> implements ItemMoveCallback.ItemTouchHelperContract {

    private static final int ITEM_MACHINE_ID = 1;
    private static final int ITEM_PROGRESSIVE = 2;
    private static final int ITEM_BUTTONS = 3;

    StartDragListener startDragListener;

    private final List<EditTextModel> editTextData = new ArrayList<>();
    private final List<String> descriptions;

    public DraggableRecyclerAdapter(int progressiveCount, StartDragListener startDragListener) {
        this(progressiveCount, null, startDragListener);
    }

    public DraggableRecyclerAdapter(int progressiveCount, List<String> descriptions, StartDragListener startDragListener) {
        this.descriptions = descriptions;
        this.startDragListener = startDragListener;
        for (int i = 0; i < progressiveCount + 1; i++) { // Add 1 for machine id
            editTextData.add(new EditTextModel(""));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return ITEM_MACHINE_ID;
        } else if (position == editTextData.size()) {
            return ITEM_BUTTONS;
        } else {
            return ITEM_PROGRESSIVE;
        }
    }

    @Override
    public @NotNull ParentViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_PROGRESSIVE) {
            return new ProgressiveHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_draggable_progressive, parent, false));
        } else if (viewType == ITEM_MACHINE_ID) {
            return new MachineIdViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_machine_id, parent, false));
        } else {
            return new ButtonsViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_buttons, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NotNull ParentViewHolder holder, int position) {
        if (holder instanceof ProgressiveHolder) {
            ((ProgressiveHolder)holder).bind();
            ((ProgressiveHolder) holder).progressiveEditText.setText(editTextData.get(position).getEditTextValue());
        } else if (holder instanceof ButtonsViewHolder) {
            ((ButtonsViewHolder) holder).button.setOnClickListener(view -> startDragListener.onSubmitButtonClick());
            ((ButtonsViewHolder) holder).button2.setOnClickListener(view -> startDragListener.onSubmitScan());
        } else if (holder instanceof MachineIdViewHolder) {
            ((MachineIdViewHolder) holder).bind();
            ((MachineIdViewHolder) holder).machineIdEditText.setText(editTextData.get(position).getEditTextValue());
        }
    }

    @Override
    public int getItemCount() {
        return editTextData.size() + 1;
    }

    @Override
    public void onRowMoved(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(editTextData, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(editTextData, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    public void setMachineId(String id) {
        editTextData.get(0).setEditTextValue(id);
        notifyDataSetChanged();
    }

    public void setItems(List<String> progressives) {
        for (int i = 0; i < Math.min(6, progressives.size()); i++) {
            if (!progressives.get(i).isEmpty()) {
                editTextData.get(i + 1).setEditTextValue(progressives.get(i));
            }
        }
        notifyDataSetChanged();
    }

//    public String getMachineId() {
//        return progs.get(0).getEditTextValue();
//    }

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

    public class ProgressiveHolder extends ParentViewHolder {

        View rowView;
        ImageView dragIcon;
        TextInputEditText progressiveEditText;
        TextInputLayout progressiveLayout;

        public ProgressiveHolder(View itemView) {
            super(itemView);
            rowView = itemView;
            dragIcon = itemView.findViewById(R.id.drag_icon);
            progressiveEditText = itemView.findViewById(R.id.progressive_edit_text);
            progressiveLayout = itemView.findViewById(R.id.progressive_layout);
        }

        @SuppressLint("ClickableViewAccessibility")
        private void bind() {
            dragIcon.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startDragListener.requestDrag(this);
                }
                return false;
            });

            progressiveLayout.setHint("Progressive " + getAdapterPosition());

            progressiveLayout.setEndIconOnClickListener(view -> {
                if (progressiveEditText.getText() != null) {
                    if (progressiveEditText.getText().toString().isEmpty()) {
                        startDragListener.onVoiceRequest(getAdapterPosition());
                    } else {
                        progressiveEditText.setText("");
                    }
                }
            });

            progressiveEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    if (progressiveEditText.getText() != null) {
                        String value = progressiveEditText.getText().toString();
                        editTextData.get(getAdapterPosition()).setEditTextValue(value);
                        if (value.isEmpty()) {
                            progressiveLayout.setEndIconDrawable(R.drawable.outline_mic_24);
                        } else {
                            progressiveLayout.setEndIconDrawable(R.drawable.ic_cancel_black_24dp);
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });

            if (descriptions != null && descriptions.size() >= getAdapterPosition()) {
                int[][] states = new int[][] {
                        new int[] { android.R.attr.state_enabled }, // enabled
                        new int[] { -android.R.attr.state_enabled }, // disabled
                        new int[] { -android.R.attr.state_checked }, // unchecked
                        new int[] { android.R.attr.state_pressed }  // pressed
                };
                int[] colors = new int[] {
                        Color.GREEN,
                        Color.GREEN,
                        Color.GREEN,
                        Color.GREEN
                };
                progressiveLayout.setHint(descriptions.get(getAdapterPosition() - 1)); // Subtract 1 for machine id
                ColorStateList colorStateList = new ColorStateList(states, colors);
                progressiveLayout.setDefaultHintTextColor(colorStateList);
            }
        }
    }

    public class MachineIdViewHolder extends ParentViewHolder {
        TextInputEditText machineIdEditText;
        TextInputLayout machineIdLayout;
        public MachineIdViewHolder(View itemView) {
            super(itemView);
            machineIdLayout = itemView.findViewById(R.id.input_layout);
            machineIdEditText = itemView.findViewById(R.id.edit_text);
        }

        private void bind() {
            machineIdLayout.setEndIconOnClickListener(view -> {
                if (machineIdEditText.getText() != null) {
                    if (machineIdEditText.getText().toString().isEmpty()) {
                        startDragListener.onVoiceRequest(getAdapterPosition());
                    } else {
                        machineIdEditText.setText("");
                    }
                }
            });

            machineIdEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    if (machineIdEditText.getText() != null) {
                        String value = machineIdEditText.getText().toString();
                        editTextData.get(getAdapterPosition()).setEditTextValue(value);
                        if (value.isEmpty()) {
                            machineIdLayout.setEndIconDrawable(R.drawable.outline_mic_24);
                        } else {
                            machineIdLayout.setEndIconDrawable(R.drawable.ic_cancel_black_24dp);
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });
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
        void onVoiceRequest(int id);
    }

    public static class EditTextModel {
        private String editTextValue;
        public EditTextModel(String val) { this.editTextValue = val; }
        public String getEditTextValue() {
            return editTextValue;
        }
        public void setEditTextValue(String editTextValue) {
            this.editTextValue = editTextValue;
        }
    }

    public List<String> getData() {
        List<String> ret = new ArrayList<>();
        for (EditTextModel i : editTextData) {
//            if (!i.getEditTextValue().trim().isEmpty()) {
//                ret.add(i.getEditTextValue());
//            }
            ret.add(i.getEditTextValue());
        }
        return ret;
    }

}