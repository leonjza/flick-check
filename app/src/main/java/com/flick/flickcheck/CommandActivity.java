package com.flick.flickcheck;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Properties;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;


public class CommandActivity extends ActionBarActivity {

    String integrity_check = "YFhaRBMNFRQDFxJEFlFDExIDVUMGEhcLAUNFBVdWQGFeXBIVWEsZWQ==";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_command, menu);
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

    public void doCmd(View view) {

        Toast.makeText(CommandActivity.this,
                "Running command: " + view.getTag().toString(),
                Toast.LENGTH_SHORT).show();

        // Base64 Encode the command to run
        String base64_command = Base64.encodeToString(
                view.getTag().toString().getBytes(), Base64.DEFAULT);

//        Log.e("Base64-Encoded-Command", base64_command);

        // Get the devices UUID
        // From:
        //  http://stackoverflow.com/questions/2785485/is-there-a-unique-android-device-id/2853253#2853253
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
        String api_auth_token = (sharedpreferences.getString("api_auth_token", null));

        new CallAPI().execute(
                "https://" + api_server + "/do/cmd/" + base64_command, deviceId, api_auth_token);
    }

    private class CallAPI extends AsyncTask<String, String, String> {

        // Handle HTTP Requests in Async so that the UI Thread is
        // not blocked.
        @Override
        protected String doInBackground(String... params) {

            String urlString = params[0];   // URL with base64 encode command to call
            String deviceId = params[1];    // DeviceID used for auth headers
            String api_auth_token = params[2];  // Auth token
            String cmd_output = "";

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
                urlConnection.setRequestProperty("X-UUID", deviceId);
                urlConnection.setRequestProperty("X-Token", api_auth_token);

                BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());

                byte[] response = new byte[1024];

                int bytesRead = 0;
                String response_contents = "";
                String chunk = "";
                while ((bytesRead = in.read(response)) != -1) {

                    chunk = new String(response, 0, bytesRead);
                    response_contents = response_contents + chunk;
                }

//                Log.e("NETWORK-RESPONSE", response_contents);

                try {

                    JSONObject responseJson = new JSONObject(response_contents);

                    if (responseJson.getString("status").equals("ok")) {

                        cmd_output = responseJson.getString("output");
                    } else if (responseJson.getString("status").equals("error")) {

                        cmd_output = "Command error: " + responseJson.getString("output");
                    }

                } catch (JSONException e) {

                    e.printStackTrace();
                }

            } catch (Exception e ) {

                e.printStackTrace();
            }

            return cmd_output;
        }

        protected void onPostExecute(String cmd_output) {

            TextView cmd_output_field = (TextView)findViewById(R.id.cmd_output);
            cmd_output_field.setText(cmd_output);

        }
    }

    public void doSSHCmd(View view) {

        Switch s = (Switch) findViewById(R.id.use_ssh);

        // Enable SSH if the toggle is set
        if (s.isChecked()) {

            Toast.makeText(CommandActivity.this,
                    "Enabling Secure Access...",
                    Toast.LENGTH_SHORT).show();

            SharedPreferences sharedpreferences = getSharedPreferences(
                    getString(R.string.preference_file), Context.MODE_PRIVATE);

            String api_server = sharedpreferences.getString("api_server", null);

            new SSHCommand().execute(api_server);

        } else {

            Toast.makeText(CommandActivity.this,
                    "Disabling Secure Access...",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // From:
    //  https://github.com/KyleBanks/XOREncryption/blob/master/Java%20(Android%20compatible)/XOREncryption.java
    private static String validate(String input) {

        char[] key = {'T', 'h', 'i', 's', ' ', 'i', 's',
                ' ', 'a', ' ', 's', 'u', 'p', 'e', 'r',
                ' ', 's', 'e', 'c', 'r', 'e', 't', ' ',
                'm', 'e', 's', 's', 'a', 'g', 'e', '!'};

        StringBuilder output = new StringBuilder();

        for(int i = 0; i < input.length(); i++) {
            output.append((char) (input.charAt(i) ^ key[i % key.length]));
        }

        return output.toString();
    }

    private class SSHCommand extends AsyncTask<String, String, String> {

        // Handle SSH Commands in Async so that the UI Thread is
        // not blocked.
        @Override
        protected String doInBackground(String... params) {

            String server = params[0];   // SSH Server to call

            byte[] data = Base64.decode(integrity_check, Base64.DEFAULT);
            String text = new String(data);

            try {

                JSch jsch = new JSch();
                Session session = jsch.getSession(
                        "robin", server, 22);
                session.setPassword(validate(text));

                // Avoid asking for key confirmation
                Properties prop = new Properties();
                prop.put("StrictHostKeyChecking", "no");
                session.setConfig(prop);

                session.connect();

                // SSH Channel
                ChannelExec channelssh = (ChannelExec)
                        session.openChannel("exec");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                channelssh.setOutputStream(baos);

                // Execute command
                channelssh.setCommand("/usr/bin/id");
                channelssh.connect();
                channelssh.disconnect();

                return "Secure Access Enabled";

            } catch (Exception e) {

                e.printStackTrace();
            }

            return "Secure Access Failed";
        }

        protected void onPostExecute(String cmd_output) {

            TextView cmd_output_field = (TextView)findViewById(R.id.cmd_output);
            cmd_output_field.setText(cmd_output);
        }
    }
}
