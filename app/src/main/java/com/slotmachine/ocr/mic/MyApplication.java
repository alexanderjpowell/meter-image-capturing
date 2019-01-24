package com.slotmachine.ocr.mic;

import android.app.Application;

public class MyApplication extends Application {
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
