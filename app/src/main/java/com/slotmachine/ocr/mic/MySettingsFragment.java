package com.slotmachine.ocr.mic;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.SummaryProvider;
import androidx.preference.PreferenceFragmentCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MySettingsFragment extends PreferenceFragmentCompat {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore database;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        EditTextPreference editTextPreference = findPreference("email_recipient");
        EditTextPreference minimum_value = findPreference("minimum_value");
        Preference sign_out_preference = findPreference("sign_out_button");
        Preference change_password_button = findPreference("change_password_button");

        if (editTextPreference != null) {
            editTextPreference.setSummaryProvider(new SummaryProvider<EditTextPreference>() {
                @Override
                public CharSequence provideSummary(EditTextPreference preference) {
                    String text = preference.getText();
                    if (TextUtils.isEmpty(text)) {
                        return "Not set";
                    }
                    return text;
                }
            });
        }

        if (minimum_value != null) {
            minimum_value.setOnBindEditTextListener(
                    new EditTextPreference.OnBindEditTextListener() {
                        @Override
                        public void onBindEditText(@NonNull EditText editText) {
                            editText.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        }
                    }
            );
        }

        if (sign_out_preference != null) {
            sign_out_preference.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Toast.makeText(getContext(), "Sign out", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    }
            );
        }

        if (change_password_button != null) {
            change_password_button.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Toast.makeText(getContext(), "Change password", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    }
            );
        }
    }

}





