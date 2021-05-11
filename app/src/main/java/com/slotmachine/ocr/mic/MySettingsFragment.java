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
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MySettingsFragment extends PreferenceFragmentCompat {

    private FirebaseUser firebaseUser;
    private FirebaseFirestore database;
    private SharedPreferences sharedPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            startActivity(new Intent(getContext(), LoginActivity.class));
            return;
        }

        database = FirebaseFirestore.getInstance();

        //
        // DONT STORE DISPLAY NAME IN PREFERENCES - FETCH FROM FIREBASE AUTH EACH TIME
        //

        sharedPref = getContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        Preference accountEmailPreference = findPreference("account_email_button");
        final EditTextPreference casinoNamePreference = findPreference("casino_name_button");
        EditTextPreference emailRecipientPreference = findPreference("email_recipient");
        EditTextPreference minimum_value = findPreference("minimum_value");
        Preference signOutPreference = findPreference("sign_out_button");
        Preference changePasswordPreference = findPreference("change_password_button");
        Preference terms_and_conditions_button = findPreference("legal_disclaimer");
        Preference version_number_preference = findPreference("version_number_preference");
        Preference add_remove_users = findPreference("add_remove_users");
        //ListPreference progressive_hint_text_from_todo = findPreference("progressive_hint_text_from_todo");

        SwitchPreference reject_duplicates = findPreference("reject_duplicates");
        final SeekBarPreference reject_duplicates_duration = findPreference("reject_duplicates_duration");

        if (reject_duplicates != null) {
            reject_duplicates_duration.setVisible(reject_duplicates.isChecked());
            reject_duplicates.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            reject_duplicates_duration.setVisible((Boolean)newValue);
                            return true;
                        }
                    }
            );
        }

        if (reject_duplicates_duration != null) {
            reject_duplicates_duration.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            //populateDuplicatesSet((Integer)newValue);
                            //Toast.makeText(getContext(), newValue.toString(), Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    }
            );
        }

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
                            editor.putInt("number_of_progressives", Integer.parseInt(newValue.toString()));
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

        if (casinoNamePreference != null) {

            DocumentReference docRef = database.collection("users").document(firebaseUser.getUid());
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Object casinoNameObject = document.get("casinoName");
                            if (casinoNameObject != null) {
                                casinoNamePreference.setText(casinoNameObject.toString());
                            } else {
                                casinoNamePreference.setText("");
                            }
                        }
                    }
                }
            });

            casinoNamePreference.setSummaryProvider(new SummaryProvider<EditTextPreference>() {
                @Override
                public CharSequence provideSummary(final EditTextPreference preference) {
                    String text = preference.getText();
                    if (text == null || TextUtils.isEmpty(text.trim())) {
                        return "Not set";
                    } else {
                        return text.trim();
                    }
                }
            });
            casinoNamePreference.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            //UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            //        .setDisplayName(newValue.toString().trim())
                            //        .build();
                            //firebaseUser.updateProfile(profileUpdates);
                            if (newValue.toString().trim().isEmpty()) {
                                Toast.makeText(getContext(), "Submit a non-empty casino name", Toast.LENGTH_LONG).show();
                                return true;
                            }
                            String casinoName = newValue.toString().trim();
                            DocumentReference docRef = database.collection("users").document(firebaseUser.getUid());
                            docRef.update("casinoName", casinoName);
                            casinoNamePreference.setText(casinoName);
                            return true;
                        }
                    }
            );
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

//        if (progressive_hint_text_from_todo != null) {
//            progressive_hint_text_from_todo.setOnPreferenceChangeListener(
//                    new Preference.OnPreferenceChangeListener() {
//                        @Override
//                        public boolean onPreferenceChange(Preference preference, Object newValue) {
//                            SharedPreferences.Editor editor = sharedPref.edit();
//                            editor.putString("progressive_hint_text_from_todo", newValue.toString());
//                            editor.apply();
//                            return true;
//                        }
//                    }
//            );
//        }
    }
}





