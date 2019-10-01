package com.slotmachine.ocr.mic;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageUsersActivity extends AppCompatActivity implements MyRecyclerViewAdapter.ItemClickListener {

    private MyRecyclerViewAdapter adapter;
    private RecyclerView recyclerView;
    private List<String> usersList;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore database;
    private boolean adminMode;

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

        FloatingActionButton fab = findViewById(R.id.fab);

        Intent intent = getIntent();
        adminMode = intent.getBooleanExtra("adminMode", false);

        if (adminMode) {
            fab.show();
        } else {
            fab.hide();
        }

        usersList = new ArrayList<>();
        adapter = new MyRecyclerViewAdapter(this, usersList);

        recyclerView = findViewById(R.id.users_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
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
                // 1. Get pin code from database
                // 2. Start PinCodeActivity and if correct pin is entered, then return to this activity

                if (adminMode) {
                    //
                } else {
                    final String username = usersList.get(position);
                    database.collection("users")
                            .document(firebaseAuth.getCurrentUser().getUid())
                            .collection("displayNames")
                            .document(username)
                            .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    Map<String, Object> map = document.getData();
                                    if (document.contains("pinCode")) {
                                        displayPinCodeActivity(map.get("pinCode").toString(), username);
                                    } else {
                                        showToast("no pin found");
                                    }
                                    //String displayName = map.get("displayName").toString();
                                    //map.get("pinCode");
                                    //showToast(document.getData().toString());
                                } else {
                                    showToast("No document");
                                }
                            } else {
                                showToast("Failed to get username");
                            }
                        }
                    });
                }
            }

            @Override
            public void onLongClick(View view, final int position) {
                //showToast("long click on " + Integer.toString(position));
                if (adminMode) {
                    AlertDialog alertDialog = new AlertDialog.Builder(ManageUsersActivity.this).create();
                    alertDialog.setMessage("Do you want to delete " + usersList.get(position) + "?");
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
                } else {

                }
            }
        }));
    }

    private void displayPinCodeActivity(String pinCode, String username) {
        Intent intent = new Intent(getApplicationContext(), PinCodeActivity.class);
        intent.putExtra("pinCode", pinCode);
        intent.putExtra("username", username);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(ManageUsersActivity.this, MainActivity.class));
        finish();
    }

    @Override
    public void onItemClick(View view, int position) {
        //Toast.makeText(this, "You clicked " + adapter.getItem(position) + " on row number " + position, Toast.LENGTH_SHORT).show();
    }

    public void addNewUser(View view) {
        final EditText input1 = new EditText(ManageUsersActivity.this);
        input1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input1.requestFocus();
        input1.setHint("Employee name");

        final EditText input2 = new EditText(ManageUsersActivity.this);
        input2.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input2.setInputType(InputType.TYPE_CLASS_NUMBER);
        // Set max length to 4
        InputFilter[] filterArray = new InputFilter[1];
        filterArray[0] = new InputFilter.LengthFilter(4);
        input2.setFilters(filterArray);
        input2.setHint("4 Digit Pin");

        Context context = ManageUsersActivity.this.getApplicationContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(input1);
        layout.addView(input2);

        AlertDialog alertDialog = new AlertDialog.Builder(ManageUsersActivity.this).create();
        alertDialog.setMessage("Create new user with name and pin code");
        //alertDialog.setView(input, 100, 70, 100, 0);
        alertDialog.setView(layout, 100, 70, 100, 0);
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "ADD",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        String newName = input1.getText().toString().trim();
                        String pinCode = input2.getText().toString().trim();
                        if (newName.equals("") || pinCode.equals("")) {
                            return;
                        }

                        // Add to database
                        Map<String, Object> user = new HashMap<>();
                        user.put("displayName", newName);
                        user.put("pinCode", pinCode);
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
        Collections.sort(usersList);
        adapter.notifyDataSetChanged();
    }

    private void deleteUserFromDatabase(String name, Integer position) {
        // Remove from database
        database.collection("users")
                .document(firebaseAuth.getCurrentUser().getUid())
                .collection("displayNames")
                .document(name)
                .delete();

        MyApplication app = (MyApplication)getApplication();
        app.setUsernameIndex(0); // Hacky fix for indexOutOfRangeException
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

}
