package com.kage049754.adquest;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.media.AudioManager;
import java.util.concurrent.atomic.AtomicBoolean;

/** Small original synthesized soundtrack and short gameplay cues; no external audio assets required. */
public final class Soundscape {
    private static final int SAMPLE_RATE = 22050;
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final AtomicBoolean musicEnabled = new AtomicBoolean(true);
    private volatile boolean playing;
    private Thread musicThread;
    private AudioTrack track;
    private ToneGenerator tones;

    public Soundscape() {
        try { tones = new ToneGenerator(AudioManager.STREAM_MUSIC, 42); } catch (Exception ignored) {}
    }

    public boolean isEnabled() { return enabled.get(); }
    public boolean isMusicEnabled() { return musicEnabled.get(); }
    public void setEnabled(boolean value) {
        enabled.set(value);
        if (!value) stopMusic();
        else if (musicEnabled.get()) startMusic();
    }
    public void setMusicEnabled(boolean value) {
        musicEnabled.set(value);
        if (value && enabled.get()) startMusic(); else stopMusic();
    }

    public void collect() { tone(ToneGenerator.TONE_PROP_BEEP, 65); }
    public void shield() { tone(ToneGenerator.TONE_PROP_ACK, 120); }
    public void hit() { tone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 220); }
    public void stage() { tone(ToneGenerator.TONE_PROP_PROMPT, 100); }
    private void tone(int type, int duration) {
        if (enabled.get() && tones != null) try { tones.startTone(type, duration); } catch (Exception ignored) {}
    }

    public synchronized void startMusic() {
        if (playing || !enabled.get() || !musicEnabled.get()) return;
        try {
            int min = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            int bufferSize = Math.max(min, 2048);
            track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(bufferSize).setTransferMode(AudioTrack.MODE_STREAM).build();
            track.setVolume(0.13f);
            track.play();
            playing = true;
            musicThread = new Thread(() -> synthesize(bufferSize / 2), "last1pct-ambient-music");
            musicThread.setDaemon(true);
            musicThread.start();
        } catch (Exception ignored) {
            playing = false;
            releaseTrack();
        }
    }

    private void synthesize(int bufferSamples) {
        short[] buffer = new short[Math.max(512, bufferSamples)];
        long sampleIndex = 0;
        int[] roots = {110, 130, 98, 147, 110, 164, 123, 147};
        while (playing && enabled.get() && musicEnabled.get()) {
            int root = roots[(int)((sampleIndex / (SAMPLE_RATE * 2L)) % roots.length)];
            for (int i = 0; i < buffer.length; i++) {
                double t = (sampleIndex + i) / (double) SAMPLE_RATE;
                double fade = Math.min(1.0, Math.min(t % 2.0 / 0.35, (2.0 - (t % 2.0)) / 0.35));
                double bass = Math.sin(2 * Math.PI * root * t) * 0.38;
                double pad = Math.sin(2 * Math.PI * root * 1.5 * t) * 0.18
                        + Math.sin(2 * Math.PI * root * 2.0 * t) * 0.12;
                buffer[i] = (short)(Math.sin(2 * Math.PI * 0.13 * t) * (bass + pad) * Math.max(0.08, fade) * 2600);
            }
            if (track == null || !playing) break;
            try {
                int written = track.write(buffer, 0, buffer.length);
                if (written < 0) break;
                sampleIndex += written;
            } catch (Exception ignored) { break; }
        }
    }

    public synchronized void stopMusic() {
        playing = false;
        if (musicThread != null) {
            musicThread.interrupt();
            musicThread = null;
        }
        releaseTrack();
    }

    private synchronized void releaseTrack() {
        if (track != null) {
            try { track.pause(); track.flush(); track.release(); } catch (Exception ignored) {}
            track = null;
        }
    }

    public void release() {
        stopMusic();
        if (tones != null) { try { tones.release(); } catch (Exception ignored) {} tones = null; }
    }
}
