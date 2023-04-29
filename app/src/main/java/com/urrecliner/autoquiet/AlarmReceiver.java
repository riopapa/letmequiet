package com.urrecliner.autoquiet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Toast;

import com.urrecliner.autoquiet.Sub.NextAlarm;
import com.urrecliner.autoquiet.Sub.Sounds;
import com.urrecliner.autoquiet.models.QuietTask;
import com.urrecliner.autoquiet.Sub.MannerMode;
import com.urrecliner.autoquiet.Sub.VarsGetPut;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class AlarmReceiver extends BroadcastReceiver {

    TextToSpeech myTTS;
    ArrayList<QuietTask> quietTasks;
    QuietTask quietTask;
    Context context;
    int loop;
    String caseSFO;
    Vars vars;
    final int STOP_SPEAK = 1022;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        Bundle args = intent.getBundleExtra("DATA");
        quietTask = (QuietTask) args.getSerializable("quietTask");
        quietTasks = new QuietTaskGetPut().get(context);
        caseSFO = Objects.requireNonNull(intent.getExtras()).getString("case");
        loop = Objects.requireNonNull(intent.getExtras()).getInt("loop");
        vars = new VarsGetPut().get(context);
        readyTTS();

        assert caseSFO != null;
        boolean beep = vars.sharedManner && (quietTask.fRepeatCount == 1);

        switch (caseSFO) {
            case "S":   // start?
                say_Started();
                break;
            case "F":   // finish
                new MannerMode().turn2Normal(beep, context);
                say_Finished();
                break;
            case "O":   // onetime
                new MannerMode().turn2Normal(beep, context);
                if (quietTask.fRepeatCount > 1) {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            String say = "지금은 " + nowTimeToString(System.currentTimeMillis()) +
                                        " 입니다. 무음모드가 종료되었습니다";
                            myTTS.speak(say, TextToSpeech.QUEUE_ADD, null, TTSId);
                        }
                    }, 3000);
                }
                quietTask.setActive(false);
                quietTasks.set(0, quietTask);
                new QuietTaskGetPut().put(quietTasks);
                new NextTask(context, quietTasks, "After oneTime");
            break;
            default:
                new Utils(context).log("Alarm Receive","Case Error " + caseSFO);
        }
        new VarsGetPut().put(vars, context);
    }

    void say_Started() {

        boolean finish99 = quietTask.finishHour == 99;
        new Timer().schedule(new TimerTask() {
            public void run() {
                if (finish99)
                    say_Started99();
                else {
                    say_StartedNormal();
                }
            }
        }, 1500);   // after beep

        Intent notification = new Intent(context, NotificationService.class);
        notification.putExtra("operation", STOP_SPEAK);
        context.startForegroundService(notification);
    }

    private void say_Started99() {
        String subject = quietTask.subject;
        if (loop > 0) {
            loop--;
            String say = "";
            if (isQuiet()) {
                say = subject;
                loop = 0;
            } else {
                say = subject + " 를 확인하세요, " +
                        ((loop == 0) ? "마지막 안내입니다 " : "") + subject + " 를 확인하세요";
            }
            myTTS.speak(say, TextToSpeech.QUEUE_ADD, null, TTSId);

            long nextTime = System.currentTimeMillis() + 70 * 1000;
            new NextAlarm().request(context, quietTask, nextTime,
                    "S", loop);   // loop 0 : no more
            Intent uIntent = new Intent(context, NotificationService.class);
            uIntent.putExtra("start", nowTimeToString(nextTime));
            uIntent.putExtra("finish", "다시");
            uIntent.putExtra("finish99", true);
            uIntent.putExtra("subject", quietTask.subject);
            uIntent.putExtra("icon", 3);
            uIntent.putExtra("isUpdate", true);
            context.startForegroundService(uIntent);
        } else {
            if (quietTask.fRepeatCount > 1) {
                String say = addPostPosition(subject) + "끝났습니다";
                myTTS.speak(say, TextToSpeech.QUEUE_ADD, null, TTSId);
            } else {
                new Sounds().beep(context, Sounds.BEEP.INFO);
            }
            new NextTask(context, quietTasks, "finish99ed");
        }
    }

    private void say_StartedNormal() {
        if (quietTask.sRepeatCount > 1) {
            new Sounds().beep(context, Sounds.BEEP.NOTY);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    String subject = quietTask.subject;
                    String say = addPostPosition(subject) + "시작됩니다";
                    myTTS.speak(say, TextToSpeech.QUEUE_ADD, null, TTSId);
                }
            }, 3000);
        } else if (quietTask.sRepeatCount == 1){
            new Sounds().beep(context, Sounds.BEEP.INFO);
        }
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                boolean beep = vars.sharedManner && (quietTask.sRepeatCount > 1);
                new MannerMode().turn2Quiet(context, beep, quietTask.vibrate);
            }
        }, 6000);
        new NextTask(context, quietTasks, "say_Started()");
    }

    String addPostPosition(String s) {
        String lastNFKD = Normalizer.normalize(s.substring(s.length() - 1), Normalizer.Form.NFKD);
        return s + ((lastNFKD.length() == 2) ? "가 " : "이 ");
    }

    void say_Finished() {
        new Timer().schedule(new TimerTask() {
            public void run () {
                if (quietTask.fRepeatCount > 1) {
                    new Sounds().beep(context, Sounds.BEEP.NOTY);
                    String subject = quietTask.subject;
                    String d = (quietTask.sayDate) ? "지금은 " + nowDateToString(System.currentTimeMillis()) : "";
                    String t = nowTimeToString(System.currentTimeMillis());
                    String s = d + t +  " 입니다. ";
                    s += addPostPosition(subject) + "끝났습니다";
                    // 받침이 있으면 이, 없으면 가
                    myTTS.speak(s, TextToSpeech.QUEUE_ADD, null, null);
                } else if (quietTask.fRepeatCount == 1) {
                    new Sounds().beep(context, Sounds.BEEP.ALARM);
                }
                if (quietTask.agenda) { // delete if agenda based
                    for (int i = 0; i < quietTasks.size(); i++) {
                        if (quietTasks.get(i).calId == quietTask.calId) {
                            quietTasks.remove(i);
                            new QuietTaskGetPut().put(quietTasks);
                            break;
                        }
                    }
                }
                new NextTask(context, quietTasks, "say_Finished()");
                new Utils(context).deleteOldLogFiles();
            }
        }, 3000);
    }

    private void readyTTS() {

        myTTS = null;
        myTTS = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                initializeTTS();
            }
        });
    }

    String TTSId = "";

    private void initializeTTS() {

        myTTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                TTSId = utteranceId;
            }

            @Override
            // this method will always called from a background thread.
            public void onDone(String utteranceId) {
                if (myTTS.isSpeaking())
                    return;
                myTTS.stop();
            }

            @Override
            public void onError(String utteranceId) { }
        });

        int result = myTTS.setLanguage(Locale.getDefault());
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(context, "Not supported Language", Toast.LENGTH_SHORT).show();
        } else {
            myTTS.setPitch(1.2f);
            myTTS.setSpeechRate(1.3f);
        }
    }

    String nowDateToString(long time) {
        final SimpleDateFormat sdfTime = new SimpleDateFormat(" MM 월 d 일 EEEE ", Locale.getDefault());
        final SimpleDateFormat sdfWeek = new SimpleDateFormat(" EEEE ", Locale.US);
        String s = sdfTime.format(time) + sdfWeek.format(time);
        return s + s;
    }
    String nowTimeToString(long time) {
        final SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdfTime.format(time);
    }

    boolean isQuiet() {
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT ||
                mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE);
    }

}