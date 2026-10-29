package com.kage049754.adquest;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

/** Android entry point for the LibGDX game. */
public class MainActivity extends AndroidApplication {
    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useImmersiveMode = true;
        initialize(new LastOnePercentGame(), config);
    }
}
