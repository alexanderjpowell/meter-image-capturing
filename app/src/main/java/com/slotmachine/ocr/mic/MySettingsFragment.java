package com.slotmachine.ocr.mic;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MySettingsFragment extends PreferenceFragmentCompat {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore database;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(getContext(), LoginActivity.class));
            return;
        }

        Preference account_email_preference = findPreference("account_email_button");
        Preference display_name_preference = findPreference("display_name_button");
        EditTextPreference email_recipient_preference = findPreference("email_recipient");
        EditTextPreference minimum_value = findPreference("minimum_value");
        Preference sign_out_preference = findPreference("sign_out_button");
        Preference change_password_button = findPreference("change_password_button");
        Preference delete_account_button = findPreference("delete_account_button");
        Preference terms_and_conditions_button = findPreference("legal_disclaimer");

        if (account_email_preference != null) {
            account_email_preference.setSummary(firebaseAuth.getCurrentUser().getEmail());
        }

        if (display_name_preference != null) {
            display_name_preference.setSummary(firebaseAuth.getCurrentUser().getDisplayName());
        }

        if (email_recipient_preference != null) {
            email_recipient_preference.setSummaryProvider(new SummaryProvider<EditTextPreference>() {
                @Override
                public CharSequence provideSummary(EditTextPreference preference) {
                    String text = preference.getText();
                    if (TextUtils.isEmpty(text)) {
                        return "Separate with commas to include multiple recipients";
                    }
                    return text;
                }
            });
            email_recipient_preference.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            //Toast.makeText(getContext(), "changed email", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    }
            );
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
                            AuthUI.getInstance()
                                    .signOut(getActivity())
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                getActivity().finish();
                                                startActivity(new Intent(getActivity(), LoginActivity.class));
                                            } else {
                                                Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
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
                            startActivity(new Intent(getActivity(), ChangePasswordActivity.class));
                            return true;
                        }
                    }
            );
        }

        if (delete_account_button != null) {
            delete_account_button.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle("Delete Account")
                                    .setMessage("Are you sure you want to delete your account? This operation cannot be undone.")
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            FirebaseUser user = firebaseAuth.getCurrentUser();
                                            user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        getActivity().finish();
                                                        startActivity(new Intent(getActivity(), LoginActivity.class));
                                                    } else {
                                                        Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                        }
                                    })
                                    .setNegativeButton("No", null)
                                    .show();
                            return true;
                        }
                    }
            );
        }

        if (terms_and_conditions_button != null) {
            terms_and_conditions_button.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            startActivity(new Intent(getActivity(), DisclaimerActivity.class));
                            return true;
                        }
                    }
            );
        }
    }

}





