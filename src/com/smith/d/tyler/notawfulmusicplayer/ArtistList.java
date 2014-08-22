package com.smith.d.tyler.notawfulmusicplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

@SuppressLint("DefaultLocale") public class ArtistList extends Activity {
	private static final String TAG = "Artist List";
	public static final String ARTIST_NAME = "ARTIST_NAME";
	public static final String ARTISTS_DIR = "ARTIST_DIRECTORY";
	public static final String ARTIST_ABS_PATH_NAME = "ARTIST_PATH";

	private List<Map<String,String>> artists;
	private SimpleAdapter simpleAdpt;
	private String baseDir;
	
	private void populateArtists(String baseDir){
		artists = new ArrayList<Map<String,String>>();
		File f = new File(baseDir);
		if(!f.exists()){
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
	}
	
    @Override
	protected void onResume() {
		super.onResume();
        SharedPreferences prefs = getSharedPreferences("NotAwfulMusicPlayer", MODE_PRIVATE);
        prefs.edit();
        String prefDir = prefs.getString("ARTIST_DIRECTORY", new File(Environment.getExternalStorageDirectory(), "Music").getAbsolutePath());
        ListView lv = (ListView) findViewById(R.id.artistListView);
        if(!prefDir.equals(baseDir)){
        	baseDir = prefDir;
        	populateArtists(baseDir);
            
            simpleAdpt = new SimpleAdapter(this, artists, android.R.layout.simple_list_item_1, new String[] {"artist"}, new int[] {android.R.id.text1});
            lv.setAdapter(simpleAdpt);
        }
        int top = prefs.getInt("ARTIST_LIST_TOP", -1);
        int index = prefs.getInt("ARTIST_LIST_INDEX", -1);
        if(top > 0 && index > 0){
        	lv.setSelectionFromTop(index, top);
        }
	}

	@Override
	protected void onPause() {
		super.onPause();
		// save index and top position
        SharedPreferences prefs = getSharedPreferences("NotAwfulMusicPlayer", MODE_PRIVATE);
		ListView lv = (ListView) findViewById(R.id.artistListView);
		int index = lv.getFirstVisiblePosition();
		View v = lv.getChildAt(0);
		int top = (v == null) ? 0 : v.getTop();
		prefs.edit().putInt("ARTIST_LIST_TOP", top).putInt("ARTIST_LIST_INDEX",index).commit();
	}

	@Override
	protected void onStart() {
		super.onStart();
        SharedPreferences prefs = getSharedPreferences("NotAwfulMusicPlayer", MODE_PRIVATE);
        Log.i(TAG, "Preferences " + prefs + " " + ((Object)prefs));
        baseDir = prefs.getString("ARTIST_DIRECTORY", new File(Environment.getExternalStorageDirectory(), "Music").getAbsolutePath());
        Log.d(TAG, "Got configured base directory of " + baseDir);

        populateArtists(baseDir);
        
        simpleAdpt = new SimpleAdapter(this, artists, android.R.layout.simple_list_item_1, new String[] {"artist"}, new int[] {android.R.id.text1});
        ListView lv = (ListView) findViewById(R.id.artistListView);
        lv.setAdapter(simpleAdpt);
    }



	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_list);

        ListView lv = (ListView) findViewById(R.id.artistListView);
        
        // React to user clicks on item
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

             public void onItemClick(AdapterView<?> parentAdapter, View view, int position,
                                     long id) {
            	 TextView clickedView = (TextView) view;
            	 Intent intent = new Intent(ArtistList.this, AlbumList.class);
            	 intent.putExtra(ARTIST_NAME, clickedView.getText());
            	 intent.putExtra(ARTIST_ABS_PATH_NAME, baseDir + File.separator + clickedView.getText());
            	 startActivity(intent);
             }
        });
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
        if (id == R.id.action_settings) {
        	Intent intent = new Intent(ArtistList.this, SettingsActivity.class);
        	startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
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
					msgIntent.putExtra("Message", MusicPlaybackService.MSG_STOP);
					startService(msgIntent);
					onBackPressed();
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
