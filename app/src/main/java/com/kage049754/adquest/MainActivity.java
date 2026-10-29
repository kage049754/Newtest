package com.kage049754.adquest;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.FrameLayout;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.AdError;

public class MainActivity extends Activity {
    private static final String REWARDED_TEST_ID = "ca-app-pub-3940256099942544/5224354917";
    private LinearLayout root;
    private TextView status, wallet;
    private Button playButton, rewardButton, shieldButton, boostButton;
    private StarCatchView game;
    private Soundscape soundscape;
    private Button soundButton;
    private RewardedAd rewardedAd;
    private int coins, best, shieldLevel, boostLevel;
    private boolean roundEnded = true;
    private boolean rewardUsedThisRound;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        coins = getPreferences(MODE_PRIVATE).getInt("coins", 0);
        best = getPreferences(MODE_PRIVATE).getInt("best", 0);
        shieldLevel = getPreferences(MODE_PRIVATE).getInt("shieldLevel", 0);
        boostLevel = getPreferences(MODE_PRIVATE).getInt("boostLevel", 0);
        soundscape = new Soundscape();
        soundscape.setEnabled(getPreferences(MODE_PRIVATE).getBoolean("soundEnabled", true));
        soundscape.setMusicEnabled(getPreferences(MODE_PRIVATE).getBoolean("musicEnabled", true));
        getWindow().setStatusBarColor(Color.rgb(7, 13, 28));
        getWindow().setNavigationBarColor(Color.rgb(7, 13, 28));
        buildUi();
        refreshWallet();
        MobileAds.initialize(this, initializationStatus -> runOnUiThread(this::loadRewardedAd));
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(7, 13, 28));
        root.setPadding(dp(12), dp(6), dp(12), dp(6));

        TextView title = new TextView(this);
        title.setText("LAST 1%");
        title.setTextColor(Color.rgb(91, 235, 255));
        title.setTextSize(29);
        title.setTypeface(android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(3), 0, dp(2));
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(42)));

        wallet = new TextView(this);
        wallet.setTextColor(Color.rgb(255, 220, 120));
        wallet.setTextSize(14);
        wallet.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
        wallet.setGravity(Gravity.CENTER);
        root.addView(wallet, new LinearLayout.LayoutParams(-1, dp(26)));

        status = new TextView(this);
        status.setText("ENDLESS SURVIVAL  •  Dodge hazards. Collect energy.");
        status.setTextColor(Color.WHITE);
        status.setTextSize(13);
        status.setGravity(Gravity.CENTER);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(38)));

        FrameLayout gameFrame = new FrameLayout(this);
        game = new StarCatchView(this, this::onRoundFinished, soundscape);
        gameFrame.addView(game, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams gameParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        gameParams.topMargin = dp(4);
        gameParams.bottomMargin = dp(5);
        root.addView(gameFrame, gameParams);

        playButton = new Button(this);
        playButton.setText("▶   START SURVIVAL");
        styleButton(playButton, 0xFF17D9E8, 0xFF087DAB, Color.WHITE, 15);
        playButton.setOnClickListener(v -> startRound());
        root.addView(playButton, new LinearLayout.LayoutParams(-1, dp(46)));

        LinearLayout upgrades = new LinearLayout(this);
        upgrades.setOrientation(LinearLayout.HORIZONTAL);
        shieldButton = new Button(this);
        boostButton = new Button(this);
        shieldButton.setTextSize(11);
        boostButton.setTextSize(11);
        shieldButton.setOnClickListener(v -> buyShield());
        boostButton.setOnClickListener(v -> buyBoost());
        upgrades.addView(shieldButton, new LinearLayout.LayoutParams(0, dp(48), 1f));
        upgrades.addView(boostButton, new LinearLayout.LayoutParams(0, dp(48), 1f));
        root.addView(upgrades);
        styleButton(shieldButton, 0xFF203B59, 0xFF132940, 0xFFBCEEFF, 11);
        styleButton(boostButton, 0xFF203B59, 0xFF132940, 0xFFFFD98A, 11);
        soundButton = new Button(this);
        soundButton.setTextSize(11);
        soundButton.setOnClickListener(v -> toggleSound());
        styleButton(soundButton, 0xFF203B59, 0xFF132940, 0xFFEAF7FF, 11);
        LinearLayout.LayoutParams soundParams = new LinearLayout.LayoutParams(-1, dp(38));
        soundParams.topMargin = dp(4);
        root.addView(soundButton, soundParams);
        refreshSoundButton();
        refreshUpgradeButtons();

        rewardButton = new Button(this);
        rewardButton.setText("▶  CLAIM BONUS COINS  •  REWARDED AD");
        styleButton(rewardButton, 0xFFFFC857, 0xFFDF8733, 0xFF172238, 11);
        rewardButton.setTextSize(11);
        rewardButton.setVisibility(View.GONE);
        rewardButton.setOnClickListener(v -> showRewardedAd());
        root.addView(rewardButton, new LinearLayout.LayoutParams(-1, dp(44)));

        setContentView(root);
    }

    private void startRound() {
        roundEnded = false;
        rewardUsedThisRound = false;
        rewardButton.setVisibility(View.GONE);
        rewardButton.setEnabled(false);
        playButton.setEnabled(false);
        shieldButton.setEnabled(false);
        boostButton.setEnabled(false);
        playButton.setText("SURVIVE!");
        status.setText("SURVIVE AS LONG AS YOU CAN  •  Stage difficulty rises every minute");
        game.setRunUpgrades(shieldLevel > 0, boostLevel);
        game.startRound();
    }

    private void onRoundFinished(int score, int survivedSeconds, boolean hitHazard) {
        roundEnded = true;
        int earned = Math.max(1, score / 4 + survivedSeconds / 5);
        coins += earned;
        if (score > best) best = score;
        getPreferences(MODE_PRIVATE).edit()
                .putInt("coins", coins).putInt("best", best)
                .putInt("shieldLevel", shieldLevel).putInt("boostLevel", boostLevel).apply();
        refreshWallet();
        int minutes = survivedSeconds / 60;
        int seconds = survivedSeconds % 60;
        status.setText((hitHazard ? "CORE DESTROYED" : "RUN ENDED")
                + "  •  " + String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
                + " survived  •  Score " + score + "  •  +" + earned + " coins");
        playButton.setEnabled(true);
        playButton.setText("↻   PLAY AGAIN");
        shieldButton.setEnabled(true);
        boostButton.setEnabled(true);
        rewardButton.setVisibility(View.VISIBLE);
        loadRewardedAd();
    }

    private void buyShield() {
        if (!roundEnded) return;
        if (shieldLevel == 0 && coins >= 50) {
            coins -= 50;
            shieldLevel = 1;
            saveProgress();
            status.setText("Shield unlocked: blocks one hit each run.");
            refreshWallet();
            refreshUpgradeButtons();
        } else if (shieldLevel > 0) {
            status.setText("Shield is permanently unlocked.");
        } else {
            status.setText("You need 50 coins to unlock the shield.");
        }
    }

    private void buyBoost() {
        if (!roundEnded) return;
        int cost = boostLevel == 0 ? 100 : boostLevel == 1 ? 200 : 350;
        if (boostLevel >= 3) {
            status.setText("Coin boost is fully upgraded.");
            return;
        }
        if (coins < cost) {
            status.setText("You need " + cost + " coins for the next coin boost.");
            return;
        }
        coins -= cost;
        boostLevel++;
        saveProgress();
        refreshWallet();
        refreshUpgradeButtons();
        status.setText("Coin boost upgraded to level " + boostLevel + ".");
    }

    private void refreshWallet() {
        if (wallet != null) wallet.setText("◈  " + coins + " COINS     ✦  BEST " + best);
    }

    private void refreshUpgradeButtons() {
        if (shieldButton != null) { shieldButton.setText(shieldLevel > 0 ? "◈ SHIELD UNLOCKED" : "◈ SHIELD  •  50"); styleButton(shieldButton, 0xFF203B59, 0xFF132940, 0xFFBCEEFF, 11); }
        if (boostButton != null) {
            int cost = boostLevel == 0 ? 100 : boostLevel == 1 ? 200 : 350;
            boostButton.setText(boostLevel >= 3 ? "✦ BOOST MAX" : "✦ BOOST " + boostLevel + "  •  " + cost);
        }
    }

    private void saveProgress() {
        getPreferences(MODE_PRIVATE).edit().putInt("coins", coins).putInt("best", best)
                .putInt("shieldLevel", shieldLevel).putInt("boostLevel", boostLevel).apply();
    }

    private void loadRewardedAd() {
        RewardedAd.load(this, REWARDED_TEST_ID, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override public void onAdLoaded(RewardedAd ad) {
                rewardedAd = ad;
                if (rewardButton != null) rewardButton.setEnabled(true);
            }
            @Override public void onAdFailedToLoad(LoadAdError error) {
                rewardedAd = null;
                if (rewardButton != null) rewardButton.setEnabled(false);
            }
        });
    }

    private void showRewardedAd() {
        if (!roundEnded || rewardUsedThisRound) return;
        if (rewardedAd == null) {
            status.setText("Rewarded ad is not ready yet. Try again shortly.");
            loadRewardedAd();
            return;
        }
        rewardButton.setEnabled(false);
        rewardUsedThisRound = true;
        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
                loadRewardedAd();
            }
            @Override public void onAdFailedToShowFullScreenContent(AdError error) {
                rewardedAd = null;
                loadRewardedAd();
            }
        });
        rewardedAd.show(this, (RewardItem item) -> {
            int bonus = Math.max(5, game.getLastScore() / 3);
            coins += bonus;
            saveProgress();
            refreshWallet();
            status.setText("Reward claimed: +" + bonus + " bonus coins.");
            rewardButton.setVisibility(View.GONE);
        });
    }

    private void toggleSound() {
        boolean enabled = !soundscape.isEnabled();
        soundscape.setEnabled(enabled);
        getPreferences(MODE_PRIVATE).edit().putBoolean("soundEnabled", enabled).apply();
        refreshSoundButton();
        status.setText(enabled ? "Sound effects enabled. Ambient music: " + (soundscape.isMusicEnabled() ? "ON" : "OFF") : "All game audio muted.");
    }

    private void refreshSoundButton() {
        if (soundButton != null) {
            soundButton.setText("♪  AUDIO " + (soundscape.isEnabled() ? "ON" : "OFF")
                    + "   •   AMBIENT MUSIC " + (soundscape.isMusicEnabled() && soundscape.isEnabled() ? "ON" : "OFF"));
        }
    }

    private void styleButton(Button button, int topColor, int bottomColor, int textColor, int textSize) {
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{topColor, bottomColor});
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), 0x554DEBFF);
        button.setBackground(bg);
        button.setTextColor(textColor);
        button.setTextSize(textSize);
        button.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD));
        button.setAllCaps(false);
        button.setElevation(dp(3));
        button.setPadding(dp(8), dp(4), dp(8), dp(4));
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onPause() {
        if (game != null) game.pauseGame();
        if (soundscape != null) soundscape.stopMusic();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (soundscape != null) soundscape.release();
        super.onDestroy();
    }

    @Override protected void onResume() {
        super.onResume();
        if (game != null) game.resumeGame();
        if (soundscape != null && soundscape.isEnabled() && soundscape.isMusicEnabled()) soundscape.startMusic();
    }
}
