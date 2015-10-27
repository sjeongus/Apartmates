package com.cs130.apartmates.activities;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.cs130.apartmates.R;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    public void onLoginClick(View v) {
        String url = "https://api.venmo.com/v1/oauth/authorize?client_id=3003&scope=make_payments%20access_profile%20access_email%20access_phone%20access_balance&response_type=code";
        final String loginEndpoint = "http://backend-apartmates.rhcloud.com/api/login";

        final Dialog diag = new Dialog(this);
        diag.setContentView(R.layout.dialog_webview);
        WebView webView = (WebView) diag.findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(url);
        webView.setWebViewClient(new WebViewClient() {
            boolean authComplete = false;

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (url.contains("?code=") && !authComplete) {
                    Uri uri = Uri.parse(url);
                    String code = uri.getQueryParameter("code");
                    System.err.println("Code is " + code);
                    diag.cancel();
                    new LoginTask().execute(loginEndpoint, code);
                }
            }
        });
        diag.show();

    }

    private class LoginTask extends AsyncTask<String, String, JSONObject> {
        private HttpURLConnection conn;
        private URL url;
        private OutputStream out;
        private InputStream in;

        @Override
        protected JSONObject doInBackground(String... args) {
            try {
                url = new URL(args[0]);
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("code", args[1]);

                out = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                System.err.println(jsonObject.toString());
                writer.write(jsonObject.toString());
                writer.flush();
                writer.close();
                out.close();

                conn.connect();

                int status = conn.getResponseCode();

                if (status >= 400) {
                    in = conn.getErrorStream();
                } else {
                    in = conn.getInputStream();
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder result = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null) {
                    result.append(line);
                }
                in.close();
                System.err.println(result.toString());
                return new JSONObject(result.toString());
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                conn.disconnect();
            }
            return null;
        }

        @Override
        public void onPostExecute(JSONObject result) {
            try {
                if (result == null || result.getString("status").equals("failed")) {
                    Snackbar.make(findViewById(R.id.login_fragment), R.string.login_error, Snackbar.LENGTH_LONG)
                            .show();
                } else {
                    // Move user to MainActivity
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

}
