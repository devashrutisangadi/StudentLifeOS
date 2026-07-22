package com.example.studentlifeos;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class NotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        LinearLayout container = findViewById(R.id.layoutNotifications);
        populateNotifications(container);
    }

    // --- Dummy/placeholder data for now ---
    private void populateNotifications(LinearLayout container) {
        container.removeAllViews();

        addNotificationItem(container, "Announcement", "New syllabus for Data Structures posted.", "#E57373", "2h ago");
        addNotificationItem(container, "Note", "CS203 lecture on logic gates postponed.", "#A2ABFA", "5h ago");
        addNotificationItem(container, "Reading", "'Network Flows' now available for Discrete Structures.", "#7FD99D", "1d ago");
        addNotificationItem(container, "Message from Prof. Pillay", "Java mid-term feedback open.", "#F0C05A", "2d ago");
    }

    private void addNotificationItem(LinearLayout container, String label, String body, String accentHex, String timeAgo) {
        int textColor = ContextCompat.getColor(this, R.color.notification_text_color);
        int metaColor = ContextCompat.getColor(this, R.color.notification_meta_color);

        // Outer card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(14);
        card.setLayoutParams(cardParams);
        card.setBackgroundResource(R.drawable.bg_notification_card);
        card.setGravity(Gravity.CENTER_VERTICAL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setClipToOutline(true); // ensures stripe respects rounded corners
        }

        // Colored left stripe
        View stripe = new View(this);
        LinearLayout.LayoutParams stripeParams = new LinearLayout.LayoutParams(dp(4), LinearLayout.LayoutParams.MATCH_PARENT);
        stripe.setLayoutParams(stripeParams);
        stripe.setBackgroundColor(Color.parseColor(accentHex));
        card.addView(stripe);

        // Text column: label, body, metadata — stacked with spacing hierarchy
        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textColumn.setLayoutParams(colParams);
        textColumn.setPadding(dp(14), dp(14), dp(14), dp(14));

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(14);
        tvLabel.setTypeface(null, Typeface.BOLD);
        tvLabel.setTextColor(textColor);
        textColumn.addView(tvLabel);

        TextView tvBody = new TextView(this);
        tvBody.setText(body);
        tvBody.setTextSize(13);
        tvBody.setTextColor(textColor);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bodyParams.topMargin = dp(4);
        tvBody.setLayoutParams(bodyParams);
        textColumn.addView(tvBody);

        TextView tvMeta = new TextView(this);
        tvMeta.setText(timeAgo);
        tvMeta.setTextSize(11);
        tvMeta.setTextColor(metaColor);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        metaParams.topMargin = dp(8);
        tvMeta.setLayoutParams(metaParams);
        textColumn.addView(tvMeta);

        card.addView(textColumn);
        container.addView(card);
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}