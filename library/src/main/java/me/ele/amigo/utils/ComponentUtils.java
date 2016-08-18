package me.ele.amigo.utils;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import me.ele.amigo.Amigo;

public class ComponentUtils {

    public static ActivityInfo[] getAppActivities(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            return info.activities;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ActivityInfo[] getNewAppActivities(Context context) {
        PackageManager pm = context.getPackageManager();
        String archiveFilePath = Amigo.getHotfixApk(context).getAbsolutePath();
        PackageInfo info = pm.getPackageArchiveInfo(archiveFilePath, PackageManager.GET_ACTIVITIES);
        return info.activities;
    }

    public static ActivityInfo getActivityInfoInNewApp(Context context, String activityClassName) {
        ActivityInfo[] infos = getNewAppActivities(context);
        for (ActivityInfo info : infos) {
            if (info.name.equals(activityClassName)) {
                return info;
            }
        }
        return null;
    }


}
