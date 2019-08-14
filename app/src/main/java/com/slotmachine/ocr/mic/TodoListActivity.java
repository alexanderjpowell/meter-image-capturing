package com.slotmachine.ocr.mic;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class TodoListActivity extends AppCompatActivity {

    private List<ToDoListData> toDoDataList;
    private RecyclerView recyclerView;
    private ToDoListDataAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_list);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recycler_view_to_do_list);
        toDoDataList = new ArrayList<>();
        mAdapter = new ToDoListDataAdapter(TodoListActivity.this, toDoDataList);

        final LinearLayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(mAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                showToast("Click position: " + position);
            }

            @Override
            public void onLongClick(View view, final int position) {
                showToast("Long click on position: " + position);
            }
        }));

        //String date = "Tue Aug 13 15:45:43 EDT 2019";
        String date = "Last scanned 43 minutes ago by Alex";

        ToDoListData row1 = new ToDoListData("AB2301-03",
                "12345",
                "TRIPLE JACKPOT GEMS - CLII",
                date,
                false,
                false);

        ToDoListData row2 = new ToDoListData("AB2304-6",
                "45789",
                "BLACK DIAMOND PLATINUM",
                date,
                false,
                false);

        ToDoListData row3 = new ToDoListData("AB2307-09",
                "33451",
                "DBL JACKPOT GEMS/DBL JACKPOT LIONS SHARE",
                date,
                true,
                false);

        ToDoListData row4 = new ToDoListData("AB3300",
                "56091",
                "SUPER JACKPOT - CLII",
                date,
                false,
                false);

        toDoDataList.add(row1);
        toDoDataList.add(row2);
        toDoDataList.add(row3);
        toDoDataList.add(row4);

        mAdapter.notifyDataSetChanged();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
