package com.example.studentlifeos;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/** A circular progress ring with text in the middle, used for the fit score. */
public class ScoreRingView extends View {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();

    private int score = 0;
    private String center = "";

    public ScoreRingView(Context context) { this(context, null); }

    public ScoreRingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        track.setStyle(Paint.Style.STROKE);
        track.setColor(0x33808080);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeCap(Paint.Cap.ROUND);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        TypedArray a = context.obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
        text.setColor(a.getColor(0, 0xFF222222));
        a.recycle();
    }

    /** @param score 0-100 (how much of the ring is filled) @param color ring colour @param centerText e.g. "56%" */
    public void set(int score, int color, String centerText) {
        this.score = Math.max(0, Math.min(100, score));
        this.center = centerText;
        arc.setColor(color);
        invalidate();
    }

    /** Colour of the text in the middle (defaults to the theme's primary text colour). */
    public void setCenterColor(int color) {
        text.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float size = Math.min(getWidth(), getHeight());
        float stroke = size * 0.11f;
        track.setStrokeWidth(stroke);
        arc.setStrokeWidth(stroke);
        float pad = stroke / 2f;
        float left = (getWidth() - size) / 2f, top = (getHeight() - size) / 2f;
        box.set(left + pad, top + pad, left + size - pad, top + size - pad);

        canvas.drawOval(box, track);
        if (score > 0) canvas.drawArc(box, -90f, 360f * score / 100f, false, arc);

        text.setTextSize(size * (center.length() > 3 ? 0.24f : 0.29f));
        float y = getHeight() / 2f - (text.descent() + text.ascent()) / 2f;
        canvas.drawText(center, getWidth() / 2f, y, text);
    }
}