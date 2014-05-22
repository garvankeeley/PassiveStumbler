package org.mozilla.manifesttest.app;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class PassiveStumblerService extends Service {
    Handler mHandler = new Handler();
    public static final String POWER_STATE_UNKNOWN = "ps_unknown";
    static final String PREF_LOW_POWER = "PREF_LOW_POWER";

    Context context;

    boolean mIsGpsListenerStarted;

    public static void setLowPowerDetectedFlag(Context context, boolean isSet) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean(PassiveStumblerService.PREF_LOW_POWER, isSet);
        preferences.edit().apply();
    }

    BroadcastReceiver mPowerLevelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            unregisterReceiver(mPowerLevelReceiver);
            int rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int level = Math.round(rawLevel * scale/100.0f);

            if (level < 15) {
                setLowPowerDetectedFlag(context, true);
                stopSelf();
            }
            else {
                setLowPowerDetectedFlag(context, false);
                if (!mIsGpsListenerStarted) {
                    startGpsListener();
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**if power state not known, add listener for the battery state
     * if pref is currently low_power, don't start gpslistener, just watch the battery until ok to start*/
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = this;

        boolean powerStateUnknown = intent.getBooleanExtra(POWER_STATE_UNKNOWN, false);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean lowPowerPreviouslyDetected = preferences.getBoolean(PREF_LOW_POWER, false);

        if (powerStateUnknown || lowPowerPreviouslyDetected) {
            context.registerReceiver(mPowerLevelReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }

        if (!lowPowerPreviouslyDetected)
            startGpsListener();

        return START_STICKY;
    }

    static int counter = 0;

    LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Toast.makeText(getBaseContext(), (counter++) +  " GPS FIX: "
                    + location.getLongitude() + " : " + location.getLatitude(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {}

        @Override
        public void onProviderEnabled(String s) {}

        @Override
        public void onProviderDisabled(String s) { }
    };

    public void startGpsListener() {
        mIsGpsListenerStarted = true;
        LocationManager locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, mLocationListener);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mPowerLevelReceiver);
        LocationManager locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(mLocationListener);
    }
}
