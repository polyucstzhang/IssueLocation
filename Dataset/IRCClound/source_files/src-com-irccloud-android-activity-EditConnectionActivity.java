/*
 * Copyright (c) 2015 IRCCloud, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.irccloud.android.activity;

import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.collection.ServersList;
import com.irccloud.android.fragment.EditConnectionFragment;

public class EditConnectionActivity extends AppCompatActivity implements NetworkConnection.IRCEventHandler {
    int reqid = -1;
    int cidToOpen = -1;
    int cid = -1;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            Bitmap cloud = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            setTaskDescription(new ActivityManager.TaskDescription(getResources().getString(R.string.app_name), cloud, 0xFFF2F7FC));
            cloud.recycle();
        }
        setContentView(R.layout.activity_edit_connection);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar));
        getSupportActionBar().setCustomView(R.layout.actionbar_edit_connection);
        getSupportActionBar().setElevation(0);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        final EditConnectionFragment newFragment = new EditConnectionFragment();
        if (getIntent() != null && getIntent().hasExtra("cid")) {
            newFragment.setCid(getIntent().getIntExtra("cid", -1));
            cid = getIntent().getIntExtra("cid", -1);
        }
        if (getIntent() != null && getIntent().hasExtra("hostname"))
            newFragment.default_hostname = getIntent().getStringExtra("hostname");
        if (getIntent() != null && getIntent().hasExtra("channels"))
            newFragment.default_channels = getIntent().getStringExtra("channels");
        newFragment.default_port = getIntent().getIntExtra("port", 6667);
        ft.replace(R.id.EditConnectionFragment, newFragment);
        ft.commit();

        getSupportActionBar().getCustomView().findViewById(R.id.action_cancel).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (ServersList.getInstance().count() < 1) {
                    NetworkConnection.getInstance().logout();
                    Intent i = new Intent(EditConnectionActivity.this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
                finish();
            }

        });

        getSupportActionBar().getCustomView().findViewById(R.id.action_done).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                reqid = newFragment.save();
            }

        });
        NetworkConnection.getInstance().addHandler(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NetworkConnection.getInstance().removeHandler(this);
    }

    public void onIRCEvent(int what, Object o) {
        IRCCloudJSONObject obj;
        Buffer buffer;
        switch (what) {
            case NetworkConnection.EVENT_MAKEBUFFER:
                buffer = (Buffer) o;
                if (buffer.getCid() == cidToOpen) {
                    Intent i = new Intent(EditConnectionActivity.this, MainActivity.class);
                    i.putExtra("bid", buffer.getBid());
                    startActivity(i);
                    finish();
                }
                break;
            case NetworkConnection.EVENT_USERINFO:
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    public void onIRCRequestSucceeded(int reqid, IRCCloudJSONObject obj) {
        if (this.reqid == reqid) {
            if (cid != -1)
                finish();
            else
                cidToOpen = obj.cid();
        }
    }

    @Override
    public void onIRCRequestFailed(int reqid, IRCCloudJSONObject obj) {
        if (this.reqid == reqid) {
            final String message = obj.getString("message");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (message) {
                        case "passworded_servers":
                            Toast.makeText(EditConnectionActivity.this, "You can’t connect to passworded servers with free accounts.", Toast.LENGTH_SHORT).show();
                            break;
                        case "networks":
                            Toast.makeText(EditConnectionActivity.this, "You've exceeded the connection limit for free accounts.", Toast.LENGTH_SHORT).show();
                            break;
                        case "unverified":
                            Toast.makeText(EditConnectionActivity.this, "You can’t connect to external servers until you confirm your email address.", Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            Toast.makeText(EditConnectionActivity.this, "Unable to add connection: invalid " + message, Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            });
        }
    }
}
