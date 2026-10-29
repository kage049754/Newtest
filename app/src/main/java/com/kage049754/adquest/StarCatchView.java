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
        canvas.drawColor(0xFF0C1830);
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
            canvas.drawText("SURVIVE THE FALL", w / 2f, h / 2f - 12 * density, paint);
            paint.setTextSize(13 * density);
            paint.setColor(0xFF8DAAC8);
            canvas.drawText("Drag to dodge • Collect cyan energy", w / 2f, h / 2f + 18 * density, paint);
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
                paint.setColor(0xFF55E8FF);
                canvas.drawCircle(object.x, object.y, object.radius * 1.55f, paint);
                paint.setColor(0xFFE8FCFF);
                canvas.drawCircle(object.x, object.y, object.radius * 0.65f, paint);
            } else {
                paint.setColor(0xFFFF4F6D);
                canvas.drawCircle(object.x, object.y, object.radius, paint);
                paint.setColor(0xFF7D1838);
                canvas.drawCircle(object.x, object.y, object.radius * 0.48f, paint);
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
        paint.setColor(invulnerable && (now / 100) % 2 == 0 ? 0xFFB9FFFC : 0xFF4DDAFF);
        canvas.drawCircle(playerX, playerY, playerRadius + 4 * density, paint);
        paint.setColor(0xFF092A4A);
        canvas.drawCircle(playerX, playerY, playerRadius * 0.58f, paint);

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
