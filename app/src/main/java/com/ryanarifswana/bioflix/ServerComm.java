package com.ryanarifswana.bioflix;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

import com.loopj.android.http.AsyncHttpClient;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.ryanarifswana.bioflix.database.model.Session;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by ariftopcu on 12/3/15.
 */
public class ServerComm {
    Context mContext;
    AsyncHttpClient client;
    private final String uploadUrl = "http://bioflix-umass.herokuapp.com/upload";

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
            client.post(mContext, uploadUrl, null, entity, "application/json", new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, String response) {
                    System.out.println(response);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String response, Throwable e) {
                    System.out.println(response);
                }
            });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
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