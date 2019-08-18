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

    private CollectionReference uploadFormDataCollectionReference;

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
        uploadFormDataCollectionReference = database.collection("formUploads")
                .document(firebaseAuth.getCurrentUser().getUid())
                .collection("uploadFormData");

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
                Intent intent = new Intent(TodoListActivity.this, MainActivity.class);
                intent.putExtra("machine_id", toDoDataList.get(position).getMachineId());
                startActivity(intent);
            }

            @Override
            public void onLongClick(View view, final int position) {
            }
        }));

        currentStatus = Status.INCOMPLETE;
        populateRecyclerView();
        //
    }

    private void populateRecyclerView() {
        toDoDataList.clear();
        boolean isComplete = currentStatus.equals(Status.COMPLETE);
        uploadFormDataCollectionReference
                .whereEqualTo("isCompleted", isComplete)
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
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

                    }
                    mAdapter.notifyDataSetChanged();
                } else {
                    showToast("Unable to refresh.  Check your connection.");
                }
            }
        });
    }

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
            populateRecyclerView();
            return true;
        } else if ((id == R.id.completedScans) && (!currentStatus.equals(Status.COMPLETE))) {
            currentStatus = Status.COMPLETE;
            populateRecyclerView();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
