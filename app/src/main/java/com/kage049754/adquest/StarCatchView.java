package com.kage049754.adquest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class StarCatchView extends View {
    public interface RoundListener { void onFinished(int score); }
    private static class Target {
        float x, y, radius, speed;
        boolean bomb;
        Target(float x, float y, float radius, float speed, boolean bomb) {
            this.x=x; this.y=y; this.radius=radius; this.speed=speed; this.bomb=bomb;
        }
    }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<Target> targets = new ArrayList<>();
    private final Random random = new Random();
    private final RoundListener listener;
    private long startTime, lastSpawn;
    private int score, lastScore, combo;
    private boolean running = false, paused = false;
    private float density;

    public StarCatchView(Context context, RoundListener listener) {
        super(context);
        this.listener = listener;
        density = getResources().getDisplayMetrics().density;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void startRound() {
        targets.clear(); score = 0; combo = 0;
        startTime = System.currentTimeMillis(); lastSpawn = 0;
        running = true; paused = false; invalidate();
    }
    public int getLastScore() { return lastScore; }
    public void pauseGame() { paused = true; }
    public void resumeGame() { paused = false; postInvalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(0xFF202F4A);
        float w=getWidth(), h=getHeight();
        paint.setColor(0xFF293D5B);
        for (int i=0;i<12;i++) {
            float x=(i*83f*density)%Math.max(1,w);
            canvas.drawCircle(x, (i*127f*density)%Math.max(1,h), 2*density, paint);
        }
        if (!running) {
            paint.setColor(0xFFFFFFFF); paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(18*density);
            canvas.drawText("TAP START TO PLAY", w/2, h/2, paint);
            paint.setTextSize(13*density); canvas.drawText("Gold stars = points   •   Red bombs = lose points", w/2, h/2+28*density, paint);
            return;
        }
        if (paused) { postInvalidateDelayed(100); return; }
        long elapsed=System.currentTimeMillis()-startTime;
        long remaining=Math.max(0,30000-elapsed);
        if (System.currentTimeMillis()-lastSpawn>520) {
            float radius=(13+random.nextInt(8))*density;
            boolean bomb=random.nextFloat()<0.22f;
            targets.add(new Target(radius+random.nextFloat()*Math.max(1,w-2*radius), -radius, radius, (2.2f+random.nextFloat()*2.2f)*density, bomb));
            lastSpawn=System.currentTimeMillis();
        }
        Iterator<Target> it=targets.iterator();
        while(it.hasNext()) {
            Target t=it.next(); t.y+=t.speed;
            paint.setColor(t.bomb?0xFFFF667A:0xFFFFD166);
            canvas.drawCircle(t.x,t.y,t.radius,paint);
            paint.setColor(t.bomb?0xFF7D1E36:0xFF9C6410);
            paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(t.radius*1.1f);
            canvas.drawText(t.bomb?"!":"★",t.x,t.y+t.radius*0.36f,paint);
            if(t.y>h+t.radius) it.remove();
        }
        paint.setColor(0xFFFFFFFF); paint.setTextAlign(Paint.Align.LEFT); paint.setTextSize(16*density);
        canvas.drawText("Score: "+score, 12*density, 26*density, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Time: "+(remaining/1000+ (remaining%1000>0?1:0)), w-12*density, 26*density, paint);
        if (remaining<=0) {
            running=false; lastScore=score; targets.clear(); listener.onFinished(score); return;
        }
        postInvalidateDelayed(16);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()!=MotionEvent.ACTION_DOWN || !running || paused) return true;
        float x=event.getX(), y=event.getY();
        for(int i=targets.size()-1;i>=0;i--) {
            Target t=targets.get(i);
            float dx=x-t.x, dy=y-t.y;
            if(dx*dx+dy*dy<=t.radius*t.radius*1.8f) {
                if(t.bomb) { score=Math.max(0,score-3); combo=0; }
                else { score+=1+Math.min(4,combo/4); combo++; }
                targets.remove(i); invalidate(); return true;
            }
        }
        return true;
    }
}
