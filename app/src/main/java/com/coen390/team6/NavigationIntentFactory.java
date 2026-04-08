package com.coen390.team6;

import android.content.Context;
import android.content.Intent;

public final class NavigationIntentFactory {
    private NavigationIntentFactory() {
    }

    public static Intent createGpsIntent(Context context) {
        Intent intent = new Intent(context, GpsNavigationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return intent;
    }
}
