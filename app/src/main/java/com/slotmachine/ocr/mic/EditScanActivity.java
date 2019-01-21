package com.slotmachine.ocr.mic;

import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;

public class EditScanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_scan);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
