package com.example.mul;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class topUp extends AppCompatActivity {
    private String TAG = topUp.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_up);
    }

    public void onClickAdd(View view) {

        EditText dollarsEdit = findViewById(R.id.dollars);

        int cents = Integer.parseInt(dollarsEdit.getText().toString()) * 100;

        MulAPI.post_balance(cents, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "failed using internets");
                finish();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String msg;
                if (response.isSuccessful()) {
                    msg = "Added to balance";
                } else {
                    msg = "Failed adding money";
                }

                topUp.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    }
                });
                finish();
            }
        });
    }
}
