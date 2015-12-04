package com.ryanarifswana.bioflix;


import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.ryanarifswana.bioflix.database.DatabaseHandler;
import com.ryanarifswana.bioflix.database.model.Session;

import java.lang.ref.WeakReference;

public class MSBandService extends Service {
    static MSBandService bandService = null;
    private static BandClient client;
    public final static int MSG_ERROR = 0;
    public final static int MSG_BAND_NOT_REGISTERED = 1;
    public final static int MSG_HR_TICK = 2;
    public final static int MSG_GSR_TICK = 3;
    public final static int MSG_TIMER_UPDATE = 4;

    public final static String BUNDLE_ERROR_TEXT = "err";
    public final static String BUNDLE_HR_HR = "hr";
    public final static String BUNDLE_HR_QUALITY = "quality";
    public final static String BUNDLE_GSR_RESISTANCE = "resistance";
    public final static String BUNDLE_TIMER_TIME = "time";

    private final static int HR_BUFFER = 20;     //buffer before writing to db
    private final static int GSR_BUFFER = 10;

    private static long baseTime;

    private static int[] hrArray;
    private static long[] hrTimeArray;
    private static int hrIndex;

    private static int[] gsrArray;
    private static long[] gsrTimeArray;
    private static int gsrIndex;

    private static long sessionId;
    public static boolean inSession;
    private static String sessionMovieName;
    private static String sessionViewerName;

    private final IBinder mBinder = new LocalBinder();
    private static ResultReceiver resultReceiver;
    private static Bundle hrBundle;
    private static Bundle gsrBundle;
    private static Bundle timerBundle;
    private static Bundle errBundle;
    private static DatabaseHandler db;
    private static Handler handler;
    private static Context baseContext;

    public class LocalBinder extends Binder {
        MSBandService getService() {
            return MSBandService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        log("onBind() called");
        db = new DatabaseHandler(this);
        bandService = this;
        inSession = false;
        hrBundle = new Bundle();
        gsrBundle = new Bundle();
        timerBundle = new Bundle();
        errBundle = new Bundle();
        handler = new Handler();
        baseContext = getBaseContext();
        return mBinder;
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        log("onStartCommand() called");
        if(intent != null) {
            resultReceiver = intent.getParcelableExtra("receiver");
            startHeartRate();
            startGsr();
        }
        return START_STICKY;
    }

//    @Override
//    public void onDestroy() {
//        log("onDestroy() called");
//    }

    public static void startSession(String movieName, String viewerName) {
        log("startSession() called");
        if (!inSession) {
            sessionMovieName = movieName;
            sessionViewerName = viewerName;
            hrArray = new int[HR_BUFFER];
            hrTimeArray = new long[HR_BUFFER];
            gsrArray = new int[GSR_BUFFER];
            gsrTimeArray = new long[GSR_BUFFER];
            hrIndex = 0;
            gsrIndex = 0;
            sessionId = db.newSession(sessionMovieName, sessionViewerName, System.currentTimeMillis());
            baseTime = System.currentTimeMillis();
            inSession = true;
            startTimer();
        }
    }

    /*
    TODO: to be completed
     */
    public static void continueSession() {
        if(!inSession) {
            baseTime = System.currentTimeMillis() - baseTime;
            Log.d("startSession()", "sessionStarted!");
        }
    }

    public static void stopSession() {
        log("stopSession() called");
        inSession = false;
        db.concludeHr(sessionId, hrArray, hrTimeArray, hrIndex);
        db.concludeGsr(sessionId, gsrArray, gsrTimeArray, gsrIndex);
        db.endSession(sessionId, System.currentTimeMillis());
        handler.removeCallbacksAndMessages(null);
    }

    public static void startHeartRate() {
        log("startHeartRate() called");
        new HeartRateSubscriptionTask().execute();
    }

    public static void startRates() {
        log("startRate() called");
        startHeartRate();
        startGsr();
    }

    public static void stopRates() {
        log("stopRate() called");
        if (client != null) {
            try {
                client.getSensorManager().unregisterAllListeners();
            } catch (BandIOException e) {
                log(e.getMessage());
            }
        }
    }

    public static void startGsr() {
        log("startGsr() called");
        new GsrSubscriptionTask().execute();
    }

    public void requestConsent(WeakReference<Activity> reference) {
        Log.d("Consent received", "True!!");
        new HeartRateConsentTask().execute(reference);
    }

    public static void addHr(Bundle bundle) {
        hrArray[hrIndex] = bundle.getInt(BUNDLE_HR_HR);
        hrTimeArray[hrIndex] = getElapsedTime();
        if(hrIndex == HR_BUFFER - 1) {
            db.appendHR(sessionId, hrArray, hrTimeArray);
            hrArray = new int[HR_BUFFER];
            hrTimeArray = new long[HR_BUFFER];
            hrIndex = 0;
        }
        else {
            hrIndex++;
        }
    }

    public static void addGsr(Bundle bundle) {
        gsrArray[gsrIndex] = bundle.getInt(BUNDLE_GSR_RESISTANCE);
        gsrTimeArray[gsrIndex] = getElapsedTime();
        if(gsrIndex == GSR_BUFFER - 1) {
            db.appendGsr(sessionId, gsrArray, gsrTimeArray);
            gsrArray = new int[GSR_BUFFER];
            gsrTimeArray = new long[GSR_BUFFER];
            gsrIndex = 0;
        }
        else {
            gsrIndex++;
        }
    }

    public static void sendError(String error) {
        errBundle.clear();
        errBundle.putString(BUNDLE_ERROR_TEXT, error);
        resultReceiver.send(MSG_ERROR, errBundle);
    }

    public static long getElapsedTime() {
        return System.currentTimeMillis() - baseTime;
    }

    private static void startTimer() {
        handler.postDelayed(new Runnable() {
            public void run() {
                timerBundle.clear();
                timerBundle.putLong(BUNDLE_TIMER_TIME, getElapsedTime());
                resultReceiver.send(MSG_TIMER_UPDATE, timerBundle);
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private static BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
                log("HR tick: " + event.getHeartRate());
                hrBundle.clear();
                hrBundle.putInt(BUNDLE_HR_HR, event.getHeartRate());
                hrBundle.putString(BUNDLE_HR_QUALITY, event.getQuality().toString());
                resultReceiver.send(MSG_HR_TICK, hrBundle);
                if (inSession) addHr(hrBundle);
            }
        }
    };

    private static BandGsrEventListener mGsrEventListener = new BandGsrEventListener() {
        @Override
        public void onBandGsrChanged(final BandGsrEvent event) {
            if (event != null) {
                gsrBundle.clear();
                gsrBundle.putInt(BUNDLE_GSR_RESISTANCE, event.getResistance());
                resultReceiver.send(MSG_GSR_TICK, gsrBundle);
                if(inSession) addGsr(gsrBundle);
            }
        }
    };

    private static boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                sendError("Band isn't paired with your phone.\n");
                return false;
            }
            client = BandClientManager.getInstance().create(baseContext, devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        //sendToSnackbar("Band is connecting...\n");
        return ConnectionState.CONNECTED == client.connect().await();
    }

    private static class GsrSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    int hardwareVersion = Integer.parseInt(client.getHardwareVersion().await());
                    if (hardwareVersion >= 20) {
                        //sendError("Band is connected.");
                        client.getSensorManager().registerGsrEventListener(mGsrEventListener);
                    } else {
                        sendError("The Gsr sensor is not supported with your Band version. Microsoft Band 2 is required.\n");
                    }
                } else {
                    sendError("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                sendError(exceptionMessage);

            } catch (Exception e) {
                sendError(e.getMessage());
            }
            return null;
        }
    }

    private static class HeartRateSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                        client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
                    } else {
                        //not subscribed. do something
                        Bundle bundle = new Bundle();
                        resultReceiver.send(MSG_BAND_NOT_REGISTERED, bundle);
                    }
                } else {
                    sendError("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                sendError(exceptionMessage);

            } catch (Exception e) {
                sendError(e.getMessage());
            }
            return null;
        }
    }

    private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
        @Override
        protected Void doInBackground(WeakReference<Activity>... params) {
            try {
                if (getConnectedBandClient()) {

                    if (params[0].get() != null) {
                        client.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
                            @Override
                            public void userAccepted(boolean consentGiven) {
                                startHeartRate();
                            }
                        });
                    }
                } else {
                    sendError("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                sendError(exceptionMessage);

            } catch (Exception e) {
                sendError(e.getMessage());
            }
            return null;
        }
    }

    private static void log(String log) {
        Log.d("MSBandService", log);
    }
}
