package com.smith.d.tyler.notawfulmusicplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class ArtistList extends Activity {
	private static final String TAG = "Artist List";
	public static final String ARTIST_NAME = "ARTIST_NAME";

	private List<Map<String,String>> artists;
	private SimpleAdapter simpleAdpt;
	
	private void populateArtists(String baseDir){
		artists = new ArrayList<Map<String,String>>();
		File f = new File(baseDir);
		for(String artist : f.list()){
			Log.v(TAG, "Adding artist " + artist);
			Map<String,String> map = new HashMap<String, String>();
			map.put("artist", artist);			
			artists.add(map);
		}
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_list);

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        String baseDir = prefs.getString("ARTIST_DIRECTORY", new File(Environment.getExternalStorageDirectory(), "music").getAbsolutePath());
        Log.d(TAG, "Got configured base directory of " + baseDir);

        populateArtists(baseDir);
        
        simpleAdpt = new SimpleAdapter(this, artists, android.R.layout.simple_list_item_1, new String[] {"artist"}, new int[] {android.R.id.text1});
        ListView lv = (ListView) findViewById(R.id.artistListView);
        lv.setAdapter(simpleAdpt);
        
        // React to user clicks on item
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

             public void onItemClick(AdapterView<?> parentAdapter, View view, int position,
                                     long id) {
            	 TextView clickedView = (TextView) view;
            	 Intent intent = new Intent(ArtistList.this, AlbumList.class);
            	 intent.putExtra(ARTIST_NAME, clickedView.getText());
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
        // TODO Auto-generated method stub
    	Log.i(TAG, " handling on key down");
        switch(keyCode)
        {
        case KeyEvent.KEYCODE_BACK:
            AlertDialog.Builder ab = new AlertDialog.Builder(ArtistList.this);
            ab.setMessage("Are you sure?").setPositiveButton("Yes", new OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					Log.d(TAG, "User actually wants to quit");
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
