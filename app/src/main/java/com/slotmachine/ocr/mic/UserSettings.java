package com.slotmachine.ocr.mic;

import android.content.Context;
import android.content.SharedPreferences;

public class UserSettings {

    private static UserSettings sInstance;

    private Context mContext;

    public static UserSettings getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new UserSettings(context.getApplicationContext());
        } else {
            sInstance.setContext(context.getApplicationContext());
        }
        return sInstance;
    }

    private UserSettings(Context context) {
        mContext = context;
    }

    private void setContext(Context context) {
        mContext = context;
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    }

    public static int getNumberOfProgressives(Context context) {
        return getPreferences(context).getInt("number_of_progressives", 6);
    }

}
