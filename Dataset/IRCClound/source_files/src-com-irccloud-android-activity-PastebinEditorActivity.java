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
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.OnErrorListener;
import com.irccloud.android.data.model.Pastebin;

public class PastebinEditorActivity extends AppCompatActivity implements NetworkConnection.IRCEventHandler {

    private OnErrorListener<Pastebin> pastebinOnErrorListener = new OnErrorListener<Pastebin>() {
        @Override
        public void onSuccess(Pastebin object) {
            result(RESULT_OK);
            finish();
        }

        @Override
        public void onFailure(Pastebin object) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(PastebinEditorActivity.this, "Unable to save pastebin, please try again shortly.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private class FetchPastebinTask extends AsyncTaskEx<String, Void, Pastebin> {

        @Override
        protected Pastebin doInBackground(String... params) {
            try {
                return Pastebin.fetch(params[0]);
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Pastebin p) {
            if (p != null) {
                pastebin = p;
                paste.setText(p.getBody());
                filename.setText(p.getName());
            }
        }
    }

    private EditText paste;
    private EditText filename;
    private EditText message;
    private TextView messages_count;
    private Pastebin pastebin = new Pastebin();
    private int current_tab = 0;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            Bitmap cloud = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            setTaskDescription(new ActivityManager.TaskDescription(getResources().getString(R.string.app_name), cloud, 0xFFF2F7FC));
            cloud.recycle();
        }
        setContentView(R.layout.activity_pastebineditor);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) {
            if(!getWindow().isFloating()) {
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        paste = (EditText) findViewById(R.id.paste);
        filename = (EditText) findViewById(R.id.filename);
        message = (EditText) findViewById(R.id.message);
        messages_count = (TextView) findViewById(R.id.messages_count);

        if (savedInstanceState != null && savedInstanceState.containsKey("message"))
            message.setText(savedInstanceState.getString("message"));

        if (savedInstanceState != null && savedInstanceState.containsKey("pastebin"))
            pastebin = (Pastebin)savedInstanceState.getSerializable("pastebin");
        else if (getIntent() != null && getIntent().hasExtra("paste_id"))
            pastebin.setId(getIntent().getStringExtra("paste_id"));

        if (getIntent() != null && getIntent().hasExtra("paste_contents"))
            pastebin.setBody(getIntent().getStringExtra("paste_contents"));

        paste.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                int count = 0;
                String lines[] = editable.toString().split("\n");
                for(String line : lines) {
                    count += Math.ceil(line.length() / 1080.0f);
                }
                messages_count.setText("Text will be sent as " + count + " message" + (count==1?"":"s"));
            }
        });
        paste.setText(pastebin.getBody());

        if (getIntent() != null && getIntent().hasExtra("filename"))
            filename.setText(getIntent().getStringExtra("filename"));

        TabLayout tabHost = (TabLayout) findViewById(android.R.id.tabhost);
        ViewCompat.setElevation(toolbar, ViewCompat.getElevation(tabHost));

        if (pastebin.getId() != null) {
            tabHost.setVisibility(View.GONE);
            message.setVisibility(View.GONE);
            findViewById(R.id.message_heading).setVisibility(View.GONE);
        } else {
            tabHost.setTabGravity(TabLayout.GRAVITY_FILL);
            tabHost.setTabMode(TabLayout.MODE_FIXED);
            tabHost.addTab(tabHost.newTab().setText("Pastebin"));
            tabHost.addTab(tabHost.newTab().setText("Messages"));
            tabHost.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    current_tab = tab.getPosition();
                    if (current_tab == 0) {
                        filename.setVisibility(View.VISIBLE);
                        message.setVisibility(View.VISIBLE);
                        messages_count.setVisibility(View.GONE);
                        findViewById(R.id.filename_heading).setVisibility(View.VISIBLE);
                        findViewById(R.id.message_heading).setVisibility(View.VISIBLE);
                    } else {
                        filename.setVisibility(View.GONE);
                        message.setVisibility(View.GONE);
                        messages_count.setVisibility(View.VISIBLE);
                        findViewById(R.id.filename_heading).setVisibility(View.GONE);
                        findViewById(R.id.message_heading).setVisibility(View.GONE);
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });
            if (savedInstanceState != null && savedInstanceState.containsKey("tab"))
                tabHost.getTabAt(savedInstanceState.getInt("tab")).select();
        }

        NetworkConnection.getInstance().addHandler(this);
        if (pastebin.getId() != null && (pastebin.getBody() == null || pastebin.getBody().length() == 0)) {
            new FetchPastebinTask().execute(pastebin.getId());
        }

        if(pastebin.getId() != null) {
            setTitle(R.string.title_activity_pastebin_editor_edit);
            toolbar.setBackgroundResource(R.drawable.actionbar);
        } else {
            setTitle(R.string.title_activity_pastebin_editor);
        }

        supportInvalidateOptionsMenu();

        result(RESULT_CANCELED);
    }

    @Override
    protected void onDestroy() {
        NetworkConnection.getInstance().removeHandler(this);
        super.onDestroy();
    }

    private String extension() {
        try {
            if (filename != null && filename.getText() != null && filename.getText().length() > 0) {
                String file = filename.getText().toString();
                if (file.contains(".")) {
                    String extension = file.substring(file.lastIndexOf(".") + 1);
                    if (extension.length() > 0)
                        return extension;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "txt";
    }

    @Override
    public void onIRCEvent(final int msg, Object obj) {

    }

    @Override
    public void onIRCRequestSucceeded(int reqid, IRCCloudJSONObject object) {

    }

    @Override
    public void onIRCRequestFailed(int reqid, IRCCloudJSONObject object) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_pastebineditor, menu);
        if(pastebin.getId() != null) {
            menu.findItem(R.id.action_save).setVisible(true);
            menu.findItem(R.id.action_send).setVisible(false);
        } else {
            menu.findItem(R.id.action_save).setVisible(false);
            menu.findItem(R.id.action_send).setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_save:
                pastebin.setBody(paste.getText().toString());
                pastebin.setName(filename.getText().toString());
                pastebin.setExtension(extension());
                pastebin.save(pastebinOnErrorListener);
                break;
            case R.id.action_send:
                pastebin.setBody(paste.getText().toString());
                if(current_tab == 0) {
                    pastebin.setName(filename.getText().toString());
                    pastebin.setExtension(extension());
                    pastebin.save(pastebinOnErrorListener);
                } else {
                    result(RESULT_OK);
                    finish();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void result(int resultCode) {
        Intent data = new Intent();
        data.putExtra("paste_contents", pastebin.getBody());
        data.putExtra("paste_id", pastebin.getId());
        data.putExtra("message", message.getText().toString());
        data.putExtra("url", pastebin.getUrl());
        setResult(resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        pastebin.setBody(paste.getText().toString());
        pastebin.setName(filename.getText().toString());
        outState.putSerializable("pastebin", pastebin);
        outState.putString("message", message.getText().toString());
        outState.putInt("tab", current_tab);
    }
}