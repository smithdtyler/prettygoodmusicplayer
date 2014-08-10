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
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class AlbumList extends Activity {
	public static final String ALBUM_NAME = "ALBUM_NAME";

	private static final String TAG = "AlbumList";
	private List<Map<String,String>> albums;
	private SimpleAdapter simpleAdpt;
	
	private void populateAlbums(String artistName){
		albums = new ArrayList<Map<String,String>>();
		
		File sdcard = Environment.getExternalStorageDirectory();
		File music = new File(sdcard, "music");
		File artist = new File(music, artistName);
		Log.d(TAG, "external storage directory = " + artist);
		// TODO add 'all' 
		
		List<File> albumFiles = new ArrayList<File>();
		for(File albumFile : artist.listFiles()){
			albumFiles.add(albumFile);
		}
		
		Collections.sort(albumFiles, new Comparator<File>(){

			@Override
			public int compare(File lhs, File rhs) {
				return lhs.getName().compareTo(rhs.getName());
			}
			
		});
		
		albumFiles.add(0,new File("All"));
		
		for(File albumFile : albumFiles){
			String album = albumFile.getName();
			Log.v(TAG, "Adding album " + album);
			Map<String,String> map = new HashMap<String, String>();
			map.put("album", album);			
			albums.add(map);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_album_list);
		
		 // Get the message from the intent
	    Intent intent = getIntent();
	    final String artist = intent.getStringExtra(ArtistList.ARTIST_NAME);
	    Log.i(TAG, "Getting albums for " + artist);
	    
	    populateAlbums(artist);
        
        simpleAdpt = new SimpleAdapter(this, albums, android.R.layout.simple_list_item_1, new String[] {"album"}, new int[] {android.R.id.text1});
        ListView lv = (ListView) findViewById(R.id.albumListView);
        lv.setAdapter(simpleAdpt);
        
        // React to user clicks on item
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

             public void onItemClick(AdapterView<?> parentAdapter, View view, int position,
                                     long id) {
            	 TextView clickedView = (TextView) view;
            	 Intent intent = new Intent(AlbumList.this, SongList.class);
            	 intent.putExtra(ALBUM_NAME, clickedView.getText());
            	 intent.putExtra(ArtistList.ARTIST_NAME, artist);
            	 startActivity(intent);
             }
        });

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.album_list, menu);
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
