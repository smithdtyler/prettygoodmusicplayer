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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class SongList extends Activity {
	public static final String SONG_ABS_FILE_NAME_LIST = "SONG_LIST";
	public static final String SONG_ABS_FILE_NAME_LIST_POSITION = "SONG_LIST_POSITION";
	private static final String TAG = "SongList";
	private List<Map<String,String>> songs;
	private SimpleAdapter simpleAdpt;
	private List<String> songAbsFileNameList;
	private String currentTheme;
	private String currentSize;
	private boolean hasResume = false;
	private int resumeFilePos = -1;
	private int resumeProgress;
	private String resume;
	private String artistDir;
	private File albumDir;
	private boolean audiobookMode;
	private BroadcastReceiver exitReceiver;
	
	private void populateSongs(String artistName, String albumDirName, String artistAbsDirName){
		
		songs = new ArrayList<Map<String,String>>();
		
		File artistDir = new File(artistAbsDirName);
		if(albumDirName != null){
			albumDir = new File(artistDir, albumDirName);
		} else {
			albumDir = artistDir; 
		}

		SharedPreferences prefs = getSharedPreferences("PrettyGoodMusicPlayer", MODE_PRIVATE);
		resume = prefs.getString(albumDir.getAbsolutePath(), null);
		if(resume != null){
			Log.i(TAG, "Found resumable time! " + resume);
		} else {
			Log.i(TAG, "Didn't find a resumable time");
		}

		List<File> songFiles = new ArrayList<File>();
		if(albumDir.exists() && albumDir.isDirectory() && (albumDir.listFiles() != null)){
			Log.d(TAG, "external storage directory = " + albumDir);
			
			for(File song : albumDir.listFiles()){
				if(Utils.isValidSongFile(song)){
					songFiles.add(song);
				} else {
					Log.v(TAG, "Found invalid song file " + song);
				}
			}
			
			// We assume that song files start with XX where XX is a number indicating the songs location within an album. 
			Collections.sort(songFiles, Utils.songFileComparator);
		} else {
			// If the album didn't exist, just list all of the songs we can find.
			// Assume we don't need full recursion
			Log.d(TAG, "Adding all songs...");
			File[] albumArray = artistDir.listFiles();
			List<File> albums = new ArrayList<File>();
			for(File alb : albumArray){
				albums.add(alb);
			}
			
			Collections.sort(albums, Utils.albumFileComparator);
			
			for(File albumFile : albums){
				if(Utils.isValidAlbumDirectory(albumFile)){
					// get the songs in the album, sort them, then
					// add them to the list
					File[] songFilesInAlbum = albumFile.listFiles();
					List<File> songFilesInAlbumList = new ArrayList<File>();
					for(File songFile : songFilesInAlbum){
						if(Utils.isValidSongFile(songFile)){
							songFilesInAlbumList.add(songFile);
						}
					}
					Collections.sort(songFilesInAlbumList, Utils.songFileComparator);
					songFiles.addAll(songFilesInAlbumList);
				}
			}
			
			if(songFiles.isEmpty()){
				// if there aren't any albums, check directly under the artist directory
				File[] songFilesInArtist = artistDir.listFiles();
				List<File> songFilesInArtistList = new ArrayList<File>();
				for(File songFile : songFilesInArtist){
					if(Utils.isValidSongFile(songFile)){
						songFilesInArtistList.add(songFile);
					}
				}
				Collections.sort(songFilesInArtistList, Utils.songFileComparator);
				songFiles.addAll(songFilesInArtistList);
			}
		}
		
		for(File song : songFiles){
			Log.v(TAG, "Adding song " + song);
			Map<String,String> map = new HashMap<String, String>();
			map.put("song", Utils.getPrettySongName(song));			
			songs.add(map);
		}
		
		// If there is a value set to resume to, and audiobook mode is enabled
		// add an option to start where they left off
		if(resume != null && audiobookMode){
			try{
				String resumeSongName = resume.substring(0, resume.lastIndexOf('~'));
				
				File resumeFile = new File(albumDir, resumeSongName);
				if(resumeFile.exists()){
					String progress = resume.substring(resume.lastIndexOf('~') + 1);
					int prog = Integer.valueOf(progress);
					resumeProgress = prog;
					resumeSongName = Utils.getPrettySongName(resumeSongName);
					int minutes = prog / (1000 * 60);
					int seconds = (prog % (1000 * 60)) / 1000;
					String time = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
					Map<String, String> map = new HashMap<String, String>();
					map.put("song", getResources().getString(R.string.resume) + ": " + resumeSongName + " (" + time + ")");
					songs.add(0, map);
					// loop over the available songs, make sure we still have it
					for(int i = 0; i< songFiles.size(); i++){
						File song = songFiles.get(i);
						if(song.equals(resumeFile)){
							resumeFilePos  = i;
							break;
						}
					}
					if(resumeFilePos >= 0){
						hasResume = true;
					}
				} else {
					Log.w(TAG, "Couldn't find file to resume");
				}
			} catch (Exception e){
				Log.w(TAG, "Couldn't add resume song name", e);
				hasResume = false;
			}
		}
		
		songAbsFileNameList = new ArrayList<String>();
		for(File song : songFiles){
			songAbsFileNameList.add(song.getAbsolutePath());
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		 // Get the message from the intent
	    Intent intent = getIntent();
	    final String artistName = intent.getStringExtra(ArtistList.ARTIST_NAME);
	    final String album = intent.getStringExtra(AlbumList.ALBUM_NAME);
	    artistDir = intent.getStringExtra(ArtistList.ARTIST_ABS_PATH_NAME);
	    
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(artistName + ": " + album);
		
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = sharedPref.getString("pref_theme", getString(R.string.light));
        String size = sharedPref.getString("pref_text_size", getString(R.string.medium));
        audiobookMode = sharedPref.getBoolean("pref_audiobook_mode", false);
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
		
		setContentView(R.layout.activity_song_list);
		
	    Log.i(TAG, "Getting songs for " + album);
	    
	    populateSongs(artistName, album, artistDir);
	    
        simpleAdpt = new SimpleAdapter(this, songs, R.layout.pgmp_list_item, new String[] {"song"}, new int[] {R.id.PGMPListItemText});
        ListView lv = (ListView) findViewById(R.id.songListView);
        lv.setAdapter(simpleAdpt);
        
        // React to user clicks on item
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

             public void onItemClick(AdapterView<?> parentAdapter, View view, int position,
                                     long id) {
            	 
            	 Intent intent = new Intent(SongList.this, NowPlaying.class);
            	 intent.putExtra(AlbumList.ALBUM_NAME, album);
            	 intent.putExtra(ArtistList.ARTIST_NAME, artistName);
            	 String[] songNamesArr = new String[songAbsFileNameList.size()];
            	 songAbsFileNameList.toArray(songNamesArr);
            	 intent.putExtra(SONG_ABS_FILE_NAME_LIST, songNamesArr);
            	 intent.putExtra(ArtistList.ARTIST_ABS_PATH_NAME, artistDir);
            	 intent.putExtra(NowPlaying.KICKOFF_SONG, true);

            	 if(hasResume){
            		 if(position == 0){
   	            		 intent.putExtra(SONG_ABS_FILE_NAME_LIST_POSITION, resumeFilePos);
   	            		 intent.putExtra(MusicPlaybackService.TRACK_POSITION, resumeProgress);
            		 } else {
            			 // a 'resume' option has been added to the beginning of the list
            			 // so adjust the selection to compensate
    	            	 intent.putExtra(SONG_ABS_FILE_NAME_LIST_POSITION, position - 1);
            		 }
            	 } else {
	            	 intent.putExtra(SONG_ABS_FILE_NAME_LIST_POSITION, position);
            	 }
            	 startActivity(intent);
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
	protected void onDestroy() {
    	unregisterReceiver(exitReceiver);
    	super.onDestroy();
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.song_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
        	Intent intent = new Intent(SongList.this, SettingsActivity.class);
        	startActivity(intent);
            return true;
        }
        if (id == R.id.action_exit) {
			Intent broadcastIntent = new Intent();
			broadcastIntent.setAction("com.smithdtyler.ACTION_EXIT");
			sendBroadcast(broadcastIntent);
			Intent startMain = new Intent(Intent.ACTION_MAIN);
		    startMain.addCategory(Intent.CATEGORY_HOME);
		    startMain.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		    startActivity(startMain);
			finish();
            return true;
        }
        if(id == android.R.id.home){
        	onBackPressed();
        	return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
	protected void onResume() {
		super.onResume();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = sharedPref.getString("pref_theme", getString(R.string.light));
        String size = sharedPref.getString("pref_text_size", getString(R.string.medium));
        boolean audiobookModePref = sharedPref.getBoolean("pref_audiobook_mode", false);
        Log.i(TAG, "got configured theme " + theme);
        Log.i(TAG, "Got configured size " + size);
        if(currentTheme == null){
        	currentTheme = theme;
        } 
        
        if(currentSize == null){
        	currentSize = size;
        }
        
        boolean resetResume = false;
        if(audiobookMode != audiobookModePref){
        	resetResume = true;
        }
        SharedPreferences prefs = getSharedPreferences("PrettyGoodMusicPlayer", MODE_PRIVATE);
        String newResume = prefs.getString(albumDir.getAbsolutePath(), null);
        if(resume != null && newResume != null && !newResume.equals(resume)){
        	resetResume = true;
        }else if(resume == null && newResume != null){
        	resetResume = true;
        }
        
        if(!currentTheme.equals(theme) || !currentSize.equals(size) || resetResume){
        	// Calling finish and startActivity will re-launch this activity, applying the new settings
        	finish();
        	startActivity(getIntent());
        }
	}

}
