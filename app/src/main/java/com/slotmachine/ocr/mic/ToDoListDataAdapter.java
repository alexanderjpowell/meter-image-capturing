package com.slotmachine.ocr.mic;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.slotmachine.ocr.mic.model.ToDoListItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ToDoListDataAdapter extends RecyclerView.Adapter<ToDoListDataAdapter.ToDoListDataHolder> implements Filterable {

    private final Context context;
    private final List<ToDoListItem> rowDataList;
    private final List<ToDoListItem> rowDataListAll;
    private OnAdapterItemClickListener onAdapterItemClickListener;

    public ToDoListDataAdapter(Context context, List<ToDoListItem> rowDataList, OnAdapterItemClickListener listener) {
        this.context = context;
        this.rowDataList = rowDataList;
        this.rowDataListAll = new ArrayList<>(rowDataList);
        this.onAdapterItemClickListener = listener;
    }

    @Override
    @NonNull
    public ToDoListDataAdapter.ToDoListDataHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view  = LayoutInflater.from(context).inflate(R.layout.to_do_list_item_row, parent,false);
        return new ToDoListDataAdapter.ToDoListDataHolder(view);
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    private final Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            List<ToDoListItem> filteredList = new ArrayList<>();

            if (charSequence.toString().trim().isEmpty()) {
                filteredList.addAll(rowDataListAll);
            } else {
                for (ToDoListItem i : rowDataListAll) {
                    if (i.getMachineId().startsWith(charSequence.toString().trim())) {
                        filteredList.add(i);
                    }
                }
            }

            Toast.makeText(context, rowDataListAll.toString(), Toast.LENGTH_SHORT).show();

            FilterResults filterResults = new FilterResults();
            filterResults.values = filteredList;

            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            rowDataList.clear();
            rowDataList.addAll((List)filterResults.values);
            notifyDataSetChanged();
        }
    };

    @Override
    public void onBindViewHolder(@NonNull final ToDoListDataAdapter.ToDoListDataHolder holder, final int position) {

        ToDoListItem toDoListItem = rowDataList.get(position);

        holder.machineIdTextView.setText(String.format(Locale.US,"%s: %s", context.getString(R.string.machine_id_text), toDoListItem.getMachineId()));
        holder.descriptionTextView.setText(toDoListItem.getDescription());
        holder.locationTextView.setText(String.format(Locale.US, "%s: %s", context.getString(R.string.location_text), toDoListItem.getLocation()));

        if ((toDoListItem.getUser() != null) && (!toDoListItem.getUser().trim().isEmpty())) {
            holder.userTextView.setText(String.format(Locale.US, "%s %s", context.getString(R.string.assigned_to_text), toDoListItem.getUser()));
            holder.userTextView.setVisibility(View.VISIBLE);
        } else {
            holder.userTextView.setVisibility(View.GONE);
        }

        StringBuilder progressiveDescriptions = new StringBuilder();
        int len = toDoListItem.getDescriptions().size();
        for (int i = 0; i < len; i++) {
            if ((toDoListItem.getDescriptions().get(i) != null) && !toDoListItem.getDescriptions().isEmpty()) {
                progressiveDescriptions.append(toDoListItem.getDescriptions().get(i)).append(" ");
            }
        }
        holder.progressiveDescriptionTitles.setText(progressiveDescriptions.toString().trim());
    }

    @Override
    public int getItemCount() {
        return rowDataList.size();
    }

    public class ToDoListDataHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView machineIdTextView;
        TextView descriptionTextView, locationTextView;
        TextView userTextView;
        TextView progressiveDescriptionTitles;

        public ToDoListDataHolder(View itemView) {
            super(itemView);

            machineIdTextView = itemView.findViewById(R.id.machineIdTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            locationTextView = itemView.findViewById(R.id.locationTextView);
            userTextView = itemView.findViewById(R.id.userTextView);
            progressiveDescriptionTitles = itemView.findViewById(R.id.progressiveDescriptionTitles);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            onAdapterItemClickListener.onItemClick(getBindingAdapterPosition());
        }
    }
}
