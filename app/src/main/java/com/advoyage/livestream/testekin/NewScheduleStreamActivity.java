package com.advoyage.livestream.testekin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.advoyage.livestream.testekin.model.Schedule;
import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class NewScheduleStreamActivity extends AppCompatActivity {

    Button btnPick,btnSave;
    TextView dateText;
    EditText inputName;
    private long time;
    SpinKitView progressBar;

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_schedule_stream);
        btnPick = findViewById(R.id.btnPickDate);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        inputName = findViewById(R.id.input_stream_name);
        btnSave = findViewById(R.id.btnSave);
        dateText = findViewById(R.id.input_date);

        progressBar = findViewById(R.id.scheduleProgress);


       // db = FirebaseFirestore.getInstance();

        btnPick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addSchedule();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                saveSchedule();
            }
        });

    }

    private void saveSchedule() {
        try {
            String date = dateText.getText().toString();
            String name = inputName.getText().toString();


            Schedule schedule = new Schedule(date, time, name);

            mDatabase.child("schedules").push().setValue(schedule)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(NewScheduleStreamActivity.this, "Schedule Added", Toast.LENGTH_SHORT).show();
                            closeActivity();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(NewScheduleStreamActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                            closeActivity();
                        }
                    });
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e.toString());
        }

    }


    private void addSchedule() {

        final View dialogView = View.inflate(this, R.layout.date_time_picker, null);
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        dialogView.findViewById(R.id.date_time_set).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                DatePicker datePicker = (DatePicker) dialogView.findViewById(R.id.date_picker);
                TimePicker timePicker = (TimePicker) dialogView.findViewById(R.id.time_picker);

                Calendar calendar = new GregorianCalendar(datePicker.getYear(),
                        datePicker.getMonth(),
                        datePicker.getDayOfMonth(),
                        timePicker.getCurrentHour(),
                        timePicker.getCurrentMinute());


                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm a");

                System.out.println(formatter.format(calendar.getTime()));


                time = calendar.getTimeInMillis();
                dateText.setText(formatter.format(calendar.getTime()));

                alertDialog.dismiss();
            }});
        alertDialog.setView(dialogView);
        alertDialog.show();

    }

    void closeActivity(){
        NewScheduleStreamActivity.this.finish();
    }
}
