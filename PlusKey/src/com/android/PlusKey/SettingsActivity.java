/*
 * Copyright (C) 2025 LineageOS
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.android.pluskey;

import android.app.Activity;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.view.MenuItem;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PlusKeyPreferenceFragment())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class PlusKeyPreferenceFragment extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener {

        private static final String KEY_PLUS_KEY_ACTION = "plus_key_action";
        private ListPreference mPlusKeyAction;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.plus_key_preferences);
            getActivity().setTitle(R.string.plus_key_title);

            mPlusKeyAction = (ListPreference) findPreference(KEY_PLUS_KEY_ACTION);
            int currentAction = Settings.System.getInt(getContext().getContentResolver(),
                    KEY_PLUS_KEY_ACTION, 0);
            mPlusKeyAction.setValue(String.valueOf(currentAction));
            mPlusKeyAction.setSummary(mPlusKeyAction.getEntry());
            mPlusKeyAction.setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putInt(getContext().getContentResolver(), KEY_PLUS_KEY_ACTION, value);
            int index = mPlusKeyAction.findIndexOfValue((String) newValue);
            mPlusKeyAction.setSummary(mPlusKeyAction.getEntries()[index]);
            return true;
        }
    }
}
