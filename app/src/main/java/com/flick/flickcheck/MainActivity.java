package com.flick.flickcheck;

import java.io.BufferedInputStream;
import java.net.URL;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if we have knowledge of a api_server
        SharedPreferences sharedpreferences = getSharedPreferences(
                getString(R.string.preference_file), Context.MODE_PRIVATE);
        String api_server = (sharedpreferences.getString("api_server", "none"));

        if (api_server.equals("none")) {

            // Redirect to an activity that can save us the api_server
            Intent intent = new Intent(this, ReadApiServerActivity.class);
            startActivity(intent);

        } else {

            TextView connection_message = (TextView)findViewById(R.id.connectionMessage);
            connection_message.append(" to " + api_server);
        }

        // Check if we can 'ping' the API Server
        new CallAPI().execute("https://" + api_server + "/ping");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

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

    public void setServer(View view) {

        // Redirect to an activity that can save us the api_server
        Intent intent = new Intent(this, ReadApiServerActivity.class);
        startActivity(intent);
    }

    private class CallAPI extends AsyncTask<String, String, String> {

        // Handle HTTP Requests in Async so that the UI Thread is
        // not blocked.
        @Override
        protected String doInBackground(String... params) {

            String urlString = params[0]; // URL to call
            String resultToDisplay = "";
            String connected = "false";

            try {

                URL url = new URL(urlString);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();

                // Create a Trustmanager Instance that will check the pin
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

                    // Check that the response contains "pong"
                    if (responseJson.getString("response").equals("pong")) {

                        connected = "true";
                    }

                } catch (JSONException e) {

                    e.printStackTrace();
                }

            } catch (Exception e ) {

               e.printStackTrace();
            }

            return connected;
        }

        protected void onPostExecute(String connected) {

            if (connected.equals("true")) {

                // Proceed to checking the registration status
                Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivity(intent);

            } else {

                // Hide the Progressbar
                ProgressBar progress_bar = (ProgressBar)findViewById(R.id.progressBar);
                progress_bar.setVisibility(View.INVISIBLE);

                // Show that the connection failed.
                TextView connection_message = (TextView) findViewById(R.id.connectionMessage);
                connection_message.setText("Connection failed.");
            }
        }
    }
}
