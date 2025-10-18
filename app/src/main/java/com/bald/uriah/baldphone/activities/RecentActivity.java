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

package com.bald.uriah.baldphone.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.adapters.CallsRecyclerViewAdapter;
import com.bald.uriah.baldphone.databases.calls.CallLogsHelper;
import com.bald.uriah.baldphone.services.NotificationListenerService;

public class RecentActivity extends BaldActivity {
    private static final String TAG = "RecentActivity";
    public RecyclerView recyclerView;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkPermissions(this, requiredPermissions()))
            return;

        setContentView(R.layout.activity_recent);

        recyclerView = findViewById(R.id.recycler_view);
        final DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(getDrawable(R.drawable.ll_divider));
        recyclerView.addItemDecoration(dividerItemDecoration);
        final CallsRecyclerViewAdapter callsRecyclerViewAdapter = new CallsRecyclerViewAdapter(CallLogsHelper.getAllCalls(getContentResolver()), this);
        CallLogsHelper.markAllAsRead(getContentResolver());
        recyclerView.setAdapter(callsRecyclerViewAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart");
        // Temporary solution to clear missed calls notification
        sendClearMissedCallsBroadcast();
    }

    private void sendClearMissedCallsBroadcast() {
        Log.i(TAG, "Sending broadcast to clear missed call notifications.");
        Intent intent = new Intent(NotificationListenerService.ACTION_CLEAR_MISSED_CALLS);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_WRITE_CALL_LOG | PERMISSION_READ_CONTACTS;
    }
}
