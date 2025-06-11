package com.project.cafeshopapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.net.InetAddress;

public class NetworkDebugUtils {
    private static final String TAG = "NetworkDebug";

    public static void logNetworkInfo(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null) {
                    Log.d(TAG, "🌐 Network Type: " + activeNetwork.getTypeName());
                    Log.d(TAG, "🌐 Network Subtype: " + activeNetwork.getSubtypeName());
                    Log.d(TAG, "🌐 Network State: " + activeNetwork.getState());
                    Log.d(TAG, "🌐 Network Connected: " + activeNetwork.isConnected());
                } else {
                    Log.w(TAG, "🌐 No active network");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting network info: " + e.getMessage());
        }
    }

    public static void testDNSResolution() {
        new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                InetAddress[] addresses = InetAddress.getAllByName("ufgxsicqlaraqaeziohf.supabase.co");
                long duration = System.currentTimeMillis() - startTime;

                Log.d(TAG, "🔍 DNS resolution took " + duration + "ms");
                for (InetAddress addr : addresses) {
                    Log.d(TAG, "🔍 Resolved IP: " + addr.getHostAddress());
                }
            } catch (Exception e) {
                Log.e(TAG, "🔍 DNS resolution failed: " + e.getMessage());
            }
        }).start();
    }
}
