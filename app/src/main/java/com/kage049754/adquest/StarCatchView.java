package com.kage049754.adquest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class StarCatchView extends View {
    public interface RoundListener { void onFinished(int score); }
    private static class Hazard {
        float x, y, radius, speed;
        boolean energy;
        Hazard(float x, float y, float radius, float speed, boolean energy) {
            this.x = x; this.y = y; this.radius = radius; this.speed = speed; this.energy = energy;
        }
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<Hazard> hazards = new ArrayList<>();
    private final Random random = new Random();
    private final RoundListener listener;
    private long startTime, lastSpawn;
    private int score, lastScore;
    private boolean running = false, paused = false, ended = false;
    private float density, playerX, playerY, playerRadius;

    public StarCatchView(Context context, RoundListener listener) {
        super(context);
        this.listener = listener;
        density = getResources().getDisplayMetrics().density;
        playerRadius = 17 * density;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void startRound() {
        hazards.clear();
        score = 0;
        ended = false;
        startTime = System.currentTimeMillis();
        lastSpawn = startTime;
        playerX = getWidth() / 2f;
        playerY = Math.max(playerRadius * 2, getHeight() - 52 * density);
        running = true;
        paused = false;
        invalidate();
    }

    public int getLastScore() { return lastScore; }
    public void pauseGame() { if (running) paused = true; }
    public void resumeGame() { paused = false; postInvalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(0xFF101B31);
        float w = getWidth(), h = getHeight();
        paint.setColor(0xFF1A2945);
        for (int i = 0; i < 22; i++) {
            float x = (i * 83f * density) % Math.max(1, w);
            float y = (i * 127f * density) % Math.max(1, h);
            canvas.drawCircle(x, y, (i % 3 + 1) * density, paint);
        }

        if (!running) {
            paint.setColor(0xFF5CE8FF);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(30 * density);
            canvas.drawText("LAST 1%", w / 2, h / 2 - 20 * density, paint);
            paint.setColor(0xFFFFFFFF);
            paint.setTextSize(14 * density);
            canvas.drawText("Drag to dodge • collect cyan energy", w / 2, h / 2 + 14 * density, paint);
            canvas.drawText("One collision ends the run", w / 2, h / 2 + 38 * density, paint);
            return;
        }
        if (paused) {
            paint.setColor(0xFFFFFFFF);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(18 * density);
            canvas.drawText("PAUSED", w / 2, h / 2, paint);
            postInvalidateDelayed(100);
            return;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - startTime;
        long remaining = Math.max(0, 45000 - elapsed);
        long spawnInterval = Math.max(220, 650 - elapsed / 90);
        if (now - lastSpawn >= spawnInterval) {
            float radius = (11 + random.nextInt(9)) * density;
            boolean energy = random.nextFloat() < 0.34f;
            float x = radius + random.nextFloat() * Math.max(1, w - 2 * radius);
            float speed = (2.4f + random.nextFloat() * 2.1f + elapsed / 14000f) * density;
            hazards.add(new Hazard(x, -radius, radius, speed, energy));
            lastSpawn = now;
        }

        Iterator<Hazard> it = hazards.iterator();
        while (it.hasNext()) {
            Hazard item = it.next();
            item.y += item.speed;
            paint.setColor(item.energy ? 0xFF5CE8FF : 0xFFFF5268);
            canvas.drawCircle(item.x, item.y, item.radius, paint);
            paint.setColor(item.energy ? 0xFF0A5266 : 0xFF781E35);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(item.radius * 1.2f);
            canvas.drawText(item.energy ? "+" : "×", item.x, item.y + item.radius * 0.38f, paint);

            float dx = playerX - item.x, dy = playerY - item.y;
            float collision = playerRadius + item.radius * 0.72f;
            if (dx * dx + dy * dy <= collision * collision) {
                if (item.energy) {
                    score += 10;
                    it.remove();
                } else {
                    finishRun();
                    return;
                }
            } else if (item.y > h + item.radius) {
                if (!item.energy) score += 1;
                it.remove();
            }
        }

        paint.setColor(0xFF5CE8FF);
        canvas.drawCircle(playerX, playerY, playerRadius, paint);
        paint.setColor(0xFF153A55);
        canvas.drawCircle(playerX, playerY, playerRadius * 0.52f, paint);
        paint.setColor(0xFFFFFFFF);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(16 * density);
        canvas.drawText("Score " + score, 12 * density, 26 * density, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Time " + (remaining / 1000 + (remaining % 1000 > 0 ? 1 : 0)), w - 12 * density, 26 * density, paint);
        if (remaining <= 0) {
            finishRun();
            return;
        }
        postInvalidateDelayed(16);
    }

    private void finishRun() {
        if (ended) return;
        ended = true;
        running = false;
        lastScore = score;
        hazards.clear();
        listener.onFinished(score);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!running || paused) return true;
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            playerX = Math.max(playerRadius, Math.min(getWidth() - playerRadius, event.getX()));
            playerY = Math.max(playerRadius * 2, Math.min(getHeight() - playerRadius, event.getY()));
            invalidate();
            return true;
        }
        return true;
    }
}
