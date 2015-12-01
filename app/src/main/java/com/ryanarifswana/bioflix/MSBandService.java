package com.ryanarifswana.bioflix;


import android.app.Activity;
import android.app.Service;
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

    private final static int HRBUFFER = 20;     //buffer before writing to db
    private final static int GSRBUFFER = 10;

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
    private static BandClient client;
    private static ResultReceiver resultReceiver;
    private static Bundle hrBundle;
    private static Bundle gsrBundle;
    private static Bundle timerBundle;
    private static Bundle errBundle;
    private static DatabaseHandler db;
    private static Handler handler;

    public class LocalBinder extends Binder {
        MSBandService getService() {
            return MSBandService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        db = new DatabaseHandler(this);
        bandService = this;
        resultReceiver = intent.getParcelableExtra("receiver");
        sessionMovieName =intent.getStringExtra("movieName");
        sessionViewerName =  intent.getStringExtra("viewerName");
        hrBundle = new Bundle();
        gsrBundle = new Bundle();
        timerBundle = new Bundle();
        errBundle = new Bundle();
        hrArray = new int[HRBUFFER];
        hrTimeArray = new long[HRBUFFER];
        gsrArray = new int[GSRBUFFER];
        gsrTimeArray = new long[GSRBUFFER];
        hrIndex = 0;
        gsrIndex = 0;
        inSession = false;
        handler = new Handler();
        return mBinder;
    }

    public static void startSession() {
        if(!inSession) {
            sessionId = db.newSession(sessionMovieName, sessionViewerName, System.currentTimeMillis());
            baseTime = System.currentTimeMillis();
            inSession = true;
            startTimer();
        }
    }

    public static void continueSession() {
        if(!inSession) {
            baseTime = System.currentTimeMillis() - baseTime;
            Log.d("startSession()", "sessionStarted!");
        }
    }

    public static void stopSession() {
        db.endSession(sessionId, System.currentTimeMillis());
        handler.removeCallbacksAndMessages(null);
        inSession = false;
    }

    public void startHeartRate() {
        new HeartRateSubscriptionTask().execute();
    }

    public void startGsr() {
        new GsrSubscriptionTask().execute();
    }

    public void requestConsent(WeakReference<Activity> reference) {
        Log.d("Consent received", "True!!");
        new HeartRateConsentTask().execute(reference);
    }

    public void addHr(Bundle bundle) {
        hrArray[hrIndex] = bundle.getInt(BUNDLE_HR_HR);
        hrTimeArray[hrIndex] = getElapsedTime();
        if(hrIndex == HRBUFFER - 1) {
            db.appenHR(sessionId, hrArray, hrTimeArray);
            hrArray = new int[HRBUFFER];
            hrTimeArray = new long[HRBUFFER];
            hrIndex = 0;
        }
        else {
            hrIndex++;
        }
    }

    public void addGsr(Bundle bundle) {
        gsrArray[gsrIndex] = bundle.getInt(BUNDLE_GSR_RESISTANCE);
        gsrTimeArray[gsrIndex] = getElapsedTime();
        if(gsrIndex == GSRBUFFER - 1) {
            db.appenGsr(sessionId, gsrArray, gsrTimeArray);
            gsrArray = new int[GSRBUFFER];
            gsrTimeArray = new long[GSRBUFFER];
            gsrIndex = 0;
        }
        else {
            gsrIndex++;
        }
    }

    public void sendError(String error) {
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

    private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
                hrBundle.clear();
                hrBundle.putInt(BUNDLE_HR_HR, event.getHeartRate());
                hrBundle.putString(BUNDLE_HR_QUALITY, event.getQuality().toString());
                resultReceiver.send(MSG_HR_TICK, hrBundle);
                if(inSession) addHr(hrBundle);
            }
        }
    };

    private BandGsrEventListener mGsrEventListener = new BandGsrEventListener() {
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

    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                sendError("Band isn't paired with your phone.\n");
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        //sendToSnackbar("Band is connecting...\n");
        return ConnectionState.CONNECTED == client.connect().await();
    }

    private class GsrSubscriptionTask extends AsyncTask<Void, Void, Void> {
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

    private class HeartRateSubscriptionTask extends AsyncTask<Void, Void, Void> {
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
}
