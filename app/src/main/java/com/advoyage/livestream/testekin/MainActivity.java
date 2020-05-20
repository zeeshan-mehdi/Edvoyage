package com.advoyage.livestream.testekin;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.advoyage.livestream.testekin.adapter.MessageListAdapter;
import com.advoyage.livestream.testekin.model.Message;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.DoubleBounce;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameFilter;
import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends Activity implements OnClickListener {

    private final static String CLASS_LABEL = "MainActivity";
    private final static String LOG_TAG = CLASS_LABEL;

    private int cameraFacing = 90;
    /* This isn't a live RTMP endpoint. You should replace this line with your own! */
    private String ffmpeg_link = "rtmp://global-live.mux.com:5222/app/";

    long startTime = 0;
    boolean recording = false;

    private FFmpegFrameRecorder recorder;

    private boolean isPreviewOn = false;

    /*Filter information, change boolean to true if adding a filter*/
    private boolean addFilter = true;
    private String filterString = "";
    FFmpegFrameFilter filter;

    private int sampleAudioRateInHz = 44100;
    private int imageWidth = 320;
    private int imageHeight = 240;
    private int frameRate = 30;

    /* audio data getting thread */
    private AudioRecord audioRecord;
    private AudioRecordRunnable audioRecordRunnable;
    private Thread audioThread;
    volatile boolean runAudioThread = true;

    /* video data getting thread */
    private Camera cameraDevice;
    private CameraView cameraView;

    private Frame yuvImage = null;

    /* layout setting */
    private final int bg_screen_bx = 232;
    private final int bg_screen_by = 128;
    private final int bg_screen_width = 700;
    private final int bg_screen_height = 500;
    private final int bg_width = 1123;
    private final int bg_height = 715;
    private final int live_width = 640;
    private final int live_height = 480;
    private int screenWidth, screenHeight;
    private Button btnRecorderControl;

    /* The number of seconds in the continuous record loop (or 0 to disable loop). */
    final int RECORD_LENGTH = 0;
    Frame[] images;
    long[] timestamps;
    ShortBuffer[] samples;
    int imagesIndex, samplesIndex;
    private int requestCode = 1;
    String streamKey;
    ProgressBar progressBar;
    ConstraintLayout constraintLayout;
    ImageButton switchCamera,rotateScreen;
    private int currentCameraId;

    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageAdapter;
    private List<Message> messageList;

    EditText input;
    private Socket socket;
    String currentUser;
    private View btnSend;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        messageList = new ArrayList<>();
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_main);
        constraintLayout = findViewById(R.id.camera);

        progressBar = (ProgressBar)findViewById(R.id.progress);
        progressBar.setVisibility(View.GONE);

        Sprite doubleBounce = new DoubleBounce();
        progressBar.setIndeterminateDrawable(doubleBounce);

        currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;

        streamKey = getIntent().getStringExtra("stream_key");

        switchCamera = findViewById(R.id.switchCamera);
        mMessageRecycler = (RecyclerView) findViewById(R.id.recyclerView);
        //rotateScreen = findViewById(R.id.rotate)
        input =(EditText) findViewById(R.id.editTextMessage);


        final int orientation =  MainActivity.this.getResources().getConfiguration().orientation;

//        rotateScreen.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                switch(orientation) {
//                    case Configuration.ORIENTATION_PORTRAIT :{
//                       // changeCameraFacing();
//                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//                    }
//                        break;
//                    case Configuration.ORIENTATION_LANDSCAPE: {
//                        //changeCameraFacing();
//                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//                    }
//
//                        break;
//                }
//            }
//        });
        switchCamera.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                changeCameraFacing();
            }
        });

        btnSend = findViewById(R.id.btnSend);

        if(streamKey!=null &&streamKey!=""){
            ffmpeg_link+=streamKey;
            initLayout();
        }else{
            Toast.makeText(this, "Could Not Create Stream Try Again", Toast.LENGTH_SHORT).show();
        }

        btnSend.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });


        switchLiveChatVisibility(false);

        //initLayout();

    }

    private void switchLiveChatVisibility(boolean visible){
        if(!visible) {
            input.setVisibility(View.GONE);
            btnSend.setVisibility(View.GONE);

        }else{
            input.setVisibility(View.VISIBLE);
            btnSend.setVisibility(View.VISIBLE);
        }
    }




    private void listenForMessages() {


        mMessageAdapter = new MessageListAdapter(this, messageList);
        mMessageRecycler.setLayoutManager(new LinearLayoutManager(this));
        mMessageRecycler.setAdapter(mMessageAdapter);

        try {
            System.out.println("inside send message");
            socket = IO.socket("https://live-education.herokuapp.com/");
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    System.out.println("connected");
                    socket.emit("username", "Admin");
                    //socket.disconnect();
                }

            }).on("is_online", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    System.out.println("recieved online user ");
                    //currentUser = String.valueOf(args[0]);
                    //socket.emit("chat_message", "Hi from Admin");
                    System.out.println(args[0]);
                }
            }).on("chat_message", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        System.out.println("message recieved................................");

                        JSONObject jsonObject = new JSONObject((String)args[0]);

                        System.out.println(jsonObject);
                        String message = jsonObject.getString("message");
                        String name = jsonObject.getString("name");
                        if(name !=null && name.equals("Admin") ){
                            name = "1";
                        }

                        Message m = new Message(message ,name);
                        //JSONObject jsonMessage = args[0];

                        messageList.add(m);

                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mMessageAdapter.notifyDataSetChanged(messageList);
                                mMessageRecycler.smoothScrollToPosition(messageList.size());
                            }
                        });

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });


            socket.connect();
        }catch (Exception e){
            System.out.println(e.toString());
        }





    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        recording = false;

        if (cameraView != null) {
            cameraView.stopPreview();
        }

        if(cameraDevice != null) {
            cameraDevice.stopPreview();
            cameraDevice.release();
            cameraDevice = null;
        }
    }




    private void initLayout() {



        /* get size of screen */
//        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
//        screenWidth = display.getWidth();
//        screenHeight = display.getHeight();
//        RelativeLayout.LayoutParams layoutParam = null;
//        LayoutInflater myInflate = null;
//        myInflate = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        RelativeLayout topLayout = new RelativeLayout(this);
//        setContentView(topLayout);
//        LinearLayout preViewLayout = (LinearLayout) myInflate.inflate(R.layout.activity_main, null);
//        layoutParam = new RelativeLayout.LayoutParams(screenWidth, screenHeight);
//        topLayout.addView(preViewLayout, layoutParam);
//
        /* add control button: start and stop */
        btnRecorderControl = (Button) findViewById(R.id.recorder_control);
        btnRecorderControl.setText("Start");
        btnRecorderControl.setOnClickListener(this);
//
//        /* add camera view */
//        int display_width_d = (int) (1.0 * bg_screen_width * screenWidth / bg_width);
//        int display_height_d = (int) (1.0 * bg_screen_height * screenHeight / bg_height);
//        int prev_rw, prev_rh;
//        if (1.0 * display_width_d / display_height_d > 1.0 * live_width / live_height) {
//            prev_rh = display_height_d;
//            prev_rw = (int) (1.0 * display_height_d * live_width / live_height);
//        } else {
//            prev_rw = display_width_d;
//            prev_rh = (int) (1.0 * display_width_d * live_height / live_width);
//        }
//        layoutParam = new RelativeLayout.LayoutParams(prev_rw, prev_rh);
//        layoutParam.topMargin = (int) (1.0 * bg_screen_by * screenHeight / bg_height);
//        layoutParam.leftMargin = (int) (1.0 * bg_screen_bx * screenWidth / bg_width);

        cameraDevice = Camera.open(1);
        Log.i(LOG_TAG, "camera open");
        cameraView = new CameraView(this, cameraDevice);
        constraintLayout.addView(cameraView);
        setCameraDisplayOrientation(MainActivity.this, currentCameraId, cameraDevice);
        Log.i(LOG_TAG, "camera preview start: OK");
    }


    void changeCameraFacing(){
        cameraDevice.stopPreview();
        cameraDevice.release();

        constraintLayout.removeView(cameraView);

//swap the id of the camera to be used
        if(cameraFacing==180){
            currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
            cameraFacing =90;
        }
        else {
            currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
            cameraFacing =180;
        }
        cameraDevice = Camera.open(currentCameraId);

        setCameraDisplayOrientation(MainActivity.this, currentCameraId, cameraDevice);
        try {

            cameraView = new CameraView(this, cameraDevice);
            constraintLayout.addView(cameraView);
        } catch (Exception e) {
            e.printStackTrace();
        }
        cameraDevice.startPreview();
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    //---------------------------------------
    // initialize ffmpeg_recorder
    //---------------------------------------
    private void initRecorder() {

        try {
            Log.w(LOG_TAG, "init recorder");

            if (RECORD_LENGTH > 0) {
                imagesIndex = 0;
                images = new Frame[RECORD_LENGTH * frameRate];
                timestamps = new long[images.length];
                for (int i = 0; i < images.length; i++) {
                    images[i] = new Frame(imageWidth, imageHeight, Frame.DEPTH_UBYTE, 2);
                    timestamps[i] = -1;
                }
            } else if (yuvImage == null) {
                yuvImage = new Frame(imageWidth, imageHeight, Frame.DEPTH_UBYTE, 2);
                Log.i(LOG_TAG, "create yuvImage");
            }

            Log.i(LOG_TAG, "ffmpeg_url: " + ffmpeg_link);
            recorder = new FFmpegFrameRecorder(ffmpeg_link, imageWidth, imageHeight, 1);
            recorder.setFormat("flv");
            recorder.setSampleRate(sampleAudioRateInHz);
            // Set in the surface changed method
            recorder.setFrameRate(frameRate);

            // The filterString  is any ffmpeg filter.
            // Here is the link for a list: https://ffmpeg.org/ffmpeg-filters.html
            if(cameraFacing==90)
                 filterString = "transpose=2,transpose=2";
            else
                filterString = "transpose=1";
            filter = new FFmpegFrameFilter(filterString, imageWidth, imageHeight);

            
            //default format on android
            filter.setPixelFormat(avutil.AV_PIX_FMT_NV21);

            Log.i(LOG_TAG, "recorder initialize success");

            audioRecordRunnable = new AudioRecordRunnable();
            audioThread = new Thread(audioRecordRunnable);
            runAudioThread = true;
        }catch (Exception e ){
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public void startRecording() {


        initRecorder();

        try {
            recorder.start();
            startTime = System.currentTimeMillis();
            recording = true;
            audioThread.start();

            if(addFilter) {
                filter.start();
            }

        } catch (FFmpegFrameRecorder.Exception | FrameFilter.Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void stopRecording() {

        runAudioThread = false;
        try {
            audioThread.join();
        } catch (InterruptedException e) {
            // reset interrupt to be nice
            Thread.currentThread().interrupt();
            return;
        }
        audioRecordRunnable = null;
        audioThread = null;

        if (recorder != null && recording) {
            if (RECORD_LENGTH > 0) {
                Log.v(LOG_TAG,"Writing frames");
                try {
                    int firstIndex = imagesIndex % samples.length;
                    int lastIndex = (imagesIndex - 1) % images.length;
                    if (imagesIndex <= images.length) {
                        firstIndex = 0;
                        lastIndex = imagesIndex - 1;
                    }
                    if ((startTime = timestamps[lastIndex] - RECORD_LENGTH * 1000000L) < 0) {
                        startTime = 0;
                    }
                    if (lastIndex < firstIndex) {
                        lastIndex += images.length;
                    }
                    for (int i = firstIndex; i <= lastIndex; i++) {
                        long t = timestamps[i % timestamps.length] - startTime;
                        if (t >= 0) {
                            if (t > recorder.getTimestamp()) {
                                recorder.setTimestamp(t);
                            }
                            recorder.record(images[i % images.length]);
                        }
                    }

                    firstIndex = samplesIndex % samples.length;
                    lastIndex = (samplesIndex - 1) % samples.length;
                    if (samplesIndex <= samples.length) {
                        firstIndex = 0;
                        lastIndex = samplesIndex - 1;
                    }
                    if (lastIndex < firstIndex) {
                        lastIndex += samples.length;
                    }
                    for (int i = firstIndex; i <= lastIndex; i++) {
                        recorder.recordSamples(samples[i % samples.length]);
                    }
                } catch (FFmpegFrameRecorder.Exception e) {
                    Log.v(LOG_TAG,e.getMessage());
                    e.printStackTrace();
                }
            }

            recording = false;
            Log.v(LOG_TAG,"Finishing recording, calling stop and release on recorder");
            try {
                recorder.stop();
                recorder.release();
                filter.stop();
                filter.release();
            } catch (FFmpegFrameRecorder.Exception | FrameFilter.Exception e) {
                e.printStackTrace();
            }
            recorder = null;

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (recording) {
                stopRecording();
            }

            finish();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }



    //---------------------------------------------
    // audio thread, gets and encodes audio data
    //---------------------------------------------
    class AudioRecordRunnable implements Runnable {

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            // Audio
            int bufferSize;
            ShortBuffer audioData;
            int bufferReadResult;

            bufferSize = AudioRecord.getMinBufferSize(sampleAudioRateInHz,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleAudioRateInHz,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

            if (RECORD_LENGTH > 0) {
                samplesIndex = 0;
                samples = new ShortBuffer[RECORD_LENGTH * sampleAudioRateInHz * 2 / bufferSize + 1];
                for (int i = 0; i < samples.length; i++) {
                    samples[i] = ShortBuffer.allocate(bufferSize);
                }
            } else {
                audioData = ShortBuffer.allocate(bufferSize);
            }

            Log.d(LOG_TAG, "audioRecord.startRecording()");
            audioRecord.startRecording();

            /* ffmpeg_audio encoding loop */
            while (runAudioThread) {
                if (RECORD_LENGTH > 0) {
                    audioData = samples[samplesIndex++ % samples.length];
                    audioData.position(0).limit(0);
                }
                //Log.v(LOG_TAG,"recording? " + recording);
                bufferReadResult = audioRecord.read(audioData.array(), 0, audioData.capacity());
                audioData.limit(bufferReadResult);
                if (bufferReadResult > 0) {
                    Log.v(LOG_TAG,"bufferReadResult: " + bufferReadResult);
                    // If "recording" isn't true when start this thread, it never get's set according to this if statement...!!!
                    // Why?  Good question...
                    if (recording) {
                        if (RECORD_LENGTH <= 0) try {
                            recorder.recordSamples(audioData);
                            //Log.v(LOG_TAG,"recording " + 1024*i + " to " + 1024*i+1024);
                        } catch (FFmpegFrameRecorder.Exception e) {
                            Log.v(LOG_TAG,e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
            Log.v(LOG_TAG,"AudioThread Finished, release audioRecord");

            /* encoding finish, release recorder */
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
                Log.v(LOG_TAG,"audioRecord released");
            }
        }
    }

    //---------------------------------------------
    // camera thread, gets and encodes video data
    //---------------------------------------------
    class CameraView extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback {

        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraView(Context context, Camera camera) {
            super(context);
            Log.w("camera","camera view");
            mCamera = camera;
            mHolder = getHolder();
            mHolder.addCallback(CameraView.this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            mCamera.setPreviewCallback(CameraView.this);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                stopPreview();
                mCamera.setPreviewDisplay(holder);
            } catch (IOException exception) {
                mCamera.release();
                mCamera = null;
            }
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            stopPreview();

            Camera.Parameters camParams = mCamera.getParameters();
            List<Camera.Size> sizes = camParams.getSupportedPreviewSizes();
            // Sort the list in ascending order
            Collections.sort(sizes, new Comparator<Camera.Size>() {

                public int compare(final Camera.Size a, final Camera.Size b) {
                    return a.width * a.height - b.width * b.height;
                }
            });

            // Pick the first preview size that is equal or bigger, or pick the last (biggest) option if we cannot
            // reach the initial settings of imageWidth/imageHeight.
            for (int i = 0; i < sizes.size(); i++) {
                if ((sizes.get(i).width >= imageWidth && sizes.get(i).height >= imageHeight) || i == sizes.size() - 1) {
                    imageWidth = sizes.get(i).width;
                    imageHeight = sizes.get(i).height;
                    Log.v(LOG_TAG, "Changed to supported resolution: " + imageWidth + "x" + imageHeight);
                    break;
                }
            }
            camParams.setPreviewSize(imageWidth, imageHeight);

            Log.v(LOG_TAG,"Setting imageWidth: " + imageWidth + " imageHeight: " + imageHeight + " frameRate: " + frameRate);

            camParams.setPreviewFrameRate(frameRate);
            Log.v(LOG_TAG,"Preview Framerate: " + camParams.getPreviewFrameRate());

            mCamera.setParameters(camParams);

            // Set the holder (which might have changed) again
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.setPreviewCallback(CameraView.this);
                startPreview();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Could not set preview display in surfaceChanged");
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            try {
                mHolder.addCallback(null);
                mCamera.setPreviewCallback(null);
            } catch (RuntimeException e) {
                // The camera has probably just been released, ignore.
            }
        }

        public void startPreview() {
            if (!isPreviewOn && mCamera != null) {
                isPreviewOn = true;
                mCamera.startPreview();
            }
        }

        public void stopPreview() {
            if (isPreviewOn && mCamera != null) {
                isPreviewOn = false;
                mCamera.stopPreview();
            }
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (audioRecord == null || audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                startTime = System.currentTimeMillis();
                return;
            }
            if (RECORD_LENGTH > 0) {
                int i = imagesIndex++ % images.length;
                yuvImage = images[i];
                timestamps[i] = 1000 * (System.currentTimeMillis() - startTime);
            }


            /* get video data */
            if (yuvImage != null && recording) {
                ((ByteBuffer)yuvImage.image[0].position(0)).put(data);

                if (RECORD_LENGTH <= 0) try {
                    Log.v(LOG_TAG,"Writing Frame");
                    long t = 1000 * (System.currentTimeMillis() - startTime);
                    if (t > recorder.getTimestamp()) {
                        recorder.setTimestamp(t);
                    }

                    if(addFilter) {
                        filter.push(yuvImage);
                        Frame frame2;
                        while ((frame2 = filter.pull()) != null) {
                            recorder.record(frame2, filter.getPixelFormat());
                        }
                    } else {

                        recorder.record(yuvImage);

                    }
                } catch (FFmpegFrameRecorder.Exception | FrameFilter.Exception e) {
                    Log.v(LOG_TAG,e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }



    @Override
    public void onClick(View v) {
        if (!recording) {
            Toast.makeText(getBaseContext(), "Initiating Stream", Toast.LENGTH_LONG).show();
            startRecording();
            progressBar.setVisibility(View.VISIBLE);
            Log.w(LOG_TAG, "Start Button Pushed");
            switchLiveChatVisibility(true);
            listenForMessages();
            btnRecorderControl.setText("Stop");
            btnRecorderControl.setVisibility(View.GONE);
        } else {

            Log.w(LOG_TAG, "Stop Button Pushed");
           // btnRecorderControl.setText("Start");
        }

    }

    void sendMessage(){
        String message = input.getText()!=null ? input.getText().toString():"";

        input.setText("");
        socket.emit("chat_message",message);

    }

    @Override
    public void onBackPressed() {
        Toast.makeText(getBaseContext(), "Stopping Recording", Toast.LENGTH_LONG).show();
        // This will trigger the audio recording loop to stop and then set isRecorderStart = false;
        stopRecording();
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Log.v("onpause", "Pause");
//        cameraDevice.stopPreview();
//        cameraDevice.release();
    }

    @Override
    protected void onResume() {
        //cameraDevice.startPreview();
        super.onResume();
    }
}
