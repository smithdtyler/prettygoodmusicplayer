package com.smith.d.tyler.notawfulmusicplayer;

import java.io.File;
import java.util.Comparator;

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
	
	private static class SongFileComparator implements Comparator<File>{

		@Override
		public int compare(File arg0, File arg1) {
			String name0 = arg0.getName().toUpperCase();
			String name1 = arg1.getName().toUpperCase();
			return name0.compareTo(name1);
		}
		
	}
}
