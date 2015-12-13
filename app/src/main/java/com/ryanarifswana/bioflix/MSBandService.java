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
import android.util.Log;

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
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.ryanarifswana.bioflix.database.DatabaseHandler;
import com.ryanarifswana.bioflix.database.model.Session;

import java.lang.ref.WeakReference;

public class MSBandService extends Service {
    static MSBandService bandService = null;
    private static ServerComm apiClient;
    private static BandClient client;
    public final static int MSG_ERROR = 0;
    public final static int MSG_BAND_NOT_REGISTERED = 1;
    public final static int MSG_HR_TICK = 2;
    public final static int MSG_GSR_TICK = 3;
    public final static int MSG_SKIN_TEMP_TICK = 4;
    public final static int MSG_TIMER_UPDATE = 5;
    public final static int MSG_LIVE_URL = 6;

    public final static String BUNDLE_ERROR_TEXT = "err";
    public final static String BUNDLE_HR_HR = "hr";
    public final static String BUNDLE_HR_QUALITY = "quality";
    public final static String BUNDLE_GSR_RESISTANCE = "resistance";
    public final static String BUNDLE_SKIN_TEMP = "skintemp";
    public final static String BUNDLE_TIME = "time";
    public final static String BUNDLE_LIVE_URL = "time";

    public static SessionState STATE;

    private final static int HR_BUFFER = 20;     //buffer before writing to db
    private final static int GSR_BUFFER = 10;
    private final static int SKIN_BUFFER = 10;

    private static long baseTime;
    private static long pauseTime;

    private static int[] hrArray;
    private static long[] hrTimeArray;
    private static int hrIndex;

    private static int[] gsrArray;
    private static long[] gsrTimeArray;
    private static int gsrIndex;

    private static double[] skinTempArray;
    private static long[] skinTempTimeArray;
    private static int skinTempIndex;

    private static long sessionId;
    public static boolean livePreviewOn;
    public static boolean hrLocked;
    public static boolean ratesOn;
    private static String sessionMovieName;
    private static String sessionViewerName;

    private final IBinder mBinder = new LocalBinder();
    private static ResultReceiver resultReceiver;
    private static Bundle hrBundle;
    private static Bundle gsrBundle;
    private static Bundle skinTempBundle;
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
        baseTime = 0;
        pauseTime = 0;
        STATE = SessionState.SESSION_STOPPED;
        livePreviewOn = false;
        hrLocked = false;
        ratesOn = false;
        hrBundle = new Bundle();
        gsrBundle = new Bundle();
        skinTempBundle = new Bundle();
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
            startSkinTemp();
        }
        return START_STICKY;
    }

    public enum SessionState{
        IN_SESSION, SESSION_PAUSED, SESSION_STOPPED
    }

//    @Override
//    public void onDestroy() {
//        log("onDestroy() called");
//    }

    public static void startSession(String movieName, String viewerName) {
        log("startSession() called");
        if (STATE == SessionState.SESSION_STOPPED) {
            log("starting new session...");
            sessionMovieName = movieName;
            sessionViewerName = viewerName;
            hrArray = new int[HR_BUFFER];
            hrTimeArray = new long[HR_BUFFER];
            gsrArray = new int[GSR_BUFFER];
            gsrTimeArray = new long[GSR_BUFFER];
            skinTempArray = new double[SKIN_BUFFER];
            skinTempTimeArray = new long[SKIN_BUFFER];
            hrIndex = 0;
            gsrIndex = 0;
            skinTempIndex = 0;
            sessionId = db.newSession(sessionMovieName, sessionViewerName, System.currentTimeMillis());
            baseTime = System.currentTimeMillis();
            STATE = SessionState.IN_SESSION;
            startTimer();
        }
    }

    public static void pauseSession() {
        log("pauseSession() called");
        if(STATE == SessionState.IN_SESSION) {
            log("pausing session...");
            pauseTime = getElapsedTime();
            stopTimer();
            STATE = SessionState.SESSION_PAUSED;
        }
    }

    public static void continueSession() {
        log("continueSession() called");
        if (STATE == SessionState.SESSION_PAUSED) {
            log("continuing session...");
            baseTime = System.currentTimeMillis() - pauseTime;
            startTimer();
            STATE = SessionState.IN_SESSION;
        }
    }

    public static void stopSession() {
        log("stopSession() called");
        if(STATE == SessionState.IN_SESSION) {
            log("stopping session...");
            STATE = SessionState.SESSION_STOPPED;
            db.concludeHr(sessionId, hrArray, hrTimeArray, hrIndex);
            db.concludeGsr(sessionId, gsrArray, gsrTimeArray, gsrIndex);
            db.concludeSkinTemp(sessionId, skinTempArray, skinTempTimeArray, skinTempIndex);
            db.endSession(sessionId, System.currentTimeMillis());
            Session session = db.getSession(sessionId);
            log(session.toString());
            if (apiClient == null) apiClient = new ServerComm(baseContext);
            apiClient.postSession(session);
            stopTimer();
        }
    }

    public static void startLivePreview() {
        log("startLivePreview() called");
        if(apiClient == null) apiClient = new ServerComm(baseContext);
        Bundle liveUrlBundle = new Bundle();
        liveUrlBundle.putString(BUNDLE_LIVE_URL, apiClient.getLiveUrl());
        resultReceiver.send(MSG_LIVE_URL, liveUrlBundle);
        livePreviewOn = true;
    }

    public static void stopLivePreview() {
        log("stopLivePreview() called");
        livePreviewOn = false;
    }

    public static void startRates() {
        log("startRate() called");
        if(!ratesOn) {
            ratesOn = true;
            startHeartRate();
            startGsr();
            startSkinTemp();
        }
    }

    public static void stopRates() {
        log("stopRate() called");
        if (STATE == SessionState.SESSION_STOPPED) {
            log("stopping rates...");
            ratesOn = false;
            if (client != null) {
                try {
                    client.getSensorManager().unregisterAllListeners();
                } catch (BandIOException e) {
                    log(e.getMessage());
                }
            }
        }
    }

    public static void startHeartRate() {
        log("startHeartRate() called");
        new HeartRateSubscriptionTask().execute();
    }

    public static void startGsr() {
        log("startGsr() called");
        new GsrSubscriptionTask().execute();
    }

    public static void startSkinTemp() {
        log("startSkingTmpe() called");
        new SkinTempSubscriptionTask().execute();
    }

    public void requestConsent(WeakReference<Activity> reference) {
        Log.d("Consent received", "True!!");
        new HeartRateConsentTask().execute(reference);
    }

    public static void addHr(Bundle bundle) {
        if (hrLocked) {
            int hr = bundle.getInt(BUNDLE_HR_HR);
            long time = bundle.getLong(BUNDLE_TIME);
            hrArray[hrIndex] = hr;
            hrTimeArray[hrIndex] = time;
            if (hrIndex == HR_BUFFER - 1) {
                db.appendHR(sessionId, hrArray, hrTimeArray);
                hrArray = new int[HR_BUFFER];
                hrTimeArray = new long[HR_BUFFER];
                hrIndex = 0;
            } else {
                hrIndex++;
            }
        }
    }

    public static void addGsr(Bundle bundle) {
        if(hrLocked) {
            int gsr = bundle.getInt(BUNDLE_GSR_RESISTANCE);
            long time = bundle.getLong(BUNDLE_TIME);
            gsrArray[gsrIndex] = gsr;
            gsrTimeArray[gsrIndex] = time;
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
    }

    public static void addSkinTemp(Bundle bundle) {
        if(hrLocked) {
            double temp = bundle.getDouble(BUNDLE_SKIN_TEMP);
            long time = bundle.getLong(BUNDLE_TIME);
            skinTempArray[skinTempIndex] = temp;
            skinTempTimeArray[skinTempIndex] = time;
            if (skinTempIndex == SKIN_BUFFER - 1) {
                db.appendSkinTemp(sessionId, skinTempArray, skinTempTimeArray);
                skinTempArray = new double[SKIN_BUFFER];
                skinTempTimeArray = new long[SKIN_BUFFER];
                skinTempIndex = 0;
            } else {
                skinTempIndex++;
            }
        }
    }

    public static void sendLiveHr(Bundle bundle) {
        if(apiClient == null) apiClient = new ServerComm(baseContext);
        apiClient.sendLiveData(MSG_HR_TICK, bundle);
    }

    public static void sendLiveGsr(Bundle bundle) {
        if(apiClient == null) apiClient = new ServerComm(baseContext);
        apiClient.sendLiveData(MSG_GSR_TICK, bundle);
    }

    public static void sendLiveSkinTemp(Bundle bundle) {
        if(apiClient == null) apiClient = new ServerComm(baseContext);
        apiClient.sendLiveData(MSG_SKIN_TEMP_TICK, bundle);
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
                timerBundle.putLong(BUNDLE_TIME, getElapsedTime());
                resultReceiver.send(MSG_TIMER_UPDATE, timerBundle);
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private static void stopTimer() {
        handler.removeCallbacksAndMessages(null);
    }

    private static BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
            //  log("HR tick: " + event.getHeartRate());
                if(!hrLocked && event.getQuality().toString().equals("LOCKED")) {
                    hrLocked = true;
                }
                else if (hrLocked) {
                    hrLocked = false;
                }
                hrBundle.clear();
                hrBundle.putInt(BUNDLE_HR_HR, event.getHeartRate());
                hrBundle.putString(BUNDLE_HR_QUALITY, event.getQuality().toString());
                hrBundle.putLong(BUNDLE_TIME, getElapsedTime());
                resultReceiver.send(MSG_HR_TICK, hrBundle);
                if (STATE == SessionState.IN_SESSION) {
                    addHr(hrBundle);
                    if(livePreviewOn) {
                        sendLiveHr(hrBundle);
                    }
                }
            }
        }
    };

    private static BandGsrEventListener mGsrEventListener = new BandGsrEventListener() {
        @Override
        public void onBandGsrChanged(final BandGsrEvent event) {
            if (event != null) {
                gsrBundle.clear();
                gsrBundle.putInt(BUNDLE_GSR_RESISTANCE, event.getResistance());
                gsrBundle.putLong(BUNDLE_TIME, getElapsedTime());
                resultReceiver.send(MSG_GSR_TICK, gsrBundle);
                if(STATE == SessionState.IN_SESSION) {
                    addGsr(gsrBundle);
                    if(livePreviewOn) {
                        sendLiveGsr(gsrBundle);
                    }
                }
            }
        }
    };

    private static BandSkinTemperatureEventListener mSkinTempEventListener = new BandSkinTemperatureEventListener() {
        @Override
        public void onBandSkinTemperatureChanged(BandSkinTemperatureEvent event) {
            if(event != null) {
                skinTempBundle.clear();
                skinTempBundle.putDouble(BUNDLE_SKIN_TEMP, event.getTemperature());
                skinTempBundle.putLong(BUNDLE_TIME, getElapsedTime());
                resultReceiver.send(MSG_SKIN_TEMP_TICK, skinTempBundle);
                if (STATE == SessionState.IN_SESSION) {
                    addSkinTemp(skinTempBundle);
                    if (livePreviewOn) {
                        sendLiveSkinTemp(skinTempBundle);
                    }
                }
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

    private static class SkinTempSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    client.getSensorManager().registerSkinTemperatureEventListener(mSkinTempEventListener);

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
