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
import java.util.Map;

import com.smithdtyler.prettygoodmusicplayer.R;

import android.app.Activity;
import android.content.Intent;
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
	
	private void populateSongs(String artistName, String albumDirName, String artistAbsDirName){
		songs = new ArrayList<Map<String,String>>();
		
		File artistDir = new File(artistAbsDirName);
		File albumDir;
		if(albumDirName != null){
			albumDir = new File(artistDir, albumDirName);
		} else {
			albumDir = artistDir; 
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
		
		songAbsFileNameList = new ArrayList<String>();
		for(File song : songFiles){
			songAbsFileNameList.add(song.getAbsolutePath());
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = sharedPref.getString("pref_theme", "light");
        String size = sharedPref.getString("pref_text_size", "medium");
        Log.i(TAG, "got configured theme " + theme);
        Log.i(TAG, "got configured size " + size);
        currentTheme = theme;
        currentSize = size;
        if(theme.equalsIgnoreCase("dark")){
        	Log.i(TAG, "setting theme to " + theme);
        	if(size.equalsIgnoreCase("small")){
        		setTheme(R.style.PGMPDarkSmall);
        	} else if (size.equalsIgnoreCase("medium")){
        		setTheme(R.style.PGMPDarkMedium);
        	} else {
        		setTheme(R.style.PGMPDarkLarge);
        	}
        } else if (theme.equalsIgnoreCase("light")){
        	Log.i(TAG, "setting theme to " + theme);
        	if(size.equalsIgnoreCase("small")){
        		setTheme(R.style.PGMPLightSmall);
        	} else if (size.equalsIgnoreCase("medium")){
        		setTheme(R.style.PGMPLightMedium);
        	} else {
        		setTheme(R.style.PGMPLightLarge);
        	}
        }
		
		setContentView(R.layout.activity_song_list);
		
		 // Get the message from the intent
	    Intent intent = getIntent();
	    final String artistName = intent.getStringExtra(ArtistList.ARTIST_NAME);
	    final String album = intent.getStringExtra(AlbumList.ALBUM_NAME);
	    final String artistDir = intent.getStringExtra(ArtistList.ARTIST_ABS_PATH_NAME);
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
            	 intent.putExtra(SONG_ABS_FILE_NAME_LIST_POSITION, position);
            	 intent.putExtra(NowPlaying.KICKOFF_SONG, true);
            	 startActivity(intent);
             }
        });
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
        return super.onOptionsItemSelected(item);
    }
    
    @Override
	protected void onResume() {
		super.onResume();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = sharedPref.getString("pref_theme", "light");
        String size = sharedPref.getString("pref_text_size", "medium");
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

}
