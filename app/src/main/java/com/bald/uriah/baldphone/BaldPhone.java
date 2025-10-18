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

package com.bald.uriah.baldphone;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.bald.uriah.baldphone.activities.UpdatesActivity;
import com.bald.uriah.baldphone.databases.alarms.AlarmScheduler;
import com.bald.uriah.baldphone.databases.reminders.ReminderScheduler;
import com.bald.uriah.baldphone.services.NotificationListenerService;
import com.bald.uriah.baldphone.utils.BaldUncaughtExceptionHandler;
import com.bald.uriah.baldphone.utils.S;

import net.danlew.android.joda.JodaTimeAndroid;

import app.baldphone.neo.helper.VibratorHelper;

public class BaldPhone extends Application {
    private static final String TAG = BaldPhone.class.getSimpleName();
    // Application class should not have any fields, http://www.developerphil.com/dont-store-data-in-the-application-object/

    @Override
    public void onCreate() {
        S.logImportant("BaldPhone was started!");
        super.onCreate();
        JodaTimeAndroid.init(this);
        AlarmScheduler.reStartAlarms(this);
        ReminderScheduler.reStartReminders(this);
        if (BuildConfig.FLAVOR.equals("baldUpdates")) {
            UpdatesActivity.removeUpdatesInfo(this);
        }
        try {
            startService(new Intent(this, NotificationListenerService.class));
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
        VibratorHelper.init(this);
        S.sendVersionInfo(this);
    }

    @Override
    protected void attachBaseContext(final Context base) {
        super.attachBaseContext(base);

        // TODO: [Error Reporting] Re-evaluate and implement a new error reporting solution.
        // The previous ACRA implementation was commented out for future replacement.
        // Investigation options, server endpoint, and configuration.
//        final CoreConfigurationBuilder builder =
//                new CoreConfigurationBuilder(this)
//                        .setBuildConfigClass(BuildConfig.class)
//                        .setReportFormat(StringFormat.JSON);
//        builder.getPluginConfigurationBuilder(HttpSenderConfigurationBuilder.class)
//                .setUri(getString(R.string.tt_url))
//                .setHttpMethod(HttpSender.Method.POST)
//                .setEnabled(false);
//        ACRA.init(this, builder);

        Thread.setDefaultUncaughtExceptionHandler(
                new BaldUncaughtExceptionHandler(this, Thread.getDefaultUncaughtExceptionHandler())
        );
    }
}