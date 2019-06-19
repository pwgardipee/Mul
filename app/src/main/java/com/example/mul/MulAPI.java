package com.example.mul;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextPaint;
import android.util.Log;

import com.android.dx.command.Main;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MulAPI {
    private static String TAG = MulAPI.class.getSimpleName();
    private static String base_url = "https://80pslhour0.execute-api.us-east-1.amazonaws.com/dev";
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");


//    private static String getUid(Context c) {
//        return Settings.Secure.getString(c.getContentResolver(),
//                Settings.Secure.ANDROID_ID);
//    }

    private static void post(String path, String json, Callback c) {
        RequestBody body = RequestBody.create(JSON, json);
        OkHttpClient client = new OkHttpClient();
        String url = MulAPI.base_url + path;
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(c);
    }

    private static void get(String path, Callback c) {
        OkHttpClient client = new OkHttpClient();
        String url = MulAPI.base_url + path;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(c);
    }


    // used by client and provider
    public  static void get_user(Callback c){
//        String uid = getUid(context);
        String uid = MainActivity.IMEINumber;
        String path = "/user/" + uid;
        Log.d(TAG, "requesting user");
        get(path, c);
    }

    // used by provider
    public static void post_limit(String limit, Callback c){
//        String uid = getUid(context);
        String uid = MainActivity.IMEINumber;
        String path = "/user/" + uid + "/limit";
        String[] data_values = limit.split(" ");
        int data_limit;

        if(data_values[1].equals("MB"))
            data_limit = Integer.parseInt(data_values[0]) * 1024;
        else
            data_limit = Integer.parseInt(data_values[0]) * 1024 * 1024;

        JSONObject obj = new JSONObject();
        try {
            obj.put("limit", new Integer(data_limit));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        post(path, obj.toString(), c);
    }

    // used by client
    public static void post_balance(int balance, Callback c){
        //        String uid = getUid(context);
        String uid = MainActivity.IMEINumber;
        String path = "/user/" + uid + "/balance";

        JSONObject obj = new JSONObject();
        try {
            obj.put("balance", new Integer(balance));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        post(path, obj.toString(), c);
    }

    // used by client
    public static void post_mulchunk(Callback c){
        Log.i(TAG, "requesting mulchunk");
        //        String uid = getUid(context);
        String uid = MainActivity.IMEINumber;
        String path = "/user/" + uid + "/mulchunk";

        JSONObject obj = new JSONObject();
        try {
            obj.put("provider_id", ClientActivity.IMEI_Provider);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        post(path, obj.toString(), c);
    }
}
