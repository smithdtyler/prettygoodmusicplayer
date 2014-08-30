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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

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
	@Deprecated
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		// TODO clean this up a bunch.
		Log.i(TAG, "User clicked " + preference.getTitle());
		if (preference.getTitle().equals("Choose Music Directory")) {
			final File path = Utils.getRootStorageDirectory();
			DirectoryPickerOnClickListener picker = new DirectoryPickerOnClickListener(
					this, path);
			picker.showDirectoryPicker();
			Log.i(TAG, "User selected " + picker.path);
			return true;
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	// TODO add icons
	// btn_check_buttonless_on
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

		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (which == 0) {
				dialog.dismiss();
				// TODO generally fix this up so the displayed path updates
//				SharedPreferences prefs = activity.getPreferenceManager()
//						.getSharedPreferences();
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
				CharSequence[] names = new CharSequence[files.size() + 2];
				for (int i = 0; i < files.size(); i++) {
					names[i + 2] = files.get(i).getName();
				}
				names[0] = "Here!"; // do an "ok" instead?
				names[1] = "Up";
				new AlertDialog.Builder(activity).setTitle("Music Directory")
						.setIcon(android.R.drawable.ic_menu_zoom)
						.setItems(names, this).show();
			} else {
				dialog.dismiss(); // TODO use cancel instead? What's the
									// difference?
				File f = files.get(which - 2);
				path = new File(path, f.getName());
				files = Utils.getPotentialSubDirectories(path);
				CharSequence[] names = new CharSequence[files.size() + 2];
				for (int i = 0; i < files.size(); i++) {
					names[i + 2] = files.get(i).getName();
				}
				names[0] = "Here!"; // do an "ok" instead?
				names[1] = "Up";
				new AlertDialog.Builder(activity).setTitle("Music Directory")
						.setIcon(android.R.drawable.ic_menu_zoom)
						.setItems(names, this).show();
			}

			// TODO Auto-generated method stub
		}

		private void showDirectoryPicker() {
			CharSequence[] names = new CharSequence[files.size() + 2];
			for (int i = 0; i < files.size(); i++) {
				names[i + 2] = files.get(i).getName();
			}
			names[0] = "Here!"; // do an "ok" instead?
			names[1] = "Up";
			new AlertDialog.Builder(activity).setTitle("Music Directory")
					.setIcon(android.R.drawable.ic_menu_zoom)
					.setItems(names, this).show();
		}

	}

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();

			if (preference instanceof ListPreference) {
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(stringValue);

				// Set the summary to reflect the new value.
				preference
						.setSummary(index >= 0 ? listPreference.getEntries()[index]
								: null);
			} else {
				Log.i(TAG,
						"Preferences updated, preference type = other. New value: "
								+ stringValue);
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(stringValue);
			}
			return true;
		}
	};

	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 * 
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private static void bindPreferenceSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		preference
				.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's
		// current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(
				preference,
				PreferenceManager.getDefaultSharedPreferences(
						preference.getContext()).getString(preference.getKey(),
						""));
	}

	/**
	 * This fragment shows general preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class GeneralPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_general);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			bindPreferenceSummaryToValue(findPreference("example_text"));
			bindPreferenceSummaryToValue(findPreference("example_list"));
		}
	}

	/**
	 * This fragment shows notification preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class NotificationPreferenceFragment extends
			PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_notification);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
		}
	}

	/**
	 * This fragment shows data and sync preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class DataSyncPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			getPreferenceManager()
			.setSharedPreferencesName("PrettyGoodMusicPlayer");
			
			addPreferencesFromResource(R.xml.pref_data_sync);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			bindPreferenceSummaryToValue(findPreference("ARTIST_DIRECTORY"));
		}
	}
	
	
}
