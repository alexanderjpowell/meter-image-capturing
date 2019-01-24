package com.slotmachine.ocr.mic;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageUsersActivity extends AppCompatActivity implements MyRecyclerViewAdapter.ItemClickListener {

    private MyRecyclerViewAdapter adapter;
    private RecyclerView recyclerView;
    private List<String> usersList;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            finish();
            startActivity(new Intent(ManageUsersActivity.this, LoginActivity.class));
            return;
        }
        database = FirebaseFirestore.getInstance();

        usersList = new ArrayList<>();
        adapter = new MyRecyclerViewAdapter(this, usersList);

        recyclerView = findViewById(R.id.users_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
        //adapter = new MyRecyclerViewAdapter(this, usersList);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);

        database.collection("users")
                .document(firebaseAuth.getCurrentUser().getUid())
                .collection("displayNames")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            prepareData(task.getResult());
                        } else {
                            showToast("Error getting users");
                        }
                    }
                });

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                //showToast("short click on " + Integer.toString(position));
                ((MyApplication)ManageUsersActivity.this.getApplication()).setUsername(usersList.get(position));
                Intent intent = new Intent(ManageUsersActivity.this, MainActivity.class);
                intent.putExtra("user", usersList.get(position));
                startActivity(intent);
            }

            @Override
            public void onLongClick(View view, final int position) {
                //showToast("long click on " + Integer.toString(position));
                AlertDialog alertDialog = new AlertDialog.Builder(ManageUsersActivity.this).create();
                alertDialog.setMessage("Do you want to user " + usersList.get(position) + "?");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int i) {
                                deleteUserFromDatabase(usersList.get(position), position);
                                usersList.remove(position);
                                adapter.notifyItemRemoved(position);
                                adapter.notifyItemRangeChanged(position, usersList.size());
                                dialog.dismiss();
                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        }));
    }

    @Override
    public void onItemClick(View view, int position) {
        //Toast.makeText(this, "You clicked " + adapter.getItem(position) + " on row number " + position, Toast.LENGTH_SHORT).show();
    }

    public void addNewUser(View view) {
        final EditText input = new EditText(ManageUsersActivity.this);
        input.setHint("Employee name");
        AlertDialog alertDialog = new AlertDialog.Builder(ManageUsersActivity.this).create();
        alertDialog.setView(input, 100, 70, 100, 0);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "ADD",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        String newName = input.getText().toString().trim();
                        if (newName.equals("")) {
                            return;
                        }

                        // Add to database
                        Map<String, Object> user = new HashMap<>();
                        user.put("displayName", newName);
                        usersList.add(newName);
                        database.collection("users")
                                .document(firebaseAuth.getCurrentUser().getUid())
                                .collection("displayNames")
                                .document(newName).set(user);
                        adapter.notifyDataSetChanged();
                        dialog.dismiss();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    private void prepareData(QuerySnapshot snapshot) {
        usersList.clear();
        for (QueryDocumentSnapshot document : snapshot) {
            usersList.add(document.get("displayName").toString());
        }
        adapter.notifyDataSetChanged();
    }

    private void deleteUserFromDatabase(String name, Integer position) {
        // Remove from database
        database.collection("users")
                .document(firebaseAuth.getCurrentUser().getUid())
                .collection("displayNames")
                .document(name)
                .delete();
        /*usersList.remove(position);
        adapter.notifyItemRemoved(position);
        adapter.notifyItemRangeChanged(position, usersList.size());*/
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

}
