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
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.AdError;

public class MainActivity extends Activity {
    private static final String REWARDED_TEST_ID = "ca-app-pub-3940256099942544/5224354917";
    private static final String BANNER_TEST_ID = "ca-app-pub-3940256099942544/9214589741";
    private LinearLayout root;
    private TextView status;
    private Button playButton, rewardButton;
    private StarCatchView game;
    private AdView banner;
    private RewardedAd rewardedAd;
    private int coins;
    private boolean roundEnded = true;
    private boolean rewardUsedThisRound;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        coins = getPreferences(MODE_PRIVATE).getInt("coins", 0);
        getWindow().setStatusBarColor(Color.rgb(10, 17, 32));
        getWindow().setNavigationBarColor(Color.rgb(10, 17, 32));
        buildUi();
        MobileAds.initialize(this, initializationStatus -> runOnUiThread(this::loadRewardedAd));
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(10, 17, 32));
        root.setPadding(dp(14), dp(10), dp(14), dp(6));

        TextView title = new TextView(this);
        title.setText("LAST 1%");
        title.setTextColor(Color.rgb(92, 232, 255));
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(5), 0, dp(4));
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(44)));

        status = new TextView(this);
        status.setText("One hit ends the run. Dodge hazards and collect energy.");
        status.setTextColor(Color.WHITE);
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(42)));

        FrameLayout gameFrame = new FrameLayout(this);
        game = new StarCatchView(this, this::onRoundFinished);
        gameFrame.addView(game, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams gameParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        gameParams.topMargin = dp(6);
        gameParams.bottomMargin = dp(8);
        root.addView(gameFrame, gameParams);

        playButton = new Button(this);
        playButton.setText("START SURVIVAL");
        playButton.setOnClickListener(v -> startRound());
        root.addView(playButton, new LinearLayout.LayoutParams(-1, dp(50)));

        rewardButton = new Button(this);
        rewardButton.setText("BONUS COINS — WATCH TEST AD");
        rewardButton.setVisibility(View.GONE);
        rewardButton.setOnClickListener(v -> showRewardedAd());
        root.addView(rewardButton, new LinearLayout.LayoutParams(-1, dp(48)));

        banner = new AdView(this);
        banner.setAdSize(AdSize.BANNER);
        banner.setAdUnitId(BANNER_TEST_ID);
        LinearLayout.LayoutParams adParams = new LinearLayout.LayoutParams(-1, dp(50));
        adParams.topMargin = dp(4);
        root.addView(banner, adParams);
        setContentView(root);
        banner.loadAd(new AdRequest.Builder().build());
    }

    private void startRound() {
        roundEnded = false;
        rewardUsedThisRound = false;
        rewardButton.setVisibility(View.GONE);
        rewardButton.setEnabled(false);
        playButton.setEnabled(false);
        playButton.setText("SURVIVE!");
        status.setText("Coins " + coins + " • One hit ends the run");
        game.startRound();
    }

    private void onRoundFinished(int score) {
        roundEnded = true;
        int earned = Math.max(1, score / 3);
        coins += earned;
        getPreferences(MODE_PRIVATE).edit()
                .putInt("coins", coins)
                .putInt("best", Math.max(score, getPreferences(MODE_PRIVATE).getInt("best", 0)))
                .apply();
        int best = getPreferences(MODE_PRIVATE).getInt("best", score);
        status.setText("Run over • Score " + score + " • Best " + best + " • +" + earned + " coins");
        playButton.setEnabled(true);
        playButton.setText("TRY AGAIN");
        rewardButton.setVisibility(View.VISIBLE);
        loadRewardedAd();
    }

    private void loadRewardedAd() {
        AdRequest request = new AdRequest.Builder().build();
        RewardedAd.load(this, REWARDED_TEST_ID, request, new RewardedAdLoadCallback() {
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
            status.setText("Bonus ad is not ready. You can still play again.");
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
            int bonus = Math.max(1, game.getLastScore() / 3);
            coins += bonus;
            getPreferences(MODE_PRIVATE).edit().putInt("coins", coins).apply();
            status.setText("Bonus earned! +" + bonus + " coins • Total " + coins);
            rewardButton.setVisibility(View.GONE);
        });
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onPause() {
        if (banner != null) banner.pause();
        if (game != null) game.pauseGame();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (banner != null) banner.resume();
        if (game != null) game.resumeGame();
    }

    @Override protected void onDestroy() {
        if (banner != null) banner.destroy();
        super.onDestroy();
    }
}
