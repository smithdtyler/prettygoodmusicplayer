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
import java.util.Comparator;
import java.util.Locale;

import android.os.Environment;

public class Utils {
	public static final Comparator<File> songFileComparator = new SongFileComparator();

	static boolean isValidArtistDirectory(File dir){
		if(dir == null){
			return false;
		}
		
		if(!dir.isDirectory()){
			return false;
		}
		
		String name = dir.getName();
		if(!name.matches("^([A-Z]|[a-z]|[0-9]).+")){
			return false;
		}
		
		return true;
	}
	
	static boolean isValidAlbumDirectory(File dir){
		if(dir == null){
			return false;
		}
		
		if(!dir.isDirectory()){
			return false;
		}
		
		String name = dir.getName();
		if(!name.matches("^([A-Z]|[a-z]|[0-9]).+")){
			return false;
		}
		
		return true;
	}
	
	static boolean isValidSongFile(File song){
		if(song == null){
			return false;
		}
		
		if(!song.isFile()){
			return false;
		}
		
		String name = song.getName();
		if(!name.matches("^([A-Z]|[a-z]|[0-9]).+")){
			return false;
		}
		
		if(!(name.endsWith(".mp3") || name.endsWith(".m4p")|| name.endsWith(".m4a"))){
			return false;
		}
		
		return true;
	}
	
	static String getPrettySongName(File songFile){
		return getPrettySongName(songFile.getName());
	}
	
	static String getPrettySongName(String songName){
		if(songName.matches("^\\d+\\s.*")){
			return songName.replaceAll("^\\d+\\s", "").replaceAll("(\\.mp3)|(\\.m4p)|(\\.m4a)", "");
		}
		return songName;
	}
	
	static String getArtistName(File songFile){
		// TODO check for songs without an album
		return songFile.getParentFile().getParentFile().getName();
	}
	
	private static class SongFileComparator implements Comparator<File>{

		@Override
		public int compare(File arg0, File arg1) {
			String name0 = arg0.getName().toUpperCase(Locale.getDefault());
			String name1 = arg1.getName().toUpperCase(Locale.getDefault());
			return name0.compareTo(name1);
		}
		
	}
	
	static File getBestGuessMusicDirectory(){
		File ext = Environment.getExternalStorageDirectory();
		for(File f : ext.listFiles()){
			if(f.getName().toLowerCase(Locale.getDefault()).contains("music")){
				return f;
			}
		}
		return new File(ext,"music");
	}
}
