package com.slotmachine.ocr.mic;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TodoListActivity extends AppCompatActivity {

    public static final int SUBMIT_PROGRESSIVE_RECORD = 0;

    private List<ToDoListData> toDoDataList;
    private RecyclerView recyclerView;
    private ToDoListDataAdapter mAdapter;
    private enum EmptyState { NO_FILE_UPLOAD, ALL_COMPLETED, NONE_COMPLETED, NORMAL }

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore database;

    private SwipeRefreshLayout swipeRefreshLayout;

    private DocumentReference uploadFormDataDocumentReference;

    private TextView empty_state_text_view;
    private TextView empty_state_completed_text_view;
    private TextView empty_state_uncompleted_text_view;
    private ProgressBar progressBar;

    private String TAG = "TodoListActivity";

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

        empty_state_text_view = findViewById(R.id.empty_state_no_file_text_view);
        empty_state_completed_text_view = findViewById(R.id.empty_state_completed_text_view);
        empty_state_uncompleted_text_view = findViewById(R.id.empty_state_uncompleted_text_view);
        progressBar = findViewById(R.id.progress_bar);

        database = FirebaseFirestore.getInstance();
        uploadFormDataDocumentReference = database.collection("formUploads")
                .document(firebaseAuth.getCurrentUser().getUid());

        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // This method performs the actual data-refresh operation.
                        // The method calls setRefreshing(false) when it's finished.
                        populateRecyclerView();
                    }
                }
        );

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
                intent.putExtra("numberOfProgressives", toDoDataList.get(position).getDescriptionsLength());
                intent.putExtra("position", position);
                //String[] progressiveDescriptionTitles = toDoDataList.get(position).getProgressiveDescriptions();
                ArrayList<String> progressiveDescriptionTitlesList = toDoDataList.get(position).getProgressiveDescriptionsList();
                //intent.putExtra("progressiveDescriptionTitles", progressiveDescriptionTitles);
                intent.putStringArrayListExtra("progressiveDescriptionTitles", progressiveDescriptionTitlesList);

                intent.putExtra("hashMap", (HashMap)toDoDataList.get(position).getMap());
                startActivityForResult(intent, SUBMIT_PROGRESSIVE_RECORD);
            }

            @Override
            public void onLongClick(View view, final int position) {
                // TODO
            }
        }));

        populateRecyclerView();
    }

    /*private String[] resizeProgressiveDescriptionsArray(String[] array) {
        List<String> tmp = new ArrayList<String>();
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null) {
                tmp.add(array[i]);
            }
        }
        String[] ret = tmp.toArray(new String[tmp.size()]);
        return ret;
    }*/

    private void populateRecyclerView() {
        recyclerView.setVisibility(View.GONE);
        empty_state_text_view.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        toDoDataList.clear();
        uploadFormDataDocumentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if ((document != null) && document.exists()) {
                        if (document.contains("uploadArray")) {

                            List<Map<String, Object>> mapList = (List<Map<String, Object>>)document.get("uploadArray");

                            for (int i = 0; i < mapList.size(); i++) {
                                Map<String, Object> map = mapList.get(i);
                                ToDoListData row = new ToDoListData(map);
                                toDoDataList.add(row);
                            }
                            toggleEmptyStateDisplays(EmptyState.NORMAL);
                        }
                        mAdapter.notifyDataSetChanged();
                    } else {
                        //showToast("No such document");
                        toggleEmptyStateDisplays(EmptyState.NO_FILE_UPLOAD);
                        Log.d(TAG, "No such document");
                    }
                } else {
                    showToast(task.getException().getMessage());
                    Log.d(TAG, "get failed with ", task.getException());
                }

                // Finish things up
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void toggleEmptyStateDisplays(EmptyState state) {
        if (state.equals(EmptyState.NO_FILE_UPLOAD)) {
            recyclerView.setVisibility(View.GONE);
            empty_state_text_view.setVisibility(View.VISIBLE);
            empty_state_completed_text_view.setVisibility(View.GONE);
            empty_state_uncompleted_text_view.setVisibility(View.GONE);
        } else if (state.equals(EmptyState.ALL_COMPLETED)) {
            recyclerView.setVisibility(View.GONE);
            empty_state_text_view.setVisibility(View.GONE);
            empty_state_completed_text_view.setVisibility(View.VISIBLE);
            empty_state_uncompleted_text_view.setVisibility(View.GONE);
        } else if (state.equals(EmptyState.NONE_COMPLETED)) {
            recyclerView.setVisibility(View.GONE);
            empty_state_text_view.setVisibility(View.GONE);
            empty_state_completed_text_view.setVisibility(View.GONE);
            empty_state_uncompleted_text_view.setVisibility(View.VISIBLE);
        } else if (state.equals(EmptyState.NORMAL)) {
            recyclerView.setVisibility(View.VISIBLE);
            empty_state_text_view.setVisibility(View.GONE);
            empty_state_completed_text_view.setVisibility(View.GONE);
            empty_state_uncompleted_text_view.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == RESULT_OK) {
            if (requestCode == SUBMIT_PROGRESSIVE_RECORD) {
                int position = intent.getIntExtra("position", 0);
                removeRowFromRecyclerView(position);
            }
        }
    }

    private void removeRowFromRecyclerView(int position) {
        toDoDataList.remove(position);
        mAdapter.notifyItemRemoved(position);
        mAdapter.notifyItemRangeChanged(position, toDoDataList.size());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.to_do_list_action_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
