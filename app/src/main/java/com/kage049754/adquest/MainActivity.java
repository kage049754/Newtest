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
    private int coins = 0;
    private boolean roundEnded = true;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(17, 27, 46));
        getWindow().setNavigationBarColor(Color.rgb(17, 27, 46));
        MobileAds.initialize(this, initializationStatus -> runOnUiThread(this::loadRewardedAd));
        buildUi();
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(17, 27, 46));
        root.setPadding(dp(14), dp(10), dp(14), dp(6));

        TextView title = new TextView(this);
        title.setText("★  STAR CATCH");
        title.setTextColor(Color.rgb(255, 221, 120));
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(5), 0, dp(4));
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(42)));

        status = new TextView(this);
        status.setText("Catch stars. Avoid bombs. You have 30 seconds!");
        status.setTextColor(Color.WHITE);
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(36)));

        FrameLayout gameFrame = new FrameLayout(this);
        game = new StarCatchView(this, this::onRoundFinished);
        gameFrame.addView(game, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams gameParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        gameParams.topMargin = dp(6);
        gameParams.bottomMargin = dp(8);
        root.addView(gameFrame, gameParams);

        playButton = new Button(this);
        playButton.setText("START ROUND");
        playButton.setOnClickListener(v -> startRound());
        root.addView(playButton, new LinearLayout.LayoutParams(-1, dp(50)));

        rewardButton = new Button(this);
        rewardButton.setText("DOUBLE COINS — WATCH TEST AD");
        rewardButton.setVisibility(View.GONE);
        rewardButton.setOnClickListener(v -> showRewardedAd());
        root.addView(rewardButton, new LinearLayout.LayoutParams(-1, dp(48)));

        banner = new AdView(this);
        banner.setAdSize(AdSize.BANNER);
        banner.setAdUnitId(BANNER_TEST_ID);
        LinearLayout.LayoutParams adParams = new LinearLayout.LayoutParams(-1, dp(50));
        adParams.topMargin = dp(4);
        root.addView(banner, adParams);
        banner.loadAd(new AdRequest.Builder().build());
        setContentView(root);
    }

    private void startRound() {
        roundEnded = false;
        rewardButton.setVisibility(View.GONE);
        playButton.setEnabled(false);
        status.setText("Score 0  •  Coins " + coins + "  •  Tap the stars!");
        game.startRound();
    }

    private void onRoundFinished(int score) {
        roundEnded = true;
        int earned = Math.max(1, score / 2);
        coins += earned;
        getPreferences(MODE_PRIVATE).edit().putInt("coins", coins).putInt("best", Math.max(score, getPreferences(MODE_PRIVATE).getInt("best", 0))).apply();
        status.setText("Round over! Score " + score + "  •  +" + earned + " coins  •  Total " + coins);
        playButton.setEnabled(true);
        playButton.setText("PLAY AGAIN");
        rewardButton.setVisibility(View.VISIBLE);
        loadRewardedAd();
    }

    private void loadRewardedAd() {
        AdRequest request = new AdRequest.Builder().build();
        RewardedAd.load(this, REWARDED_TEST_ID, request, new RewardedAdLoadCallback() {
            @Override public void onAdLoaded(RewardedAd ad) { rewardedAd = ad; rewardButton.setEnabled(true); }
            @Override public void onAdFailedToLoad(LoadAdError error) { rewardedAd = null; rewardButton.setEnabled(false); }
        });
    }

    private void showRewardedAd() {
        if (!roundEnded || rewardedAd == null) {
            status.setText("Test ad is not ready yet. Try again in a moment.");
            loadRewardedAd();
            return;
        }
        rewardButton.setEnabled(false);
        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() { rewardedAd = null; loadRewardedAd(); }
            @Override public void onAdFailedToShowFullScreenContent(AdError error) { rewardedAd = null; loadRewardedAd(); }
        });
        rewardedAd.show(this, (RewardItem item) -> {
            int bonus = Math.max(1, game.getLastScore() / 2);
            coins += bonus;
            getPreferences(MODE_PRIVATE).edit().putInt("coins", coins).apply();
            status.setText("Reward earned! +" + bonus + " bonus coins  •  Total " + coins);
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
