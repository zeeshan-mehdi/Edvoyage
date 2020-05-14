package com.advoyage.livestream.testekin;

import android.content.Context;
import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Network {


    public static void createNewStream(Context context,final ResponseListener myResponce){
        myResponce.requestStarted();
        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest sr = new StringRequest(Request.Method.POST,"https://api.mux.com/video/v1/live-streams", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                myResponce.requestCompleted(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                myResponce.requestEndedWithError(error);
            }
        }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<String, String>();
                params.put("playback_policy","public");
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/x-www-form-urlencoded");
                String credentials = "e35f71a2-d6d6-41fa-8339-152763993f6b:w6hJR6heuf7YkStKSTHWpIudJqbt87nXYS+FKZVsuddmQL80L45ibJc0JMZZJhG4sVx0O0u1LMM";
                String auth = "Basic "
                        + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                params.put("Authorization", auth);
                return params;
            }
        };
        queue.add(sr);
    }

    static  void deleteAllLiveStreams(final Context context, final ResponseListener listener){
        RequestQueue queue = Volley.newRequestQueue(context);
        listener.requestStarted();
        StringRequest sr = new StringRequest(Request.Method.GET,"https://api.mux.com/video/v1/live-streams", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject json = new JSONObject(response);

                    JSONArray jsonArray = json.getJSONArray("data");

                    ArrayList<String> keys = new ArrayList<>();
                    for (int i=0;i<jsonArray.length();i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        keys.add(jsonObject.getString("id"));
                        System.out.println(jsonObject);
                    }
                    deleteAllLiveStreams(context,keys);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.requestCompleted(response);


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.requestEndedWithError(error);
            }
        }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<String, String>();
                params.put("playback_policy","public");
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/x-www-form-urlencoded");
                String credentials = "e35f71a2-d6d6-41fa-8339-152763993f6b:w6hJR6heuf7YkStKSTHWpIudJqbt87nXYS+FKZVsuddmQL80L45ibJc0JMZZJhG4sVx0O0u1LMM";
                String auth = "Basic "
                        + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                params.put("Authorization", auth);
                return params;
            }
        };
        queue.add(sr);
    }

    private static  void deleteAllLiveStreams(Context context,ArrayList<String> keys){
        RequestQueue queue = Volley.newRequestQueue(context);

        for(int i =0;i<keys.size();i++) {

            String key = keys.get(i);
            StringRequest sr = new StringRequest(Request.Method.DELETE, "https://api.mux.com/video/v1/live-streams/"+key, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("playback_policy", "public");
                    return params;
                }

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("Content-Type", "application/x-www-form-urlencoded");
                    String credentials = "e35f71a2-d6d6-41fa-8339-152763993f6b:w6hJR6heuf7YkStKSTHWpIudJqbt87nXYS+FKZVsuddmQL80L45ibJc0JMZZJhG4sVx0O0u1LMM";
                    String auth = "Basic "
                            + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                    params.put("Authorization", auth);
                    return params;
                }
            };
            queue.add(sr);
        }
    }




}

interface ResponseListener {
    public void requestStarted();
    public void requestCompleted(String response);
    public void requestEndedWithError(VolleyError error);
}
