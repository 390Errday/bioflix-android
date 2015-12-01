package com.ryanarifswana.bioflix;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.ryanarifswana.bioflix.database.DatabaseHandler;
import com.ryanarifswana.bioflix.database.model.Session;

/**
 * Created by ariftopcu on 11/26/15.
 */
public class NewSessionActivity extends AppCompatActivity {

    EditText movieNameText;
    EditText viewerNameText;
    Activity newSessionActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_session);
        newSessionActivity = this;
        setupToolbar();
        movieNameText = (EditText) findViewById(R.id.movieName);
        viewerNameText = (EditText) findViewById(R.id.viewerName);
    }

    @Override
    public boolean onSupportNavigateUp() {
        //check to see if user entered input
        if(movieNameText.getText().toString().length() > 1
                || viewerNameText.getText().toString().length() > 1) {
            discardDialog();
        }
        else {
            finish();
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_session, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.new_session_save) {
            onSaveButton();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.newSessionToolbar);
        setSupportActionBar(toolbar);
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
    }

    private void onSaveButton() {
        String movieName = movieNameText.getText().toString();
        String viewerName = viewerNameText.getText().toString();
        boolean error = false;
        if(movieName.trim().equals("")) {
            error = true;
            movieNameText.setError("Movie name is required.");
        }
        if(viewerName.trim().equals("")) {
            error = true;
            viewerNameText.setError("Viewer name is required.");
        }
        if(!error) {
            Intent intent = new Intent(newSessionActivity, CurrentSessionActivity.class);
            intent.putExtra("movieName", movieName);
            intent.putExtra("viewerName", viewerName);
            startActivity(intent);
            finish();
//            Session newSession = new Session();
//            newSession.setMovieName(movieName);
//            newSession.setViewerName(viewerName);
//            db.newSession(newSession);
        }

    }
    private void discardDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("Discard new session?");
        alertDialog.setCancelable(true);
        alertDialog.setPositiveButton("Discard",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.i("discardDialog", "Clicked discard.");
                        dialog.cancel();
                        finish(); // close this activity as oppose to navigating up
                    }
                });
        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.i("discardDialog", "Clicked cancel.");
                        dialog.cancel();
                    }
                });

        AlertDialog alert11 = alertDialog.create();
        alert11.show();
    }

}
