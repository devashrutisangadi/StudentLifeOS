package com.example.studentlifeos;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/** Hosts PapersFragment (the subject picker for PYQ papers) now that Papers is no longer a bottom-nav tab. */
public class PapersActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_papers);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.papersContainer, new PapersFragment())
                    .commit();
        }
    }
}