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

package com.smithdtyler.prettygoodmusicplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MusicPlaybackService extends Service {
	static final int MSG_REGISTER_CLIENT = 1;
	static final int MSG_UNREGISTER_CLIENT = 2;

	// Playback control
	static final int MSG_PLAYPAUSE = 3;
	static final int MSG_NEXT = 4;
	static final int MSG_PREVIOUS = 5;
	static final int MSG_SET_PLAYLIST = 6;
	static final int MSG_PAUSE = 7;
	static final int MSG_PAUSE_IN_ONE_SEC = 8;
	static final int MSG_CANCEL_PAUSE_IN_ONE_SEC = 9;
	static final int MSG_TOGGLE_SHUFFLE = 10;
	static final int MSG_SEEK_TO = 11;
	static final int MSG_JUMPBACK = 12;
	static final int MSG_PLAY = 13;

	// State management
	static final int MSG_REQUEST_STATE = 17;
	static final int MSG_SERVICE_STATUS = 18;
	static final int MSG_STOP_SERVICE = 19;

	public enum PlaybackState {
		PLAYING, PAUSED, UNKNOWN
	}

	static final String PRETTY_SONG_NAME = "PRETTY_SONG_NAME";
	static final String PRETTY_ARTIST_NAME = "PRETTY_ARTIST_NAME";
	static final String PRETTY_ALBUM_NAME = "PRETTY_ALBUM_NAME";
	static final String ALBUM_NAME = "ALBUM_NAME";
	static final String PLAYBACK_STATE = "PLAYBACK_STATE";
	static final String TRACK_DURATION = "TRACK_DURATION";
	static final String TRACK_POSITION = "TRACK_POSITION";
	static final String IS_SHUFFLING = "IS_SHUFFLING";

	private static final ComponentName cn = new ComponentName(
			MusicBroadcastReceiver.class.getPackage().getName(),
			MusicBroadcastReceiver.class.getName());

	private FileInputStream fis;
	private File songFile;
	private String[] songAbsoluteFileNames;
	private int songAbsoluteFileNamesPosition;

	private Timer timer;

	private AudioManager am;
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	private MediaPlayer mp;
	private static final String TAG = "MusicPlaybackService";
	private static boolean isRunning = false;

	private static int uniqueid = new String("Music Playback Service")
	.hashCode();

	private OnAudioFocusChangeListener audioFocusListener = new PrettyGoodAudioFocusChangeListener();

	private static IntentFilter filter = new IntentFilter();
	static {
		filter.addAction("android.intent.action.HEADSET_PLUG");
		filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
		filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
	}
	private static MusicBroadcastReceiver receiver = new MusicBroadcastReceiver();

	/**
	 * Keeps track of all current registered clients.
	 */
	List<Messenger> mClients = new ArrayList<Messenger>();

	final Messenger mMessenger = new Messenger(new IncomingHandler(this));

	public AudioManager mAudioManager;

	// These are used to report song progress when the song isn't started yet.
	private int lastDuration = 0;
	private int lastPosition = 0;
	public long audioFocusLossTime = 0;
	private long pauseTime = Long.MAX_VALUE;
	private boolean _shuffle = false;
	private List<Integer> shuffleFrontList = new ArrayList<Integer>();
	private Random random;
	private List<Integer> shuffleBackList = new ArrayList<Integer>();
	private String artist;
	private String artistAbsPath;
	private String album;
	private long lastResumeUpdateTime;
	private SharedPreferences sharedPref;
	private HeadphoneBroadcastReceiver headphoneReceiver;
	private PowerManager powerManager;
	WakeLock wakeLock;

	// Handler that receives messages from the thread
	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			Log.i(TAG, "ServiceHandler got a message!" + msg);
		}
	}

	@Override
	public synchronized void onCreate() {
		Log.i(TAG, "Music Playback Service Created!");
		isRunning = true;
		sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

		powerManager =(PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"PGMPWakeLock");

		random = new Random();

		mp = new MediaPlayer();

		mp.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				Log.i(TAG, "Song complete");
				next();
			}

		});

		// https://developer.android.com/training/managing-audio/audio-focus.html
		audioFocusListener = new PrettyGoodAudioFocusChangeListener();

		// Get permission to play audio
		am = (AudioManager) getBaseContext().getSystemService(
				Context.AUDIO_SERVICE);

		HandlerThread thread = new HandlerThread("ServiceStartArguments");
		thread.start();

		// Get the HandlerThread's Looper and use it for our Handler
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);

		// https://stackoverflow.com/questions/19474116/the-constructor-notification-is-deprecated
		// https://stackoverflow.com/questions/6406730/updating-an-ongoing-notification-quietly/15538209#15538209
		Intent resultIntent = new Intent(this, NowPlaying.class);
		resultIntent.putExtra("From_Notification", true);
		resultIntent.putExtra(AlbumList.ALBUM_NAME, album);
		resultIntent.putExtra(ArtistList.ARTIST_NAME, artist);
		resultIntent.putExtra(ArtistList.ARTIST_ABS_PATH_NAME, artistAbsPath);

		// Use the FLAG_ACTIVITY_CLEAR_TOP to prevent launching a second
		// NowPlaying if one already exists.
		resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				resultIntent, 0);

		Builder builder = new NotificationCompat.Builder(
				this.getApplicationContext());

		String contentText = getResources().getString(R.string.ticker_text);
		if (songFile != null) {
			contentText = Utils.getPrettySongName(songFile);
		}

		Notification notification = builder
				.setContentText(contentText)
				.setSmallIcon(R.drawable.ic_pgmp_launcher)
				.setWhen(System.currentTimeMillis())
				.setContentIntent(pendingIntent)
				.setContentTitle(
						getResources().getString(R.string.notification_title))
						.build();

		startForeground(uniqueid, notification);

		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				onTimerTick();
			}
		}, 0, 500L);

		Log.i(TAG, "Registering event receiver");
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		// Apparently audio registration is persistent across lots of things...
		// restarts, installs, etc.
		mAudioManager.registerMediaButtonEventReceiver(cn);
		// I tried to register this in the manifest, but it doesn't seen to
		// accept it, so I'll do it this way.
		getApplicationContext().registerReceiver(receiver, filter);

		headphoneReceiver = new HeadphoneBroadcastReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("android.intent.action.HEADSET_PLUG");
		registerReceiver(headphoneReceiver, filter);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("MyService", "Received start id " + startId + ": " + intent);
		if (intent == null) {
			// intent can be null if this is called by the OS due to
			// "START STICKY"
			// Start, but don't do anything until we get a message from the
			// user.
			return START_STICKY;
		}
		int command = intent.getIntExtra("Message", -1);
		if (command != -1) {
			Log.i(TAG, "I got a message! " + command);
			if (command == MSG_PLAYPAUSE) {
				Log.i(TAG, "I got a playpause message");
				playPause();
			} else if (command == MSG_PAUSE) {
				Log.i(TAG, "I got a pause message");
				pause();
			} else if (command == MSG_PLAY) {
				Log.i(TAG, "I got a play message");
				play();
			} else if (command == MSG_NEXT) {
				Log.i(TAG, "I got a next message");
				next();
			} else if (command == MSG_PREVIOUS) {
				Log.i(TAG, "I got a previous message");
				previous();
			} else if (command == MSG_JUMPBACK) {
				Log.i(TAG, "I got a jumpback message");
				jumpback();
			} else if (command == MSG_STOP_SERVICE) {
				Log.i(TAG, "I got a stop message");
				headphoneReceiver.active = false;
				timer.cancel();
				stopForeground(true);
				stopSelf();
			} else if (command == MSG_PAUSE_IN_ONE_SEC) {
				pauseTime = System.currentTimeMillis() + 1000;
			} else if (command == MSG_CANCEL_PAUSE_IN_ONE_SEC) {
				pauseTime = Long.MAX_VALUE;
			}
			return START_STICKY;
		}

		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		mServiceHandler.sendMessage(msg);
		// If we get killed, after returning from here, restart
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	// Receives messages from activities which want to control the jams
	private static class IncomingHandler extends Handler {
		MusicPlaybackService _service;

		private IncomingHandler(MusicPlaybackService service) {
			_service = service;
		}

		@Override
		public void handleMessage(Message msg) {
			Log.i(TAG, "Music Playback service got a message!");
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				Log.i(TAG, "Got MSG_REGISTER_CLIENT");
				synchronized (_service.mClients) {
					_service.mClients.add(msg.replyTo);
				}
				break;
			case MSG_UNREGISTER_CLIENT:
				Log.i(TAG, "Got MSG_UNREGISTER_CLIENT");
				synchronized (_service.mClients) {
					_service.mClients.remove(msg.replyTo);
				}
				break;
			case MSG_PLAYPAUSE:
				// if we got a playpause message, assume that the user can hear
				// what's happening and wants to switch it.
				Log.i(TAG, "Got a playpause message!");
				// Assume that we're not changing songs
				_service.playPause();
				break;
			case MSG_NEXT:
				Log.i(TAG, "Got a next message!");
				_service.next();
				break;
			case MSG_PREVIOUS:
				Log.i(TAG, "Got a previous message!");
				_service.previous();
				break;
			case MSG_JUMPBACK:
				Log.i(TAG, "Got a jump back message!");
				_service.jumpback();
				break;
			case MSG_TOGGLE_SHUFFLE:
				Log.i(TAG, "Got a toggle shuffle message!");
				_service.toggleShuffle();
				break;
			case MSG_SET_PLAYLIST:
				Log.i(TAG, "Got a set playlist message!");
				_service.songAbsoluteFileNames = msg.getData().getStringArray(
						SongList.SONG_ABS_FILE_NAME_LIST);
				_service.songAbsoluteFileNamesPosition = msg.getData().getInt(
						SongList.SONG_ABS_FILE_NAME_LIST_POSITION);
				_service.songFile = new File(
						_service.songAbsoluteFileNames[_service.songAbsoluteFileNamesPosition]);
				_service.artist = msg.getData().getString(ArtistList.ARTIST_NAME);
				_service.artistAbsPath = msg.getData().getString(ArtistList.ARTIST_ABS_PATH_NAME);
				_service.album = msg.getData().getString(AlbumList.ALBUM_NAME);
				int songPosition = msg.getData().getInt(TRACK_POSITION, 0);
				_service.startPlayingFile(songPosition);
				_service.updateNotification();
				_service.resetShuffle();
				break;
			case MSG_REQUEST_STATE:
				Log.i(TAG, "Got a state request message!");
				break;
			case MSG_SEEK_TO:
				Log.i(TAG, "Got a seek request message!");
				int progress = msg.getData().getInt(TRACK_POSITION);
				_service.jumpTo(progress);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private void onTimerTick() {
		long currentTime = System.currentTimeMillis();
		if (pauseTime < currentTime) {
			pause();
		}
		updateResumePosition();
		sendUpdateToClients();
	}

	private void updateResumePosition(){
		long currentTime = System.currentTimeMillis();
		if(currentTime - 10000 > lastResumeUpdateTime){
			if(mp != null && songFile != null && mp.isPlaying()){
				int pos = mp.getCurrentPosition();
				SharedPreferences prefs = getSharedPreferences("PrettyGoodMusicPlayer", MODE_PRIVATE);
				Log.i(TAG,
						"Preferences update success: "
								+ prefs.edit()
								.putString(songFile.getParentFile().getAbsolutePath(),songFile.getName() + "~" + pos)
								.commit());
			}
			lastResumeUpdateTime = currentTime;
		}
	}


	private void sendUpdateToClients() {
		List<Messenger> toRemove = new ArrayList<Messenger>();
		synchronized (mClients) {
			for (Messenger client : mClients) {
				Message msg = Message.obtain(null, MSG_SERVICE_STATUS);
				Bundle b = new Bundle();
				if (songFile != null) {
					b.putString(PRETTY_SONG_NAME,
							Utils.getPrettySongName(songFile));
					b.putString(PRETTY_ALBUM_NAME, songFile.getParentFile()
							.getName());
					b.putString(PRETTY_ARTIST_NAME, songFile.getParentFile()
							.getParentFile().getName());
				} else {
					// songFile can be null while we're shutting down.
					b.putString(PRETTY_SONG_NAME, " ");
					b.putString(PRETTY_ALBUM_NAME, " ");
					b.putString(PRETTY_ARTIST_NAME, " ");
				}

				b.putBoolean(IS_SHUFFLING, this._shuffle);

				if (mp.isPlaying()) {
					b.putInt(PLAYBACK_STATE, PlaybackState.PLAYING.ordinal());
				} else {
					b.putInt(PLAYBACK_STATE, PlaybackState.PAUSED.ordinal());
				}
				// We might not be able to send the position right away if mp is
				// still being created
				// so instead let's send the last position we knew about.
				if (mp.isPlaying()) {
					lastDuration = mp.getDuration();
					lastPosition = mp.getCurrentPosition();
				}
				b.putInt(TRACK_DURATION, lastDuration);
				b.putInt(TRACK_POSITION, lastPosition);
				msg.setData(b);
				try {
					client.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
					toRemove.add(client);
				}
			}

			for (Messenger remove : toRemove) {
				mClients.remove(remove);
			}
		}
	}

	public static boolean isRunning() {
		return isRunning;
	}

	@Override
	public synchronized void onDestroy() {
		super.onDestroy();
		unregisterReceiver(headphoneReceiver);
		am.abandonAudioFocus(MusicPlaybackService.this.audioFocusListener);
		mAudioManager.unregisterMediaButtonEventReceiver(cn);
		getApplicationContext().unregisterReceiver(receiver);
		mp.stop();
		mp.reset();
		mp.release();
		if (wakeLock.isHeld()) {
			wakeLock.release();
		}
		Log.i("MyService", "Service Stopped.");
		isRunning = false;
	}

	private synchronized void jumpback(){
		if (mp.isPlaying()) {
			int progressMillis = mp.getCurrentPosition();
			if (progressMillis <= 20000) {
				mp.seekTo(0);
			} else {
				mp.seekTo(progressMillis - 20000);
			}
			lastPosition = mp.getCurrentPosition();
		} else {
			// if we're paused but initialized, try to seek
			try{
				int progressMillis = mp.getCurrentPosition();
				if (progressMillis <= 20000) {
					mp.seekTo(0);
				} else {
					mp.seekTo(progressMillis - 20000);
				}
				lastPosition = mp.getCurrentPosition();
			} catch (Exception e){
				Log.w(TAG, "Unable to seek to position, file may not have been loaded");
			}
		}

	}

	private synchronized void previous() {
		// if we're playing, and we're more than 3 seconds into the file, then
		// just
		// start the song over
		if (mp.isPlaying()) {
			int progressMillis = mp.getCurrentPosition();
			if (progressMillis > 3000) {
				mp.seekTo(0);
				return;
			}
		}

		mp.stop();
		mp.reset();
		try {
			fis.close();
		} catch (IOException e) {
			Log.w(TAG, "Failed to close the file");
			e.printStackTrace();
		}
		songAbsoluteFileNamesPosition = songAbsoluteFileNamesPosition - 1;
		if (songAbsoluteFileNamesPosition < 0) {
			songAbsoluteFileNamesPosition = songAbsoluteFileNames.length - 1;
		}
		String next = songAbsoluteFileNames[songAbsoluteFileNamesPosition];
		try {
			songFile = new File(next);
			fis = new FileInputStream(songFile);
			mp.setDataSource(fis.getFD());
			mp.prepare();
			mp.start();
		} catch (IOException e) {
			Log.w(TAG, "Failed to open " + next);
			e.printStackTrace();
			// Just go to the next song back
			previous();
		}
		updateNotification();
	}

	private synchronized void startPlayingFile(int songProgress) {
		// Have we loaded a file yet?
		if (mp.getDuration() > 0) {
			pause();
			mp.stop();
			mp.reset();
		}

		// open the file, pass it into the mp
		try {
			fis = new FileInputStream(songFile);
			mp.setDataSource(fis.getFD());
			mp.prepare();
			if(songProgress > 0){
				mp.seekTo(songProgress);
			}
			wakeLock.acquire();
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

	private synchronized void jumpTo(int position){
		if(mp.isPlaying()){
			mp.seekTo(position);
		} else {
			// if we're paused but initialized, try to seek
			try{
				mp.seekTo(position);
				lastPosition = mp.getCurrentPosition();
			} catch (Exception e){
				Log.w(TAG, "Unable to seek to position, file may not have been loaded");
			}
		}
	}

	private synchronized void playPause() {
		if (mp.isPlaying()) {
			pause();
		} else {
			play();
		}
	}


	private synchronized void play() {
		pauseTime = Long.MAX_VALUE;
		if (mp.isPlaying()) {
			// do nothing
		} else {
			// Request audio focus for playback
			int result = am.requestAudioFocus(
					MusicPlaybackService.this.audioFocusListener,
					// Use the music stream.
					AudioManager.STREAM_MUSIC,
					// Request permanent focus.
					AudioManager.AUDIOFOCUS_GAIN);
			Log.d(TAG, "requestAudioFocus result = " + result);
			Log.i(TAG, "About to play " + songFile);

			if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
				Log.d(TAG, "We got audio focus!");
				mp.start();
				updateNotification();
				wakeLock.acquire();
			} else {
				Log.e(TAG, "Unable to get audio focus");
			}
		}
	}

	/**
	 * Pause the currently playing song.
	 */
	private synchronized void pause() {
		// Sometimes the call to isPlaying can throw an error "internal/external state mismatch corrected"
		// When this happens, I think the player moves itself to "paused" even though it's still playing.
		//if (mp.isPlaying()) {
		//	mp.pause();
		//} else {
		//	Log.w(TAG, "Odd condition - pause was called but the media player reported that it is already paused");
		try{
			// this is a hack, but it seems to be the most consistent way to address the problem
			// this forces the media player to check its current state before trying to pause.
			int position = mp.getCurrentPosition();
			mp.stop();
			mp.prepare();
			mp.seekTo(position);
			wakeLock.release();
		} catch (Exception e){
			Log.w(TAG, "Caught exception while trying to pause ", e);
		}
		//}
		updateNotification();
	}

	/**
	 *
	 */
	private void resetShuffle(){
		synchronized(shuffleFrontList){
			shuffleFrontList.clear();
			shuffleBackList.clear();
			for(int i = 0;i < songAbsoluteFileNames.length;i++){
				shuffleFrontList.add(i);
			}
		}
	}

	// Props to this fellow: https://stackoverflow.com/questions/5467174/how-to-implement-a-repeating-shuffle-thats-random-but-not-too-random
	private int grabNextShuffledPosition(){
		synchronized(shuffleFrontList){
			int threshold = (int) Math.ceil((songAbsoluteFileNames.length + 1) / 2);
			Log.d(TAG, "threshold: " + threshold);
			if(shuffleFrontList.size() < threshold){
				Log.d(TAG, "Shuffle queue is half empty, adding a new song...");
				shuffleFrontList.add(shuffleBackList.get(0));
				shuffleBackList.remove(0);
			}
			int rand = Math.abs(random.nextInt()) % shuffleFrontList.size();
			int loc = shuffleFrontList.get(rand);
			shuffleFrontList.remove(rand);
			shuffleBackList.add(loc);
			Log.i(TAG, "next position is: " + loc);
			String front = "";
			String back = "";
			for(int i : shuffleFrontList){
				front = front + "," + i;
			}
			for(int i : shuffleBackList){
				back = back + "," + i;
			}
			Log.i(TAG, "Front list = " + front);
			Log.i(TAG, "Back list = " + back);
			return loc;
		}
	}

	private synchronized void next() {
		mp.stop();
		mp.reset();
		try {
			fis.close();
		} catch (Exception e) {
			Log.w(TAG, "Failed to close the file");
			e.printStackTrace();
		}

		if(!this._shuffle){
			songAbsoluteFileNamesPosition = (songAbsoluteFileNamesPosition + 1)
					% songAbsoluteFileNames.length;
		} else {
			songAbsoluteFileNamesPosition = grabNextShuffledPosition();
		}
		String next = songAbsoluteFileNames[songAbsoluteFileNamesPosition];
		try {
			songFile = new File(next);
			fis = new FileInputStream(songFile);
			mp.setDataSource(fis.getFD());
			mp.prepare();
			mp.start();
		} catch (IOException e) {
			Log.w(TAG, "Failed to open " + next);
			e.printStackTrace();
			// I think our best chance is to go to the next song
			next();
		}
		updateNotification();
	}

	public void toggleShuffle() {
		this._shuffle = !this._shuffle ;
	}

	private void updateNotification() {
		boolean audiobookMode = sharedPref.getBoolean("pref_audiobook_mode", false);

		// https://stackoverflow.com/questions/5528288/how-do-i-update-the-notification-text-for-a-foreground-service-in-android
		Intent resultIntent = new Intent(this, NowPlaying.class);
		// Use the FLAG_ACTIVITY_CLEAR_TOP to prevent launching a second
		// NowPlaying if one already exists.
		resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		resultIntent.putExtra("From_Notification", true);
		resultIntent.putExtra(AlbumList.ALBUM_NAME, album);
		resultIntent.putExtra(ArtistList.ARTIST_NAME, artist);
		resultIntent.putExtra(ArtistList.ARTIST_ABS_PATH_NAME, artistAbsPath);

		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Builder builder = new NotificationCompat.Builder(
				this.getApplicationContext());
		int icon = R.drawable.ic_pgmp_launcher;
		String contentText = getResources().getString(R.string.ticker_text);
		if (songFile != null) {
			SharedPreferences prefs = getSharedPreferences(
					"PrettyGoodMusicPlayer", MODE_PRIVATE);
			prefs.edit();
			File bestGuessMusicDir = Utils.getBestGuessMusicDirectory();
			String musicRoot = prefs.getString("ARTIST_DIRECTORY",
					bestGuessMusicDir.getAbsolutePath());
			contentText = Utils.getArtistName(songFile, musicRoot) + ": "
					+ Utils.getPrettySongName(songFile);
			if (mp != null) {
				if (mp.isPlaying()) {
					icon = R.drawable.ic_pgmp_launcher;
				}
			}
		}

		Intent previousIntent = new Intent("Previous", null, this, MusicPlaybackService.class);
		previousIntent.putExtra("Message", MSG_PREVIOUS);
		PendingIntent previousPendingIntent = PendingIntent.getService(this, 0, previousIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Intent jumpBackIntent = new Intent("JumpBack", null, this, MusicPlaybackService.class);
		jumpBackIntent.putExtra("Message", MSG_JUMPBACK);
		PendingIntent jumpBackPendingIntent = PendingIntent.getService(this, 0, jumpBackIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Intent nextIntent = new Intent("Next", null, this, MusicPlaybackService.class);
		nextIntent.putExtra("Message", MSG_NEXT);
		PendingIntent nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		PendingIntent playPausePendingIntent;
		Intent playPauseIntent = new Intent("PlayPause", null, this, MusicPlaybackService.class);
		playPauseIntent.putExtra("Message", MSG_PLAYPAUSE);
		playPausePendingIntent = PendingIntent.getService(this, 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		int playPauseIcon;
		if(mp != null && mp.isPlaying()){
			playPauseIcon = R.drawable.ic_action_pause;
		} else {
			playPauseIcon = R.drawable.ic_action_play;
		}

		Notification notification;
		if(audiobookMode){
			notification = builder
					.setContentText(contentText)
					.setSmallIcon(icon)
					.setWhen(System.currentTimeMillis())
					.setContentIntent(pendingIntent)
					.setContentTitle(
							getResources().getString(R.string.notification_title))
							.addAction(R.drawable.ic_action_rewind20, "", jumpBackPendingIntent)
							.addAction(playPauseIcon, "", playPausePendingIntent)
							.addAction(R.drawable.ic_action_next, "", nextPendingIntent)
							.build();
		} else {
			notification = builder
					.setContentText(contentText)
					.setSmallIcon(icon)
					.setWhen(System.currentTimeMillis())
					.setContentIntent(pendingIntent)
					.setContentTitle(
							getResources().getString(R.string.notification_title))
							.addAction(R.drawable.ic_action_previous, "", previousPendingIntent)
							.addAction(playPauseIcon, "", playPausePendingIntent)
							.addAction(R.drawable.ic_action_next, "", nextPendingIntent)
							.build();
		}

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(uniqueid, notification);
	}

	private class PrettyGoodAudioFocusChangeListener implements
	AudioManager.OnAudioFocusChangeListener {

		private PlaybackState stateOnFocusLoss = PlaybackState.UNKNOWN;

		public void onAudioFocusChange(int focusChange) {
			Log.w(TAG, "Focus change received " + focusChange);
			if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
				Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
				if (mp.isPlaying()) {
					stateOnFocusLoss = PlaybackState.PLAYING;
				} else {
					stateOnFocusLoss = PlaybackState.PAUSED;
				}
				pause();
				MusicPlaybackService.this.audioFocusLossTime = System
						.currentTimeMillis();
				// Pause playback
			} else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
				Log.i(TAG, "AUDIOFOCUS_GAIN");
				// If it's been less than 20 seconds, resume playback
				long curr = System.currentTimeMillis();
				if (((curr - MusicPlaybackService.this.audioFocusLossTime) < 30000)
						&& stateOnFocusLoss == PlaybackState.PLAYING) {
					play();
				} else {
					Log.i(TAG,
							"It's been more than 30 seconds or we were paused, don't auto-play");
				}
			} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
				Log.i(TAG, "AUDIOFOCUS_LOSS");
				if (mp.isPlaying()) {
					stateOnFocusLoss = PlaybackState.PLAYING;
				} else {
					stateOnFocusLoss = PlaybackState.PAUSED;
				}
				pause();
				MusicPlaybackService.this.audioFocusLossTime = System
						.currentTimeMillis();
				// Stop playback
			} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
				Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
				MusicPlaybackService.this.audioFocusLossTime = System
						.currentTimeMillis();
				if (mp.isPlaying()) {
					stateOnFocusLoss = PlaybackState.PLAYING;
				} else {
					stateOnFocusLoss = PlaybackState.PAUSED;
				}
				pause();
			} else if (focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) {
				Log.i(TAG, "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK");
				long curr = System.currentTimeMillis();
				if (((curr - MusicPlaybackService.this.audioFocusLossTime) < 30000)
						&& stateOnFocusLoss == PlaybackState.PLAYING) {
					play();
				} else {
					Log.i(TAG,
							"It's been more than 30 seconds or we were paused, don't auto-play");
				}
			}
		}
	}

	private static class HeadphoneBroadcastReceiver extends BroadcastReceiver{

		/**
		 * If the option to automatically resume on headphone re-connect is selected, 
		 * keep track of the time they were unplugged.
		 */
		private long resumeOnQuickReconnectDisconnectTime = 0;

		/**
		 * There seems to be a race condition that causes headphone events to get fired while the service is shutting down
		 * use this flag to indicate that events should be ignored.
		 */ 
		private boolean active = true;

		@Override
		public void onReceive(Context context, Intent intent) {
			if(!active){
				return;
			}
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
			if (Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
				Log.i(TAG, "Got headset plug action");
				String disconnectBehavior = sharedPref.getString("pref_disconnect_behavior", context.getString(R.string.pause_after_one_sec));
				/*
				 * state - 0 for unplugged, 1 for plugged. name - Headset type,
				 * human readable string microphone - 1 if headset has a microphone,
				 * 0 otherwise
				 */
				if(disconnectBehavior.equals(context.getString(R.string.resume_on_quick_reconnect))){
					if (intent.getIntExtra("state", -1) == 0) {
						Log.i(TAG, "headphones disconnected, pausing");
						Intent msgIntent = new Intent(context, MusicPlaybackService.class);
						msgIntent.putExtra("Message", MusicPlaybackService.MSG_PAUSE);
						context.startService(msgIntent);
						resumeOnQuickReconnectDisconnectTime = System.currentTimeMillis();
					} else if (intent.getIntExtra("state", -1) == 1) {
						long currentTime = System.currentTimeMillis();
						if(currentTime - resumeOnQuickReconnectDisconnectTime < 1000){
							// Resume
							Log.i(TAG, "headphones plugged back in within 1000ms, resuming");
							Intent msgIntent = new Intent(context, MusicPlaybackService.class);
							msgIntent.putExtra("Message", MusicPlaybackService.MSG_PLAY);
							context.startService(msgIntent);
						}
					}
				} else if(disconnectBehavior.equals(context.getString(R.string.resume_on_reconnect))){
					if (intent.getIntExtra("state", -1) == 0) {
						Log.i(TAG, "headphones disconnected, pausing");
						Intent msgIntent = new Intent(context, MusicPlaybackService.class);
						msgIntent.putExtra("Message", MusicPlaybackService.MSG_PAUSE);
						context.startService(msgIntent);
						resumeOnQuickReconnectDisconnectTime = System.currentTimeMillis();
					} else if (intent.getIntExtra("state", -1) == 1) {
						// check to make sure we were playing at one point.
						if(resumeOnQuickReconnectDisconnectTime > 0){
							// Resume
							Log.i(TAG, "headphones plugged back in, resuming");
							Intent msgIntent = new Intent(context, MusicPlaybackService.class);
							msgIntent.putExtra("Message", MusicPlaybackService.MSG_PLAY);
							context.startService(msgIntent);
						}
					}
				} else if(disconnectBehavior.equals(context.getString(R.string.pause_after_one_sec))){
					if (intent.getIntExtra("state", -1) == 0) {
						Log.i(TAG, "headphones disconnected, pausing in 1 seconds");
						Intent msgIntent = new Intent(context, MusicPlaybackService.class);
						msgIntent.putExtra("Message", MusicPlaybackService.MSG_PAUSE_IN_ONE_SEC);
						context.startService(msgIntent);
						// If the headphone is plugged back in quickly after being
						// unplugged, keep playing
					} else if (intent.getIntExtra("state", -1) == 1) {
						Log.i(TAG, "headphones plugged back in, cancelling disconnect");
						Intent msgIntent = new Intent(context, MusicPlaybackService.class);
						msgIntent.putExtra("Message", MusicPlaybackService.MSG_CANCEL_PAUSE_IN_ONE_SEC);
						context.startService(msgIntent);
					}
				} else {
					// Pause immediately
					Log.i(TAG, "headphones disconnected, pausing");
					Intent msgIntent = new Intent(context, MusicPlaybackService.class);
					msgIntent.putExtra("Message", MusicPlaybackService.MSG_PAUSE);
					context.startService(msgIntent);
				}
			} 
		}
	}

}
