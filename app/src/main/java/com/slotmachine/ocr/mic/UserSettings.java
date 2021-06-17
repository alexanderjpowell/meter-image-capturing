package com.slotmachine.ocr.mic;

import android.content.Context;
import android.content.SharedPreferences;

public class UserSettings {

    private static UserSettings sInstance;

    private Context mContext;

    // To Do List Firestore field values
    public static final String ORDER_BY_UPLOAD_FILE_POSITION = "fileIndex";
    public static final String ORDER_BY_MACHINE_ID = "machineId";
    public static final String ORDER_BY_LOCATION = "location";

    public static UserSettings getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new UserSettings(context);
        } else {
            sInstance.setContext(context);
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

    public static String getToDoListOrderByField(Context context) {
        int orderCode = getPreferences(context).getInt("SORT_BY_FIELD", 0);
        if (orderCode == 1) {
            return ORDER_BY_MACHINE_ID;
        } else if (orderCode == 2) {
            return ORDER_BY_LOCATION;
        } else { // orderCode == 0
            return ORDER_BY_UPLOAD_FILE_POSITION;
        }
    }

    public static void setToDoListOrderByField(Context context, int orderBy) {
        getPreferences(context).edit().putInt("SORT_BY_FIELD", orderBy).apply();
    }

}
