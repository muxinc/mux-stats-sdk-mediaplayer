package com.mux.stats.sdk.muxstats.mediaplayer.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SampleChooserActivity extends Activity
        implements ExpandableListView.OnChildClickListener {
    private static final String TAG = "SampleChooserActivity";

    private SampleAdapter sampleAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_chooser_activity);
        sampleAdapter = new SampleAdapter();
        ExpandableListView sampleListView = findViewById(R.id.sample_list);
        sampleListView.setAdapter(sampleAdapter);
        sampleListView.setOnChildClickListener(this);

        List<SampleGroup> sampleGroups = readSampleGroups();
        sampleAdapter.setSampleGroups(sampleGroups);
    }

    @Override
    public boolean onChildClick(
            ExpandableListView parent, View view, int groupPosition, int childPosition, long id) {
        Sample sample = (Sample) view.getTag();
        startActivity(sample.buildIntent(this));
        return true;
    }

    private List<SampleGroup> readSampleGroups() {
        List<SampleGroup> sampleGroups = new ArrayList<>();

        try {
            InputStream inputStream = getAssets().open("media.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            String jsonString = new String(buffer, "UTF-8");
            JSONArray sampleGroupArray = new JSONArray(jsonString);
            for (int i = 0; i < sampleGroupArray.length(); i++) {
                JSONObject sampleGroupJson = sampleGroupArray.getJSONObject(i);
                SampleGroup sampleGroup = new SampleGroup(sampleGroupJson.getString("name"));
                sampleGroups.add(sampleGroup);
                JSONArray sampleArray = sampleGroupJson.getJSONArray("samples");
                for (int j = 0; j < sampleArray.length(); j++) {
                    JSONObject sampleJson = sampleArray.getJSONObject(j);
                    Sample sample = new Sample(sampleJson.getString("name"),
                            Uri.parse(sampleJson.getString("uri")));
                    sampleGroup.samples.add(sample);
                }
            }
        } catch (IOException | JSONException exc) {
            Log.e(TAG, Log.getStackTraceString(exc));
            Toast.makeText(getApplicationContext(),
                    "Sample list failed to load", Toast.LENGTH_LONG)
                    .show();
        }
    return sampleGroups;
    }

    private static final class SampleGroup {

        public final String title;
        public final List<Sample> samples;

        public SampleGroup(String title) {
            this.title = title;
            this.samples = new ArrayList<>();
        }

    }

    private static final class Sample {
        public final String name;
        public final Uri uri;

        public Sample(String name, Uri uri) {
            this.name = name;
            this.uri = uri;
        }

        public Intent buildIntent(Context context) {
            return new Intent(context, PlayerActivity.class)
                    .setData(uri)
                    .putExtra(PlayerActivity.VIDEO_TITLE_EXTRA, name);
        }
    }

    private final class SampleAdapter extends BaseExpandableListAdapter {
        private List<SampleGroup> sampleGroups;

        public SampleAdapter() {
            sampleGroups = Collections.emptyList();
        }

        public void setSampleGroups(List<SampleGroup> sampleGroups) {
            this.sampleGroups = sampleGroups;
            notifyDataSetChanged();
        }

        @Override
        public int getGroupCount() {
            return sampleGroups.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return getGroup(groupPosition).samples.size();
        }

        @Override
        public SampleGroup getGroup(int groupPosition) {
            return sampleGroups.get(groupPosition);
        }

        @Override
        public Sample getChild(int groupPosition, int childPosition) {
            return getGroup(groupPosition).samples.get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                                 ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view =
                        getLayoutInflater()
                                .inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
            }
            ((TextView) view).setText(getGroup(groupPosition).title);
            return view;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                                 View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.sample_list_item, parent, false);
            }
            initializeChildView(view, getChild(groupPosition, childPosition));
            return view;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        private void initializeChildView(View view, Sample sample) {
            view.setTag(sample);
            TextView sampleTitle = view.findViewById(R.id.sample_title);
            sampleTitle.setText(sample.name);
        }
    }
}
