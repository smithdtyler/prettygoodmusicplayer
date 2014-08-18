package com.smith.d.tyler.notawfulmusicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

// Still trying to figure out how to receive bluetooth button presses...
public class MusicBroadcastReceiver extends BroadcastReceiver {
	private static final String TAG = "MusicBroadcastReceiver";

	boolean mIsBound;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "got a thingy!");

		if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
			Log.i(TAG, "Media Button Receiver: received media button intent: " + intent);

			KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
			Log.i(TAG, "Got a key event");
			if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
				Log.i(TAG, "Got a key up event");
				// connect to the service
				// send a message
				// Create an intent with the message type, then send it to "start service"
				// Looks like it's OK to call this multiple times
				//https://stackoverflow.com/questions/13124115/starting-android-service-already-running
				
				int keyCode = keyEvent.getKeyCode();
				Intent msgIntent = new Intent(context, MusicPlaybackService.class);
				
				switch (keyCode) {
				case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
					// code for fast forward
					Log.i(TAG, "key pressed KEYCODE_MEDIA_FAST_FORWARD");
					break;
				case KeyEvent.KEYCODE_MEDIA_NEXT:
					// code for next
					Log.i(TAG, "key pressed KEYCODE_MEDIA_NEXT");
					msgIntent.putExtra("Message", MusicPlaybackService.MSG_NEXT);
					context.startService(msgIntent);
					break;
				case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
					// code for play/pause
					Log.i(TAG, "key pressed KEYCODE_MEDIA_PLAY_PAUSE");
					msgIntent.putExtra("Message", MusicPlaybackService.MSG_PLAYPAUSE);
					context.startService(msgIntent);
					break;
				case KeyEvent.KEYCODE_MEDIA_PAUSE:
					// code for play/pause
					Log.i(TAG, "key pressed KEYCODE_MEDIA_PAUSE");
					msgIntent.putExtra("Message", MusicPlaybackService.MSG_PLAYPAUSE);
					context.startService(msgIntent);
					break;
				case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
					Log.i(TAG, "key pressed KEYCODE_MEDIA_PREVIOUS");
					msgIntent.putExtra("Message", MusicPlaybackService.MSG_PREVIOUS);
					context.startService(msgIntent);
					// code for previous
					break;
				case KeyEvent.KEYCODE_MEDIA_REWIND:
					Log.i(TAG, "key pressed KEYCODE_MEDIA_REWIND");
					// code for rewind
					break;
				case KeyEvent.KEYCODE_MEDIA_STOP:
					Log.i(TAG, "key pressed KEYCODE_MEDIA_STOP");
					// code for stop
					break;
				default:
					Log.i(TAG, "key pressed " + keyCode);
					// code for stop
					break;
			}
				
			}
		}

	}
}