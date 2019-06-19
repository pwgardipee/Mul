package com.example.mul;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.google.android.gms.common.internal.safeparcel.SafeParcelable.NULL;

public class Session_Limits extends AppCompatActivity {
    private Spinner spinner;
    private ArrayAdapter<CharSequence> mAdapter;
    private static final String TAG = "Session_Limits";
    private static String MB_or_GB;
    private static String data_limit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.session_limits);

        spinner = (Spinner) findViewById(R.id.spinner);

        // Create an ArrayAdapter using the string array and a default spinner layout
        mAdapter = ArrayAdapter.createFromResource(this, R.array.ui_data_spinner_entries, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setAdapter(mAdapter);

        loadUserData();

        // Define the listener interface
        AdapterView.OnItemSelectedListener mListener = new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {
                // An item was selected. You can retrieve the selected item using
                MB_or_GB = parent.getItemAtPosition(pos).toString();
                mAdapter.notifyDataSetChanged();
            }

            public void onNothingSelected(AdapterView parent) {
                // Do nothing.
            }
        };

        // Get the ListView and wired the listener
        spinner.setOnItemSelectedListener(mListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // load all information from the screen into a "shared preferences"
        // using private helper function
        loadUserData();
    }

    public void onClickSave(View v){
        saveUserData();
        finish();
    }

    // ****************** private helper functions ***************************//

    // load the user data from shared preferences if there is no data make sure
    // that we set it to something reasonable
    private void loadUserData() {
        // We can also use log.d to print to the LogCat
        Log.d(TAG, "loadUserData()");

        // Load and update all profile views

        // Get the shared preferences - create or retrieve the activity
        // preference object
        String mKey = getString(R.string.preference);
        SharedPreferences mPrefs = getSharedPreferences(mKey, MODE_PRIVATE);

        // Load the list
        mKey = getString(R.string.preference_key);
        String saved_info = mPrefs.getString(mKey, null);

        // check if list is null
        if(saved_info == null) {
            MB_or_GB = "MB";
            data_limit = "500";
        }
        else{
            String[] data_vals = saved_info.split(" ");

            MB_or_GB = data_vals[1];
            data_limit = data_vals[0];
        }

        EditText data_limit_text = (EditText) findViewById(R.id.limit);
        data_limit_text.setText(data_limit);

        int spinnerPosition = mAdapter.getPosition(MB_or_GB);
        spinner.setSelection(spinnerPosition);
    }

    // load the user data from shared preferences if there is no data make sure
    // that we set it to something reasonable
    private void saveUserData() {
        Log.d(TAG, "saveUserData()");

        // get data value
        EditText data_limit_text = (EditText) findViewById(R.id.limit);
        data_limit = data_limit_text.getText().toString();

        // check if list is null
        if(data_limit.isEmpty()) {
            MB_or_GB = "MB";
            data_limit = "500";
        }

        String saved_info = data_limit + " " + MB_or_GB;

        MulAPI.post_limit(saved_info, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "failed internetting somehow");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String msg;
                if (response.isSuccessful()) {
                    msg = "Updated monthly limit!";

                    // Only save shared preference update if we were successful updating database
                    String mKey = getString(R.string.preference);
                    SharedPreferences mPrefs = getSharedPreferences(mKey, MODE_PRIVATE);

                    SharedPreferences.Editor mEditor = mPrefs.edit();
                    mEditor.clear();
                    // Save list information
                    mKey = getString(R.string.preference_key);
                    mEditor.putString(mKey, saved_info);

                    // Commit all the changes into the shared preference
                    mEditor.commit();
                } else {
                    msg = "Couldn't update limit";
                }
                Session_Limits.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
    }
}
