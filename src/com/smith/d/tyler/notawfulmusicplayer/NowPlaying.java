package com.smith.d.tyler.notawfulmusicplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
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
import android.widget.TextView;

// TODO track progress in the song.
// TODO organize this source code so it's in some kind of reasonable order
public class NowPlaying extends Activity {

	private static final String TAG = "Now Playing";

	// Stuff from the Android API and listeners
	private MediaPlayer mp = new MediaPlayer();
	private BroadcastReceiver receiver;
	private NotAwfulAudioFocusChangeListener audioFocusListener;
	private AudioManager am;

	// State information
	private String artistName;
	private String albumName;
	private String[] songNames;
	private int songNamesPosition;
	private FileInputStream fis;
	private File song;

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

		song = new File(songNames[songNamesPosition]);
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
				next();
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
		
		//https://developer.android.com/training/managing-audio/audio-focus.html
		audioFocusListener = new NotAwfulAudioFocusChangeListener();
		
		// Get permission to play audio
		am = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);

		// Request audio focus for playback
		int result = am.requestAudioFocus(this.audioFocusListener,
		                                 // Use the music stream.
		                                 AudioManager.STREAM_MUSIC,
		                                 // Request permanent focus.
		                                 AudioManager.AUDIOFOCUS_GAIN);
		Log.d(TAG, "requestAudioFocus result = " + result);
		   
		if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			Log.d(TAG, "We got audio focus!");
		    //am.registerMediaButtonEventReceiver(receiver); // TODO do I need this?
		    // Start playback.
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
		} else {
			Log.e(TAG, "Unable to get audio focus");
		}
		
		final Button pause = (Button) findViewById(R.id.playPause);
		pause.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d(TAG, "Play/Pause clicked...");
				playPause();
			}

		});

		Button previous = (Button) findViewById(R.id.previous);
		previous.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d(TAG, "Previous clicked...");
				previous();
			}

		});
		
		Button next = (Button) findViewById(R.id.next);
		next.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d(TAG, "next clicked...");
				next();
			}

		});
		
	}

	// On stop is called when we get a phone call.
	@Override
	protected void onStop() {
		super.onPause();
		Log.i(TAG, "onStop()");
		pause();
		mp.stop();
		mp.reset();
		try {
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.onStop();
		Log.i(TAG, "Releasing audio focus");
		this.am.abandonAudioFocus(this.audioFocusListener);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		// When onStop was called, we closed the file handle and stopped the media player
		// Don't start playing on restart, but get everything ready
		try {
			fis = new FileInputStream(song);
			mp.setDataSource(fis.getFD());
			mp.prepare();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// On pause is called when the screen is put to sleep.
	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
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
			next();
			return true;
		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
			// code for play/pause
			Log.i(TAG, "key pressed KEYCODE_MEDIA_PLAY_PAUSE");
			playPause();
			return true;
		case KeyEvent.KEYCODE_MEDIA_PAUSE:
			// code for play/pause
			Log.i(TAG, "key pressed KEYCODE_MEDIA_PAUSE");
			playPause();
			return true;
		case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			Log.i(TAG, "key pressed KEYCODE_MEDIA_PREVIOUS");
			// code for previous
			previous();
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
			Log.i(TAG, "key pressed "+ keyCode);
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
	}
	
	private void previous(){
		mp.stop();
		mp.reset();
		try {
			fis.close();
			songNamesPosition = songNamesPosition - 1;
			if(songNamesPosition < 0){
				songNamesPosition = songNames.length - 1;
			}
			String next = songNames[songNamesPosition];
			song = new File(next);
			fis = new FileInputStream(song);
			mp.setDataSource(fis.getFD());
			mp.prepare();
			mp.start();
			TextView et = (TextView) findViewById(R.id.songName);
			et.setText(song.getName().replaceAll("\\d\\d\\s", "").replaceAll("(\\.mp3)|(\\.m4p)|(\\.m4a)", ""));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void playPause(){
		Button pause = (Button) findViewById(R.id.playPause);
		Log.i(TAG, "Play pause button = " + pause);
		if (mp.isPlaying()) {
			mp.pause();
			if(pause != null){
				pause.setText("Play");
			}
		} else {
			mp.start();
			if(pause != null){
				pause.setText("Pause");
			}
		}
	}
	
	private void play(){
		Button pause = (Button) findViewById(R.id.playPause);
		Log.i(TAG, "Play pause button = " + pause);
		if (mp.isPlaying()) {
			// do nothing
		} else {
			mp.start();
			if(pause != null){
				pause.setText("Pause");
			}
		}
	}
	
	private void pause(){
		Button pause = (Button) findViewById(R.id.playPause);
		Log.i(TAG, "Play pause button = " + pause);
		if (mp.isPlaying()) {
			mp.pause();
			if(pause != null){
				pause.setText("Play");
			}
		} else {
			// do nothing
		}
	}
	
	private void next(){
		mp.stop();
		mp.reset();
		try {
			fis.close();
			songNamesPosition = (songNamesPosition + 1)
					% songNames.length;
			String next = songNames[songNamesPosition];
			song = new File(next);
			fis = new FileInputStream(song);
			mp.setDataSource(fis.getFD());
			mp.prepare();
			mp.start();
			TextView et = (TextView) findViewById(R.id.songName);
			et.setText(song.getName().replaceAll("\\d\\d\\s", "").replaceAll("(\\.mp3)|(\\.m4p)|(\\.m4a)", ""));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private class NotAwfulAudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener{

	    public void onAudioFocusChange(int focusChange) {
	    	Log.w(TAG, "Focus change received " + focusChange);
	        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
	        	Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
	        	pause();
	            // Pause playback
	        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
	        	Log.i(TAG, "AUDIOFOCUS_GAIN");
	        	// It bugs the crap out of me when things just start playing on their own.
	        	// Don't start playing again till someone pushes a friggin' button.
//	        	play();
	            // Resume playback 
	        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
	            //am.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
	        	Log.i(TAG, "AUDIOFOCUS_LOSS");
	            am.abandonAudioFocus(this);
	            pause();
	            // Stop playback
	        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){
	        	Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
	        	pause();
	        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK){
	        	Log.i(TAG, "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK");
	        	play();
	        }
	    }
	}
}
