package com.example.mmcc.javacvexample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.github.ybq.android.spinkit.SpinKitView;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class StartStreamActivity extends AppCompatActivity {

    String streamKey;
    private int requestCode =1;

    SpinKitView progressBar;

    Socket socket;

    Button streamBtn,sendMessage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_stream);

        streamBtn = findViewById(R.id.livestream);
       // sendMessage = findViewById(R.id.sendMessage);

        progressBar = findViewById(R.id.grid);

//        sendMessage.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                sendMessage();
//            }
//        });


        progressBar.setVisibility(View.GONE);
        //progressBar.setVisibility(View.INVISIBLE);


        streamBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // progressBar.setVisibility(View.GONE);
                //Sprite doubleBounce = new DoubleBounce();
                //progressBar.setIndeterminateDrawable(doubleBounce);
                deleteStreams();
            }
        });

    }

    private void sendMessage() {
        try {
            System.out.println("inside send message");
            socket = IO.socket("https://live-education.herokuapp.com/");
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    System.out.println("connected");
                    socket.emit("username", "Zeeshan");
                    //socket.disconnect();
                }

            }).on("is_online", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    System.out.println("recieved online user ");

                    //socket.emit("chat_message", "Hi from Admin");
                    System.out.println(args[0]);
                }
            }).on("chat_message", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    System.out.println("message recieved");
                    System.out.println(args[0]);
                }
            });


            socket.connect();
        }catch (Exception e){
            System.out.println(e.toString());
        }
    }

    private void  deleteStreams(){
        try {
            Network.deleteAllLiveStreams(getApplicationContext(), new ResponseListener() {
                @Override
                public void requestStarted() {
                    System.out.println("starting stream deletion process");
                    progressBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void requestCompleted(String response) {
                    System.out.println("deleted all streams");
                    createNewStream();
                }

                @Override
                public void requestEndedWithError(VolleyError error) {
                    System.out.println(error.toString());
                    createNewStream();
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void createNewStream(){
        try {
            Network.createNewStream(getApplicationContext(), new ResponseListener() {
                @Override
                public void requestStarted() {
                    System.out.println("request started");
                }

                @Override
                public void requestCompleted(String response) {

                    System.out.println("request completed");
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    JSONObject data = null;
                    try {
                        data = jsonObject.getJSONObject("data");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    try {
                        streamKey = data.getString("stream_key");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (streamKey != null) {
                    } else {
                        Toast.makeText(StartStreamActivity.this, "Could Not Create New Stream", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    progressBar.setVisibility(View.GONE);
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_DENIED) {
                        ActivityCompat.requestPermissions(StartStreamActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
                    } else {
                        startMainActivity();
                    }

                }

                @Override
                public void requestEndedWithError(VolleyError error) {

                    Toast.makeText(StartStreamActivity.this, error.toString(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    System.out.println(error.toString());
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }


    void startMainActivity(){
        Intent intent = new Intent(StartStreamActivity.this,MainActivity.class);
        intent.putExtra("stream_key",streamKey);
        startActivity(intent);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode ==this.requestCode) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED&&grantResults[1] == PackageManager.PERMISSION_GRANTED&&grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                startMainActivity();
            }
        } else {
            Toast.makeText(this, "Permissions Denied Can't Start App", Toast.LENGTH_SHORT).show();
        }
    }
}
