package com.flick.flickcheck;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.net.URL;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;


public class RegisterActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Get the devices UUID
        // From:
        // http://stackoverflow.com/questions/2785485/is-there-a-unique-android-device-id/2853253#2853253
        final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
        String deviceId = deviceUuid.toString();

        SharedPreferences sharedpreferences = getSharedPreferences(
                getString(R.string.preference_file), Context.MODE_PRIVATE);
        String api_server = (sharedpreferences.getString("api_server", null));

        // Check if we have an API token. If not, assume we
        // are not registered
        String auth_token = (sharedpreferences.getString("api_auth_token", "none"));

        if (auth_token.equals("none")) {

            // Redirect to the registration
            Toast.makeText(RegisterActivity.this,
                    "Registering device with API",
                    Toast.LENGTH_SHORT).show();

            // Proceed to registration
            Intent intent = new Intent(getApplicationContext(), DoRegisterActivity.class);
            startActivity(intent);

        }

        // Check if we are registered server side
        new CallAPI().execute("https://" + api_server + "/register/status/" + deviceId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_register, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_secure_close) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class CallAPI extends AsyncTask<String, String, String> {

        // Handle HTTP Requests in Async so that the UI Thread is
        // not blocked.
        @Override
        protected String doInBackground(String... params) {

            String urlString = params[0]; // URL to call
            String registered = "no";

            try {

                URL url = new URL(urlString);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();

                // Create the SSL connection
                TrustManager tm[] = { new PubKeyManager() };

                // Prevent:
                //  java.io.IOException: Hostname '192.168.217.248' was not verified
                urlConnection.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });

                SSLContext sc;
                sc = SSLContext.getInstance("TLS");
                sc.init(null, tm, null);
                urlConnection.setSSLSocketFactory(sc.getSocketFactory());

                // Set some parameters
                urlConnection.setConnectTimeout(5000);
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());

                byte[] response = new byte[1024];

                int bytesRead=0;
                String response_contents = "";
                while ((bytesRead = in.read(response)) != -1) {

                    response_contents = new String(response, 0, bytesRead);
                }

//                Log.e("NETWORK-RESPONSE", response_contents);

                try {

                    JSONObject responseJson = new JSONObject(response_contents);

                    if (responseJson.getString("registered").equals("yes")) {

                        registered = "yes";
                    }

                } catch (JSONException e) {

                    e.printStackTrace();
                }

            } catch (Exception e ) {

                e.printStackTrace();
            }

            return registered;
        }

        protected void onPostExecute(String registered) {

            if (registered.equals("yes")) {

                // Hide the Progressbar
                ProgressBar progress_bar = (ProgressBar)findViewById(R.id.progressBarRegister);
                progress_bar.setVisibility(View.INVISIBLE);

                // Show that the connection failed.
                TextView connection_message = (TextView) findViewById(R.id.registration_text);
                connection_message.setText("Already Registered");

                Intent intent = new Intent(getApplicationContext(), CommandActivity.class);
                startActivity(intent);

            } else {

                Toast.makeText(RegisterActivity.this,
                        "Registering device with API",
                        Toast.LENGTH_SHORT).show();

                // Proceed to registration
                Intent intent = new Intent(getApplicationContext(), DoRegisterActivity.class);
                startActivity(intent);
            }
        }
    }
}
