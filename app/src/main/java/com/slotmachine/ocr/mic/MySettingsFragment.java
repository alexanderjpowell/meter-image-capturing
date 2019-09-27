package com.slotmachine.ocr.mic;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.google.firebase.auth.UserProfileChangeRequest;

public class MySettingsFragment extends PreferenceFragmentCompat {

    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            startActivity(new Intent(getContext(), LoginActivity.class));
            return;
        }

        //
        // DONT STORE DISPLAY NAME IN PREFERENCES - FETCH FROM FIREBASE AUTH EACH TIME
        //

        Preference account_email_preference = findPreference("account_email_button");
        EditTextPreference display_name_preference = findPreference("display_name_button");
        EditTextPreference email_recipient_preference = findPreference("email_recipient");
        EditTextPreference minimum_value = findPreference("minimum_value");
        Preference sign_out_preference_button = findPreference("sign_out_button");
        Preference change_password_button = findPreference("change_password_button");
        Preference delete_account_button = findPreference("delete_account_button");
        Preference terms_and_conditions_button = findPreference("legal_disclaimer");
        //Preference verify_email_preference_button = findPreference("verify_email_button");
        Preference version_number_preference = findPreference("version_number_preference");

        //EditTextPreference testPref = findPreference("test_pref");
        /*if (display_name_preference != null)
            showToast("not null");
        else
            showToast("null");*/

        //showToast(display_name_preference.getText());

        //showToast("Name: " + display_name_preference.getText());

        if (version_number_preference != null) {
            version_number_preference.setSummary(BuildConfig.VERSION_NAME);
        }

        if (account_email_preference != null) {
            account_email_preference.setSummary(firebaseAuth.getCurrentUser().getEmail());
        }

        if (display_name_preference != null) {
            //display_name_preference.setSummary(firebaseUser.getDisplayName());
            display_name_preference.setSummaryProvider(new SummaryProvider<EditTextPreference>() {
                @Override
                public CharSequence provideSummary(EditTextPreference preference) {
                    //showToast("provideSummary");
                    //String text = preference.getText();

                    if (firebaseUser.getDisplayName() == null)
                        return "Not set";
                    else
                        return firebaseUser.getDisplayName();
                    //else if (TextUtils.isEmpty(text)) {
                    //    return "Not set";
                    //}
                    //return text;
                }
            });
            display_name_preference.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(newValue.toString().trim())
                                    .build();
                            firebaseUser.updateProfile(profileUpdates);
                            //display_name_preference.setText(newValue.toString().trim());
                            return true;
                        }
                    }
            );

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
                            return true;
                        }
                    }
            );
        }

        if (minimum_value != null) {
            if (minimum_value.getText() == null || minimum_value.getText().trim().isEmpty())
                minimum_value.setSummary(getString(R.string.minimum_value_summary));
            else
                minimum_value.setSummary("$" + minimum_value.getText().trim());

            minimum_value.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            if (newValue.toString().trim().isEmpty())
                                preference.setSummary(getString(R.string.minimum_value_summary));
                            else
                                preference.setSummary("$" + newValue.toString().trim());
                            return true;
                        }
                    }
            );

            minimum_value.setOnBindEditTextListener(
                    new EditTextPreference.OnBindEditTextListener() {
                        @Override
                        public void onBindEditText(@NonNull EditText editText) {
                            editText.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        }
                    }
            );
        }

        if (sign_out_preference_button != null) {
            sign_out_preference_button.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            AuthUI.getInstance()
                                    .signOut(requireActivity())
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                requireActivity().finish();
                                                startActivity(new Intent(requireActivity(), LoginActivity.class));
                                            } else {
                                                if (task.getException() != null)
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
                            startActivity(new Intent(requireActivity(), ChangePasswordActivity.class));
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
                            new AlertDialog.Builder(requireActivity())
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
                                                        requireActivity().finish();
                                                        startActivity(new Intent(requireActivity(), LoginActivity.class));
                                                    } else {
                                                        if (task.getException() != null)
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

        /*if (verify_email_preference_button != null) {

            verify_email_preference_button.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            firebaseUser.sendEmailVerification();
                            return true;
                        }
                    }
            );
        }*/
    }

    /*private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }*/
}





