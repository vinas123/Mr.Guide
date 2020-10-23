package com.mrguide.mrguide.Pomodoro;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.drawerlayout.widget.DrawerLayout;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.navigation.NavigationView;
import com.mrguide.mrguide.MainActivity;
import com.mrguide.mrguide.R;
import java.util.Locale;


public class MainActivityPomodoro extends AppCompatActivity {

    //Navigation
    private DrawerLayout dl;
    private ActionBarDrawerToggle abdt;

    //Dark Mode
    Switch darkMode;
    MenuItem menuItem;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor sharedPreferencesEditor;
    Boolean isNightModeOn;

    //Spinner
    Spinner hour;
    Spinner min;

    //Timer
    private static int Hours = 1;
    private static int Minutes = 30;
    private static long START_TIME_IN_MILLIS = (Minutes*60*1000) + (Hours*60*60*1000);
    private TextView mTextViewCountDown;
    private Button mButtonStartPause;
    private Button mButtonReset;
    private CountDownTimer mCountDownTimer;
    private boolean mTimerRunning;
    private long mTimeLeftInMillis;
    private long mEndTime;
    private Intent PlayAlarmService;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_pomodoro);

        if (getIntent().getBooleanExtra("EXIT", false)) {
//            mTimeLeftInMillis = getIntent().getLongExtra("mTimeLeftInMillis", 0);
//            Toast.makeText(getApplicationContext(),"Call by Service" + String.valueOf(getIntent().getLongExtra("left", 99)),Toast.LENGTH_SHORT).show();
//            finish();
        }
        //Spinner
        hour = (Spinner) findViewById(R.id.hour);
        min = (Spinner) findViewById(R.id.min);
        hour.setSelection(0);
        min.setSelection(5);
        hour.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Hours = Integer.parseInt(hour.getSelectedItem().toString());
                resetTimer();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        min.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Minutes = Integer.parseInt(min.getSelectedItem().toString());
                resetTimer();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });



        //Navigation
        dl = (DrawerLayout) findViewById(R.id.dl_pomo);
        abdt = new ActionBarDrawerToggle(this, dl, R.string.Open, R.string.Close);
        abdt.setDrawerIndicatorEnabled(true);
        dl.addDrawerListener(abdt);
        abdt.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        NavigationView nav_view = (NavigationView) findViewById(R.id.nav_view_pomo);

        nav_view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                int id = menuItem.getItemId();

                if (id == R.id.todo) {
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                }
                if (id == R.id.timer) {
                    Intent intent = new Intent(getApplicationContext(), MainActivityPomodoro.class);
                    startActivity(intent);
                    finish();
                }
//                if (id == R.id.money_manager) {
//                    Toast.makeText(MainActivityPomodoro.this, "Money Manager", Toast.LENGTH_LONG).show();
//                }
//                if(id==R.id.dark_menu){
//                    Toast.makeText(MainActivity.this,"Dark Mode",Toast.LENGTH_LONG).show();
//                }

                return true;
            }
        });



        //Dark Mode
        menuItem = nav_view.getMenu().findItem(R.id.dark_menu);
        darkMode = (Switch) menuItem.getActionView().findViewById(R.id.dark_mode_switch);
        sharedPreferences = getSharedPreferences("AppSettingPrefs", 0);
        sharedPreferencesEditor = sharedPreferences.edit();
        isNightModeOn = sharedPreferences.getBoolean("NightMode", false);

        if (isNightModeOn) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            darkMode.setChecked(true);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            darkMode.setChecked(false);
        }

        darkMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    sharedPreferencesEditor.putBoolean("NightMode", true);
                    sharedPreferencesEditor.apply();
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    sharedPreferencesEditor.putBoolean("NightMode", false);
                    sharedPreferencesEditor.apply();
                }
            }
        });


        //Timer
        PlayAlarmService = new Intent(getApplicationContext(),PlayAlarmService.class);
        mTextViewCountDown = findViewById(R.id.text_view_countdown);
        mButtonStartPause = findViewById(R.id.button_start_pause);
        mButtonReset = findViewById(R.id.button_reset);
        mButtonStartPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(PlayAlarmService);
                if(mButtonStartPause.getText().toString().equals("Start")){
                    PlayAlarmService.putExtra("StartTime", mTimeLeftInMillis);
                    startService(PlayAlarmService);
                }
                if (mTimerRunning) {
                    pauseTimer();
                } else {
                    startTimer();
                }
            }
        });
        mButtonReset.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mButtonReset.setText("Reset");
                stopService(PlayAlarmService);
                resetTimer();
            }
        });

    }


    //Navigation
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return abdt.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }


    //Timer
    private void startTimer() {
        mEndTime = System.currentTimeMillis() + mTimeLeftInMillis;
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }
            @Override
            public void onFinish() {
                mTimerRunning = false;
                mButtonReset.setEnabled(true);
                mButtonReset.setText("Stop");
                mButtonStartPause.setEnabled(false);
//                updateButtons();
            }
        }.start();
        mTimerRunning = true;
        updateButtons();
    }



    private void pauseTimer() {
        mCountDownTimer.cancel();
        mTimerRunning = false;
        updateButtons();
    }
    private void resetTimer() {
        hour.setEnabled(true);
        min.setEnabled(true);
        START_TIME_IN_MILLIS = (Minutes*60*1000) + (Hours*60*60*1000);
        mTimeLeftInMillis = START_TIME_IN_MILLIS;
        updateCountDownText();
        updateButtons();
    }
    private void updateCountDownText() {
        int hours = (int) (mTimeLeftInMillis / 1000) / 60 / 60;
        int minutes = (int) (mTimeLeftInMillis / 1000) / 60;
        minutes = minutes % 60;
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        mTextViewCountDown.setText(timeLeftFormatted);
    }
    private void updateButtons() {
        if (mTimerRunning) {
            hour.setEnabled(false);
            min.setEnabled(false);
            mButtonReset.setEnabled(false);
            mButtonStartPause.setText("Pause");
        } else {
            mButtonStartPause.setText("Start");
            mButtonReset.setEnabled(true);
            if (mTimeLeftInMillis < 1000) {
                mButtonStartPause.setEnabled(false);
            } else {
                mButtonStartPause.setEnabled(true);
            }
//            if (mTimeLeftInMillis < START_TIME_IN_MILLIS) {
//                mButtonReset.setEnabled(true);
//            } else {
//                mButtonReset.setEnabled(false);
//            }
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("millisLeft", mTimeLeftInMillis);
        editor.putBoolean("timerRunning", mTimerRunning);
        editor.putLong("endTime", mEndTime);
        editor.apply();
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        mTimeLeftInMillis = prefs.getLong("millisLeft", START_TIME_IN_MILLIS);
        mTimerRunning = prefs.getBoolean("timerRunning", false);
        updateCountDownText();
        updateButtons();
        if (mTimerRunning) {
            mEndTime = prefs.getLong("endTime", 0);
            mTimeLeftInMillis = mEndTime - System.currentTimeMillis();
            if (mTimeLeftInMillis < 0) {
                mTimeLeftInMillis = 0;
                mTimerRunning = false;
                updateCountDownText();
                updateButtons();
            } else {
                startTimer();
            }
        }
    }



}
