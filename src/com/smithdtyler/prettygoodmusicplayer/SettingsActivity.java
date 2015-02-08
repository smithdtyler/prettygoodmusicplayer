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
import java.util.List;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {
	private static final String TAG = "SettingsActivity";

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		setupSimplePreferencesScreen();
	}

	/**
	 * Shows the simplified settings UI if the device configuration if the
	 * device configuration dictates that a simplified, single-pane UI should be
	 * shown.
	 */
	@SuppressWarnings("deprecation")
	private void setupSimplePreferencesScreen() {

		// In the simplified UI, fragments are not used at all and we instead
		// use the older PreferenceActivity APIs.

		// Add 'general' preferences.
		// TODO this is deprecated, update to use fragments I guess?
		addPreferencesFromResource(R.xml.pretty_good_preferences);
	}
	
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
        if(id == android.R.id.home){
        	onBackPressed();
        	return true;
        }
		return super.onOptionsItemSelected(item);
	}

	@Override
	@Deprecated
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		// TODO clean this up a bunch.
		Log.i(TAG, "User clicked " + preference.getTitle());
		if (preference.getKey().equals("choose_music_directory_prompt")) {
			final File path = Utils.getRootStorageDirectory();
			DirectoryPickerOnClickListener picker = new DirectoryPickerOnClickListener(
					this, path);
			picker.showDirectoryPicker();
			Log.i(TAG, "User selected " + picker.path);
			return true;
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	// https://stackoverflow.com/questions/3920640/how-to-add-icon-in-alert-dialog-before-each-item
	private static class DirectoryPickerOnClickListener implements
			OnClickListener {
		private SettingsActivity activity;
		private File path;
		private List<File> files;
		
		private DirectoryPickerOnClickListener(SettingsActivity activity,
				File root) {
			this.path = root;
			files = Utils.getPotentialSubDirectories(root);
			this.activity = activity;
		}

		// TODO handle root case where there isn't an 'up'
		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (which == 0) {
				dialog.dismiss();
		        SharedPreferences prefs = activity.getSharedPreferences("PrettyGoodMusicPlayer", MODE_PRIVATE);

				Log.i(TAG,
						"Preferences update success: "
								+ prefs.edit()
										.putString("ARTIST_DIRECTORY",
												path.getAbsolutePath())
										.commit());
				// reset the positions in the artist list, since we've changed
				// lists
				prefs.edit().putInt("ARTIST_LIST_TOP", Integer.MIN_VALUE)
						.putInt("ARTIST_LIST_INDEX", Integer.MIN_VALUE)
						.commit();
				return;
			}
			if (which == 1) {
				dialog.dismiss(); // TODO use cancel instead? What's the
									// difference?
				if (path.getParentFile() != null) {
					path = path.getParentFile();
				}
				files = Utils.getPotentialSubDirectories(path);
				showDirectoryPicker();
				
			} else {
				dialog.dismiss(); // TODO use cancel instead? What's the
									// difference?
				File f = files.get(which - 2);
				path = new File(path, f.getName());
				files = Utils.getPotentialSubDirectories(path);
				showDirectoryPicker();
			}

		}

		private void showDirectoryPicker() {
			final Item[] items = new Item[files.size() + 2];
			for(int i = 0;i<files.size();i++){
				items[i + 2] = new Item(files.get(i).getName(), R.drawable.ic_action_collection);
			}
			
			items[0] = new Item(path.getAbsolutePath(), R.drawable.ic_pgmp_launcher);
			//items[0] = new Item(activity.getResources().getString(R.string.directorydialoghere), R.drawable.ic_pgmp_launcher);
			items[1] = new Item(activity.getResources().getString(R.string.directorydialogup), android.R.drawable.ic_menu_upload);
			
			ListAdapter adapter = new ArrayAdapter<Item>(
				    activity,
				    android.R.layout.select_dialog_item,
				    android.R.id.text1, items){
				        public View getView(int position, View convertView, ViewGroup parent) {
				            //User super class to create the View
				            View v = super.getView(position, convertView, parent);
				            TextView tv = (TextView)v.findViewById(android.R.id.text1);

				            //Put the image on the TextView
				            tv.setCompoundDrawablesWithIntrinsicBounds(items[position].icon, 0, 0, 0);

				            //Add margin between image and text (support various screen densities)
				            int dp5 = (int) (5 * activity.getResources().getDisplayMetrics().density + 0.5f);
				            tv.setCompoundDrawablePadding(dp5);

				            return v;
				        }
				    };
			new AlertDialog.Builder(activity).setTitle(activity.getResources().getString(R.string.directorydialogprompt))
					.setIcon(android.R.drawable.ic_menu_zoom)
					.setAdapter(adapter, this).show();
		}
		
	}
	
	
	public static class Item{
	    public final String text;
	    public final int icon;
	    public Item(String text, Integer icon) {
	        this.text = text;
	        this.icon = icon;
	    }
	    @Override
	    public String toString() {
	        return text;
	    }
	}
	
}
