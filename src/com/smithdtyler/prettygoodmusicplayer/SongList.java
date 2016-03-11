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

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SongList extends AbstractMusicList {
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

	private void populateSongs(String artistName, String albumDirName, String albumPath, String artistAbsDirName){
		
		songs = new ArrayList<Map<String,String>>();
		
		File artistDir = new File(artistAbsDirName);
		if(albumDirName != null && albumPath != null){
			albumDir = new File(albumPath);
			if(!albumDir.exists()) {
				albumDir = new File(artistDir, albumDirName);
			}
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
		Log.i(TAG, "Artist dir: " + artistDir);
		Log.i(TAG, "Album dir: " + albumDir);

		List<File> songFiles = new ArrayList<File>();

		// Did the user provide an exact path to the album?
		if(albumDir.exists() && albumDir.isDirectory() && (albumDir.listFiles() != null)){
			Log.d(TAG, "external storage directory = " + albumDir);
			
			for(File song : Utils.getAllSongsInDirRecursive(albumDir)){
				if(Utils.isValidSongFile(song)){
					songFiles.add(song);
				} else {
					Log.v(TAG, "Found invalid song file " + song);
				}
			}
			
			// We assume that song files start with XX where XX is a number indicating the songs location within an album. 
			Collections.sort(songFiles, Utils.songFileComparator);
			// Did the user provide an artist, but no album?
		} else if(artistDir.exists() && artistDir.listFiles() != null) {
			for (File f : artistDir.listFiles()) {
				if (f.isDirectory()) {
					List<File> songs = Utils.getAllSongsInDirRecursive(f);
					songFiles.addAll(songs);
				} else if (Utils.isValidSongFile(f)) {
					songFiles.add(f);
				}
			}
			// Did the user pick an album from 'All'?
		}else if(albumDir.exists() && albumDir.isDirectory() && albumDir.listFiles() != null){
			List<File> songs = Utils.getAllSongsInDirRecursive(albumDir);
			songFiles.addAll(songs);
		} else {
			// If the album didn't exist, just list all of the songs we can find.
			Log.d(TAG, "Adding all songs...");
			File bestGuessMusicDir = Utils.getBestGuessMusicDirectory();
			String prefDir = prefs.getString("ARTIST_DIRECTORY", bestGuessMusicDir.getAbsolutePath());
			File baseDir = new File(prefDir);
			List<File> albums = Utils.getAllAlbumsInDirRecursive(baseDir);

			Collections.sort(albums, Utils.albumFileComparator);
			
			for(File albumFile : albums){
				if(Utils.isValidAlbumDirectory(albumFile)){
					// get the songs in the album, sort them, then
					// add them to the list
					List<File> songFilesInAlbum = Utils.getAllSongsInDirRecursive(albumFile);

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
		final String albumPath = intent.getStringExtra(AlbumList.ALBUM_PATH);
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
	    
	    populateSongs(artistName, album, albumPath, artistDir);
	    
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

		lv.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				showSongSettingsDialog();
				return true;
			}
		});

	}

	private void showSongSettingsDialog(){
		new AlertDialog.Builder(this).setTitle("Song Details")
				.setItems(new String[]{"enabled"}, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

					}
				});
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
