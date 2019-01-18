package com.slotmachine.ocr.mic;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class DataReportActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private List<RowData> rowDataList = new ArrayList<>();
    private RecyclerView recyclerView;
    private ReportDataAdapter mAdapter;
    private Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_report);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        spinner = (Spinner)findViewById(R.id.spinner);

        recyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        mAdapter = new ReportDataAdapter(rowDataList);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(mAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                RowData rowData = rowDataList.get(position);
                showToast(rowData.getMachineId() + " is selected!");
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

        prepareData();

        spinner.setOnItemSelectedListener(this);
        List<String> categories = new ArrayList<String>();
        categories.add("1 Hour");
        categories.add("1 Day");
        categories.add("1 Week");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);

        //ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.spinner_array, android.R.layout.simple_spinner_item);
        //adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //spinner.setAdapter(adapter);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        String item = parent.getItemAtPosition(position).toString();

        // Showing selected spinner item
        Toast.makeText(parent.getContext(), "Selected: " + item, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

    private void prepareData() {
        RowData rowData = new RowData("Machine ID: 1234", "January 18, 2019", "$12,345.67", "$2342.50", "$500.00", "$38.42");
        rowDataList.add(rowData);
        rowDataList.add(rowData);
        rowDataList.add(rowData);
        rowDataList.add(rowData);
        rowDataList.add(rowData);
        rowDataList.add(rowData);
        rowDataList.add(rowData);
        rowDataList.add(rowData);
        rowDataList.add(rowData);
        rowDataList.add(rowData);
        rowDataList.add(rowData);
        rowDataList.add(rowData);
        rowDataList.add(rowData);
        rowDataList.add(rowData);
        rowDataList.add(rowData);
        mAdapter.notifyDataSetChanged();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
