package com.slotmachine.ocr.mic;

import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static android.support.v4.content.FileProvider.getUriForFile;

public class DataReportActivity extends AppCompatActivity {// implements AdapterView.OnItemSelectedListener {

    private List<RowData> rowDataList;
    private RecyclerView recyclerView;
    private ReportDataAdapter mAdapter;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_report);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Ensure user is signed in
        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            finish();
            startActivity(new Intent(DataReportActivity.this, LoginActivity.class));
            return;
        }

        database = FirebaseFirestore.getInstance();

        rowDataList = new ArrayList<>();

        recyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        mAdapter = new ReportDataAdapter(DataReportActivity.this, rowDataList);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(mAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                RowData rowData = rowDataList.get(position);
                showToast(rowData.getMachineId() + " is selected");
                startActivity(new Intent(DataReportActivity.this, EditScanActivity.class));
            }

            @Override
            public void onLongClick(View view, int position) {
                RowData rowData = rowDataList.get(position);
                showToast("Long Click on " + rowData.getMachineId());

                final EditText input = new EditText(DataReportActivity.this);
                //final EditText input2 = new EditText(DataReportActivity.this);
                AlertDialog alertDialog = new AlertDialog.Builder(DataReportActivity.this).create();
                alertDialog.setTitle("Alert");
                alertDialog.setMessage("Alert message to be shown");
                alertDialog.setView(input, 100, 0, 100, 0);
                //alertDialog.setView(input2, 100, 100, 100, 100);
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "CLEAR",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "DELETE",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        }));

        // Read from database
        CollectionReference scansReference = database.collection("scans");
        Query query = scansReference.whereEqualTo("uid", firebaseAuth.getCurrentUser().getUid()).orderBy("timestamp", Query.Direction.DESCENDING).limit(100); // order and limit here
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    prepareData(task.getResult());
                } else {
                    Log.d("DBREADER", "Error getting documents: ", task.getException());
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reports_action_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete) {

            String data = "";
            List<RowData> listRowData = mAdapter.getRowDataList();

            Iterator<RowData> listRowDataIterator = listRowData.iterator();
            int count = 0;
            while (listRowDataIterator.hasNext()) {
                RowData r = listRowDataIterator.next();
                RowData rowData2 = listRowData.get(count);
                if (rowData2.isSelected()) {
                    data = data + "\n" + r.getMachineId() + ", Index: " + Integer.toString(count);
                    listRowDataIterator.remove();
                }
                count++;
            }
            showToast(data);
        } else if (id == R.id.action_past_hour) {
            executeQuery("HOUR");
        } else if (id == R.id.action_past_day) {
            executeQuery("DAY");
        } else if (id == R.id.action_past_week) {
            executeQuery("WEEK");
        }
        return super.onOptionsItemSelected(item);
    }

    private void executeQuery(String dateRange) {
        int offset = 0;
        if (dateRange.equals("HOUR"))
            offset = 3600;
        else if (dateRange.equals("DAY"))
            offset = 86400;
        else if (dateRange.equals("WEEK"))
            offset = 604800;
        Date time = new Date(System.currentTimeMillis() - offset * 1000);
        CollectionReference collectionReference = database.collection("scans");
        Query query = collectionReference.whereEqualTo("uid", firebaseAuth.getCurrentUser().getUid())
                .whereGreaterThan("timestamp", time)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100);
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    prepareData(task.getResult());
                } else {
                    showToast("Unable to refresh.  Check your connection.");
                }
            }
        });
    }

    private void prepareData(QuerySnapshot snapshot) {

        String machine_id, timestamp, user, progressive1, progressive2, progressive3, progressive4, progressive5, progressive6;
        RowData rowData;
        rowDataList.clear(); // reset the current data list

        for (QueryDocumentSnapshot document : snapshot) {

            machine_id = document.get("machine_id").toString();
            timestamp = document.get("timestamp").toString();
            //user = "Alex";
            user = (document.get("userName") == null) ? "User not specified" : document.get("userName").toString();
            progressive1 = document.get("progressive1").toString().trim().isEmpty() ? "" : "$" + document.get("progressive1").toString().trim();
            progressive2 = document.get("progressive2").toString().trim().isEmpty() ? "" : "$" + document.get("progressive2").toString().trim();
            progressive3 = document.get("progressive3").toString().trim().isEmpty() ? "" : "$" + document.get("progressive3").toString().trim();
            progressive4 = document.get("progressive4").toString().trim().isEmpty() ? "" : "$" + document.get("progressive4").toString().trim();
            progressive5 = document.get("progressive5").toString().trim().isEmpty() ? "" : "$" + document.get("progressive5").toString().trim();
            progressive6 = document.get("progressive6").toString().trim().isEmpty() ? "" : "$" + document.get("progressive6").toString().trim();

            rowData = new RowData("Machine ID: " + machine_id,
                    timestamp,
                    user,
                    progressive1,
                    progressive2,
                    progressive3,
                    progressive4,
                    progressive5,
                    progressive6,
                    false);
            rowDataList.add(rowData);
        }

        mAdapter.notifyDataSetChanged();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void showSnackBar(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private File createImageFile(String data) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "report_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera");
        File image = File.createTempFile(
                imageFileName,   /* prefix */
                ".csv",   /* suffix */
                storageDir      /* directory */
        );
        FileWriter fw = new FileWriter(image, true);
        fw.write(data);
        fw.flush();
        fw.close();

        return image;
    }

    private String getMachineIdFromString(String text) {
        //Machine ID: 9792// <- Example of what text looks like
        text = text.replace("Machine ID: ", "");
        return text;
    }

    private String createCsvFile() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("'Machine','Progressive1','Progressive2','Progressive3','Progressive4','Progressive5','Progressive6','Date',\n");
        for (RowData rowData : rowDataList) {
            stringBuilder.append("'" + getMachineIdFromString(rowData.getMachineId()) + "',");
            stringBuilder.append("'" + rowData.getProgressive1() + "',");
            stringBuilder.append("'" + rowData.getProgressive2() + "',");
            stringBuilder.append("'" + rowData.getProgressive3() + "',");
            stringBuilder.append("'" + rowData.getProgressive4() + "',");
            stringBuilder.append("'" + rowData.getProgressive5() + "',");
            stringBuilder.append("'" + rowData.getProgressive6() + "',");
            stringBuilder.append("'" + rowData.getDate() + "',\n");
        }
        return stringBuilder.toString();
    }

    public void generateReport(View view) {
        showSnackBar(view, "Download ");

        File file = new File("");
        try {
            file = createImageFile(createCsvFile());
        } catch (Exception ex) {
            showToast(ex.getMessage());
            return;
        }

        // Share with other apps
        Uri contentUri = getUriForFile(getApplicationContext(), "com.slotmachine.ocr.mic.fileprovider", file);
        Intent intentToEmailFile = new Intent(Intent.ACTION_SEND);
        intentToEmailFile.setType("text/plain");
        intentToEmailFile.putExtra(Intent.EXTRA_STREAM, contentUri);
        intentToEmailFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intentToEmailFile.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);//
        Intent chooserIntent = Intent.createChooser(intentToEmailFile,"Send email");
        //startActivityForResult(chooserIntent, 1);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, chooserIntent, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "CHANNEL_ID")
                //.setSmallIcon(R.mipmap.ic_stat_onesignal_default)
                //.setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Your download has completed")
                .setContentText("Share via email, text, or other")
                .setContentIntent(pendingIntent)
                .setColor(Color.argb(255, 0, 0, 255))
                .setAutoCancel(true);
                //.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                //.addAction(R.drawable.ic_launcher, "Share", pendingIntent);

        createNotificationChannel();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, mBuilder.build());

    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Reports generated";
            String description = "channel description";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("CHANNEL_ID", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
