/**
 The Pretty Good Music Player
 Copyright (C) 2015  Tyler Smith

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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * This class handles operations which are common to all lists of music at
 * various levels of granularity.
 * Created by tyler on 6/27/15.
 */
public abstract class AbstractMusicList extends Activity {
    private static final String TAG = "AbstractMusicList";

    protected BroadcastReceiver exitReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        getMenuInflater().inflate(R.menu.abstract_music_list_menu, menu);
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
                Intent intent = new Intent(AbstractMusicList.this, NowPlaying.class);
                intent.putExtra("From_Notification", true);
                startActivity(intent);
            } else {
                Toast.makeText(AbstractMusicList.this, R.string.nothing_playing, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if (id == R.id.action_settings) {
            Intent intent = new Intent(AbstractMusicList.this, SettingsActivity.class);
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
        if(id == android.R.id.home){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
