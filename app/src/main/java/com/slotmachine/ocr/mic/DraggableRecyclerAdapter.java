package com.slotmachine.ocr.mic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

public class DraggableRecyclerAdapter extends RecyclerView.Adapter<DraggableRecyclerAdapter.ParentViewHolder> implements ItemMoveCallback.ItemTouchHelperContract {

    private static final int ITEM_MACHINE_ID = 1;
    private static final int ITEM_PROGRESSIVE = 2;

    StartDragListener startDragListener;

    private final List<EditTextModel> editTextData = new ArrayList<>();
    private final List<String> descriptions;
    private final int progressiveCount;
    private final SharedPreferences sharedPreferences;
    private final boolean showHint;

    public DraggableRecyclerAdapter(boolean showHint, Context context, int progressiveCount, List<String> descriptions, StartDragListener startDragListener) {
        this.descriptions = descriptions;
        this.startDragListener = startDragListener;
        this.progressiveCount = progressiveCount;
        for (int i = 0; i < progressiveCount + 1; i++) { // Add 1 for machine id
            editTextData.add(new EditTextModel("", ""));
        }
        this.showHint = showHint;
        this.sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return ITEM_MACHINE_ID;
        } else {
            return ITEM_PROGRESSIVE;
        }
    }

    @NonNull
    @Override
    public ParentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_PROGRESSIVE) {
            return new ProgressiveHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_draggable_progressive, parent, false));
        } else {
            return new MachineIdViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_machine_id, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ParentViewHolder holder, int position) {
        if (holder instanceof ProgressiveHolder) {
            ((ProgressiveHolder)holder).bind();
            ((ProgressiveHolder) holder).progressiveEditText.setText(editTextData.get(position).getEditTextValue());
//            ((ProgressiveHolder) holder).progressiveEditText.setHint(editTextData.get(position).getPreviousValue());
        } else if (holder instanceof MachineIdViewHolder) {
            ((MachineIdViewHolder) holder).bind();
            ((MachineIdViewHolder) holder).machineIdEditText.setText(editTextData.get(position).getEditTextValue());
        }
    }

    @Override
    public int getItemCount() {
        return editTextData.size();
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
        notifyItemChanged(0);
    }

    public void setItems(List<String> progressives) {
        for (int i = 0; i < Math.min(progressiveCount, progressives.size()); i++) {
            if (!progressives.get(i).isEmpty()) {
                editTextData.get(i + 1).setEditTextValue(progressives.get(i));
            }
        }
        notifyDataSetChanged();
    }

    public void setPrevItems(List<String> values) {
        for (int i = 0; i < Math.min(progressiveCount, values.size()); i++) {
            if (!values.get(i).isEmpty()) {
                editTextData.get(i + 1).setPreviousValue(values.get(i));
            }
        }
        notifyDataSetChanged();
    }

    public void setItem(int pos, String value) {
        editTextData.get(pos).setEditTextValue(value);
        notifyItemChanged(pos);
    }

    public void resetItems() {
        for (EditTextModel i : editTextData) {
            i.setEditTextValue("");
        }
        notifyDataSetChanged();
    }

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

            //String hint = "Prog " + getBindingAdapterPosition() + " - " + editTextData.get(getBindingAdapterPosition()).getPreviousValue();
//            progressiveLayout.setHint("Progressive " + getBindingAdapterPosition());
            //progressiveLayout.setHint(editTextData.get(getBindingAdapterPosition()).getPreviousValue());

            progressiveLayout.setEndIconOnClickListener(view -> {
                if (progressiveEditText.getText() != null) {
                    if (progressiveEditText.getText().toString().isEmpty()) {
                        startDragListener.onVoiceRequest(getBindingAdapterPosition());
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
                        editTextData.get(getBindingAdapterPosition()).setEditTextValue(value);
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

            //
            int[][] states = new int[][]{
                    new int[]{android.R.attr.state_enabled}, // enabled
                    new int[]{-android.R.attr.state_enabled}, // disabled
                    new int[]{-android.R.attr.state_checked}, // unchecked
                    new int[]{android.R.attr.state_pressed}  // pressed
            };
            int[] colors = new int[]{
                    Color.GREEN,
                    Color.GREEN,
                    Color.GREEN,
                    Color.GREEN
            };
            ColorStateList colorStateList = new ColorStateList(states, colors);
            //

            if (showHint) {
                progressiveLayout.setHint("Progressive " + getBindingAdapterPosition());
            } else {
                String hint = sharedPreferences.getString("progressive_hint_text_from_todo", "description");
                if (hint.equals("description")) {
                    if (descriptions != null && descriptions.size() >= getBindingAdapterPosition()) {
                        progressiveLayout.setHint(descriptions.get(getBindingAdapterPosition() - 1)); // Subtract 1 for machine id
                        progressiveLayout.setDefaultHintTextColor(colorStateList);
                    }
                }
                else if (hint.equals("previous")) {
                    progressiveLayout.setHint(editTextData.get(getBindingAdapterPosition()).getPreviousValue());
                    progressiveLayout.setDefaultHintTextColor(colorStateList);
                }
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
                        startDragListener.onVoiceRequest(getBindingAdapterPosition());
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
                        editTextData.get(getBindingAdapterPosition()).setEditTextValue(value);
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

    interface StartDragListener {
        void requestDrag(RecyclerView.ViewHolder viewHolder);
        void onVoiceRequest(int id);
    }

    public static class EditTextModel {
        private String editTextValue;
        private String previousValue;
        public EditTextModel(String val, String prevVal) {
            this.editTextValue = val;
            this.previousValue = prevVal;
        }
        public String getEditTextValue() {
            return editTextValue;
        }
        public String getPreviousValue() {
            return previousValue;
        }
        public void setEditTextValue(String editTextValue) {
            this.editTextValue = editTextValue;
        }
        public void setPreviousValue(String previousValue) {
            this.previousValue = previousValue;
        }
    }

    public List<String> getData() {
        List<String> ret = new ArrayList<>();
        for (EditTextModel i : editTextData) {
            ret.add(i.getEditTextValue());
        }
        return ret;
    }

}