package com.termux.x11;

import static android.os.Build.VERSION.SDK_INT;
import static com.termux.x11.CmdEntryPoint.ACTION_START;
import static com.termux.x11.LoriePreferences.ACTION_PREFERENCES_CHANGED;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

import com.termux.shared.errors.Error;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxBootstrap;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.crash.TermuxCrashUtils;
import com.termux.shared.termux.file.TermuxFileUtils;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.shell.command.environment.TermuxShellEnvironment;
import com.termux.shared.termux.shell.am.TermuxAmSocketServer;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.shared.termux.theme.TermuxThemeUtils;
import com.termux.x11.input.TouchInputHandler;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * One application class for the merged apk. First runs the normal Termux
 * startup (same steps as TermuxApplication — kept in sync by hand, it lives
 * in the app module where this library can't reach it), then the X11 side
 * init (prefs, notification channel, server broadcasts).
 *
 * Lives in com.termux.x11 on purpose so it can keep touching MainActivity's
 * package-private bits like the old LorieApp did.
 */
public class XermuxApplication extends Application {

    private static final String LOG_TAG = "XermuxApplication";

    public static final int NOTIFICATION_ID = 7892;

    public final Prefs builtInPrefs = new Prefs();
    public final Prefs secondaryPrefs = new Prefs();
    public NotificationManager notificationManager;
    public final Map<String, IBinder> pendingConnections = new HashMap<>();

    private Notification baseNotification;

    private final SharedPreferences.OnSharedPreferenceChangeListener preferencesChangedListener = (__, key) -> {
        for (MainActivity activity : MainActivity.getInstances())
            activity.onPreferencesChanged(key);
    };

    @Override
    @SuppressLint("ObsoleteSdkInt")
    public void onCreate() {
        super.onCreate();

        Context context = getApplicationContext();

        // --- Termux half (mirrors TermuxApplication.onCreate) ---

        // Set crash handler for the app
        TermuxCrashUtils.setDefaultCrashHandler(this);

        // Set log config for the app
        setLogConfig(context);

        Logger.logDebug("Starting Application");

        // Set TermuxBootstrap.TERMUX_APP_PACKAGE_MANAGER and TermuxBootstrap.TERMUX_APP_PACKAGE_VARIANT
        TermuxBootstrap.setTermuxPackageManagerAndVariant(BuildConfig.TERMUX_PACKAGE_VARIANT);

        // Init app wide SharedProperties loaded from termux.properties
        TermuxAppSharedProperties properties = TermuxAppSharedProperties.init(context);

        // Init app wide shell manager
        TermuxShellManager shellManager = TermuxShellManager.init(context);

        // Set NightMode.APP_NIGHT_MODE
        TermuxThemeUtils.setAppNightMode(properties.getNightMode());

        // Check and create termux files directory. If failed to access it like in case of secondary
        // user or external sd card installation, then don't run files directory related code
        Error error = TermuxFileUtils.isTermuxFilesDirectoryAccessible(this, true, true);
        boolean isTermuxFilesDirectoryAccessible = error == null;
        if (isTermuxFilesDirectoryAccessible) {
            Logger.logInfo(LOG_TAG, "Termux files directory is accessible");

            error = TermuxFileUtils.isAppsTermuxAppDirectoryAccessible(true, true);
            if (error != null) {
                Logger.logErrorExtended(LOG_TAG, "Create apps/termux-app directory failed\n" + error);
                return;
            }

            // Setup termux-am-socket server
            TermuxAmSocketServer.setupTermuxAmSocketServer(context);
        } else {
            Logger.logErrorExtended(LOG_TAG, "Termux files directory is not accessible\n" + error);
        }

        // Init TermuxShellEnvironment constants and caches after everything has been setup including termux-am-socket server
        TermuxShellEnvironment.init(this);

        if (isTermuxFilesDirectoryAccessible) {
            TermuxShellEnvironment.writeEnvironmentToFile(this);
        }

        // --- X11 half (what LorieApp.onCreate used to do) ---

        // our package id IS com.termux, so no createPackageContext dance needed
        Context prefsCtx = this;

        builtInPrefs.attach(prefsCtx, PreferenceManager.getDefaultSharedPreferences(prefsCtx));
        secondaryPrefs.attach(prefsCtx, prefsCtx.getSharedPreferences("secondary", Context.MODE_PRIVATE));

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        builtInPrefs.get().registerOnSharedPreferenceChangeListener(preferencesChangedListener);
        secondaryPrefs.get().registerOnSharedPreferenceChangeListener(preferencesChangedListener);

        String channelId = getResources().getString(R.string.lorie_app_name);
        if (SDK_INT >= VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH);
            channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            if (SDK_INT >= VERSION_CODES.Q)
                channel.setAllowBubbles(false);
            notificationManager.createNotificationChannel(channel);
        }

        // The parts of the notification that don't depend on preferences, built once and reused as a template.
        baseNotification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Termux:X11")
                .setSmallIcon(R.drawable.ic_x11_icon)
                .setContentText(getResources().getText(R.string.lorie_notification_content_text))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MAX)
                .setSilent(true)
                .setShowWhen(false)
                .setColor(0xFF607D8B)
                .build();
    }

    public static void setLogConfig(Context context) {
        Logger.setDefaultLogTag(TermuxConstants.TERMUX_APP_NAME);

        // Load the log level from shared preferences and set it to the {@link Logger.CURRENT_LOG_LEVEL}
        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(context);
        if (preferences == null) return;
        preferences.setLogLevel(null, preferences.getLogLevel());
    }

    /** Picks builtInPrefs or secondaryPrefs depending on which display ctx's window is on. */
    public Prefs getPrefs(Context ctx) {
        boolean isSecondaryDisplay = ((WindowManager) ctx.getSystemService(WINDOW_SERVICE))
                .getDefaultDisplay().getDisplayId() != Display.DEFAULT_DISPLAY;
        return (isSecondaryDisplay && builtInPrefs.storeSecondaryDisplayPreferencesSeparately.get())
                ? secondaryPrefs : builtInPrefs;
    }

    public void onActivityResumed(MainActivity activity) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(activity));
    }

    public void onActivityPaused() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    void refreshNotificationIfShown() {
        MainActivity activity = MainActivity.getFocusedOrAnyInstance();
        if (activity == null)
            return;
        for (StatusBarNotification notification : notificationManager.getActiveNotifications())
            if (notification.getId() == NOTIFICATION_ID) {
                notificationManager.notify(NOTIFICATION_ID, buildNotification(activity));
                return;
            }
    }

    private Notification buildNotification(MainActivity activity) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, baseNotification).setSilent(true);
        return TouchInputHandler.setupNotification(activity, activity.prefs, builder).build();
    }

    public void onBroadcastReceive(Intent intent) {
        String action = intent.getAction();
        String extraTag = ACTION_START.equals(action) || MainActivity.ACTION_STOP.equals(action) || MainActivity.ACTION_CUSTOM.equals(action)
                ? intent.getStringExtra(CmdEntryPoint.EXTRA_DOCUMENT_TAG) : null;
        String tag = extraTag == null ? "" : extraTag;
        MainActivity activity = MainActivity.getInstance(tag);
        if (activity != null)
            activity.prefs = getPrefs(activity);

        switch (action == null ? "" : action) {
            case ACTION_START: {
                Bundle bundle = intent.getBundleExtra(null);
                IBinder binder = bundle == null ? null : bundle.getBinder(null);
                if (binder == null)
                    break;

                IBinder activeService = activity != null && activity.service != null ? activity.service.asBinder() : null;
                IBinder pendingConnection = pendingConnections.get(tag);
                boolean sameConnection = binder.equals(activeService) || binder.equals(pendingConnection);
                if (!sameConnection && ((activeService != null && activeService.isBinderAlive()) || (pendingConnection != null && pendingConnection.isBinderAlive()))) {
                    try {
                        ICmdEntryInterface.Stub.asInterface(binder).reportFatalError("Termux:X11 already has an active X server connection.");
                    } catch (RemoteException ignored) {}
                    break;
                }

                try {
                    if (!sameConnection) {
                        WeakReference<IBinder> binderRef = new WeakReference<>(binder);
                        binder.linkToDeath(() -> {
                            IBinder deadBinder = binderRef.get();
                            if (deadBinder != null)
                                onConnectionDied(tag, deadBinder);
                        }, 0);
                    }
                } catch (RemoteException ignored) {}

                if (activity != null) {
                    try {
                        Log.v("LorieApp", "Got new ACTION_START intent");
                        activity.connectToService(binder);
                    } catch (Exception e) {
                        Log.e("LorieApp", "Something went wrong while we extracted connection details from binder.", e);
                    }
                } else {
                    pendingConnections.put(tag, binder);
                }
                break;
            }
            case MainActivity.ACTION_STOP:
                if (activity != null)
                    activity.finishAffinity();
                break;
            case ACTION_PREFERENCES_CHANGED:
                for (MainActivity a : MainActivity.getInstances()) {
                    a.prefs = getPrefs(a);
                    a.onPreferencesChanged(intent.getStringExtra("key"));
                }
                break;
            case MainActivity.ACTION_CUSTOM:
                Log.d("ACTION_CUSTOM", "action " + intent.getStringExtra("what"));
                if (activity != null)
                    activity.mInputHandler.extractUserActionFromPreferences(activity.prefs, intent.getStringExtra("what")).accept(0, true);
                break;
        }

        if (activity == null && !ACTION_START.equals(action) && !ACTION_PREFERENCES_CHANGED.equals(action))
            Log.w("LorieApp", "Got " + action + " but no MainActivity instance in this process");
    }

    private void onConnectionDied(String tag, IBinder binder) {
        pendingConnections.remove(tag, binder);

        MainActivity activity = MainActivity.getInstance(tag);
        if (activity != null && activity.service != null && activity.service.asBinder() == binder)
            activity.disconnectService();
    }

    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ((XermuxApplication) context.getApplicationContext()).onBroadcastReceive(intent);
        }
    }
}
