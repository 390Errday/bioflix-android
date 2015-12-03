package com.ryanarifswana.bioflix;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.microsoft.band.BandException;
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
    private Button startSessionButton;
    private ImageView heartIcon;
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
        heartIcon = (ImageView) findViewById(R.id.heartIcon);
        warningText.setVisibility(View.INVISIBLE);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(movieName);
        setSupportActionBar(toolbar);

        timerFormat = new SimpleDateFormat("HH:mm:ss");
        timerFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        bindToService();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ///
        ImageView colorFrom = (ImageView) findViewById(R.id.heartIcon);
        ImageView colorTo = (ImageView) findViewById(R.id.heartIcon2);
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                //textView.setBackgroundColor((Integer) animator.getAnimatedValue());
            }

        });



    }

    @Override
    public void onPause() {
        Log.d("onPause", "called");
        if(!MSBandService.inSession) {
            MSBandService.stopRates();
            this.unbindService(bandConnection);
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        if(!MSBandService.inSession) {
            bindToService();
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Log.d("onDestroyed", "called");
        MSBandService.stopRates();
        super.onDestroy();
        finish();
    }

    private void bindToService() {
        if(!serviceBound) {
            Log.d("binding", "binding");
            resultsReceiver = new BandResultsReceiver(null);
            Intent intent = new Intent(this, MSBandService.class);
            intent.putExtra("receiver", resultsReceiver);
            intent.putExtra("movieName", movieName);
            intent.putExtra("viewerName", viewerName);
            this.bindService(intent, bandConnection, Context.BIND_AUTO_CREATE);
        }
    }

    //onClickListener for startMovieButton
    public void startButton(View view) {
        if(MSBandService.inSession) {
            MSBandService.stopSession();
            startSessionButton.setText("Start Session");
        }
        else {
            MSBandService.startSession();
            startSessionButton.setText("Stop Session");
        }

    }

    public void doNotLocked() {
        warningText.setText("Acquiring sensors...");
        warningText.setVisibility(View.VISIBLE);
        startSessionButton.setClickable(false);
    }

    public void doLocked() {
        warningText.setVisibility(View.INVISIBLE);
        startSessionButton.setClickable(true);
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

//    private int calculateFadeOutTime(int hr) {
//        return (60000 - (fadeInTime * 60)) / hr;
//    }

    class UpdateGSR implements Runnable {
        int updatedGSR;

        public UpdateGSR(int updatedGSR) {
            this.updatedGSR = updatedGSR;
        }
        public void run() {
            gsrView.setText((hrLocked) ? ""+updatedGSR : "...");
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
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MSBandService.LocalBinder binder = (MSBandService.LocalBinder) service;
            bandService = binder.getService();
            bandService.startHeartRate();
            bandService.startGsr();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d("CURRENT SESSION:", "UNBOUND FROM SERVICE");
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
                case MSBandService.MSG_BAND_NOT_REGISTERED:
                    showRegisterBandSnackbar();
                    break;
                case MSBandService.MSG_TIMER_UPDATE:
                    runOnUiThread(new UpdateTimer(resultData.getLong(MSBandService.BUNDLE_TIMER_TIME)));
                    break;
                case MSBandService.MSG_ERROR:
                    Log.d("Error: ", ""+resultData.getString(MSBandService.BUNDLE_ERROR_TEXT));
                    break;
            }
        }
    }
}
