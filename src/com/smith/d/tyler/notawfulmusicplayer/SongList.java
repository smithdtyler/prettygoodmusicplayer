package com.smith.d.tyler.notawfulmusicplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class SongList extends Activity {
	public static final String SONG_LIST = "SONG_LIST";
	public static final String SONG_LIST_POSITION = "SONG_LIST_POSITION";
	private static final String TAG = "SongList";
	private List<Map<String,String>> songs;
	private SimpleAdapter simpleAdpt;
	private List<String> songList;
	
	private void populateSongs(String artistName, String albumName, String artistDirName){
		songs = new ArrayList<Map<String,String>>();
		
		File artistDir = new File(artistDirName);
		File album = new File(artistDir, albumName);

		List<File> songFiles = new ArrayList<File>();
		if(album.exists()){
			Log.d(TAG, "external storage directory = " + album);
			
			for(File song : album.listFiles()){
				if(Utils.isValidSongFile(song)){
					songFiles.add(song);
				} else {
					Log.v(TAG, "Found invalid song file " + song);
				}
			}
			
		} else {
			// If the album didn't exist, just list all of the songs we can find.
			// Assume we don't need full recursion
			// TODO for the all song view, sort albums but not all together.
			Log.d(TAG, "Adding all songs...");
			for(File albumFile : artistDir.listFiles()){
				if(albumFile.isDirectory()){
					for(File song : albumFile.listFiles()){
						if(Utils.isValidSongFile(song)){
							songFiles.add(song);
						} else {
							Log.v(TAG, "Found invalid song file " + song);
						}
					}
				}
			}
		}
		
		// We assume that song files start with XX where XX is a number indicating the songs location within an album. 
		Collections.sort(songFiles, new Comparator<File>(){

			@Override
			public int compare(File lhs, File rhs) {
				return lhs.getName().compareTo(rhs.getName());
			}
			
		});
		
		for(File song : songFiles){
			Log.v(TAG, "Adding song " + song);
			Map<String,String> map = new HashMap<String, String>();
			map.put("song", Utils.getPrettySongName(song));			
			songs.add(map);
		}
		
		songList = new ArrayList<String>();
		for(File song : songFiles){
			songList.add(song.getAbsolutePath());
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_song_list);
		
		 // Get the message from the intent
	    Intent intent = getIntent();
	    final String artist = intent.getStringExtra(ArtistList.ARTIST_NAME);
	    final String album = intent.getStringExtra(AlbumList.ALBUM_NAME);
	    final String artistDir = intent.getStringExtra(ArtistList.ARTIST_PATH);
	    Log.i(TAG, "Getting songs for " + album);
	    
	    populateSongs(artist, album, artistDir);
	    
        simpleAdpt = new SimpleAdapter(this, songs, android.R.layout.simple_list_item_1, new String[] {"song"}, new int[] {android.R.id.text1});
        ListView lv = (ListView) findViewById(R.id.songListView);
        lv.setAdapter(simpleAdpt);
        
        // React to user clicks on item
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

             public void onItemClick(AdapterView<?> parentAdapter, View view, int position,
                                     long id) {
            	 Intent intent = new Intent(SongList.this, NowPlaying.class);
            	 intent.putExtra(AlbumList.ALBUM_NAME, album);
            	 intent.putExtra(ArtistList.ARTIST_NAME, artist);
            	 String[] songNamesArr = new String[songList.size()];
            	 songList.toArray(songNamesArr);
            	 intent.putExtra(SONG_LIST, songNamesArr);
            	 intent.putExtra(SONG_LIST_POSITION, position);
            	 startActivity(intent);
             }
        });
	}

	// TODO remove this?
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
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
