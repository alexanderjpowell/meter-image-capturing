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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
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
        //uploadFormDataCollectionReference = database.collection("formUploads")
        //        .document(firebaseAuth.getCurrentUser().getUid())
        //        .collection("uploadFormData");
        //
        uploadFormDataDocumentReference = database.collection("formUploads")
                .document(firebaseAuth.getCurrentUser().getUid());
        //

        //currentStatus = Status.INCOMPLETE;
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
                intent.putExtra("numberOfProgressives", toDoDataList.get(position).getNumberOfProgressives());
                intent.putExtra("position", position);
                String[] progressiveDescriptionTitles = toDoDataList.get(position).getProgressiveDescriptions();
                intent.putExtra("progressiveDescriptionTitles", progressiveDescriptionTitles);

                intent.putExtra("hashMap", toDoDataList.get(position).getMap());
                startActivityForResult(intent, SUBMIT_PROGRESSIVE_RECORD);
            }

            @Override
            public void onLongClick(View view, final int position) {
                // TODO
            }
        }));

        populateRecyclerView();
    }

    private String[] resizeProgressiveDescriptionsArray(String[] array) {
        List<String> tmp = new ArrayList<String>();
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null) {
                tmp.add(array[i]);
            }
        }
        String[] ret = tmp.toArray(new String[tmp.size()]);
        return ret;
    }

    private void populateRecyclerView() {
        recyclerView.setVisibility(View.GONE);
        empty_state_text_view.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        toDoDataList.clear();
        /*uploadFormDataCollectionReference
                .whereEqualTo("completed", false)
                .orderBy("location")
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult().size() == 0)
                        toggleEmptyStateDisplays(EmptyState.NO_FILE_UPLOAD);
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String[] progressiveDescriptions = new String[10];
                        String[] progressiveDescriptionsNew;

                        progressiveDescriptions[0] = (document.get("p_1") == null) ? null : document.get("p_1").toString();
                        progressiveDescriptions[1] = (document.get("p_2") == null) ? null : document.get("p_2").toString();
                        progressiveDescriptions[2] = (document.get("p_3") == null) ? null : document.get("p_3").toString();
                        progressiveDescriptions[3] = (document.get("p_4") == null) ? null : document.get("p_4").toString();
                        progressiveDescriptions[4] = (document.get("p_5") == null) ? null : document.get("p_5").toString();
                        progressiveDescriptions[5] = (document.get("p_6") == null) ? null : document.get("p_6").toString();
                        progressiveDescriptions[6] = (document.get("p_7") == null) ? null : document.get("p_7").toString();
                        progressiveDescriptions[7] = (document.get("p_8") == null) ? null : document.get("p_8").toString();
                        progressiveDescriptions[8] = (document.get("p_9") == null) ? null : document.get("p_9").toString();
                        progressiveDescriptions[9] = (document.get("p_10") == null) ? null : document.get("p_10").toString();

                        //
                        progressiveDescriptionsNew = resizeProgressiveDescriptionsArray(progressiveDescriptions);
                        //

                        ToDoListData row = new ToDoListData(document.get("location").toString().trim(),
                                document.get("machine_id").toString().trim(),
                                document.get("description").toString().trim(),
                                (document.get("user") == null) ? null : document.get("user").toString(),
                                (document.get("progressive_count") == null) ? null : Integer.valueOf((String)document.get("progressive_count")),
                                progressiveDescriptionsNew,
                                false,
                                false);
                        toDoDataList.add(row);

                        //
                        toggleEmptyStateDisplays(EmptyState.NORMAL);
                    }
                    mAdapter.notifyDataSetChanged();
                } else {
                    showToast("Unable to refresh.  Check your connection.");
                }
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        });*/
        uploadFormDataDocumentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        if (document.contains("timestamp") && document.contains("uploadArray")) {
                            List<Map<String, Object>> mapList = (List<Map<String, Object>>)document.get("uploadArray");
                            for (int i = 0; i < mapList.size(); i++) {

                                String description = mapList.get(i).get("description").toString();
                                String location = mapList.get(i).get("location").toString();
                                String machine_id = mapList.get(i).get("machine_id").toString();
                                String user = mapList.get(i).get("user").toString();
                                Integer progressive_count = Integer.valueOf(mapList.get(i).get("progressive_count").toString());
                                String p_1 = mapList.get(i).get("p_1").toString();
                                String p_2 = mapList.get(i).get("p_2").toString();
                                String p_3 = mapList.get(i).get("p_3").toString();
                                String p_4 = mapList.get(i).get("p_4").toString();
                                String p_5 = mapList.get(i).get("p_5").toString();
                                String p_6 = mapList.get(i).get("p_6").toString();
                                String p_7 = mapList.get(i).get("p_7").toString();
                                String p_8 = mapList.get(i).get("p_8").toString();
                                String p_9 = mapList.get(i).get("p_9").toString();
                                String p_10 = mapList.get(i).get("p_10").toString();

                                ToDoListData row = new ToDoListData(location,
                                        machine_id,
                                        description,
                                        user,
                                        progressive_count,
                                        new String[] {p_1, p_2, p_3, p_4, p_5, p_6, p_7, p_8, p_9, p_10},
                                        false,
                                        false);
                                toDoDataList.add(row);
                                toggleEmptyStateDisplays(EmptyState.NORMAL);
                            }
                        }
                        mAdapter.notifyDataSetChanged();
                    } else {
                        showToast("No such document");
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
        int id = item.getItemId();

        /*if ((id == R.id.incompleteScans) && (!currentStatus.equals(Status.INCOMPLETE))) {
            currentStatus = Status.INCOMPLETE;
            populateRecyclerView();
            setTitle("Incomplete Scans");
            return true;
        } else if ((id == R.id.completedScans) && (!currentStatus.equals(Status.COMPLETE))) {
            currentStatus = Status.COMPLETE;
            populateRecyclerView();
            setTitle("Completed Scans");
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
