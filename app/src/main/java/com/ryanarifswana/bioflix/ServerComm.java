package com.ryanarifswana.bioflix;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

import com.loopj.android.http.AsyncHttpClient;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.ryanarifswana.bioflix.database.model.Session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Random;

/**
 * Created by ariftopcu on 12/3/15.
 */
public class ServerComm {
    Context mContext;
    AsyncHttpClient client;
    private final int liveUrlRandom;
    private final String apiPath = "/api";
    private final String livePath = "/live";
    private final String uploadPath = "/upload";
    private final String serverUrl = "http://bioflix-umass.herokuapp.com";

    public ServerComm(Context context) {
        Random generator = new Random();
        liveUrlRandom = generator.nextInt(1000);
        this.mContext = context;
        client = new AsyncHttpClient();
    }

    //TODO
    public void getRemoteSessions(final ResultReceiver receiver) {
        String url = serverUrl + apiPath + "/getallsessions";
        client.get(url, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.d("Jsonresponse", response.toString());
            }
        });
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
            jSession.put("temp_data", session.getTempArray());
            jSession.put("temp_times", session.getTempTimes());
            jSession.put("viewer_name", session.getViewerName());
            jSession.put("start_time", session.getStartTime());
            jSession.put("end_time", session.getEndTime());

            StringEntity entity = new StringEntity(jSession.toString());
            Log.d("Posting json file", jSession.toString());
            String postUrl = serverUrl + uploadRoute;
            client.post(mContext, postUrl, null, entity, "application/json", new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, String response) {
                    Log.d("POST response", response);
                    Toast.makeText(mContext, "Uploaded Successfully!", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    Log.d("POST response", response.toString());
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

    public String getLiveUrl() {
        return serverUrl + livePath + "/" + liveUrlRandom;
    }

    public void sendLiveData(int type, Bundle data) {
        try {
            JSONObject jsonData = new JSONObject();
//            jsonData.put("type", type);
//            jsonData.put("data", data);
            jsonData.put("time", data.getLong(MSBandService.BUNDLE_TIME));
            switch(type) {
                case MSBandService.MSG_HR_TICK:
                    jsonData.put("type", "hr");
                    jsonData.put("data", data.getInt(MSBandService.BUNDLE_HR_HR));
                    break;
                case MSBandService.MSG_GSR_TICK:
                    jsonData.put("type", "gsr");
                    jsonData.put("data", data.getInt(MSBandService.BUNDLE_GSR_RESISTANCE));
                    break;
                case MSBandService.MSG_SKIN_TEMP_TICK:
                    jsonData.put("type", "temp");
                    jsonData.put("data", data.getDouble(MSBandService.BUNDLE_SKIN_TEMP));
                    break;
            }
            Log.d("Posting: ", jsonData.toString());
            StringEntity entity = new StringEntity(jsonData.toString());
            client.post(mContext, getLiveUrl(), null, entity, "application/json", new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, String response) {
                    Log.d("Live Post response:", response);
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    Log.d("Live Post response:", response.toString());
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String response, Throwable e) {
                    Log.d("POST:", response);
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

    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) mContext.getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }
}
