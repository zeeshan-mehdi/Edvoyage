package com.advoyage.livestream.testekin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import com.advoyage.livestream.testekin.adapter.StreamScheduleListAdapter;
import com.advoyage.livestream.testekin.model.Schedule;
import com.github.ybq.android.spinkit.SpinKitView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

public class ScheduleActivity extends AppCompatActivity {

    StreamScheduleListAdapter scheduleAdapter;
    Button btnAdd;
    RecyclerView recyclerView;
    ArrayList<Schedule> scheduleArrayList;
    long  time;
    SpinKitView progressBar;
    private DatabaseReference mDatabase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        recyclerView = findViewById(R.id.scheduleRecyclerView);
        btnAdd = findViewById(R.id.addSchedule);
        scheduleArrayList = new ArrayList<>();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        progressBar = findViewById(R.id.loading);


        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ScheduleActivity.this,NewScheduleStreamActivity.class));
            }
        });
        scheduleAdapter = new StreamScheduleListAdapter(getApplicationContext(),scheduleArrayList);

        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        recyclerView.setAdapter(scheduleAdapter);

        readSchedules();

    }

    void readSchedules(){
        mDatabase.child("schedules").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren()){
                    scheduleArrayList = new ArrayList<>();
                }else{
                    Toast.makeText(ScheduleActivity.this, "No Streams Found", Toast.LENGTH_SHORT).show();
                }
                for(DataSnapshot dc : dataSnapshot.getChildren()){
                    Schedule schedule = dc.getValue(Schedule.class);
                    scheduleArrayList.add(schedule);
                }



                progressBar.setVisibility(View.GONE);

                scheduleAdapter.notifyDataSetChanged(scheduleArrayList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }


}
