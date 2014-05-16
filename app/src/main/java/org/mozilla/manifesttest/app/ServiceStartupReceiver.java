package org.mozilla.manifesttest.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ServiceStartupReceiver extends BroadcastReceiver {

    void startServiceWithPowerStateUnknown(Context context) {
        Intent intent = new Intent(context, PassiveStumblerService.class);
        intent.putExtra(PassiveStumblerService.POWER_STATE_UNKNOWN, true);
        context.startService(intent);
    }

    void startServiceWithPowerStateOk(Context context) {
        context.startService(new Intent(context, PassiveStumblerService.class));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("", action);
        //Toast.makeText(context, ":" + action, 1000).show();

        if (action.contains("GPS_ENABLED_CHANGE") ||
            action.contains("GPS_FIX_CHANGE") ||
            // These 2 are common events to hook into to start the service
            action.contains("ACTION_POWER_CONNECTED") ||
            action.contains("ACTION_POWER_DISCONNECTED"))
        {
            startServiceWithPowerStateUnknown(context);
        } else if (action.contains("BOOT_COMPLETED")) {
            PassiveStumblerService.setLowPowerDetectedFlag(context, false);
            startServiceWithPowerStateUnknown(context);
        } else if (action.contains("BATTERY_LOW")) {
            PassiveStumblerService.setLowPowerDetectedFlag(context, true);
            context.stopService(new Intent(context, PassiveStumblerService.class));
        } else if (action.contains("BATTERY_OKAY")) {
            PassiveStumblerService.setLowPowerDetectedFlag(context, false);
            startServiceWithPowerStateOk(context);
        }
    }
}
