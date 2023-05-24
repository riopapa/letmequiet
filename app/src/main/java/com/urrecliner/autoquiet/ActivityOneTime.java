package com.urrecliner.autoquiet;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.urrecliner.autoquiet.databinding.ActivityOneTimeBinding;
import com.urrecliner.autoquiet.models.QuietTask;
import com.urrecliner.autoquiet.Sub.MannerMode;
import com.urrecliner.autoquiet.Sub.VarsGetPut;

import java.util.ArrayList;
import java.util.Calendar;

public class ActivityOneTime extends AppCompatActivity {

    QuietTask quietTask;
    ArrayList<QuietTask> quietTasks;
    private String subject;
    private int begHour, begMin, endHour, endMin, fRepeatCount; // 0: silent 1: bell 2: talk
    private boolean vibrate;
    private int durationMin = 0;       // in minutes
    Calendar calendar;
    boolean timePicker_UpDown = false;
    ActivityOneTimeBinding binding;
    int shortInterval, longInterval;
    Vars vars;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        vars = new VarsGetPut().get(this);
        Intent intent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        getApplicationContext().sendBroadcast(intent);
        super.onCreate(savedInstanceState);
        binding = ActivityOneTimeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ActionBar actionBar;
        actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(getResources().getString(R.string.Quiet_Once));
        actionBar.setIcon(R.drawable.right_now) ;
        actionBar.setDisplayUseLogoEnabled(true) ;
        actionBar.setDisplayShowHomeEnabled(true) ;
        quietTasks = new QuietTaskGetPut().get(this);
        quietTask = quietTasks.get(0);
        subject = quietTask.subject;
        vibrate = quietTask.vibrate;
        fRepeatCount = quietTask.endLoop;
        durationMin = Integer.parseInt(vars.sharedTimeInit);
        shortInterval = Integer.parseInt(vars.sharedTimeShort);
        longInterval = Integer.parseInt(vars.sharedTimeLong);
        calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND,0);
        begHour = calendar.get(Calendar.HOUR_OF_DAY);
        begMin = calendar.get(Calendar.MINUTE);
        calendar.add(Calendar.MINUTE, durationMin);
        endHour = calendar.get(Calendar.HOUR_OF_DAY);
        endMin = calendar.get(Calendar.MINUTE);

        binding.oneTimePicker.setIs24HourView(true);
        binding.oneTimePicker.setOnTimeChangedListener((timePicker, hour, min) -> {
            if (timePicker_UpDown)
                return;
            endHour = hour; endMin = min;
            durationMin = (endHour - begHour) * 60 + endMin - begMin;
            binding.oneDuration.setText(durationText());
        });
        buildScreen();
        buttonSetting();
        adjustTimePicker();
    }

    private String durationText() {
        String text;
        if (durationMin > 1)
            text = " "+(""+(100 + durationMin / 60)).substring(1) + "시간 \n" + (""+(100 + durationMin % 60)).substring(1)+  "분후";
        else
            text = getString(R.string.already_passed_time);
        return text;
    }

    void buildScreen() {
        String text;
        text = "▼"+vars.sharedTimeShort+"분▼"; binding.minus10Min.setText(text);
        text = "▲"+vars.sharedTimeShort+"분▲"; binding.plus10Min.setText(text);
        text = "▼"+vars.sharedTimeLong+"분▼"; binding.minus30Min.setText(text);
        text = "▲"+vars.sharedTimeLong+"분▲"; binding.plus30Min.setText(text);
    }

    void buttonSetting() {
        binding.oneVibrate.setImageResource((vibrate)? R.drawable.phone_normal :R.drawable.phone_off);
        binding.oneVibrate.setOnClickListener(v -> {
            vibrate ^= true;
            binding.oneVibrate.setImageResource((vibrate)? R.drawable.phone_normal :R.drawable.phone_off);
            v.invalidate();
        });
        binding.minus10Min.setOnClickListener(v -> {
            if (durationMin > shortInterval) {
                durationMin -= shortInterval;
                adjustTimePicker();
            }
        });

        binding.plus10Min.setOnClickListener(v -> {
            durationMin += shortInterval;
            adjustTimePicker();
        });

        binding.minus30Min.setOnClickListener(v -> {
            if (durationMin > longInterval) {
                durationMin -= longInterval;
                adjustTimePicker();
            }
        });

        binding.plus30Min.setOnClickListener(v -> {
            durationMin += longInterval;
            adjustTimePicker();
        });

        binding.oneFinishRepeat.setImageResource((fRepeatCount == 0)? R.drawable.speak_off: (fRepeatCount == 1)? R.drawable.bell_onetime : R.drawable.speak_on);
        binding.oneFinishRepeat.setOnClickListener(v -> {
            if (fRepeatCount == 0)
                fRepeatCount = 1;
            else if (fRepeatCount == 1)
                fRepeatCount = 11;
            else
                fRepeatCount = 0;
            binding.oneFinishRepeat.setImageResource((fRepeatCount == 0)? R.drawable.speak_off : (fRepeatCount == 1)? R.drawable.bell_onetime : R.drawable.speak_on);
            v.invalidate();
        });

    }

    void adjustTimePicker() {
        int time = begHour * 60 + begMin + durationMin;
        endHour = time / 60; endMin = time % 60;
        if (endHour >= 24)
            endHour -= 24;
        timePicker_UpDown = true;  // to prevent double TimeChanged action
        binding.oneTimePicker.setHour(endHour);
        binding.oneTimePicker.setMinute(endMin);
        binding.oneDuration.setText(durationText());
        timePicker_UpDown = false;
        binding.oneTimePicker.invalidate();
    }

    private void saveOneTime() {

        boolean [] week = new boolean[]{true, true, true, true, true, true, true};
        quietTask = new QuietTask(subject, begHour, begMin, endHour, endMin,
                week, true, vibrate, 0, fRepeatCount, false);    // onetime repeat is 0

        quietTasks.set(0, quietTask);
        new QuietTaskGetPut().put(quietTasks);
        new MannerMode().turn2Quiet(this, vars.sharedManner, vibrate);
//        new NextTask(this,"One Time");
//        new VarsGetPut().put(vars, getApplicationContext());
        new NextTask(this, quietTasks, "Quit RightNow");
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_onetime, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            saveOneTime();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}