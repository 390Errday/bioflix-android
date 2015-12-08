package com.ryanarifswana.bioflix;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

import com.loopj.android.http.AsyncHttpClient;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.ryanarifswana.bioflix.database.model.Session;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by ariftopcu on 12/3/15.
 */
public class ServerComm {
    Context mContext;
    AsyncHttpClient client;
    private final String socketRequestRoute = "/getsocket";
    private final String uploadRoute = "/upload";
    private final String serverUrl = "http://bioflix-umass.herokuapp.com";

    public ServerComm(Context context) {
        this.mContext = context;
        client = new AsyncHttpClient();
    }

    public void postSession(Session session) {
        try {
            JSONObject jSession = new JSONObject();
            jSession.put("local_id", session.getId());
            jSession.put("movie_name", session.getMovieName());
            jSession.put("poster_url", "http://images.techtimes.com/data/images/full/83420/john-cena.jpg?w=600");
            jSession.put("hr_data", session.getHrArray());
            jSession.put("hr_times", session.getHrTimes());
            jSession.put("gsr_data", session.getGsrArray());
            jSession.put("gsr_times", session.getGsrTimes());
            jSession.put("viewer_name", session.getViewerName());
            jSession.put("start_time", session.getStartTime());
            jSession.put("end_time", session.getEndTime());

            StringEntity entity = new StringEntity(jSession.toString());
            String postUrl = serverUrl + uploadRoute;
            client.post(mContext, postUrl, null, entity, "application/json", new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, String response) {
                    Log.d("POST:", response);
                    Toast.makeText(mContext, "Uploaded Successfully!", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    Log.d("POST:", response.toString());
                    Toast.makeText(mContext, "Uploaded Successfully!", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String response, Throwable e) {
                    Log.d("POST:", response);
                    Toast.makeText(mContext, "Upload error!", Toast.LENGTH_LONG).show();
                }
            });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestSocket() {
        String url = serverUrl + socketRequestRoute;
        client.get(url, new AsyncHttpResponseHandler() {

            @Override
            public void onStart() {
                // called before request is started
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                String socketUrl = response.toString();
                MSBandService.startLivePreview();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });
    }

    public void sendSocketData(int type, int data, long time) {

        if(type == MSBandService.MSG_HR_TICK) {
            Log.d("SendSocketData: ", "Sending HR data through socket: " + data + ", " + time);
        } else if (type == MSBandService.MSG_GSR_TICK) {
            Log.d("SendSocketData: ", "Sending GSR data through socket: " + data + ", " + time);
        }


    }

    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) mContext.getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }
}
