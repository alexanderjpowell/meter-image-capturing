package com.slotmachine.ocr.mic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
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

    private FirebaseUser firebaseUser;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            startActivity(new Intent(getContext(), LoginActivity.class));
            return;
        }

        //
        // DONT STORE DISPLAY NAME IN PREFERENCES - FETCH FROM FIREBASE AUTH EACH TIME
        //

        final SharedPreferences sharedPref = getContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        Preference accountEmailPreference = findPreference("account_email_button");
        EditTextPreference displayNamePreference = findPreference("display_name_button");
        EditTextPreference adminEmailPreference = findPreference("admin_email_button");
        EditTextPreference emailRecipientPreference = findPreference("email_recipient");
        EditTextPreference minimum_value = findPreference("minimum_value");
        Preference signOutPreference = findPreference("sign_out_button");
        Preference changePasswordPreference = findPreference("change_password_button");
        //Preference delete_account_button = findPreference("delete_account_button");
        Preference terms_and_conditions_button = findPreference("legal_disclaimer");
        //Preference verify_email_preference_button = findPreference("verify_email_button");
        Preference version_number_preference = findPreference("version_number_preference");
        Preference add_remove_users = findPreference("add_remove_users");

        //
        final ListPreference number_of_progressives_preference = findPreference("number_of_progressives");
        if (number_of_progressives_preference != null) {
            number_of_progressives_preference.setSummary(number_of_progressives_preference.getValue() + " progressive options");
            number_of_progressives_preference.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            preference.setSummary(newValue.toString() + " progressive options");
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putInt("number_of_progressives", Integer.valueOf(newValue.toString()));
                            editor.apply();
                            return true;
                        }
                    }
            );
        }
        //

        if (version_number_preference != null) {
            version_number_preference.setSummary(BuildConfig.VERSION_NAME);
        }

        if (accountEmailPreference != null) {
            accountEmailPreference.setSummary(firebaseAuth.getCurrentUser().getEmail());
        }

        if (displayNamePreference != null) {
            displayNamePreference.setSummaryProvider(new SummaryProvider<EditTextPreference>() {
                @Override
                public CharSequence provideSummary(EditTextPreference preference) {
                    if (firebaseUser.getDisplayName() == null)
                        return "Not set";
                    else
                        return firebaseUser.getDisplayName();
                }
            });
            displayNamePreference.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(newValue.toString().trim())
                                    .build();
                            firebaseUser.updateProfile(profileUpdates);
                            //displayNamePreference.setText(newValue.toString().trim());
                            return true;
                        }
                    }
            );
        }

        if (adminEmailPreference != null) {
            adminEmailPreference.setSummaryProvider(new SummaryProvider<EditTextPreference>() {
                @Override
                public CharSequence provideSummary(EditTextPreference preference) {
                    String text = preference.getText().trim();
                    if (TextUtils.isEmpty(text)){
                        return "Grant read privileges to an administrator";
                    }
                    return text;
                }
            });
        }

        if (emailRecipientPreference != null) {
            emailRecipientPreference.setSummaryProvider(new SummaryProvider<EditTextPreference>() {
                @Override
                public CharSequence provideSummary(EditTextPreference preference) {
                    String text = preference.getText();
                    if (TextUtils.isEmpty(text)) {
                        return "Separate with commas to include multiple recipients";
                    }
                    return text;
                }
            });
            emailRecipientPreference.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString("email_recipient", newValue.toString());
                            editor.apply();
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

        if (add_remove_users != null) {
            add_remove_users.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            // Start Manage users activity
                            Intent intent = new Intent(getActivity(), ManageUsersActivity.class);
                            intent.putExtra("adminMode", true);
                            startActivity(intent);
                            return true;
                        }
                    }
            );
        }

        if (signOutPreference != null) {
            signOutPreference.setOnPreferenceClickListener(
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

        if (changePasswordPreference != null) {
            changePasswordPreference.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            startActivity(new Intent(requireActivity(), ChangePasswordActivity.class));
                            return true;
                        }
                    }
            );
        }

        /*if (delete_account_button != null) {
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
        }*/

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





