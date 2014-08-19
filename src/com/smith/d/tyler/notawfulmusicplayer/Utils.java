package com.smith.d.tyler.notawfulmusicplayer;

import java.io.File;

public class Utils {

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
		return songFile.getName().replaceAll("\\d\\d\\s", "").replaceAll("(\\.mp3)|(\\.m4p)|(\\.m4a)", "");
	}
}
