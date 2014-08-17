package com.smith.d.tyler.notawfulmusicplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

// TODO track progress in the song.
// TODO organize this source code so it's in some kind of reasonable order
public class NowPlaying extends Activity {

	private static final String TAG = "Now Playing";

	// Stuff from the Android API and listeners
	private MediaPlayer mp;
	private BroadcastReceiver receiver;

	// State information
	private String artistName;
	private String albumName;
	private String[] songNames;
	private int songNamesPosition;
	private Messenger mService;
	boolean mIsBound;
	
	final Messenger mMessenger = new Messenger(new IncomingHandler());

	void doBindService() {
		Log.i(TAG, "Binding to the service!");
		bindService(new Intent(this, MusicPlaybackService.class), mConnection,
				Context.BIND_IMPORTANT | Context.BIND_AUTO_CREATE);
		mIsBound = true;
		// Need to start the service so it won't be stopped when this activity is destroyed.
		// https://developer.android.com/guide/components/bound-services.html
		startService(new Intent(this, MusicPlaybackService.class));
		// textStatus.setText("Binding.");
	}

	void doUnbindService() {
		Log.i(TAG, "Unbinding the service!");
		if (mIsBound) {
			// If we have received the service, and hence registered with it,
			// then now is the time to unregister.
			if (mService != null) {
				try {
					Message msg = Message.obtain(null,
							MusicPlaybackService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					// There is nothing special we need to do if the service has
					// crashed.
				}
			}
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
			// textStatus.setText("Unbinding.");
		}
	}

	private static class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			default:
				super.handleMessage(msg);
			}
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			// textStatus.setText("Attached.");
			try {
				Message msg = Message.obtain(null,
						MusicPlaybackService.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				// In this case the service has crashed before we could even do
				// anything with it
			}
			
			// set the playlist
	        Message msg = Message.obtain(null, MusicPlaybackService.MSG_SET_PLAYLIST);
	        msg.getData().putStringArray(SongList.SONG_LIST, songNames);
	        msg.getData().putInt(SongList.SONG_LIST_POSITION, songNamesPosition);
	        try {
	        	Log.i(TAG, "Sending a playlist!");
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
	        
			// start playing!
	        msg = Message.obtain(null, MusicPlaybackService.MSG_PLAYPAUSE);
	        try {
	        	Log.i(TAG, "Sending a playlist!");
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected - process crashed.
			mService = null;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_now_playing);
		
		doBindService();

		// Trying this approach to register for media button presses...
		// https://stackoverflow.com/questions/10154118/listen-to-volume-buttons-in-background-service
		// AudioManager mAudioManager = (AudioManager)
		// getSystemService(Context.AUDIO_SERVICE);
		// ComponentName rec = new ComponentName(getPackageName(),
		// NowPlaying.class.getName());
		// mAudioManager.registerMediaButtonEventReceiver(rec);

		mp = new MediaPlayer();
		// Get the message from the intent
		Intent intent = getIntent();
		artistName = intent.getStringExtra(ArtistList.ARTIST_NAME);
		albumName = intent.getStringExtra(AlbumList.ALBUM_NAME);
		songNames = intent.getStringArrayExtra(SongList.SONG_LIST);
		songNamesPosition = intent.getIntExtra(SongList.SONG_LIST_POSITION, 0);

		Log.d(TAG, "Got song names " + songNames + " position "
				+ songNamesPosition);

		String songName = songNames[songNamesPosition];

		TextView et = (TextView) findViewById(R.id.artistName);
		et.setText(artistName);

		et = (TextView) findViewById(R.id.albumName);
		et.setText(albumName);

		et = (TextView) findViewById(R.id.songName);
		et.setText(songName);

		IntentFilter filter = new IntentFilter();
		filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
		filter.addAction("android.intent.action.MEDIA_BUTTON");

		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.i(TAG, "received a broadcast: " + intent);
				// do something based on the intent's action
			}
		};
		registerReceiver(receiver, filter);

		final Button pause = (Button) findViewById(R.id.playPause);
		pause.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d(TAG, "Play/Pause clicked...");
                Message msg = Message.obtain(null, MusicPlaybackService.MSG_PLAYPAUSE);
                try {
                	Log.i(TAG, "SEnding a request to start playing!");
					mService.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}

		});

		Button previous = (Button) findViewById(R.id.previous);
		previous.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d(TAG, "Previous clicked...");
                Message msg = Message.obtain(null, MusicPlaybackService.MSG_PREVIOUS);
                try {
                	Log.i(TAG, "SEnding a request to go to previous!");
					mService.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}

		});

		Button next = (Button) findViewById(R.id.next);
		next.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d(TAG, "next clicked...");
                Message msg = Message.obtain(null, MusicPlaybackService.MSG_NEXT);
                try {
                	Log.i(TAG, "SEnding a request to go to next!");
					mService.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}			}

		});
		
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
			// code for fast forward
			Log.i(TAG, "key pressed KEYCODE_MEDIA_FAST_FORWARD");
			return true;
		case KeyEvent.KEYCODE_MEDIA_NEXT:
			// code for next
			Log.i(TAG, "key pressed KEYCODE_MEDIA_NEXT");
//			next();
			return true;
		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
			// code for play/pause
			Log.i(TAG, "key pressed KEYCODE_MEDIA_PLAY_PAUSE");
//			playPause();
			return true;
		case KeyEvent.KEYCODE_MEDIA_PAUSE:
			// code for play/pause
			Log.i(TAG, "key pressed KEYCODE_MEDIA_PAUSE");
//			playPause();
			return true;
		case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			Log.i(TAG, "key pressed KEYCODE_MEDIA_PREVIOUS");
			// code for previous
//			previous();
			return true;
		case KeyEvent.KEYCODE_MEDIA_REWIND:
			Log.i(TAG, "key pressed KEYCODE_MEDIA_REWIND");
			// code for rewind
			return true;
		case KeyEvent.KEYCODE_MEDIA_STOP:
			Log.i(TAG, "key pressed KEYCODE_MEDIA_STOP");
			// code for stop
			return true;
		default:
			Log.i(TAG, "key pressed " + keyCode);
			// code for stop
			return super.onKeyDown(keyCode, event);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.now_playing, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiver);
		unbindService(mConnection);
	}


}
