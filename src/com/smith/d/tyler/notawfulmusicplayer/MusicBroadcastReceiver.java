package com.smith.d.tyler.notawfulmusicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

// Still trying to figure out how to receive bluetooth button presses...
public class MusicBroadcastReceiver extends BroadcastReceiver {
	private static final String TAG = "MusicBroadcastReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "got a thingy!");
	}

}
