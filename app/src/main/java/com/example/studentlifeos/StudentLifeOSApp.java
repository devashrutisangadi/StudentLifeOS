package com.example.studentlifeos;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

public class StudentLifeOSApp extends Application {

    public static final String PREFS_NAME = "app_prefs";
    public static final String KEY_DARK_MODE = "dark_mode_enabled";

    // Replace with YOUR actual cloud name from Step 1
    private static final String CLOUDINARY_CLOUD_NAME = "wtyq71mi";

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", CLOUDINARY_CLOUD_NAME);
        MediaManager.init(this, config);
    }
}