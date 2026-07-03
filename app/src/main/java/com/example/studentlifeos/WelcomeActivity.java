package com.example.studentlifeos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button btnCreateAccount = findViewById(R.id.btnCreateAccount);
        Button btnLogIn = findViewById(R.id.btnLogIn);

        btnCreateAccount.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, SignUpActivity.class)));

        btnLogIn.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class)));
    }
}