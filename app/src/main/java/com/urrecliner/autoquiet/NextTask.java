package com.urrecliner.autoquiet;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.urrecliner.autoquiet.Sub.CalculateNext;
import com.urrecliner.autoquiet.Sub.IsScreen;
import com.urrecliner.autoquiet.Sub.NextAlarm;
import com.urrecliner.autoquiet.models.QuietTask;

import java.util.ArrayList;

public class NextTask {
    long nextTime;
    static int icon, loop;
    static String timeInfoS, timeInfoF, timeInfo, soonOrUntill, subject, msg;

    public NextTask(Context context, ArrayList<QuietTask> quietTasks, String headInfo) {

        nextTime = System.currentTimeMillis() + 240*60*60*1000L;
        int saveIdx = 0;
        String begEnd = "";
        boolean[] week;
        for (int idx = 0; idx < quietTasks.size(); idx++) {
            QuietTask qTaskNext = quietTasks.get(idx);
            if (qTaskNext.active) {
                week = qTaskNext.week;
                long nextStart = CalculateNext.calc(false, qTaskNext.begHour, qTaskNext.begMin, week, 0);
                if (nextStart < nextTime) {
                    nextTime = nextStart;
                    saveIdx = idx;
                    begEnd = "S";
                    icon = 0;
                }
                if (qTaskNext.endHour != 99) {
                    long nextEnd = CalculateNext.calc(true, qTaskNext.endHour, qTaskNext.endMin, week, (qTaskNext.begHour > qTaskNext.endHour) ? (long) 24 * 60 * 60 * 1000 : 0);
                    if (nextEnd < nextTime) {
                        nextTime = nextEnd;
                        saveIdx = idx;
                        begEnd = (idx == 0) ? "O" : "F";
                        icon = (qTaskNext.vibrate) ? 1 : 2;
                    }
                }
            }
        }
        QuietTask qT = quietTasks.get(saveIdx);
        // if endHour == 99 then it means alarm, if endLoop > 1 then repeat 3 times
        loop = (qT.endHour == 99 && qT.endLoop == 11) ? 3 : 0; // if alarm repeat 3 times
        new NextAlarm().request(context, qT, nextTime, begEnd, loop);
        subject = qT.subject;
        timeInfoS = getHourMin(qT.begHour, qT.begMin);
        boolean end99 = qT.endHour == 99;
        if (!end99) {
            timeInfoF = getHourMin(qT.endHour, qT.endMin);
            if  (begEnd.equals("S")) {
                timeInfo = timeInfoS;
                soonOrUntill = "예정";
            } else {
                timeInfo = timeInfoF;
                soonOrUntill = "까지";
            }
            msg = headInfo + " " + subject + "\n" + timeInfo
                    + " " + soonOrUntill + " " + begEnd;
            if  (begEnd.equals("S"))
                msg += " ~ " + timeInfoF;
        } else {
            timeInfo = timeInfoS;
            soonOrUntill = "알림";
            msg = headInfo + "\n" + timeInfo + " " + subject;
            icon = (qT.endLoop == 0) ? 4:3;
        }

        new Utils(context).log("NextTask",msg);
        Intent intent = new Intent(context, NotificationService.class);
        intent.putExtra("beg", timeInfo);
        intent.putExtra("end", soonOrUntill);
        intent.putExtra("subject", subject);
        intent.putExtra("isUpdate", true);
        intent.putExtra("end99", false);
        intent.putExtra("icon", icon);
        context.startForegroundService(intent);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (IsScreen.On(context))
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getHourMin(int hour, int min) {
        return (""+ (100+hour)).substring(1) + ":" + (""+(100+min)).substring(1);
    }

}