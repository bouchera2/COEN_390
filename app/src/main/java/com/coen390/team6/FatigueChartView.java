package com.coen390.team6;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FatigueChartView extends View {

    //  Data
    private List<Float> scores     = new ArrayList<>();
    private List<Long>  timestamps = new ArrayList<>();
    private boolean     isLive     = true; // true = live session, false = historical

    //  Paint
    private final Paint linePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint zonePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int   PADDING_LEFT   = 48;
    private static final int   PADDING_RIGHT  = 16;
    private static final int   PADDING_TOP    = 16;
    private static final int   PADDING_BOTTOM = 32;

    public FatigueChartView(Context context) {
        super(context);
        init();
    }

    public FatigueChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FatigueChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint.setColor(Color.parseColor("#135BEC"));
        linePaint.setStrokeWidth(3f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(Color.WHITE);
        dotPaint.setStrokeWidth(2f);

        fillPaint.setStyle(Paint.Style.FILL);

        gridPaint.setColor(Color.parseColor("#1AE2E8F0"));
        gridPaint.setStrokeWidth(1f);
        gridPaint.setStyle(Paint.Style.STROKE);

        labelPaint.setColor(Color.parseColor("#94A3B8"));
        labelPaint.setTextSize(24f);
        labelPaint.setAntiAlias(true);

        zonePaint.setStyle(Paint.Style.FILL);
    }

    //  Public API

    public void setLiveData(List<Float> s, List<Long> t) {
        this.scores     = new ArrayList<>(s);
        this.timestamps = new ArrayList<>(t);
        this.isLive     = true;
        invalidate();
    }

    public void setHistoricalData(List<Float> s, List<Long> t) {
        // Merge: historical first, then live appended on top
        // Actually we keep both separate and draw historical as background dots
        // For simplicity: if live is empty, show historical
        if (this.scores.isEmpty()) {
            this.scores     = new ArrayList<>(s);
            this.timestamps = new ArrayList<>(t);
            this.isLive     = false;
            invalidate();
        }
        // If live already has data, we ignore historical (live takes precedence)
    }

    //  Draw
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float chartW = w - PADDING_LEFT - PADDING_RIGHT;
        float chartH = h - PADDING_TOP  - PADDING_BOTTOM;

        if (scores.size() < 2) {
            // Draw empty state
            labelPaint.setTextAlign(Paint.Align.CENTER);
            labelPaint.setTextSize(28f);
            canvas.drawText("Collecting data…", w / 2f, h / 2f, labelPaint);
            return;
        }

        // ── Background zones (0-30 green, 30-60 orange, 60-100 red) ──────────
        drawZones(canvas, chartW, chartH);

        // ── Grid lines
        drawGrid(canvas, chartW, chartH);

        // ── Y-axis labels
        drawYLabels(canvas, chartH);

        // ── X-axis time labels
        drawXLabels(canvas, chartW, chartH);

        // ── Fill area under curve
        drawFill(canvas, chartW, chartH);

        // ── Line
        drawLine(canvas, chartW, chartH);

        // ── Dots
        drawDots(canvas, chartW, chartH);
    }

    private void drawZones(Canvas canvas, float cw, float ch) {
        float y30  = PADDING_TOP + ch * (1f - 30f  / 100f);
        float y60  = PADDING_TOP + ch * (1f - 60f  / 100f);
        float yTop = PADDING_TOP;
        float yBot = PADDING_TOP + ch;
        float xL   = PADDING_LEFT;
        float xR   = PADDING_LEFT + cw;

        // Green zone (0-30) — bottom
        zonePaint.setColor(Color.parseColor("#0A22C55E"));
        canvas.drawRect(xL, y30, xR, yBot, zonePaint);

        // Orange zone (30-60)
        zonePaint.setColor(Color.parseColor("#0AF97316"));
        canvas.drawRect(xL, y60, xR, y30, zonePaint);

        // Red zone (60-100) — top
        zonePaint.setColor(Color.parseColor("#0AEF4444"));
        canvas.drawRect(xL, yTop, xR, y60, zonePaint);
    }

    private void drawGrid(Canvas canvas, float cw, float ch) {
        for (int val = 0; val <= 100; val += 25) {
            float y = PADDING_TOP + ch * (1f - val / 100f);
            canvas.drawLine(PADDING_LEFT, y, PADDING_LEFT + cw, y, gridPaint);
        }
    }

    private void drawYLabels(Canvas canvas, float ch) {
        labelPaint.setTextAlign(Paint.Align.RIGHT);
        labelPaint.setTextSize(22f);
        for (int val = 0; val <= 100; val += 25) {
            float y = PADDING_TOP + ch * (1f - val / 100f) + 8f;
            canvas.drawText(String.valueOf(val), PADDING_LEFT - 6, y, labelPaint);
        }
    }

    private void drawXLabels(Canvas canvas, float cw, float ch) {
        if (timestamps.isEmpty()) return;
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(20f);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        int n = scores.size();
        // Show first, middle, last
        int[] indices = {0, n / 2, n - 1};
        for (int i : indices) {
            float x = PADDING_LEFT + (i / (float)(n - 1)) * cw;
            float y = PADDING_TOP + ch + PADDING_BOTTOM - 4f;
            canvas.drawText(sdf.format(new Date(timestamps.get(i))), x, y, labelPaint);
        }
    }

    private void drawFill(Canvas canvas, float cw, float ch) {
        int n = scores.size();
        Path path = new Path();
        float x0 = PADDING_LEFT;
        float y0 = PADDING_TOP + ch * (1f - scores.get(0) / 100f);
        path.moveTo(x0, y0);
        for (int i = 1; i < n; i++) {
            float x = PADDING_LEFT + (i / (float)(n - 1)) * cw;
            float y = PADDING_TOP + ch * (1f - scores.get(i) / 100f);
            path.lineTo(x, y);
        }
        float xLast = PADDING_LEFT + cw;
        float yBot  = PADDING_TOP + ch;
        path.lineTo(xLast, yBot);
        path.lineTo(x0, yBot);
        path.close();

        LinearGradient gradient = new LinearGradient(
                0, PADDING_TOP, 0, PADDING_TOP + ch,
                Color.parseColor("#33135BEC"),
                Color.parseColor("#00135BEC"),
                Shader.TileMode.CLAMP
        );
        fillPaint.setShader(gradient);
        canvas.drawPath(path, fillPaint);
    }

    private void drawLine(Canvas canvas, float cw, float ch) {
        int n = scores.size();
        Path path = new Path();
        float x0 = PADDING_LEFT;
        float y0 = PADDING_TOP + ch * (1f - scores.get(0) / 100f);
        path.moveTo(x0, y0);
        for (int i = 1; i < n; i++) {
            float x = PADDING_LEFT + (i / (float)(n - 1)) * cw;
            float y = PADDING_TOP + ch * (1f - scores.get(i) / 100f);
            path.lineTo(x, y);
        }
        // Color line based on last score
        float last = scores.get(n - 1);
        if (last < 30)      linePaint.setColor(Color.parseColor("#22C55E"));
        else if (last < 60) linePaint.setColor(Color.parseColor("#F97316"));
        else                linePaint.setColor(Color.parseColor("#EF4444"));
        canvas.drawPath(path, linePaint);
    }

    private void drawDots(Canvas canvas, float cw, float ch) {
        int n = scores.size();
        // Only draw dot on the last point
        float x = PADDING_LEFT + cw;
        float y = PADDING_TOP + ch * (1f - scores.get(n - 1) / 100f);
        float last = scores.get(n - 1);
        int dotColor;
        if (last < 30)      dotColor = Color.parseColor("#22C55E");
        else if (last < 60) dotColor = Color.parseColor("#F97316");
        else                dotColor = Color.parseColor("#EF4444");
        dotPaint.setColor(dotColor);
        canvas.drawCircle(x, y, 8f, dotPaint);
        dotPaint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, 4f, dotPaint);
    }
}