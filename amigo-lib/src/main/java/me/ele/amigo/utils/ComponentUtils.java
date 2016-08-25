package me.ele.amigo.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import me.ele.amigo.Amigo;
import me.ele.amigo.reflect.FieldUtils;
import me.ele.amigo.reflect.MethodUtils;

public class ComponentUtils {

    private static final String TAG = ComponentUtils.class.getName();

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

    public static ActivityInfo[] getAppReceivers(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_RECEIVERS);
            return info.receivers;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ActivityInfo[] getNewAppReceivers(Context context) {
        PackageManager pm = context.getPackageManager();
        String archiveFilePath = Amigo.getHotfixApk(context).getAbsolutePath();
        PackageInfo info = pm.getPackageArchiveInfo(archiveFilePath, PackageManager.GET_RECEIVERS);
        return info.receivers;
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

    private static List<ActivityInfo> getNewAddedReceivers(Context context) {
        ActivityInfo[] newAppReceivers = getNewAppReceivers(context);
        ActivityInfo[] appReceivers = getAppReceivers(context);
        List<ActivityInfo> addedReceivers = new ArrayList<>();
        if (newAppReceivers != null && newAppReceivers.length > 0) {
            for (ActivityInfo newAppReceiver : newAppReceivers) {
                boolean isNew = true;
                if (appReceivers != null && appReceivers.length > 0) {
                    for (ActivityInfo appReceiver : appReceivers) {
                        if (newAppReceiver.name.equals(appReceiver.name)) {
                            isNew = false;
                            break;
                        }
                    }
                }

                if (isNew) {
                    Log.e(TAG, "newReceiver-->" + newAppReceiver);
                    addedReceivers.add(newAppReceiver);
                }
            }
        }
        return addedReceivers;
    }


    public static void registerNewReceivers(Context context) {
        try {
            List<ActivityInfo> addedReceivers = getNewAddedReceivers(context);
            for (ActivityInfo addedReceiver : addedReceivers) {
                List<IntentFilter> filters = getReceiverIntentFilter(context, addedReceiver);
                for (IntentFilter filter : filters) {
                    BroadcastReceiver receiver = (BroadcastReceiver) ComponentUtils.class.getClassLoader().loadClass(addedReceiver.name).newInstance();
                    context.registerReceiver(receiver, filter);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static List<IntentFilter> getReceiverIntentFilter(Context context, ActivityInfo receiverInfo) throws InvocationTargetException, NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        List<Object> receivers = getReceivers(context);
        Object data = null;
        for (Object receiver : receivers) {
            ActivityInfo info = (ActivityInfo) FieldUtils.readField(receiver, "info");
            if (info.name.equals(receiverInfo.name)) {
                data = receiver;
                break;
            }
        }
        return (List<IntentFilter>) FieldUtils.readField(data, "intents");
    }

    private static List<Object> receivers;

    private static List<Object> getReceivers(Context context) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException, ClassNotFoundException {
        if (receivers != null) {
            return receivers;
        }
        Class sPackageParserClass = Class.forName("android.content.pm.PackageParser");
        Object mPackageParser;
        Object mPackage;
        File file = Amigo.getHotfixApk(context);
        try {
            mPackageParser = sPackageParserClass.newInstance();
            mPackage = MethodUtils.invokeMethod(mPackageParser, "parsePackage", file, 0);
        } catch (Exception e) {
            e.printStackTrace();

            mPackageParser = sPackageParserClass.getDeclaredConstructor(String.class).newInstance(file.getPath());
            Method m = sPackageParserClass.getDeclaredMethod("parsePackage", File.class, String.class, DisplayMetrics.class, int.class);
            m.setAccessible(true);
            DisplayMetrics metrics = new DisplayMetrics();
            metrics.setToDefaults();
            mPackage = m.invoke(mPackageParser, file, file.getPath(), metrics, 0);
        }

        receivers = (List<Object>) FieldUtils.readField(mPackage, "receivers");
        if (receivers == null) {
            receivers = new ArrayList<>();
        }
        return receivers;
    }


}