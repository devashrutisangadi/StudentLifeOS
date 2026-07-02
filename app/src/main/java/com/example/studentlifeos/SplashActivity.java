package com.example.studentlifeos;

import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView splashImage = findViewById(R.id.splashImage);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(splashImage, "alpha", 0f, 1f);
        fadeIn.setDuration(200);

        ObjectAnimator zoomX = ObjectAnimator.ofFloat(splashImage, "scaleX", 1.1f, 1f);
        ObjectAnimator zoomY = ObjectAnimator.ofFloat(splashImage, "scaleY", 1.1f, 1f);
        zoomX.setDuration(1000);
        zoomY.setDuration(1000);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fadeIn, zoomX, zoomY);
        set.start();

        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }, 1500); // splash stays visible for 1.5    seconds total
    }
}