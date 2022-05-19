package com.flick.flickcheck;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

/**
 * Created by leon.jacobs on 6/24/15.
 */
public class CallApi extends AsyncTask<String, String, String> {

    // Handle HTTP Requests in Async so that the UI Thread is
    // not blocked.
    @Override
    protected String doInBackground(String... params) {

        String urlString = params[0]; // URL to call
        String responseString = "";

        try {

            URL url = new URL(urlString);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();

            // Create the SSL connection
            TrustManager tm[] = { new PubKeyManager() };

            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }

            });

            SSLContext sc;
            sc = SSLContext.getInstance("TLS");
//                sc.init(null, null, new java.security.SecureRandom());
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

//            Log.e("NETWORK-RESPONSE", response_contents);

            try {

                JSONObject responseJson = new JSONObject(response_contents);

                // Check that the response contains "pong"
                if (responseJson.getString("response").equals("pong")) {

                    responseString = "true";
                }

            } catch (JSONException e) {

                e.printStackTrace();
            }

        } catch (Exception e ) {

            e.printStackTrace();
        }

        return responseString;
    }

    protected void onPostExecute(String connected) {
        // Nothing. This should probably be overriden.
    }
}

