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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("DefaultLocale") public class ArtistList extends Activity {
	private static final String TAG = "Artist List";
	public static final String ARTIST_NAME = "ARTIST_NAME";
	public static final String ARTISTS_DIR = "ARTIST_DIRECTORY";
	public static final String ARTIST_ABS_PATH_NAME = "ARTIST_PATH";
	
	private static final String PICK_DIR_TEXT = "Click to configure...";

	private List<Map<String,String>> artists;
	private SimpleAdapter simpleAdpt;
	private String baseDir;
	private Object currentTheme;
	private String currentSize;
	private BroadcastReceiver exitReceiver;
	
	private void populateArtists(String baseDir){
		artists = new ArrayList<Map<String,String>>();
		File f = new File(baseDir);
		if(!f.exists() || !f.isDirectory()){
			Log.e(TAG, "Storage directory " + f + " does not exist!");
			return;
		}
		
		List<String> artistDirs = new ArrayList<String>();
		for(File dir : f.listFiles()){
			if(Utils.isValidArtistDirectory(dir)){
				artistDirs.add(dir.getName());
			}
		}
		
		Collections.sort(artistDirs, new Comparator<String>(){

			@Override
			public int compare(String arg0, String arg1) {
		        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ArtistList.this);
		        boolean ignoreleadingthe = sharedPref.getBoolean("ignore_leading_the_in_artist", false);
				if(ignoreleadingthe){
					if(arg0.toLowerCase().startsWith("the ")){
						arg0 = arg0.substring(4);
					}
					if(arg1.toLowerCase().startsWith("the ")){
						arg1 = arg1.substring(4);
					}
				}
		        return(arg0.toUpperCase().compareTo(arg1.toUpperCase()));
			}
			
		});
		
		for(String artist : artistDirs){
			Log.v(TAG, "Adding artist " + artist);
			// listview requires a map
			Map<String,String> map = new HashMap<String, String>();
			map.put("artist", artist);			
			artists.add(map);
		}
		
		if(!f.exists() || artistDirs.isEmpty()){
			Map<String, String> map = new HashMap<String, String>();
			map.put("artist", PICK_DIR_TEXT);
			artists.add(map);
		}
	}
	
    @Override
	protected void onResume() {
		super.onResume();
        SharedPreferences prefs = getSharedPreferences("PrettyGoodMusicPlayer", MODE_PRIVATE);
        prefs.edit();
        File bestGuessMusicDir = Utils.getBestGuessMusicDirectory();
        String prefDir = prefs.getString("ARTIST_DIRECTORY", bestGuessMusicDir.getAbsolutePath());
        ListView lv = (ListView) findViewById(R.id.artistListView);
        if(!prefDir.equals(baseDir)){
        	baseDir = prefDir;
        	populateArtists(baseDir);
            
            simpleAdpt = new SimpleAdapter(this, artists,  R.layout.pgmp_list_item, new String[] {"artist"}, new int[] {R.id.PGMPListItemText});
            lv.setAdapter(simpleAdpt);
        }
        
        int top = prefs.getInt("ARTIST_LIST_TOP", Integer.MIN_VALUE);
        int index = prefs.getInt("ARTIST_LIST_INDEX", Integer.MIN_VALUE);
        if(top > Integer.MIN_VALUE && index > Integer.MIN_VALUE){
        	Log.i(TAG, "Setting position from saved preferences");
        	lv.setSelectionFromTop(index, top);
        } else {
        	Log.i(TAG, "No saved position found");
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = sharedPref.getString("pref_theme", getString(R.string.light));
        String size = sharedPref.getString("pref_text_size", getString(R.string.medium));
        Log.i(TAG, "got configured theme " + theme);
        Log.i(TAG, "Got configured size " + size);
        if(currentTheme == null){
        	currentTheme = theme;
        } 
        
        if(currentSize == null){
        	currentSize = size;
        }
        if(!currentTheme.equals(theme) || !currentSize.equals(size)){
        	finish();
        	startActivity(getIntent());
        }
        
	}

	@Override
	protected void onPause() {
		super.onPause();
		// save index and top position
        SharedPreferences prefs = getSharedPreferences("PrettyGoodMusicPlayer", MODE_PRIVATE);
		ListView lv = (ListView) findViewById(R.id.artistListView);
		int index = lv.getFirstVisiblePosition();
		View v = lv.getChildAt(0);
		int top = (v == null) ? 0 : v.getTop();
		prefs.edit().putInt("ARTIST_LIST_TOP", top).putInt("ARTIST_LIST_INDEX",index).commit();
		Log.i(TAG, "Saving position top " + top + " index " + index);
	}

	@Override
	protected void onStart() {
		super.onStart();
        SharedPreferences prefs = getSharedPreferences("PrettyGoodMusicPlayer", MODE_PRIVATE);
        Log.i(TAG, "Preferences " + prefs + " " + ((Object)prefs));
        baseDir = prefs.getString("ARTIST_DIRECTORY", new File(Environment.getExternalStorageDirectory(), "Music").getAbsolutePath());
        Log.d(TAG, "Got configured base directory of " + baseDir);

        populateArtists(baseDir);
        
        simpleAdpt = new SimpleAdapter(this, artists, R.layout.pgmp_list_item, new String[] {"artist"}, new int[] {R.id.PGMPListItemText});
        ListView lv = (ListView) findViewById(R.id.artistListView);
        lv.setAdapter(simpleAdpt);
    }

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.pretty_good_preferences, false);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = sharedPref.getString("pref_theme", getString(R.string.light));
        String size = sharedPref.getString("pref_text_size", getString(R.string.medium));
        Log.i(TAG, "got configured theme " + theme);
        Log.i(TAG, "got configured size " + size);
        currentTheme = theme;
        currentSize = size;
        // These settings were fixed in english for a while, so check for old style settings as well as language specific ones.
        if(theme.equalsIgnoreCase(getString(R.string.dark)) || theme.equalsIgnoreCase("dark")){
        	Log.i(TAG, "setting theme to " + theme);
        	if(size.equalsIgnoreCase(getString(R.string.small)) || size.equalsIgnoreCase("small")){
        		setTheme(R.style.PGMPDarkSmall);
        	} else if (size.equalsIgnoreCase(getString(R.string.medium)) || size.equalsIgnoreCase("medium")){
        		setTheme(R.style.PGMPDarkMedium);
        	} else {
        		setTheme(R.style.PGMPDarkLarge);
        	}
        } else if (theme.equalsIgnoreCase(getString(R.string.light)) || theme.equalsIgnoreCase("light")){
        	Log.i(TAG, "setting theme to " + theme);
        	if(size.equalsIgnoreCase(getString(R.string.small)) || size.equalsIgnoreCase("small")){
        		setTheme(R.style.PGMPLightSmall);
        	} else if (size.equalsIgnoreCase(getString(R.string.medium)) || size.equalsIgnoreCase("medium")){
        		setTheme(R.style.PGMPLightMedium);
        	} else {
        		setTheme(R.style.PGMPLightLarge);
        	}
        }
        setContentView(R.layout.activity_artist_list);
        
        ListView lv = (ListView) findViewById(R.id.artistListView);

        // React to user clicks on item
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

             public void onItemClick(AdapterView<?> parentAdapter, View view, int position,
                                     long id) {
            	 // TODO just use the position instead
            	 TextView clickedView = (TextView) view.findViewById(R.id.PGMPListItemText);
            	 // Apparently sometimes clickedview returns the listview, other times it returns the text view
            	 if(clickedView == null){
            		 if(view instanceof TextView){
            			 clickedView = (TextView)view;
            		 } else{
            			 Log.w(TAG, "Got null clicked view");
            			 return;
            		 }
            	 }
            	 if(!clickedView.getText().equals(PICK_DIR_TEXT)){
	            	 Intent intent = new Intent(ArtistList.this, AlbumList.class);
	            	 intent.putExtra(ARTIST_NAME, clickedView.getText());
	            	 intent.putExtra(ARTIST_ABS_PATH_NAME, baseDir + File.separator + clickedView.getText());
	            	 startActivity(intent);
            	 } else {
            		 Intent intent = new Intent(ArtistList.this, SettingsActivity.class);
            		 startActivity(intent);
            	 }
             }
        });
        
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.smithdtyler.ACTION_EXIT");
        exitReceiver = new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				Log.i(TAG, "Received exit request, shutting down...");
				Intent msgIntent = new Intent(getBaseContext(), MusicPlaybackService.class);
				msgIntent.putExtra("Message", MusicPlaybackService.MSG_STOP_SERVICE);
				startService(msgIntent);
				finish();
			}
        	
        };
        registerReceiver(exitReceiver, intentFilter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.artist_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
		if (id == R.id.action_now_playing) {
			if (MusicPlaybackService.isRunning()) {
				Intent intent = new Intent(ArtistList.this, NowPlaying.class);
				intent.putExtra("From_Notification", true);
				startActivity(intent);
			} else {
				Toast.makeText(ArtistList.this, R.string.nothing_playing, Toast.LENGTH_SHORT).show();
			}
			return true;
		}
        if (id == R.id.action_settings) {
        	Intent intent = new Intent(ArtistList.this, SettingsActivity.class);
        	startActivity(intent);
            return true;
        }
        if (id == R.id.action_exit) {
			Intent msgIntent = new Intent(getBaseContext(), MusicPlaybackService.class);
			msgIntent.putExtra("Message", MusicPlaybackService.MSG_STOP_SERVICE);
			startService(msgIntent);
			Intent startMain = new Intent(Intent.ACTION_MAIN);
		    startMain.addCategory(Intent.CATEGORY_HOME);
		    startMain.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		    startActivity(startMain);
			finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
	protected void onDestroy() {
    	unregisterReceiver(exitReceiver);
    	super.onDestroy();
	}

	// If the back key is pressed, ask if they really want to quit
    // if they do, pass the key press along. If they don't,
    // eat it.
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	Log.i(TAG, " handling on key down");
        switch(keyCode)
        {
        case KeyEvent.KEYCODE_BACK:
            AlertDialog.Builder ab = new AlertDialog.Builder(ArtistList.this);
            ab.setMessage("Are you sure?").setPositiveButton("Yes", new OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					Log.d(TAG, "User actually wants to quit");
					// Kill the service
					Intent msgIntent = new Intent(getBaseContext(), MusicPlaybackService.class);
					msgIntent.putExtra("Message", MusicPlaybackService.MSG_STOP_SERVICE);
					startService(msgIntent);
					finish();
				}
            	
            })
            .setNegativeButton("No", new OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					Log.d(TAG, "User doesn't actually want to quit");
				}
            	
            }).show();
            return true;// Consume the event so "back" isn't actually fired.
        }

        return super.onKeyDown(keyCode, event);
    }

}
