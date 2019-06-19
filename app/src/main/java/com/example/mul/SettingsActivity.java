package com.example.mul;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

    private EditText ssidEdit;
    private EditText passwordEdit;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ssidEdit = findViewById(R.id.ssidEdit);
        passwordEdit = findViewById(R.id.passwordEdit);

        String ssid = readSetting("ssid");
        String password = readSetting("password");
        ssidEdit.setText(ssid);
        passwordEdit.setText(password);
    }

    public void onClickSubmit(View view) {
        String ssid = ssidEdit.getText().toString();
        String password = passwordEdit.getText().toString();

        //ssid cannot be blank
        if(ssid.isEmpty()){
            ssidEdit.setError("SSID cannot be empty.");
            ssidEdit.requestFocus();
            return;
        }

        //password cannot be blank
        if(password.isEmpty()){
            passwordEdit.setError("Password cannot be empty.");
            passwordEdit.requestFocus();
            return;
        }

        //Remove focus from both edit texts
        passwordEdit.clearFocus();
        ssidEdit.clearFocus();

        //Hide keyboard
        InputMethodManager inputManager = (InputMethodManager) getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(view.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);


        Toast.makeText(this, String.format("%s and %s", ssid, password), Toast.LENGTH_SHORT).show();
        writeSetting("ssid", ssid);
        writeSetting("password", password);
    }

    public void writeSetting(String key, String data) {
        SharedPreferences settings = getApplicationContext().getSharedPreferences(SettingsActivity.class.getSimpleName(), 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(key, data);
        editor.commit();
    }

    public String readSetting(String key) {
        SharedPreferences settings = getApplicationContext().getSharedPreferences(SettingsActivity.class.getSimpleName(), 0);
        return settings.getString(key, "");
    }
}
