package com.mrguide.mrguide.Pomodoro;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Vibrator;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleObserver;
import com.mrguide.mrguide.R;

import static com.mrguide.mrguide.Pomodoro.AlarmNotification.SERVICE_ID;

public class PlayAlarmService extends Service implements LifecycleObserver {

    private long startTime;
    private static CountDownTimer timer;
    private static MediaPlayer mediaPlayer;
    private static Vibrator vibrator;
    private long[] pattern = {/*Delay*/1000, /*vibration*/1500};

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startTime = intent.getLongExtra("StartTime",0);
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm);
        mediaPlayer.setLooping(true);
        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);


        //Notification
        Intent notificationIntent = new Intent(getApplicationContext(),MainActivityPomodoro.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this,SERVICE_ID)
                .setContentTitle("Mr.Guide Pomodoro Service")
                .setContentText("Pomodoro")
                .setSmallIcon(R.drawable.ic_baseline_timer_24)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1,notification);


        //Background Timer
        timer = new CountDownTimer(startTime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                mediaPlayer.start();
                vibrator.vibrate(pattern,0);
            }
        }.start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
//        Toast.makeText(getApplicationContext(),"STOP",Toast.LENGTH_SHORT).show();
        timer.cancel();
        mediaPlayer.reset();
        vibrator.cancel();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
