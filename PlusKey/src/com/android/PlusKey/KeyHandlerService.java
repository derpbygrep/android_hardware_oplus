/*
 * Copyright (C) 2025 LineageOS
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.android.pluskey;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManagerGlobal;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class KeyHandlerService extends Service {
    private static final String TAG = "PlusKeyHandlerService";

    private static final String INPUT_DEVICE_PATH = "/dev/input/event7"; 
    
    private static final int EV_KEY = 0x01;
    private static final int BTN_TRIGGER_HAPPY32 = 735;
    private static final int KEY_STATE_PRESS = 1;

    private static final int ACTION_NONE = 0;
    private static final int ACTION_SCREENSHOT = 1;
    private static final int ACTION_RINGER_MODES = 2;

    private Thread mWorkerThread;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        mWorkerThread = new Thread(this::listenForInput, "PlusKeyWorker");
        mWorkerThread.start();
    }

    private void listenForInput() {
        try (InputStream is = new FileInputStream(new File(INPUT_DEVICE_PATH))) {
            byte[] buffer = new byte[16]; // Input event struct size on 64-bit systems
            while (!Thread.currentThread().isInterrupted()) {
                if (is.read(buffer) == 16) {
                    int type = (buffer[8] & 0xFF) | ((buffer[9] & 0xFF) << 8);
                    int code = (buffer[10] & 0xFF) | ((buffer[11] & 0xFF) << 8);
                    int value = (buffer[12] & 0xFF) | ((buffer[13] & 0xFF) << 8) |
                                ((buffer[14] & 0xFF) << 16) | ((buffer[15] & 0xFF) << 24);
                    if (type == EV_KEY && code == BTN_TRIGGER_HAPPY32 && value == KEY_STATE_PRESS) {
                        handleKeyPress();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading input device. Path correct? Permissions set?", e);
        }
    }

    private void handleKeyPress() {
        final int action = Settings.System.getIntForUser(getContentResolver(),
                "plus_key_action", ACTION_NONE, UserHandle.USER_CURRENT);
        switch (action) {
            case ACTION_SCREENSHOT: takeScreenshot(); break;
            case ACTION_RINGER_MODES: cycleRingerMode(); break;
        }
    }

    private void takeScreenshot() {
        mHandler.post(() -> {
            try { WindowManagerGlobal.getWindowManagerService().takeScreenshot(0); } catch (Exception e) {}
        });
    }

    private void cycleRingerMode() {
        AudioManager am = getSystemService(AudioManager.class);
        if (am == null) return;
        int currentMode = am.getRingerMode();
        if (currentMode == AudioManager.RINGER_MODE_NORMAL) {
            am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        } else if (currentMode == AudioManager.RINGER_MODE_VIBRATE) {
            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        } else {
            am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() { if (mWorkerThread != null) mWorkerThread.interrupt(); super.onDestroy(); }
}
