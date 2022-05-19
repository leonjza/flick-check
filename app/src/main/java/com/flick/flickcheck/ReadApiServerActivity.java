package com.flick.flickcheck;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ReadApiServerActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_api_server);

        // Check if we have knowledge of a api_server
        SharedPreferences sharedpreferences = getSharedPreferences(
                getString(R.string.preference_file), Context.MODE_PRIVATE);
        String api_server = (sharedpreferences.getString("api_server", null));

        if (api_server != null) {

            TextView connection_message = (TextView)findViewById(R.id.server_address);
            connection_message.append(api_server);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_read_api_server, menu);
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

        Intent intent = new Intent(this, MainActivity.class);
        EditText editText = (EditText) findViewById(R.id.server_address);
        String server_address = editText.getText().toString();

        // If no value was entered, freak out or something
        if (server_address.isEmpty()) {

            Toast.makeText(ReadApiServerActivity.this,
                    "Error: A value is required!",
                    Toast.LENGTH_LONG).show();
            return;
        }

        SharedPreferences sharedpreferences = getSharedPreferences(
                getString(R.string.preference_file), Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString("api_server", server_address);
        editor.commit();

        Toast.makeText(ReadApiServerActivity.this,
                "Server set to: " + server_address,
                Toast.LENGTH_SHORT).show();

        // Back to main!
        startActivity(intent);
    }
}
