/* SPDX-License-Identifier: Apache-2.0 */
/*
 * Copyright 2025 Damian Kuzmiak
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

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.RequiresApi;

/** An {@link AccessibilityService} that provides the capability to lock the device screen. */
public class DeviceLockService extends AccessibilityService {

    private static final String TAG = "DeviceLockService";
    private static DeviceLockService instance;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(TAG, "onAccessibilityEvent: " + event.toString());
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this; // Store instance
        Log.d(TAG, "Accessibility Service Connected");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        instance = null;
        Log.d(TAG, "Accessibility Service Unbound");
        return super.onUnbind(intent);
    }

    /**
     * Attempts to lock the device screen using the Accessibility Service's global action. This
     * action is only available on Android P (API 28) and above.
     *
     * @return true if the lock screen action was successfully dispatched, false otherwise (e.g.,
     *     service not available, or action failed).
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    public boolean lockScreen() {
        if (instance == null) {
            Log.e(TAG, "Accessibility Service instance is null. Cannot lock screen.");
            return false;
        }

        boolean success = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        Log.v(TAG, "Lock screen action performed result: " + success);
        return success;
    }

    // Static method to get the instance (use with caution, ensure service is running)
    public static DeviceLockService getInstance() {
        return instance;
    }
}
