package com.kage049754.adquest;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
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
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(3), 0, dp(2));
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(42)));

        wallet = new TextView(this);
        wallet.setTextColor(Color.rgb(255, 220, 120));
        wallet.setTextSize(14);
        wallet.setGravity(Gravity.CENTER);
        root.addView(wallet, new LinearLayout.LayoutParams(-1, dp(26)));

        status = new TextView(this);
        status.setText("Drag to move. Collect energy. One hit ends your run.");
        status.setTextColor(Color.WHITE);
        status.setTextSize(13);
        status.setGravity(Gravity.CENTER);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(38)));

        FrameLayout gameFrame = new FrameLayout(this);
        game = new StarCatchView(this, this::onRoundFinished);
        gameFrame.addView(game, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams gameParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        gameParams.topMargin = dp(4);
        gameParams.bottomMargin = dp(5);
        root.addView(gameFrame, gameParams);

        playButton = new Button(this);
        playButton.setText("START SURVIVAL");
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
        refreshUpgradeButtons();

        rewardButton = new Button(this);
        rewardButton.setText("CLAIM BONUS COINS — REWARDED TEST AD");
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
        status.setText("Drag left and right • Avoid the red hazards");
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
        status.setText((hitHazard ? "Core destroyed" : "45 seconds survived!")
                + " • Score " + score + " • +" + earned + " coins");
        playButton.setEnabled(true);
        playButton.setText("RUN IT BACK");
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
        if (wallet != null) wallet.setText("COINS  " + coins + "     BEST  " + best);
    }

    private void refreshUpgradeButtons() {
        if (shieldButton != null) shieldButton.setText(shieldLevel > 0 ? "SHIELD ✓" : "SHIELD • 50");
        if (boostButton != null) {
            int cost = boostLevel == 0 ? 100 : boostLevel == 1 ? 200 : 350;
            boostButton.setText(boostLevel >= 3 ? "COIN BOOST MAX" : "COIN BOOST " + boostLevel + " • " + cost);
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

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onPause() {
        if (game != null) game.pauseGame();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (game != null) game.resumeGame();
    }
}
