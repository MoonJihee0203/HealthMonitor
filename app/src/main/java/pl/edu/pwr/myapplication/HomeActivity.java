package pl.edu.pwr.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener, StepListener {
    private TextView numberOfStepsTxtView, distanceTxtView;
    private Button returnButton, startTrainingBtn, stopTrainingBtn, pauseTrainingBtn;
    private String numberOfStepsTxt = "Steps counted: ";
    private String numberOfMetersTxt = "Distance measured [m]: ";

    private StepDetector stepDetector;
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private int numSteps = 0;
    private double metersForStep = 0.72;
    private String sharedPrefs = "mySharedPrefs";

    private boolean startedFlag = false;
    private boolean stoppedFlag = false;
    private boolean pausedFlag = false;

    DatabaseHelper dbHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        dbHelper = new DatabaseHelper(this);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        stepDetector = new StepDetector();
        stepDetector.registerListener(this);

        numberOfStepsTxtView = findViewById(R.id.tv_steps);
        distanceTxtView = findViewById(R.id.distance);

        returnButton = findViewById(R.id.returnButton);
        startTrainingBtn = findViewById(R.id.startTrainingBtn);
        stopTrainingBtn = findViewById(R.id.stopTrainingBtn);
        pauseTrainingBtn = findViewById(R.id.pauseTrainingBtn);

        returnButton.setOnClickListener(this);
        startTrainingBtn.setOnClickListener(this);
        stopTrainingBtn.setOnClickListener(this);
        pauseTrainingBtn.setOnClickListener(this);

        loadData();
        scheduleAlarm();
    }

    public void scheduleAlarm() {
        Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, AlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        long firstMillis = System.currentTimeMillis();
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES/15, pIntent); /// !!!!!!!!!!
    }
    public void cancelAlarm() {
        Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, AlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pIntent);
    }


    private void saveData()
    {
        SharedPreferences sharedPreferences = getSharedPreferences(sharedPrefs, MODE_PRIVATE);
        SharedPreferences.Editor myEdit = sharedPreferences.edit();
        myEdit.putInt("steps", numSteps);
        myEdit.commit();
    }
    private void saveFlags()
    {
        SharedPreferences sharedPreferences = getSharedPreferences(sharedPrefs, MODE_PRIVATE);
        SharedPreferences.Editor myEdit = sharedPreferences.edit();
        myEdit.putBoolean("startFlag", startedFlag);
        myEdit.putBoolean("stopFlag", stoppedFlag);
        myEdit.putBoolean("pauseFlag", pausedFlag);
        myEdit.commit();
    }

    private void loadData()
    {
        SharedPreferences sharedPref = getSharedPreferences(sharedPrefs, MODE_PRIVATE);
        int a = sharedPref.getInt("steps", 0);
        startedFlag = sharedPref.getBoolean("startFlag", false);
        stoppedFlag = sharedPref.getBoolean("stopFlag", false);
        pausedFlag = sharedPref.getBoolean("pauseFlag", false);
        numSteps = 0;
        System.out.println("LOAD DATA!!!!!!!!!!!!!!!");
        numberOfStepsTxtView.setText(numberOfStepsTxt + numSteps);
        distanceTxtView.setText(numberOfMetersTxt+ (double)numSteps * metersForStep);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.returnButton)
        {
            Intent returnIntent = new Intent(HomeActivity.this, MainActivity.class);
            startActivity(returnIntent);
        }
        if(v.getId() == R.id.startTrainingBtn)
        {
            startedFlag = true;
            stoppedFlag = false;
            pausedFlag = false;
            Toast.makeText(HomeActivity.this, "Started training", Toast.LENGTH_SHORT).show();
            startTrainingBtn.setEnabled(false);
            returnButton.setEnabled(false);

        }
        if(v.getId() == R.id.stopTrainingBtn)
        {
            stoppedFlag = true;
            startedFlag = false;
            startTrainingBtn.setEnabled(true);
            returnButton.setEnabled(true);
            Toast.makeText(HomeActivity.this, "Stopped training", Toast.LENGTH_SHORT).show();
            if(stopTraining())
            {
                Toast.makeText(HomeActivity.this, "Data added successfully to db", Toast.LENGTH_SHORT).show();
            }
            else  Toast.makeText(HomeActivity.this, "Sth went wrong while adding to db", Toast.LENGTH_SHORT).show();
        }
        if(v.getId() == R.id.pauseTrainingBtn)
        {
            pausedFlag = true;
            startTrainingBtn.setEnabled(true);
            Toast.makeText(HomeActivity.this, "Paused training", Toast.LENGTH_SHORT).show();
        }
    }
    boolean stopTraining()
    {
        Date today = new Date();
        DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        String strDate = dateFormat.format(today);

        return dbHelper.addData(numSteps, numSteps * metersForStep, strDate);
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        if(startedFlag==true)
        {
            numberOfStepsTxtView.setText(numberOfStepsTxt + numSteps);
            distanceTxtView.setText(numberOfMetersTxt + (double)numSteps * metersForStep);
        }
        System.out.println("NOT HERE ON RESUME !!!!!!!!!!!!!!!!!!");
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        saveFlags();
        if(startedFlag==true)
        {
            saveData();
        }
        System.out.println("NOT HERE ON PAUSE !!!!!!!!!!!!!!!!!!");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        System.out.println("ON CHANGED SENSOR !!!!!!!!!!!!");
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            stepDetector.updateAccelerometer(event.timestamp, event.values[0], event.values[1], event.values[2]);
        }
    }

    @Override
    public void step(long timeNs) {
        if(startedFlag==true && pausedFlag==false && stoppedFlag==false)
        {
            numSteps++;
            numberOfStepsTxtView.setText(numberOfStepsTxt + numSteps);
            distanceTxtView.setText(numberOfMetersTxt + (double)numSteps * metersForStep);
        }
    }

}

