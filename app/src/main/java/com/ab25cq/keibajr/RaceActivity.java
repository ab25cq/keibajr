package com.ab25cq.keibajr;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

public class RaceActivity extends Activity {
    private static final int RACE_MS = 14000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String summary;
    private boolean resultStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String raceName = getIntent().getStringExtra("raceName");
        summary = getIntent().getStringExtra("summary");
        String[] names = getIntent().getStringArrayExtra("runnerNames");
        int[] ranks = getIntent().getIntArrayExtra("runnerRanks");
        float[] paces = getIntent().getFloatArrayExtra("runnerPaces");
        boolean[] players = getIntent().getBooleanArrayExtra("runnerPlayers");

        setTitle(raceName == null ? Texts.t("レース", "Race") : raceName);
        setContentView(new RaceView(this, raceName, names, ranks, paces, players));
        handler.postDelayed(this::showResult, RACE_MS + 600);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void showResult() {
        if (resultStarted) {
            return;
        }
        resultStarted = true;
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("summary", summary == null ? Texts.t("結果を取得できませんでした。", "Could not load the result.") : summary);
        startActivity(intent);
        finish();
    }

    private class RaceView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final long startAt = System.currentTimeMillis();
        private final String raceName;
        private final String[] names;
        private final int[] ranks;
        private final float[] paces;
        private final boolean[] players;

        RaceView(Context context, String raceName, String[] names, int[] ranks, float[] paces, boolean[] players) {
            super(context);
            this.raceName = raceName == null ? "Race Watch" : raceName;
            this.names = names == null ? new String[0] : names;
            this.ranks = ranks == null ? new int[0] : ranks;
            this.paces = paces == null ? new float[0] : paces;
            this.players = players == null ? new boolean[0] : players;
            setBackgroundColor(Color.rgb(60, 108, 71));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            drawCourse(canvas, width, height);
            float elapsed = Math.min(1f, (System.currentTimeMillis() - startAt) / (float) RACE_MS);
            drawRunners(canvas, width, height, elapsed);
            if (elapsed < 1f) {
                invalidate();
            }
        }

        private void drawCourse(Canvas canvas, int width, int height) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(60, 108, 71));
            canvas.drawRect(0, 0, width, height, paint);
            paint.setColor(Color.rgb(206, 176, 107));
            RectF track = new RectF(dp(18), dp(58), width - dp(18), height - dp(30));
            canvas.drawRoundRect(track, dp(28), dp(28), paint);
            paint.setColor(Color.rgb(236, 217, 166));
            canvas.drawRoundRect(new RectF(track.left + dp(10), track.top + dp(10), track.right - dp(10), track.bottom - dp(10)),
                    dp(22), dp(22), paint);
            paint.setColor(Color.rgb(255, 252, 244));
            canvas.drawRect(width - dp(42), track.top - dp(10), width - dp(36), track.bottom + dp(8), paint);
            paint.setTextSize(dp(16));
            paint.setColor(Color.WHITE);
            canvas.drawText(raceName, dp(18), dp(35), paint);
        }

        private void drawRunners(Canvas canvas, int width, int height, float elapsed) {
            int count = Math.max(1, names.length);
            float laneTop = dp(88);
            float laneHeight = (height - dp(150)) / count;
            for (int i = 0; i < names.length; i++) {
                float progress = progressFor(i, elapsed);
                float x = dp(34) + progress * (width - dp(86));
                float y = laneTop + laneHeight * i + laneHeight * 0.5f;
                boolean player = i < players.length && players[i];
                int rank = i < ranks.length ? ranks[i] : i + 1;

                paint.setColor(i % 2 == 0 ? Color.rgb(190, 159, 95) : Color.rgb(181, 146, 82));
                canvas.drawRect(dp(28), y + dp(11), width - dp(46), y + dp(13), paint);
                paint.setColor(player ? Color.rgb(201, 47, 54) : Color.rgb(35, 45, 40));
                canvas.drawOval(new RectF(x - dp(12), y - dp(9), x + dp(12), y + dp(9)), paint);
                paint.setColor(player ? Color.rgb(255, 232, 87) : Color.rgb(236, 236, 226));
                canvas.drawCircle(x + dp(7), y - dp(11), dp(5), paint);
                paint.setColor(Color.rgb(33, 45, 38));
                paint.setTextSize(dp(10));
                canvas.drawText(String.valueOf(rank), x - dp(3), y + dp(4), paint);
                if (player) {
                    paint.setTextSize(dp(12));
                    canvas.drawText(names[i], dp(30), y - dp(12), paint);
                }
            }
        }

        private float progressFor(int index, float elapsed) {
            float pace = index < paces.length ? paces[index] : 0.85f;
            float progress = elapsed * pace;
            if (elapsed >= 1f) {
                int rank = index < ranks.length ? ranks[index] : index + 1;
                progress = 0.88f + (8 - rank) * 0.014f;
            }
            return Math.max(0.02f, Math.min(0.98f, progress));
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
