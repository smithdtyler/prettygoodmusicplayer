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
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;

/**
 * @author tyler
 *
 */
public class GaplessMediaPlayer {
	
	private MediaPlayer front;
	private MediaPlayer back;
	private String[] songFileNames;
	private int songAbsoluteFileNamesPosition;
	private File nextSongFile;

	public GaplessMediaPlayer(){
		this.front = new MediaPlayer();
		this.back = new MediaPlayer();
		
		front.setOnCompletionListener(new OnCompletionListener() {
			

			@Override
			public void onCompletion(MediaPlayer mp) {
				MediaPlayer temp = back;
				back = front;
				front = temp;
				back.reset();
				nextSongFile = new File(GaplessMediaPlayer.this.songFileNames[(songAbsoluteFileNamesPosition + 1) % GaplessMediaPlayer.this.songFileNames.length]);
				FileInputStream fis;
				try {
					fis = new FileInputStream(nextSongFile);
					back.setDataSource(fis.getFD());
					back.prepare();
					front.setNextMediaPlayer(back);
					songAbsoluteFileNamesPosition++;
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public void setOnCompletionListener(
			OnCompletionListener onCompletionListener) {
		// Don't do anything - completion is handled by gapless playback
	}

	public int getCurrentPosition() {
		return front.getCurrentPosition();
	}

	public boolean isPlaying() {
		return front.isPlaying();
	}

	public int getDuration() {
		return front.getDuration();
	}

	public void stop() {
		front.stop();
	}

	public void reset() {
		front.reset();
	}

	public void release() {
		front.release();
		back.release();
	}

	public void seekTo(int i) {
		front.seekTo(i);
	}

	public void setDataSource(FileDescriptor fd) throws IllegalArgumentException, IllegalStateException, IOException {
		front.setDataSource(fd);
	}

	public void start() {
		front.start();
		front.setNextMediaPlayer(back);
	}

	public void prepare() throws IllegalStateException, IOException {
		front.prepare();
	}

	public void pause() {
		front.pause();
	}

	public void setPlaylist(String[] songAbsoluteFileNames,
			int songAbsoluteFileNamesPosition) {
		this.songAbsoluteFileNamesPosition = songAbsoluteFileNamesPosition;
		this.songFileNames = songAbsoluteFileNames;
		nextSongFile = new File(this.songFileNames[(songAbsoluteFileNamesPosition + 1) % this.songFileNames.length]);
		FileInputStream fis;
		try {
			fis = new FileInputStream(nextSongFile);
			back.setDataSource(fis.getFD());
			back.prepare();
			songAbsoluteFileNamesPosition++;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
