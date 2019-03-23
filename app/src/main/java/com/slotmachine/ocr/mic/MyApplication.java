package com.slotmachine.ocr.mic;

import android.app.Application;

public class MyApplication extends Application {
    private String username;
    private Integer usernameIndex;

    public String getUsername() {
        return username;
    }
    public Integer getUsernameIndex() {
        return usernameIndex;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    public void setUsernameIndex(Integer usernameIndex) {
        this.usernameIndex = usernameIndex;
    }

}
