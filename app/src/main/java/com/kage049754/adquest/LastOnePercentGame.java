package com.kage049754.adquest;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import java.util.Iterator;
import java.util.Random;

/** LibGDX-powered endless survival game. No downloaded assets or network required. */
public class LastOnePercentGame extends Game {
    @Override public void create() { setScreen(new ArenaScreen(this)); }

    private static final class Falling {
        float x, y, speed, size, rotation;
        boolean orb;
        Falling(float x, float y, float speed, float size, boolean orb) {
            this.x=x; this.y=y; this.speed=speed; this.size=size; this.orb=orb;
            this.rotation=MathUtils.random(0,360);
        }
    }
    private static final class Spark {
        float x,y,vx,vy,life,max;
        Spark(float x,float y) { this.x=x; this.y=y; vx=MathUtils.random(-150,150); vy=MathUtils.random(-150,150); life=max=MathUtils.random(.25f,.7f); }
    }

    private static final class ArenaScreen extends InputAdapter implements com.badlogic.gdx.Screen {
        final LastOnePercentGame game;
        final OrthographicCamera camera = new OrthographicCamera();
        final ShapeRenderer shapes = new ShapeRenderer();
        final SpriteBatch batch = new SpriteBatch();
        final BitmapFont font = new BitmapFont();
        final Preferences prefs = Gdx.app.getPreferences("last1percent-libgdx");
        final Random random = new Random();
        final Array<Falling> objects = new Array<>();
        final Array<Spark> sparks = new Array<>();
        final Vector3 touch = new Vector3();
        final Color cyan = new Color(.18f,.94f,1f,1f);
        final Color violet = new Color(.48f,.31f,1f,1f);
        float w=480,h=854,px=240,py=330,elapsed,spawnTimer,flash,shake,scoreFloat;
        int score,coins,best,stage=1,shield=1,boost;
        boolean running=false,over=false,paused=false,dragging=false,audio=true;
        String screen="home";
        ArenaScreen(LastOnePercentGame g) { game=g; coins=prefs.getInteger("coins",0); best=prefs.getInteger("best",0); boost=prefs.getInteger("boost",0); }
        @Override public void show() { Gdx.input.setInputProcessor(this); Gdx.input.setCatchKey(com.badlogic.gdx.Input.Keys.BACK, true); }
        @Override public void render(float delta) {
            float dt=Math.min(delta,.033f);
            if (running && !paused) update(dt);
            ScreenUtils.clear(.018f,.025f,.075f,1);
            camera.setToOrtho(false,w,h); camera.update();
            shapes.setProjectionMatrix(camera.combined); batch.setProjectionMatrix(camera.combined);
            drawBackground();
            if (screen.equals("home")) drawHome();
            else if (screen.equals("shop")) drawShop();
            else if (screen.equals("game")) drawGame();
            else if (screen.equals("over")) drawOver();
            if (paused) drawPause();
        }
        void update(float dt) {
            elapsed+=dt; stage=1+(int)(elapsed/45f);
            spawnTimer-=dt;
            float difficulty=Math.min(2.8f,1f+elapsed/95f);
            if(spawnTimer<=0) {
                boolean orb=random.nextFloat()<.43f;
                objects.add(new Falling(MathUtils.random(32,w-32),h+30,MathUtils.random(175,265)*difficulty,orb?12:MathUtils.random(15,24),orb));
                spawnTimer=Math.max(.19f,.68f-elapsed/250f);
            }
            for(Iterator<Falling> it=objects.iterator();it.hasNext();) {
                Falling o=it.next(); o.y-=o.speed*dt; o.rotation+=dt*100;
                float dx=o.x-px,dy=o.y-py;
                if(dx*dx+dy*dy < (o.size+17)*(o.size+17)) {
                    if(o.orb) { score+=10+boost*5; burst(o.x,o.y,8); it.remove(); }
                    else if(shield>0) { shield--; burst(px,py,22); flash=.22f; it.remove(); }
                    else { endRun(); return; }
                } else if(o.y < -40) it.remove();
            }
            for(Iterator<Spark> it=sparks.iterator();it.hasNext();) { Spark s=it.next(); s.x+=s.vx*dt; s.y+=s.vy*dt; s.life-=dt; if(s.life<=0)it.remove(); }
            flash=Math.max(0,flash-dt); shake=Math.max(0,shake-dt);
        }
        void endRun() { running=false; screen="over"; over=true; coins+=Math.max(1,score/30+(int)(elapsed/20)); best=Math.max(best,score); prefs.putInteger("coins",coins); prefs.putInteger("best",best); prefs.flush(); burst(px,py,30); }
        void burst(float x,float y,int n) { for(int i=0;i<n;i++) sparks.add(new Spark(x,y)); }
        void drawBackground() {
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(.018f,.025f,.075f,1); shapes.rect(0,0,w,h);
            for(int i=0;i<9;i++) { float yy=(i*113f+(elapsed*8)%113); shapes.setColor(.08f,.15f,.3f,.12f); shapes.rect(0,yy,w,1); }
            for(int i=0;i<7;i++) { float xx=i*80; shapes.setColor(.08f,.15f,.3f,.12f); shapes.rect(xx,0,1,h); }
            shapes.setColor(.08f,.3f,.52f,.18f); shapes.rect(12,12,w-24,h-24);
            shapes.end();
            batch.begin(); font.getData().setScale(1); font.setColor(.17f,.55f,.76f,.24f);
            for(int i=0;i<12;i++) font.draw(batch,"+",24+(i*79)%430,40+(i*137)%780);
            batch.end();
            if(flash>0) { Gdx.gl.glEnable(GL20.GL_BLEND); shapes.begin(ShapeRenderer.ShapeType.Filled); shapes.setColor(1,.1f,.25f,flash*1.4f); shapes.rect(0,0,w,h); shapes.end(); Gdx.gl.glDisable(GL20.GL_BLEND); }
        }
        void drawGame() {
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            for(Falling o:objects) {
                if(o.orb) {
                    shapes.setColor(.1f,.8f,1,.12f); shapes.circle(o.x,o.y,o.size*2.3f);
                    shapes.setColor(.2f,.95f,1,1); shapes.circle(o.x,o.y,o.size);
                    shapes.setColor(1,1,1,.9f); shapes.circle(o.x-o.size*.25f,o.y+o.size*.3f,o.size*.25f);
                } else {
                    shapes.setColor(1,.12f,.29f,.15f); shapes.circle(o.x,o.y,o.size*1.8f);
                    shapes.setColor(1,.16f,.28f,1);
                    shapes.triangle(o.x,o.y+o.size*1.5f,o.x-o.size,o.y-o.size,o.x+o.size,o.y-o.size);
                    shapes.setColor(1,.72f,.8f,1); shapes.circle(o.x,o.y,o.size*.22f);
                }
            }
            for(Spark s:sparks) { shapes.setColor(.2f,.9f,1,Math.max(0,s.life/s.max)); shapes.circle(s.x,s.y,2.5f); }
            shapes.setColor(.15f,.8f,1,.13f); shapes.circle(px,py,37);
            shapes.setColor(.15f,.9f,1,.25f); shapes.circle(px,py,28);
            shapes.setColor(.2f,.96f,1,1); shapes.circle(px,py,19);
            shapes.setColor(.8f,1,1,1); shapes.circle(px-5,py+6,5);
            shapes.end();
            drawHud();
        }
        void drawHud() {
            batch.begin(); font.getData().setScale(1.5f); font.setColor(Color.WHITE);
            font.draw(batch,"SCORE",24,h-36); font.getData().setScale(2.2f); font.setColor(cyan); font.draw(batch,String.valueOf(score),24,h-66);
            font.getData().setScale(1); font.setColor(.62f,.77f,.9f,1); font.draw(batch,"STAGE "+stage,24,h-91);
            font.setColor(Color.WHITE); font.draw(batch,"BEST "+best,w-120,h-36);
            font.setColor(cyan); font.draw(batch,"◈ "+coins,w-120,h-62);
            font.setColor(shield>0?cyan:Color.GRAY); font.draw(batch,shield>0?"SHIELD READY":"NO SHIELD",24,38);
            font.setColor(.55f,.68f,.83f,1); font.draw(batch,"DRAG TO MOVE  •  SURVIVE",w/2-91,20);
            font.getData().setScale(1); batch.end();
            button(424,h-56,32,32,"Ⅱ",cyan);
        }
        void drawHome() {
            panel(28,150,w-56,460);
            batch.begin();
            font.getData().setScale(1.15f); font.setColor(cyan); font.draw(batch,"NEON SURVIVAL // PROTOCOL 01",70,574);
            font.getData().setScale(4.2f); font.setColor(Color.WHITE); font.draw(batch,"LAST",91,510);
            font.setColor(cyan); font.draw(batch,"1%",210,455);
            font.getData().setScale(1.1f); font.setColor(.65f,.8f,.95f,1); font.draw(batch,"ONE CORE. ENDLESS THREATS.",82,408);
            font.setColor(.48f,.6f,.78f,1); font.draw(batch,"COLLECT ENERGY  /  DODGE SHARDS",65,384);
            font.getData().setScale(1); font.setColor(Color.WHITE); font.draw(batch,"BEST SCORE  "+best,164,320);
            font.setColor(cyan); font.draw(batch,"◈  "+coins+" CREDITS",177,294);
            font.getData().setScale(1); font.setColor(.4f,.52f,.68f,1); font.draw(batch,"FREE • OFFLINE • ENDLESS",150,175);
            batch.end();
            button(65,225,w-130,62,"START RUN",cyan);
            button(65,88,w-130,52,"UPGRADE CORE  •  SHOP",violet);
        }
        void drawShop() {
            panel(28,130,w-56,550);
            batch.begin(); font.getData().setScale(2.4f); font.setColor(Color.WHITE); font.draw(batch,"CORE LAB",68,625);
            font.getData().setScale(1); font.setColor(cyan); font.draw(batch,"AVAILABLE CREDITS: "+coins,70,590);
            font.setColor(Color.WHITE); font.draw(batch,"01  SHIELD MODULE",70,520); font.setColor(.55f,.7f,.85f,1); font.draw(batch,"Blocks one collision each run",70,493);
            font.setColor(cyan); font.draw(batch,shield>0?"EQUIPPED THIS RUN":"COST 50 CREDITS",70,465);
            font.setColor(Color.WHITE); font.draw(batch,"02  ENERGY AMPLIFIER  LV."+boost,70,390); font.setColor(.55f,.7f,.85f,1); font.draw(batch,"More points from every energy orb",70,363);
            font.setColor(cyan); font.draw(batch,"COST "+(100+boost*100)+" CREDITS",70,335);
            font.setColor(.5f,.65f,.8f,1); font.draw(batch,"UPGRADES SAVE ON THIS DEVICE",70,185);
            batch.end();
            button(65,415,w-130,42,shield>0?"SHIELD EQUIPPED":"BUY SHIELD  •  50",cyan);
            button(65,255,w-130,48,"UPGRADE AMPLIFIER",violet);
            button(65,145,w-130,48,"BACK TO BASE",new Color(.25f,.42f,.62f,1));
        }
        void drawOver() {
            panel(32,185,w-64,460);
            batch.begin(); font.getData().setScale(1.2f); font.setColor(1,.25f,.38f,1); font.draw(batch,"SIGNAL LOST",132,585);
            font.getData().setScale(3.4f); font.setColor(Color.WHITE); font.draw(batch,String.valueOf(score),185,510);
            font.getData().setScale(1); font.setColor(.55f,.72f,.86f,1); font.draw(batch,"FINAL SCORE",185,480);
            font.setColor(cyan); font.draw(batch,"SURVIVED  "+(int)elapsed+" SECONDS",122,425);
            font.setColor(Color.WHITE); font.draw(batch,"STAGE REACHED  "+stage,145,390);
            font.setColor(cyan); font.draw(batch,"+"+Math.max(1,score/30+(int)(elapsed/20))+" CREDITS EARNED",112,350);
            font.getData().setScale(1); font.setColor(.45f,.57f,.72f,1); font.draw(batch,"EVERY RUN MAKES THE CORE STRONGER",75,230);
            batch.end();
            button(65,265,w-130,58,"TRY AGAIN",cyan);
            button(65,110,w-130,52,"RETURN TO BASE",violet);
        }
        void drawPause() {
            shapes.begin(ShapeRenderer.ShapeType.Filled); shapes.setColor(0,0,.04f,.82f); shapes.rect(0,0,w,h); shapes.end();
            panel(42,285,w-84,250); batch.begin(); font.getData().setScale(2.3f); font.setColor(Color.WHITE); font.draw(batch,"PAUSED",160,480); font.getData().setScale(1); font.setColor(.6f,.75f,.9f,1); font.draw(batch,"TAKE A BREATH. THE CORE WAITS.",95,445); batch.end();
            button(70,365,w-140,54,"RESUME",cyan); button(70,300,w-140,48,"END RUN",violet);
        }
        void panel(float x,float y,float ww,float hh) {
            shapes.begin(ShapeRenderer.ShapeType.Filled); shapes.setColor(.035f,.065f,.14f,.96f); shapes.rect(x,y,ww,hh); shapes.end();
            shapes.begin(ShapeRenderer.ShapeType.Line); shapes.setColor(.12f,.62f,.88f,.85f); shapes.rect(x,y,ww,hh); shapes.end();
            shapes.begin(ShapeRenderer.ShapeType.Filled); shapes.setColor(cyan); shapes.rect(x+2,y+hh-3,ww-4,2); shapes.end();
        }
        void button(float x,float y,float ww,float hh,String label,Color c) {
            shapes.begin(ShapeRenderer.ShapeType.Filled); shapes.setColor(c.r,c.g,c.b,.14f); shapes.rect(x,y,ww,hh); shapes.end();
            shapes.begin(ShapeRenderer.ShapeType.Line); shapes.setColor(c); shapes.rect(x,y,ww,hh); shapes.end();
            batch.begin(); font.getData().setScale(1.15f); font.setColor(Color.WHITE); font.draw(batch,label,x+ww/2-font.getRegion().getRegionWidth()*.0f-label.length()*6.2f,y+hh/2+5); font.getData().setScale(1); batch.end();
        }
        boolean hit(float x,float y,float bx,float by,float bw,float bh) { return x>=bx&&x<=bx+bw&&y>=by&&y<=by+bh; }
        @Override public boolean touchDown(int sx,int sy,int pointer,int button) {
            touch.set(sx,sy,0); camera.unproject(touch); float x=touch.x,y=touch.y;
            if(screen.equals("home")) {
                if(hit(x,y,65,225,w-130,62)) startRun();
                else if(hit(x,y,65,88,w-130,52)) screen="shop";
            } else if(screen.equals("shop")) {
                if(hit(x,y,65,415,w-130,42)) { if(coins>=50&&shield==0) { coins-=50; shield=1; save(); } }
                else if(hit(x,y,65,255,w-130,48)) { int cost=100+boost*100; if(coins>=cost&&boost<5) { coins-=cost; boost++; prefs.putInteger("boost",boost); save(); } }
                else if(hit(x,y,65,145,w-130,48)) screen="home";
            } else if(screen.equals("game")) {
                if(hit(x,y,395,h-75,65,65)) paused=true;
                else if(!paused) { px=MathUtils.clamp(x,25,w-25); py=MathUtils.clamp(y,90,h-120); dragging=true; }
                else if(hit(x,y,70,365,w-140,54)) paused=false;
                else if(hit(x,y,70,300,w-140,48)) endRun();
            } else if(screen.equals("over")) {
                if(hit(x,y,65,265,w-130,58)) startRun();
                else if(hit(x,y,65,110,w-130,52)) screen="home";
            }
            return true;
        }
        @Override public boolean touchDragged(int sx,int sy,int pointer) {
            if(dragging&&screen.equals("game")&&!paused) { touch.set(sx,sy,0); camera.unproject(touch); px=MathUtils.clamp(touch.x,25,w-25); py=MathUtils.clamp(touch.y,90,h-120); }
            return true;
        }
        @Override public boolean touchUp(int sx,int sy,int pointer,int button) { dragging=false; return true; }
        void startRun() { objects.clear(); sparks.clear(); elapsed=0; spawnTimer=.3f; score=0; stage=1; shield=1; px=w/2; py=h*.38f; running=true; paused=false; over=false; screen="game"; }
        void save() { prefs.putInteger("coins",coins); prefs.flush(); }
        @Override public void resize(int width,int height) { }
        @Override public void pause() { paused=true; }
        @Override public void resume() { }
        @Override public void hide() { }
        @Override public void dispose() { shapes.dispose(); batch.dispose(); font.dispose(); }
        @Override public boolean keyDown(int keycode) { if(keycode==com.badlogic.gdx.Input.Keys.BACK){ if(screen.equals("game")) paused=!paused; else if(screen.equals("shop")) screen="home"; else screen="home"; return true; } return false; }
    }
}
