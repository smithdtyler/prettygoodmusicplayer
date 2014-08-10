package com.smith.d.tyler.notawfulmusicplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class NowPlaying extends Activity {

	private static final String TAG = "Now Playing";
	private MediaPlayer mp = new MediaPlayer();
	private File sdcard;
	private File music;
	private String artistName;
	private String albumName;
	private String[] songNames;
	private int songNamesPosition;
	private FileInputStream fis;

	private BroadcastReceiver receiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_now_playing);

		// Get the message from the intent
		Intent intent = getIntent();
		artistName = intent.getStringExtra(ArtistList.ARTIST_NAME);
		albumName = intent.getStringExtra(AlbumList.ALBUM_NAME);
		songNames = intent.getStringArrayExtra(SongList.SONG_LIST);
		songNamesPosition = intent.getIntExtra(SongList.SONG_LIST_POSITION, 0);

		Log.d(TAG, "Got song names " + songNames + " position "
				+ songNamesPosition);

		File song = new File(songNames[songNamesPosition]);
		String songName = song.getName().replaceAll("\\d\\d\\s", "").replaceAll("(\\.mp3)|(\\.m4p)|(\\.m4a)", "");
		Log.i(TAG, "Getting file for " + songName);

		TextView et = (TextView) findViewById(R.id.artistName);
		et.setText(artistName);

		et = (TextView) findViewById(R.id.albumName);
		et.setText(albumName);

		et = (TextView) findViewById(R.id.songName);
		et.setText(songName);

		mp.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				Log.i(TAG, "Song complete");
				try {
					mp.stop();
					mp.reset();
					fis.close();
					songNamesPosition = (songNamesPosition + 1)
							% songNames.length;
					String next = songNames[songNamesPosition];
					fis = new FileInputStream(new File(next));
					mp.setDataSource(fis.getFD());
					mp.prepare();
					mp.start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		});

		try {
			fis = new FileInputStream(song);
			Log.i(TAG, "About to play " + song);
			mp.setDataSource(fis.getFD());
			mp.prepare();
			mp.start();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Button pause = (Button) findViewById(R.id.playPause);
		pause.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d(TAG, "Play/Pause clicked...");
				if (mp.isPlaying()) {
					mp.pause();
				} else {
					mp.start();
				}
			}

		});

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
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
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
			return true;
		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
			// code for play/pause
			Log.i(TAG, "key pressed KEYCODE_MEDIA_PLAY_PAUSE");
			return true;
		case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			Log.i(TAG, "key pressed KEYCODE_MEDIA_PREVIOUS");
			// code for previous
			return true;
		case KeyEvent.KEYCODE_MEDIA_REWIND:
			Log.i(TAG, "key pressed KEYCODE_MEDIA_REWIND");
			// code for rewind
			return true;
		case KeyEvent.KEYCODE_MEDIA_STOP:
			Log.i(TAG, "key pressed KEYCODE_MEDIA_STOP");
			// code for stop
			return true;
		}
		return super.onKeyDown(keyCode, event);
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
	}
}
