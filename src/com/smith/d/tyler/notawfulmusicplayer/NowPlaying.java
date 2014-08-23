/**
   The Pretty Good Music Player
   Copyright (C) 2014  Tyler Smith
 
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.smith.d.tyler.notawfulmusicplayer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.smith.d.tyler.notawfulmusicplayer.MusicPlaybackService.PlaybackState;

public class NowPlaying extends Activity {
	
	private static final String TAG = "Now Playing";
	static final String KICKOFF_SONG = "KICKOFF_SONG";
	
	// State information
	private String desiredArtistName;
	private String desiredAlbumName;
	private String[] desiredSongAbsFileNames;
	private int desiredAbsSongFileNamesPosition;

	// Messaging and service stuff
	boolean mIsBound;
	private Messenger mService;
	final Messenger mMessenger = new Messenger(new IncomingHandler());
	private ServiceConnection mConnection = new NowPlayingServiceConnection();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_now_playing);
		
		doBindService();

		// Get the message from the intent
		Intent intent = getIntent();
		if(intent.getBooleanExtra(KICKOFF_SONG, false)){
			desiredArtistName = intent.getStringExtra(ArtistList.ARTIST_NAME);
			desiredAlbumName = intent.getStringExtra(AlbumList.ALBUM_NAME);
			desiredSongAbsFileNames = intent.getStringArrayExtra(SongList.SONG_ABS_FILE_NAME_LIST);
			desiredAbsSongFileNamesPosition = intent.getIntExtra(SongList.SONG_ABS_FILE_NAME_LIST_POSITION, 0);

			Log.d(TAG, "Got song names " + desiredSongAbsFileNames + " position "
					+ desiredAbsSongFileNamesPosition);
			
			TextView et = (TextView) findViewById(R.id.artistName);
			et.setText(desiredArtistName);
	
			et = (TextView) findViewById(R.id.albumName);
			et.setText(desiredAlbumName);
		}
		
		// The song name field will be set when we get our first update update from the service.

		final Button pause = (Button) findViewById(R.id.playPause);
		pause.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				playPause();
			}

		});

		Button previous = (Button) findViewById(R.id.previous);
		previous.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				previous();
			}

		});

		Button next = (Button) findViewById(R.id.next);
		next.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				next();
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mConnection);
	}
	
	// Playback control methods
	private void playPause(){
		Log.d(TAG, "Play/Pause clicked...");
        Message msg = Message.obtain(null, MusicPlaybackService.MSG_PLAYPAUSE);
        try {
        	Log.i(TAG, "Sending a request to start playing!");
			mService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	private void next(){
		Log.d(TAG, "next...");
        Message msg = Message.obtain(null, MusicPlaybackService.MSG_NEXT);
        try {
        	Log.i(TAG, "SEnding a request to go to next!");
			mService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public void previous(){
		Log.d(TAG, "Previous clicked...");
        Message msg = Message.obtain(null, MusicPlaybackService.MSG_PREVIOUS);
        try {
        	Log.i(TAG, "SEnding a request to go to previous!");
			mService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	// Service connection management
	private class NowPlayingServiceConnection implements ServiceConnection {

		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			
			// Register with the service
			try {
				Message msg = Message.obtain(null,
						MusicPlaybackService.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				// In this case the service has crashed before we could even do
				// anything with it
			}
			
			if(desiredSongAbsFileNames != null){
				// set the playlist
				Message msg = Message.obtain(null, MusicPlaybackService.MSG_SET_PLAYLIST);
				msg.getData().putStringArray(SongList.SONG_ABS_FILE_NAME_LIST, desiredSongAbsFileNames);
				msg.getData().putInt(SongList.SONG_ABS_FILE_NAME_LIST_POSITION, desiredAbsSongFileNamesPosition);
				try {
					Log.i(TAG, "Sending a playlist!");
					mService.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				
				// start playing!
				msg = Message.obtain(null, MusicPlaybackService.MSG_PLAYPAUSE);
				try {
					Log.i(TAG, "Sending a play command!");
					mService.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected - process crashed.
			mService = null; // TODO need to do some null checks
		}
	};
	
	private class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			// TODO should minimize GUI access here as much as possible.
			switch (msg.what) {
			case MusicPlaybackService.MSG_SERVICE_STATUS:
				String currentSongName = msg.getData().getString(MusicPlaybackService.PRETTY_SONG_NAME);
				TextView tv = (TextView) findViewById(R.id.songName);
				if(!tv.getText().equals(currentSongName)){
					tv.setText(currentSongName);
				}
				
				String currentAlbumName = msg.getData().getString(MusicPlaybackService.PRETTY_ALBUM_NAME);
				tv = (TextView) findViewById(R.id.albumName);
				if(!tv.getText().equals(currentAlbumName)){
					tv.setText(currentAlbumName);
				}
				
				String currentArtistName = msg.getData().getString(MusicPlaybackService.PRETTY_ARTIST_NAME);
				tv = (TextView) findViewById(R.id.artistName);
				if(!tv.getText().equals(currentArtistName)){
					tv.setText(currentArtistName);
				}
				
				PlaybackState state = PlaybackState.values()[msg.getData().getInt(MusicPlaybackService.PLAYBACK_STATE, 0)];
				Button playPause = (Button)findViewById(R.id.playPause);
				if(playPause.getText().equals("Play")){
					if(state == PlaybackState.PLAYING){
						playPause.setText("Pause");
					}
				} else {
					if(state == PlaybackState.PAUSED){
						playPause.setText("Play");
					}
				}
				int duration = msg.getData().getInt(MusicPlaybackService.TRACK_DURATION, -1);
				int position = msg.getData().getInt(MusicPlaybackService.TRACK_POSITION, -1);
				if(duration > 0){
					SeekBar seekBar = (SeekBar)findViewById(R.id.seekBar1);
					seekBar.setMax(duration);
					seekBar.setProgress(position);
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	// Service Management Methods
	void doBindService() {
		Log.i(TAG, "Binding to the service!");
		bindService(new Intent(this, MusicPlaybackService.class), mConnection,
				Context.BIND_IMPORTANT | Context.BIND_AUTO_CREATE);
		mIsBound = true;
		// Need to start the service so it won't be stopped when this activity is destroyed.
		// https://developer.android.com/guide/components/bound-services.html
		startService(new Intent(this, MusicPlaybackService.class));
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
		}
	}


}
