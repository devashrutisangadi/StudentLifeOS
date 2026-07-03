package com.example.studentlifeos;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashGif";
    private static final long SPLASH_DURATION_MS = 6000; // matches gif runtime

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        setContentView(R.layout.activity_splash);

        ImageView ivSplashGif = findViewById(R.id.ivSplashGif);

        // Detect current system theme
        int nightModeFlags = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkMode = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);

        String gifFileName = isDarkMode ? "nocturne_splash.gif" : "nocturne_splash_light.gif";

        Glide.with(this)
                .asGif()
                .load("file:///android_asset/" + gifFileName)
                .listener(new RequestListener<GifDrawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<GifDrawable> target, boolean isFirstResource) {
                        Log.e(TAG, "Failed to load: " + gifFileName, e);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GifDrawable resource, Object model,
                                                   Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d(TAG, "Loaded successfully: " + gifFileName);
                        resource.setLoopCount(1); // play once, then freeze on last frame
                        return false;
                    }
                })
                .into(ivSplashGif);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
            finish();
        }, SPLASH_DURATION_MS);
    }
}