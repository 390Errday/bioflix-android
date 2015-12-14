package com.ryanarifswana.bioflix;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.microsoft.band.sensors.HeartRateQuality;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class CurrentSessionActivity extends AppCompatActivity {
    private String movieName;
    private String viewerName;
    private TextView hrRateView;
    private TextView gsrView;
    private TextView timer;
    private TextView warningText;
    private TextView liveUrlText;
    private Button startSessionButton;
    private Button stopSessionButton;
    private Menu menu;
    MSBandService bandService;
    BandResultsReceiver resultsReceiver;
    boolean serviceBound = false;
    boolean hrLocked = false;
    CurrentSessionActivity currentSessionActivity;
    CoordinatorLayout mainLayout;
    SimpleDateFormat timerFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_session);
        currentSessionActivity = this;
        mainLayout = (CoordinatorLayout) findViewById(R.id.currentSessionCoordinatorLayout);
        Intent intent = getIntent();

        movieName = intent.getStringExtra("movieName");
        viewerName = intent.getStringExtra("viewerName");
        hrRateView = (TextView) findViewById(R.id.hrText);
        gsrView = (TextView) findViewById(R.id.gsrText);
        timer = (TextView) findViewById(R.id.timer);
        warningText = (TextView) findViewById(R.id.warningText);
        startSessionButton = (Button) findViewById(R.id.startButton);
        stopSessionButton = (Button) findViewById(R.id.stopButton);
        liveUrlText = (TextView) findViewById(R.id.liveUrl);

        liveUrlText.setVisibility(View.INVISIBLE);
        warningText.setVisibility(View.INVISIBLE);
        stopSessionButton.setClickable(false);
        startSessionButton.setClickable(false);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(movieName);
        setSupportActionBar(toolbar);

        timerFormat = new SimpleDateFormat("HH:mm:ss");
        timerFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        bindToService();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_current_session, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_startLivePreview) {
            MenuItem liveMenuItem = menu.findItem(R.id.action_startLivePreview);
            if(!MSBandService.livePreviewOn) {
                MSBandService.startLivePreview();
                liveMenuItem.setTitle(R.string.action_stopLivePreview);
            } else {
                MSBandService.stopLivePreview();
                liveUrlText.setVisibility(View.INVISIBLE);
                liveMenuItem.setTitle(R.string.action_startLivePreview);

            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        log("onPause() called");
        if (serviceBound) {
            MSBandService.stopRates();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        log("onResume() called");
        super.onResume();
        if (serviceBound) {
            MSBandService.startRates();
        }
    }

//    @Override
//    protected void onDestroy() {
//        log("onDestroy() called");
//        super.onDestroy();
//        finish();
//    }

    private void bindToService() {
        log("bindToService() called");
        if (!serviceBound) {
            log("Binding to service...");
            resultsReceiver = new BandResultsReceiver(null);
            Intent intent = new Intent(this, MSBandService.class);
            this.bindService(intent, bandConnection, Context.BIND_AUTO_CREATE);
        }
    }

    //onClickListener for startMovieButton
    public void startButton(View view) {
        log("startButton() called");
        if (MSBandService.STATE == MSBandService.SessionState.IN_SESSION) {
            log("pausing session...");
            MSBandService.pauseSession();
            startSessionButton.setText("Continue Session");
        } else if(MSBandService.STATE == MSBandService.SessionState.SESSION_PAUSED) {
            log("continuing session");
            MSBandService.continueSession();
            startSessionButton.setText("Pause Session");
        } else {
            log("starting session...");
            MSBandService.startSession(movieName, viewerName);
            enableStopButton();
            startSessionButton.setText("Pause Session");
        }
    }

    //onClickListener for startMovieButton
    public void stopButton(View view) {
        log("stopButton() called");
        if (MSBandService.STATE == MSBandService.SessionState.IN_SESSION) {
            log("stopping session...");
            MSBandService.stopSession();
            disableStopButton();
            //TODO: Go to different activity
        }
    }

    public void enableStartButton() {
        startSessionButton.setBackgroundResource(R.color.colorPrimary);
        startSessionButton.setClickable(true);
    }

    public void disableStartButton() {
        startSessionButton.setBackgroundResource(R.color.colorPrimaryLight);
        startSessionButton.setClickable(false);
    }

    public void enableStopButton() {
        stopSessionButton.setBackgroundResource(R.color.colorPrimary);
        stopSessionButton.setClickable(true);
    }

    public void disableStopButton() {
        stopSessionButton.setBackgroundResource(R.color.colorPrimaryLight);
        stopSessionButton.setClickable(false);
    }

    public void setLiveUrl(String url) {
        liveUrlText.setText("Your Live Url: \n" + url);
        liveUrlText.setVisibility(View.VISIBLE);
    }

    public void doNotLocked() {
        warningText.setText("Acquiring sensors...");
        warningText.setVisibility(View.VISIBLE);
        if(MSBandService.STATE == MSBandService.SessionState.IN_SESSION) {
            disableStartButton();
        }
    }

    public void doLocked() {
        warningText.setVisibility(View.INVISIBLE);
        startSessionButton.setClickable(true);
        if (MSBandService.STATE == MSBandService.SessionState.SESSION_STOPPED) {
            enableStartButton();
        }
    }

    class UpdateHr implements Runnable {
        int hr;
        String quality;

        public UpdateHr(Bundle hrBundle) {
            this.hr = hrBundle.getInt(MSBandService.BUNDLE_HR_HR);
            this.quality = hrBundle.getString(MSBandService.BUNDLE_HR_QUALITY);
        }
        public void run() {
            if(quality.equals(HeartRateQuality.ACQUIRING.toString())) {
                hrLocked = false;
                doNotLocked();
            }
            else if(quality.equals("LOCKED") && !hrLocked) {
                hrLocked = true;
                doLocked();
            }
            if(hrLocked) {
                hrRateView.setText(""+hr);
            }
            else{
                hrRateView.setText("...");
            }

        }
    }

    class UpdateGSR implements Runnable {
        int updatedGSR;

        public UpdateGSR(int updatedGSR) {
            this.updatedGSR = updatedGSR;
        }
        public void run() {
            gsrView.setText((hrLocked) ? ""+updatedGSR : "...");
        }
    }

    class UpdateSkinTemp implements Runnable {
        double temp;

        public UpdateSkinTemp(Bundle bundle) {
            this.temp = bundle.getDouble(MSBandService.BUNDLE_SKIN_TEMP);
        }
        public void run() {
            Log.d("Temp:", "" + temp);

        }
    }

    class UpdateTimer implements Runnable {
        long updatedTime;

        public UpdateTimer(long updatedTime) {
            this.updatedTime = updatedTime;
        }
        public void run() {
            timer.setText(timerFormat.format(new Date(updatedTime)));
        }
    }

    public void showRegisterBandSnackbar() {
        Snackbar snackbar = Snackbar
            .make(mainLayout, "You haven't given consent to access heart rate yet.", Snackbar.LENGTH_LONG)
            .setAction("Give Consent", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final WeakReference<Activity> reference = new WeakReference<Activity>(currentSessionActivity);
                    bandService.requestConsent(reference);
                }
            });
        snackbar.show();
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection bandConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            log("Bound to service.");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MSBandService.LocalBinder binder = (MSBandService.LocalBinder) service;
            bandService = binder.getService();
            Intent intent = new Intent(currentSessionActivity, MSBandService.class);
            intent.putExtra("receiver", resultsReceiver);
            log("Starting service...");
            bandService.startService(intent);
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            log("Unbound from service");
            serviceBound = false;
        }
    };

    // Recieves data from MSBandService
    class BandResultsReceiver extends ResultReceiver {
        public BandResultsReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            switch (resultCode) {
                case MSBandService.MSG_HR_TICK:
                    runOnUiThread(new UpdateHr(resultData));
                    break;
                case MSBandService.MSG_GSR_TICK:
                    runOnUiThread(new UpdateGSR(resultData.getInt(MSBandService.BUNDLE_GSR_RESISTANCE)));
                    break;
                case MSBandService.MSG_SKIN_TEMP_TICK:
                    runOnUiThread(new UpdateSkinTemp(resultData));
                    break;
                case MSBandService.MSG_BAND_NOT_REGISTERED:
                    showRegisterBandSnackbar();
                    break;
                case MSBandService.MSG_TIMER_UPDATE:
                    runOnUiThread(new UpdateTimer(resultData.getLong(MSBandService.BUNDLE_TIME)));
                    break;
                case MSBandService.MSG_LIVE_URL:
                    setLiveUrl(resultData.getString(MSBandService.BUNDLE_LIVE_URL));
                    break;
                case MSBandService.MSG_ERROR:
                    Log.d("Error: ", ""+resultData.getString(MSBandService.BUNDLE_ERROR_TEXT));
                    break;
            }
        }
    }

    private void log(String log) {
        Log.d("CurrentSessionActivity", log);
    }
}
