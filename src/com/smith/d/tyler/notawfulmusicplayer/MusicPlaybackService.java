/**
 * 
 */
package com.smith.d.tyler.notawfulmusicplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

/**
 * TODO make foreground
 */
public class MusicPlaybackService extends Service {

	// Idea - let's set up a timer task 
	
	private FileInputStream fis;
	private File songFile;
	private String[] songAbsoluteFileNames;
	private int songAbsoluteFileNamesPosition;

	private AudioManager am;
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	private MediaPlayer mp;
	private static final String TAG = "MusicPlaybackService";
	private NotificationManager nm;
	private static boolean isRunning = false;
	
	public AudioManager mAudioManager;
	public ComponentName mComponentName;

	private OnAudioFocusChangeListener audioFocusListener = new NotAwfulAudioFocusChangeListener();

	List<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all
															// current
															// registered
															// clients.
	int mValue = 0; // Holds last value set by a client.
	static final int MSG_REGISTER_CLIENT = 1;
	static final int MSG_UNREGISTER_CLIENT = 2;

	// Playback control
	static final int MSG_PLAYPAUSE = 3;
	static final int MSG_NEXT = 4;
	static final int MSG_PREVIOUS = 5;
	static final int MSG_SET_PLAYLIST = 6;
	
	// State management
	static final int MSG_REQUEST_STATE = 7;

	final Messenger mMessenger = new Messenger(new IncomingHandler());
	// Target we publish for clients to send messages to IncomingHandler.
	private int unique;
	private BroadcastReceiver receiver;

	// Handler that receives messages from the thread
	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			Log.i(TAG, "Got message!");
			// Normally we would do some work here, like download a file.
			// For our sample, we just sleep for 5 seconds.
			long endTime = System.currentTimeMillis() + 5 * 1000;
			while (System.currentTimeMillis() < endTime) {
				synchronized (this) {
					try {
						wait(endTime - System.currentTimeMillis());
					} catch (Exception e) {
					}
				}
			}
			// Stop the service using the startId, so that we don't stop
			// the service in the middle of handling another job
			// TODO it's not entirely clear to me why we do this.
			// stopSelf(msg.arg1);
		}
	}

	@Override
	public void onCreate() {
		Log.i(TAG, "Music Playback Service Created!");
		showNotification();
		// timer.scheduleAtFixedRate(new TimerTask(){ public void run()
		// {onTimerTick();}}, 0, 100L);
		isRunning = true;
		// Start up the thread running the service. Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block. We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		mp = new MediaPlayer();

		mp.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				Log.i(TAG, "Song complete");
				next();
			}

		});

		// https://developer.android.com/training/managing-audio/audio-focus.html
		audioFocusListener = new NotAwfulAudioFocusChangeListener();

		// Get permission to play audio
		am = (AudioManager) getBaseContext().getSystemService(
				Context.AUDIO_SERVICE);

		HandlerThread thread = new HandlerThread("ServiceStartArguments");
		thread.start();

		// Get the HandlerThread's Looper and use it for our Handler
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);

		// I think we have to do this via manifest
		IntentFilter filter = new IntentFilter();
		filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
		filter.addAction("android.intent.action.MEDIA_BUTTON");
		
		receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            	Log.i(TAG, "Received a thing!");
                String action = intent.getAction();
                if(action.equals("android.intent.action.MEDIA_BUTTON")){
                    Log.e("test", "ok");
                }
            }
        };
        registerReceiver(receiver, filter);
        
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mComponentName = new ComponentName(getPackageName(), MusicBroadcastReceiver.class.getName());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
		Log.i("MyService", "Received start id " + startId + ": " + intent);
		int command = intent.getIntExtra("Message", -1);
		if(command != -1){
			Log.i(TAG, "I got a message! " + command);
			if(command == MSG_PLAYPAUSE){
				Log.i(TAG, "I got a playpause message");
				playPause();
			} else if(command == MSG_NEXT){
				Log.i(TAG, "I got a next message");
				next();
			} else if(command == MSG_PREVIOUS){
				Log.i(TAG, "I got a previous message");
				previous();
			}
			return START_STICKY;
		}
		Log.d(TAG, "registerMediaButtonReceiver()");
        mAudioManager.registerMediaButtonEventReceiver(mComponentName);
		
		// For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the
		// job
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		mServiceHandler.sendMessage(msg);
		// If we get killed, after returning from here, restart
		return START_STICKY;
		// TODO make this a foreground service.
		
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	// Receives messages from activities which want to control the jams
	class IncomingHandler extends Handler { // Handler of incoming messages from
											// clients.
		private String songName;

		@Override
		public void handleMessage(Message msg) {
			Log.i(TAG, "Music Playback service got a message!");
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				Log.i(TAG, "Got MSG_REGISTER_CLIENT");
				mClients.add(msg.replyTo);
				break;
			case MSG_UNREGISTER_CLIENT:
				Log.i(TAG, "Got MSG_UNREGISTER_CLIENT");
				mClients.remove(msg.replyTo);
				break;
			case MSG_PLAYPAUSE:
				// if we got a playpause message, assume that the user can hear
				// what's happening and wants to switch it.
				Log.i(TAG, "Got a playpause message!");
				// Assume that we're not changing songs
				playPause();
				break;
			case MSG_NEXT:
				Log.i(TAG, "Got a next message!");
				next();
				break;
			case MSG_PREVIOUS:
				Log.i(TAG, "Got a previous message!");
				previous();
				break;
			case MSG_SET_PLAYLIST:
				Log.i(TAG, "Got a set playlist message!");
				songAbsoluteFileNames = msg.getData().getStringArray(
						SongList.SONG_LIST);
				songAbsoluteFileNamesPosition = msg.getData().getInt(
						SongList.SONG_LIST_POSITION);
				songFile = new File(
						songAbsoluteFileNames[songAbsoluteFileNamesPosition]);
				songName = songFile.getName().replaceAll("\\d\\d\\s", "")
						.replaceAll("(\\.mp3)|(\\.m4p)|(\\.m4a)", "");
				Log.i(TAG, "The song is " + songName);
				setPlaylist();
				break;
			case MSG_REQUEST_STATE:
				Log.i(TAG, "Got a state request message!");
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private void sendMessageToUI(int intvaluetosend) {
		// for (int i = mClients.size() - 1; i >= 0; i--) {
		// try {
		// Send data as an Integer
		// mClients.get(i).send(
		// Message.obtain(null, MSG_SET_INT_VALUE, intvaluetosend,
		// 0));
		//
		// // Send data as a String
		// Bundle b = new Bundle();
		// b.putString("str1", "ab" + intvaluetosend + "cd");
		// Message msg = Message.obtain(null, MSG_SET_STRING_VALUE);
		// msg.setData(b);
		// mClients.get(i).send(msg);

		// } catch (RemoteException e) {
		// // The client is dead. Remove it from the list; we are going
		// // through the list from back to front so this is safe to do
		// // inside the loop.
		// mClients.remove(i);
		// }
		// }
	}

	private void showNotification() {
		// TODO showing a notification requires more than just garbage text,
		// apparently.
		// nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// // In this sample, we'll use the same text for the ticker and the
		// // expanded notification
		// CharSequence text = "Service started yo";
		// // Set the icon, scrolling text and timestamp
		// Notification notification = new Notification(R.drawable.icon, text,
		// System.currentTimeMillis());
		// // The PendingIntent to launch our activity if the user selects this
		// // notification
		// // PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
		// new
		// // Intent(this, MainActivity.class), 0);
		// // Set the info for the views that show in the notification panel.
		// // notification.setLatestEventInfo(this,
		// // getText(R.string.service_label), text, contentIntent);
		// // Send the notification.
		// // We use a layout id because it is a unique number. We use it later
		// to
		// // cancel.
		// unique = (int) System.currentTimeMillis();
		// nm.notify("started", unique, notification);
	}

	public static boolean isRunning() {
		return isRunning;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// nm.cancel(R.string.service_started); // Cancel the persistent
		// notification.
		nm.cancel(unique);
		Log.i("MyService", "Service Stopped.");
		isRunning = false;
	}

	private void previous() {
		mp.stop();
		mp.reset();
		try {
			fis.close();
			songAbsoluteFileNamesPosition = songAbsoluteFileNamesPosition - 1;
			if (songAbsoluteFileNamesPosition < 0) {
				songAbsoluteFileNamesPosition = songAbsoluteFileNames.length - 1;
			}
			String next = songAbsoluteFileNames[songAbsoluteFileNamesPosition];
			songFile = new File(next);
			fis = new FileInputStream(songFile);
			mp.setDataSource(fis.getFD());
			mp.prepare();
			mp.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setPlaylist() {
		if (mp.isPlaying()) {
			pause();
			mp.stop();
			mp.reset();
		}

		// open the file, pass it into the mp
		try {
			fis = new FileInputStream(songFile);
			mp.setDataSource(fis.getFD());
			mp.prepare();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void playPause() {
		if (mp.isPlaying()) {
			mp.pause();
		} else {
			play();
		}
	}

	private void play() {
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
				// am.registerMediaButtonEventReceiver(receiver); // TODO do I
				// need
				// this?
				mp.start();
			} else {
				Log.e(TAG, "Unable to get audio focus");
			}
		}
	}

	private void pause() {
		if (mp.isPlaying()) {
			mp.pause();
		} else {
			// do nothing
		}
	}

	// TODO synchronize on these functions to avoid starting two songs at once.
	private void next() {
		mp.stop();
		mp.reset();
		try {
			fis.close();
			songAbsoluteFileNamesPosition = (songAbsoluteFileNamesPosition + 1)
					% songAbsoluteFileNames.length;
			String next = songAbsoluteFileNames[songAbsoluteFileNamesPosition];
			songFile = new File(next);
			fis = new FileInputStream(songFile);
			mp.setDataSource(fis.getFD());
			mp.prepare();
			mp.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private class NotAwfulAudioFocusChangeListener implements
			AudioManager.OnAudioFocusChangeListener {

		public void onAudioFocusChange(int focusChange) {
			Log.w(TAG, "Focus change received " + focusChange);
			if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
				Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
				pause();
				// Pause playback
			} else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
				Log.i(TAG, "AUDIOFOCUS_GAIN");
				// It bugs the crap out of me when things just start playing on
				// their own.
				// Don't start playing again till someone pushes a friggin'
				// button.
				// play();
				// Resume playback
			} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
				// am.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
				Log.i(TAG, "AUDIOFOCUS_LOSS");
				am.abandonAudioFocus(this);
				pause();
				// Stop playback
			} else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
				Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
				pause();
			} else if (focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) {
				Log.i(TAG, "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK");
				play();
			}
		}
	}
	
	public static class MusicPlaybackReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "received a broadcast: " + intent);
			// do something based on the intent's action
		}
	}

}
