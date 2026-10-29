package com.kage049754.adquest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class StarCatchView extends View {
    public interface RoundListener { void onFinished(int score, int survivedSeconds, boolean hitHazard); }

    private static class FallingObject {
        float x, y, radius, speed;
        boolean orb;
        FallingObject(float x, float y, float radius, float speed, boolean orb) {
            this.x = x; this.y = y; this.radius = radius; this.speed = speed; this.orb = orb;
        }
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<FallingObject> objects = new ArrayList<>();
    private final Random random = new Random();
    private final RoundListener listener;
    private final Soundscape soundscape;
    private int displayedStage = 1;
    private long stageBannerUntil;
    private final float density;
    private long startTime, lastSpawn, lastFrame, pauseStartedAt, pausedTotal;
    private int score, lastScore, coinBoost;
    private boolean running, paused, shieldOwned, shieldAvailable, hitHazard;
    private float playerX, playerY, playerRadius, elapsedSeconds;
    private boolean invulnerable;
    private long invulnerableUntil;

    public StarCatchView(Context context, RoundListener listener, Soundscape soundscape) {
        super(context);
        this.listener = listener;
        this.soundscape = soundscape;
        density = getResources().getDisplayMetrics().density;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setRunUpgrades(boolean shield, int boost) {
        shieldOwned = shield;
        coinBoost = Math.max(0, Math.min(3, boost));
    }

    public void startRound() {
        objects.clear();
        score = 0;
        elapsedSeconds = 0;
        displayedStage = 1;
        stageBannerUntil = 0;
        hitHazard = false;
        shieldAvailable = shieldOwned;
        invulnerable = false;
        playerRadius = 15 * density;
        playerX = Math.max(playerRadius, getWidth() / 2f);
        playerY = Math.max(playerRadius * 2, getHeight() - 58 * density);
        startTime = System.currentTimeMillis();
        lastFrame = startTime;
        pauseStartedAt = 0;
        pausedTotal = 0;
        lastSpawn = startTime;
        running = true;
        paused = false;
        invalidate();
    }

    public int getLastScore() { return lastScore; }
    public void pauseGame() { if (running && !paused) { paused = true; pauseStartedAt = System.currentTimeMillis(); } }
    public void resumeGame() {
        if (running && paused) {
            long now = System.currentTimeMillis();
            long pauseDuration = Math.max(0, now - pauseStartedAt);
            pausedTotal += pauseDuration;
            startTime += pauseDuration;
            lastSpawn += pauseDuration;
            paused = false;
            lastFrame = now;
            postInvalidate();
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        paint.setShader(new LinearGradient(0, 0, w, h, new int[]{0xFF071322, 0xFF102B4A, 0xFF08152A}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1 * density);
        paint.setColor(0x222CE5FF);
        for (float gx = 0; gx < w; gx += 34 * density) canvas.drawLine(gx, 0, gx, h, paint);
        for (float gy = 0; gy < h; gy += 34 * density) canvas.drawLine(0, gy, w, gy, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2 * density);
        paint.setColor(0x6639DDF7);
        canvas.drawRoundRect(3 * density, 3 * density, w - 3 * density, h - 3 * density, 18 * density, 18 * density, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 30; i++) {
            paint.setColor(i % 4 == 0 ? 0xFF31516E : 0xFF1D3554);
            float x = (i * 97f * density) % Math.max(1, w);
            float y = (i * 139f * density) % Math.max(1, h);
            canvas.drawCircle(x, y, (i % 3 == 0 ? 2.0f : 1.1f) * density, paint);
        }

        if (!running) {
            paint.setColor(0xFFEAF7FF);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(20 * density);
            canvas.drawText("LAST 1%", w / 2f, h / 2f - 12 * density, paint);
            paint.setTextSize(13 * density);
            paint.setColor(0xFF8DAAC8);
            canvas.drawText("ENDLESS ARENA  •  DODGE  •  COLLECT", w / 2f, h / 2f + 18 * density, paint);
            return;
        }
        if (paused) return;

        long now = System.currentTimeMillis();
        float delta = Math.min(0.04f, Math.max(0f, (now - lastFrame) / 1000f));
        lastFrame = now;
        elapsedSeconds = (now - startTime) / 1000f;
        int stage = 1 + (int)(elapsedSeconds / 60f);
        if (stage > displayedStage) {
            displayedStage = stage;
            stageBannerUntil = now + 2200;
            if (soundscape != null) soundscape.stage();
        }
        float difficulty = Math.min(4.2f, 1f + elapsedSeconds / 38f);
        long spawnInterval = Math.max(235, 650 - (long)(elapsedSeconds * 2.1f));
        if (now - lastSpawn >= spawnInterval) {
            boolean orb = random.nextFloat() < Math.max(0.28f, 0.43f - elapsedSeconds / 180f);
            float radius = (orb ? 10f : 13f + random.nextInt(6)) * density;
            float minX = radius + 3 * density;
            float maxX = Math.max(minX, w - radius - 3 * density);
            float x = minX + random.nextFloat() * (maxX - minX);
            float speed = (orb ? 155f : 185f + random.nextFloat() * 85f) * density * difficulty;
            objects.add(new FallingObject(x, -radius, radius, speed, orb));
            lastSpawn = now;
        }

        Iterator<FallingObject> iterator = objects.iterator();
        while (iterator.hasNext()) {
            FallingObject object = iterator.next();
            object.y += object.speed * delta;
            if (object.orb) {
                paint.setColor(0x3355E8FF);
                canvas.drawCircle(object.x, object.y, object.radius * 2.0f, paint);
                paint.setColor(0xFF20C9F4);
                canvas.drawCircle(object.x, object.y, object.radius, paint);
                paint.setColor(0xFFBDFBFF);
                canvas.drawCircle(object.x, object.y, object.radius * 0.45f, paint);
                paint.setColor(0xFFFFFFFF);
                canvas.drawCircle(object.x - object.radius * 0.22f, object.y - object.radius * 0.24f, object.radius * 0.16f, paint);
            } else {
                paint.setColor(0x55FF345B);
                canvas.drawCircle(object.x, object.y, object.radius * 1.55f, paint);
                Path shard = new Path();
                shard.moveTo(object.x, object.y - object.radius * 1.2f);
                shard.lineTo(object.x + object.radius * 0.88f, object.y - object.radius * 0.25f);
                shard.lineTo(object.x + object.radius * 0.65f, object.y + object.radius * 0.82f);
                shard.lineTo(object.x - object.radius * 0.55f, object.y + object.radius);
                shard.lineTo(object.x - object.radius, object.y - object.radius * 0.15f);
                shard.close();
                paint.setColor(0xFFFF4268);
                canvas.drawPath(shard, paint);
                paint.setColor(0xFFFFB2C2);
                canvas.drawCircle(object.x - object.radius * 0.2f, object.y - object.radius * 0.15f, object.radius * 0.18f, paint);
            }

            float dx = playerX - object.x;
            float dy = playerY - object.y;
            float collisionDistance = playerRadius + object.radius * 0.78f;
            if (dx * dx + dy * dy < collisionDistance * collisionDistance) {
                if (object.orb) {
                    score += 10 * (1 + coinBoost);
                    if (soundscape != null) soundscape.collect();
                    iterator.remove();
                    continue;
                } else if (shieldAvailable) {
                    shieldAvailable = false;
                    if (soundscape != null) soundscape.shield();
                    invulnerable = true;
                    invulnerableUntil = now + 1300;
                    iterator.remove();
                    continue;
                } else if (invulnerable) {
                    iterator.remove();
                    continue;
                } else {
                    hitHazard = true;
                    if (soundscape != null) soundscape.hit();
                    finishRun();
                    return;
                }
            }
            if (object.y > h + object.radius) iterator.remove();
        }

        if (now >= invulnerableUntil) invulnerable = false;
        paint.setColor(0x334DDAFF);
        canvas.drawCircle(playerX, playerY, playerRadius * 1.85f, paint);
        paint.setColor(invulnerable && (now / 100) % 2 == 0 ? 0xFFFFFFFF : 0xFF43E7FF);
        canvas.drawCircle(playerX, playerY, playerRadius + 4 * density, paint);
        paint.setColor(0xFF102E50);
        canvas.drawCircle(playerX, playerY, playerRadius * 0.72f, paint);
        paint.setColor(0xFF9CFAFF);
        canvas.drawCircle(playerX, playerY, playerRadius * 0.28f, paint);
        paint.setColor(0xFFEAF7FF);
        canvas.drawCircle(playerX - playerRadius * 0.2f, playerY - playerRadius * 0.28f, playerRadius * 0.12f, paint);

        paint.setColor(0xFFEAF7FF);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(15 * density);
        canvas.drawText("ENERGY " + score, 12 * density, 25 * density, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        int totalSeconds = (int) elapsedSeconds;
        String timeText = String.format(java.util.Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60);
        canvas.drawText("STAGE " + displayedStage + "  •  " + timeText, w - 12 * density, 25 * density, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(11 * density);
        paint.setColor(shieldAvailable ? 0xFFB9FFFC : 0xFF6C819A);
        canvas.drawText(shieldAvailable ? "SHIELD READY" : "DODGE!", w / 2f, h - 15 * density, paint);

        if (now < stageBannerUntil) {
            paint.setColor(0xCC102A48);
            canvas.drawRoundRect(w * 0.20f, h * 0.34f, w * 0.80f, h * 0.44f, 16 * density, 16 * density, paint);
            paint.setColor(0xFF70F2FF);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(23 * density);
            canvas.drawText("STAGE " + displayedStage, w / 2f, h * 0.405f, paint);
        }
        postInvalidateDelayed(16);
    }

    private void finishRun() {
        running = false;
        lastScore = score;
        objects.clear();
        listener.onFinished(score, (int) elapsedSeconds, hitHazard);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!running || paused) return true;
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            playerX = Math.max(playerRadius, Math.min(getWidth() - playerRadius, event.getX()));
            playerY = Math.max(playerRadius * 2, Math.min(getHeight() - playerRadius * 2, event.getY()));
            invalidate();
            return true;
        }
        return true;
    }
}
