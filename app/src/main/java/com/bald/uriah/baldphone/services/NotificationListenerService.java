/*
 * Copyright 2019 Uriah Shaul Mandel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bald.uriah.baldphone.services;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public class NotificationListenerService
        extends android.service.notification.NotificationListenerService {
    private static final String TAG = NotificationListenerService.class.getSimpleName();
    public static final String NOTIFICATIONS_ACTIVITY_BROADCAST =
            "NOTIFICATIONS_ACTIVITY_BROADCAST";
    public static final String HOME_SCREEN_ACTIVITY_BROADCAST = "HOME_SCREEN_ACTIVITY_BROADCAST";
    public static final String ACTION_CLEAR_MISSED_CALLS =
            "app.baldphone.neo.CLEAR_MISSED_CALLS_NOTIFICATION";
    public static final String ACTION_REGISTER_ACTIVITY = "ACTION_REGISTER_ACTIVITY";
    public static final String ACTION_CLEAR = "ACTION_CLEAR";
    public static final String KEY_EXTRA_KEY = "KEY_EXTRA_KEY";
    public static final String KEY_EXTRA_NOTIFICATIONS = "KEY_EXTRA_NOTIFICATIONS";
    public static final String KEY_EXTRA_ACTIVITY = "KEY_EXTRA_ACTIVITY";

    public static final int NOTIFICATIONS_NONE = 0;
    public static final int NOTIFICATIONS_SOME = 2;
    public static final int NOTIFICATIONS_ALOT = 5;
    public static final int ACTIVITY_NONE = -1;
    public static final int NOTIFICATIONS_ACTIVITY = 1;
    public static final int NOTIFICATIONS_HOME_SCREEN = 2;

    @IntDef({ACTIVITY_NONE, NOTIFICATIONS_ACTIVITY, NOTIFICATIONS_HOME_SCREEN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SupportedActivitys {}

    @SupportedActivitys private int activity = ACTIVITY_NONE;
    private boolean isServiceListening = false;

    private String defaultDialerPackage;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private boolean isDefaultDialerRinging =
            false; // Tracks if the phone was ringing and not picked up
    private boolean isMissedCall;

    private final BroadcastReceiver broadcastMessageReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null) {
                        return;
                    }

                    String action = intent.getAction();
                    if (action == null) {
                        Log.w(TAG, "Intent action is null");
                        return;
                    }

                    Log.d(TAG, "Received broadcast: " + action);
                    switch (action) {
                        case ACTION_REGISTER_ACTIVITY:
                            activity = intent.getIntExtra(KEY_EXTRA_ACTIVITY, ACTIVITY_NONE);
                            sendBroadcastToActivity(); // Send current state on registration
                            break;
                        case ACTION_CLEAR:
                            String keyToCancel = intent.getStringExtra(KEY_EXTRA_KEY);
                            if (keyToCancel != null) {
                                cancelNotification(keyToCancel);
                                Log.i(TAG, "Cancelled notification with key: " + keyToCancel);
                            } else {
                                Log.w(TAG, "ACTION_CLEAR received with null key.");
                            }
                            break;
                        case ACTION_CLEAR_MISSED_CALLS:
                            Log.i(TAG, "Received broadcast to clear missed call notifications.");
                            clearAllIdentifiedMissedCallNotifications();
                            break;
                        default:
                            Log.e(TAG, "Received unknown broadcast action: " + action);
                            // throw new AssertionError("Unknown action: " + action);
                            break;
                    }
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate");
        defaultDialerPackage = getDefaultDialerPackage(this);
        Log.d(
                TAG,
                "Default dialer package: "
                        + (defaultDialerPackage != null ? defaultDialerPackage : "Not found"));

        telephonyManager = ContextCompat.getSystemService(this, TelephonyManager.class);
        if (telephonyManager == null) {
            Log.e(TAG, "TelephonyManager is null. Missed call detection will be impaired.");
        }

        initializePhoneStateListener();

        // Register broadcast receivers for actions this service can perform
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_REGISTER_ACTIVITY);
        intentFilter.addAction(ACTION_CLEAR);
        intentFilter.addAction(ACTION_CLEAR_MISSED_CALLS);
        lbm.registerReceiver(broadcastMessageReceiver, intentFilter);
        Log.v(TAG, "Internal broadcast receiver registered.");
    }

    private void initializePhoneStateListener() {
        phoneStateListener =
                new PhoneStateListener() {
                    @Override
                    public void onCallStateChanged(int state, String incomingNumber) {
                        // Use a descriptive incomingNumber if available, otherwise a placeholder
                        String numberForLog =
                                (incomingNumber != null && !incomingNumber.isEmpty())
                                        ? incomingNumber
                                        : "unknown_number";
                        Log.d(
                                TAG,
                                "onCallStateChanged: state="
                                        + callStateToString(state)
                                        + ", incomingNumber="
                                        + numberForLog
                                        + ", isDefaultDialerRinging_before="
                                        + isDefaultDialerRinging);

                        switch (state) {
                            case TelephonyManager.CALL_STATE_RINGING:
                                isDefaultDialerRinging = true; // Set flag when ringing starts
                                onIncomingCall(numberForLog, defaultDialerPackage);
                                break;
                            case TelephonyManager.CALL_STATE_OFFHOOK:
                                isDefaultDialerRinging =
                                        false; // Call is answered or outgoing, reset flag
                                onCallOngoing(numberForLog, defaultDialerPackage);
                                break;
                            case TelephonyManager.CALL_STATE_IDLE:
                                if (isDefaultDialerRinging) {
                                    // If it was ringing and didn't go to OFFHOOK before IDLE, it's
                                    // a missed call
                                    onMissedCall(numberForLog, defaultDialerPackage);
                                }
                                isDefaultDialerRinging =
                                        false; // Reset flag after handling IDLE state
                                break;
                            default:
                                Log.w(TAG, "Unknown call state: " + state);
                                isDefaultDialerRinging = false; // Reset on unknown state too
                                break;
                        }
                        Log.d(
                                TAG,
                                "onCallStateChanged: isDefaultDialerRinging_after="
                                        + isDefaultDialerRinging);
                    }
                };
    }

    @Nullable
    private String getDefaultDialerPackage(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            TelecomManager tm = ContextCompat.getSystemService(context, TelecomManager.class);
            if (tm != null) {
                return tm.getDefaultDialerPackage();
            } else {
                Log.w(TAG, "TelecomManager is null, cannot get default dialer package.");
            }
        } else {
            // For older APIs, this is more complex and less reliable.
            // One common approach is to find apps handling the DIAL intent.
            // However, this might not always give the "default" in the same sense.
            // For simplicity, we'll return null here. A more robust solution for pre-M
            // might involve querying PackageManager for dialer activities.
            Log.i(
                    TAG,
                    "Cannot get default dialer package on pre-M reliably without PackageManager query.");
            return null;
        }
        return null; // Fallback
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) {
            Log.w(TAG, "onNotificationPosted: StatusBarNotification is null.");
            return;
        }
        Log.v(TAG, "onNotificationPosted: " + sbn.getPackageName() + " ID: " + sbn.getId());

        // For debugging, print full details of posted notifications
        // String notificationDetails = NotificationUtils.statusBarNotificationToString(sbn);
        // Log.d(TAG, "Notification Posted Details:\n" + notificationDetails);

        // Optional: Implement logic based on specific notifications if needed,
        // e.g., identifying types of notifications other than missed calls.
        // Example: if (sbn.getPackageName().equals("com.whatsapp")) { /* Handle WhatsApp */ }

        sendBroadcastToActivity();
    }

    // Callback stubs - replace with actual UI update or logic
    private void onIncomingCall(String caller, String pkg) {
        Log.i(TAG, "Incoming call from " + caller + (pkg != null ? " via " + pkg : ""));
        // Potentially trigger UI updates for incoming call if needed by your app
    }

    private void onCallOngoing(String caller, String pkg) {
        Log.i(TAG, "Call ongoing with " + caller + (pkg != null ? " via " + pkg : ""));
        // Potentially trigger UI updates for ongoing call
    }

    private void onMissedCall(String caller, String pkg) {
        Log.i(TAG, "Missed call from " + caller + (pkg != null ? " via " + pkg : ""));
        // This is where you might trigger logic specific to a missed call event,
        // such as incrementing a badge count or logging the event internally.
        // The "Missed Call" notification itself is typically handled by the system or dialer.
        // This callback is based on TelephonyManager state changes.
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null) {
            Log.w(TAG, "onNotificationRemoved: StatusBarNotification is null.");
            return;
        }
        Log.v(TAG, "onNotificationRemoved: " + sbn.getPackageName() + " ID: " + sbn.getId());

        // For debugging, print full details of removed notifications
        // String notificationDetails = NotificationUtils.statusBarNotificationToString(sbn);
        // Log.d(TAG, "Notification Removed Details:\n" + notificationDetails);

        sendBroadcastToActivity();
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.v(TAG, "onListenerConnected");
        if (!isServiceListening) {
            isServiceListening = true;

            if (telephonyManager != null && phoneStateListener != null) {
                try {
                    telephonyManager.listen(
                            phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                    Log.i(TAG, "PhoneStateListener registered.");
                } catch (SecurityException e) {
                    Log.e(
                            TAG,
                            "SecurityException registering PhoneStateListener. Check READ_PHONE_STATE permission.",
                            e);
                    // This typically happens if READ_PHONE_STATE permission is missing or revoked
                    // at runtime.
                }
            } else {
                Log.w(
                        TAG,
                        "TelephonyManager or PhoneStateListener is null. Cannot register for call state changes.");
            }
            sendBroadcastToActivity(); // Send current notifications list to newly connected
            // activity
        }
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastMessageReceiver);
        Log.v(TAG, "Internal broadcast receiver unregistered.");

        if (telephonyManager != null && phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            Log.i(TAG, "PhoneStateListener unregistered due to service destruction.");
        }
        super.onDestroy();
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.w(TAG, "onListenerDisconnected.");
        if (isServiceListening) {
            isServiceListening = false;

            if (telephonyManager != null && phoneStateListener != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
                Log.i(TAG, "PhoneStateListener unregistered.");
            }
        }
    }

    private void sendBroadcastToActivity() {
        if (!isServiceListening) {
            Log.w(
                    TAG,
                    "Notification Listener is not actively listening (isServiceListening=false). Cannot send broadcast.");
            return;
        }

        try {
            switch (activity) {
                case NotificationListenerService.NOTIFICATIONS_ACTIVITY:
                    Log.v(TAG, "Sending broadcast to notifications activity.");
                    sendBroadcastToNotificationsActivity();
                    break;
                case NotificationListenerService.NOTIFICATIONS_HOME_SCREEN:
                    Log.v(TAG, "Sending broadcast to home screen activity.");
                    sendBroadcastToHomeScreenActivity();
                    break;
                default:
                    Log.v(TAG, "No specific activity registered to send broadcast to.");
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending broadcast to activity", e);
        }
    }

    private void sendBroadcastToNotificationsActivity() {
        try {
            final StatusBarNotification[] statusBarNotifications = getActiveNotifications();
            final Bundle[] bundlesToSend = new Bundle[statusBarNotifications.length];

            for (int i = 0, statusBarNotificationsLength = statusBarNotifications.length;
                    i < statusBarNotificationsLength;
                    i++) {
                Log.d(
                        TAG,
                        "Sending notification: Package="
                                + statusBarNotifications[i].getPackageName()
                                + ", ID="
                                + statusBarNotifications[i].getId()
                                + ", Key="
                                + statusBarNotifications[i].getKey());
                final StatusBarNotification statusBarNotification = statusBarNotifications[i];
                final Notification notification = statusBarNotification.getNotification();
                final Bundle bundle = bundlesToSend[i] = new Bundle();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    bundle.putParcelable("small_icon", notification.getSmallIcon());
                    bundle.putParcelable("large_icon", notification.getLargeIcon());
                } else {
                    bundle.putInt("small_icon", notification.icon);
                    bundle.putParcelable("large_icon", notification.largeIcon);
                }
                bundle.putCharSequence(
                        "title", notification.extras.getCharSequence(Notification.EXTRA_TITLE));
                bundle.putCharSequence(
                        "text", notification.extras.getCharSequence(Notification.EXTRA_TEXT));
                bundle.putLong("time_stamp", notification.when);
                final CharSequence packageName = statusBarNotification.getPackageName();
                bundle.putCharSequence("packageName", packageName);

                String appName;
                try {
                    PackageManager packageManager = getPackageManager();
                    ApplicationInfo ai =
                            packageManager.getApplicationInfo(String.valueOf(packageName), 0);
                    appName = packageManager.getApplicationLabel(ai).toString();
                } catch (final PackageManager.NameNotFoundException ignore) {
                    appName = "(unknown)";
                }

                bundle.putCharSequence("app_name", appName);
                bundle.putParcelable("clear_intent", notification.deleteIntent);
                bundle.putParcelable("content_intent", notification.contentIntent);
                bundle.putBoolean(
                        "clearable", (notification.flags & Notification.FLAG_NO_CLEAR) == 0);
                bundle.putBoolean(
                        "summery",
                        (notification.flags & Notification.FLAG_GROUP_SUMMARY)
                                == Notification.FLAG_GROUP_SUMMARY);
                bundle.putString(KEY_EXTRA_KEY, statusBarNotification.getKey());
            }
            LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(
                            new Intent(NOTIFICATIONS_ACTIVITY_BROADCAST)
                                    .putExtra(KEY_EXTRA_NOTIFICATIONS, bundlesToSend));
        } catch (SecurityException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendBroadcastToHomeScreenActivity() {
        try {
            final StatusBarNotification[] statusBarNotifications = getActiveNotifications();
            final ArrayList<String> packages = new ArrayList<>(statusBarNotifications.length);
            for (final StatusBarNotification statusBarNotification : statusBarNotifications) {
                packages.add(statusBarNotification.getPackageName());
            }
            final Intent intent =
                    new Intent(HOME_SCREEN_ACTIVITY_BROADCAST)
                            .putExtra("amount", statusBarNotifications.length)
                            .putStringArrayListExtra("packages", packages);

            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } catch (SecurityException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    private String getAppNameFromPackage(String packageName) {
        if (packageName == null) return "(unknown)";
        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo ai = packageManager.getApplicationInfo(packageName, 0);
            return packageManager.getApplicationLabel(ai).toString();
        } catch (final PackageManager.NameNotFoundException e) {
            Log.w(TAG, "App name not found for package: " + packageName);
            return "(unknown)";
        }
    }

    private void clearAllIdentifiedMissedCallNotifications() {
        StatusBarNotification[] activeNotifications = getActiveNotifications();
        if (activeNotifications == null) return;

        for (StatusBarNotification sbn : activeNotifications) {
            if (sbn != null
                    && sbn.getNotification() != null
                    && Notification.CATEGORY_MISSED_CALL.equals(sbn.getNotification().category)) {
                // Check if the notification is clearable
                if ((sbn.getNotification().flags & Notification.FLAG_NO_CLEAR) == 0
                        && (sbn.getNotification().flags & Notification.FLAG_ONGOING_EVENT) == 0) {
                    cancelNotification(sbn.getKey());
                    Log.i(
                            TAG,
                            "Cleared missed call notification: Key="
                                    + sbn.getKey()
                                    + ", Package="
                                    + sbn.getPackageName());
                } else {
                    Log.i(
                            TAG,
                            "Missed call notification is not clearable (ongoing or no_clear flag): Key="
                                    + sbn.getKey());
                }
            }
        }
    }

    private String callStateToString(int state) {
        return switch (state) {
            case TelephonyManager.CALL_STATE_IDLE -> "IDLE";
            case TelephonyManager.CALL_STATE_RINGING -> "RINGING";
            case TelephonyManager.CALL_STATE_OFFHOOK -> "OFFHOOK";
            default -> "UNKNOWN_STATE_" + state;
        };
    }
}
