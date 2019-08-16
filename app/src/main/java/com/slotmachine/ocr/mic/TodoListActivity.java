package com.slotmachine.ocr.mic;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class TodoListActivity extends AppCompatActivity {

    private List<ToDoListData> toDoDataList;
    private RecyclerView recyclerView;
    private ToDoListDataAdapter mAdapter;
    private enum Status {INCOMPLETE, COMPLETE};
    private Status currentStatus;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_list);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Ensure user is signed in
        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(TodoListActivity.this, LoginActivity.class));
            finish();
            return;
        }
        //

        database = FirebaseFirestore.getInstance();

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
                Intent intent = new Intent(TodoListActivity.this, MainActivity.class);
                intent.putExtra("machine_id", toDoDataList.get(position).getMachineId());
                startActivity(intent);
            }

            @Override
            public void onLongClick(View view, final int position) {
                showToast("Long click on position: " + position);
            }
        }));

        currentStatus = Status.INCOMPLETE;

        //
        populateRecyclerView();
        //

        //String date = "Tue Aug 13 15:45:43 EDT 2019";
        /*String date = "Last scanned 43 minutes ago by Alex";

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
        toDoDataList.add(row4);*/

        CollectionReference collectionReference = database.collection("formUploads")
                .document(firebaseAuth.getCurrentUser().getUid())
                .collection("uploadFormData");

        collectionReference.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        ToDoListData row = new ToDoListData(document.get("location").toString().trim(),
                                document.get("machine_id").toString().trim(),
                                document.get("description").toString().trim(),
                                false,
                                false);
                        toDoDataList.add(row);
                        mAdapter.notifyDataSetChanged();
                    }
                } else {
                    showToast("Unable to refresh.  Check your connection.");
                }
            }
        });

        //mAdapter.notifyDataSetChanged();
    }

    //
    private void populateRecyclerView() {
    	if (currentStatus.equals(Status.INCOMPLETE)) {
    		// Get all records from below path where !isComplete
    		// path - formUploads/<UID>/uploadFormData/<documentId>

    	} else { // currentStatus == Status.COMPLETE

    	}
    }
    //

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.to_do_list_action_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if ((id == R.id.incompleteScans) && (!currentStatus.equals(Status.INCOMPLETE))) {
            currentStatus = Status.INCOMPLETE;
            showToast("incomplete scans");
            return true;
        } else if ((id == R.id.completedScans) && (!currentStatus.equals(Status.COMPLETE))) {
            currentStatus = Status.COMPLETE;
            showToast("completed scans");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
