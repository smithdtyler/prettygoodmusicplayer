prettygoodmusicplayer
=====================

Update: As of 2017.04.01 I am no longer actively maintaining this app. If you've created a fork and are actively maintaining it, feel free to submit a pull request adding a link to your version.

=====================

A music player for Android that hits the basics hard

I do my best to address issues when they arise, but I'm pretty busy, so it may take a long time for me to push updates.

If you want to motivate me to work on a particular feature, please consider a donation to [Doctors Without Borders](http://www.doctorswithoutborders.org/).Just tell me about your donation by commenting on the appropriate issue and I'll be like "woah, I better work on this!" 

If you're having troubles with media button presses being ignored, check out Media Button Router: https://github.com/harleensahni/media-button-router

Building: 
I've been building this in Eclipse. For Android Studio do 'Import Project' -> select build.gradle -> click 'Yes' for the gradle wrapper (or choose your own gradle folder. Wrapper is recommended as AStudio is finicky about which version it uses)


Installing:
The production version is available here:

Google Play: https://play.google.com/store/apps/details?id=com.smithdtyler.prettygoodmusicplayer

F-Droid: https://f-droid.org/repository/browse/?fdfilter=pretty+good+music+player&fdid=com.smithdtyler.prettygoodmusicplayer

Amazon: http://www.amazon.com/gp/product/B00Q9OOQBK

Description:

Tired of music players that take forever to start up because they're loading ads or trying to download stuff? Of music players that ignore Bluetooth controls, or stop responding after a few minutes? I was too, so I wrote this app!
The Pretty Good Music Player is an open source (GPL) folder-based music player with no frills. It's small, it's responsive, and it plays music.

Feature Details:
- Audiobook mode for resuming playback where you left off
- Notification controls
- The color theme and text size are now configurable!
- This player protects against accidental pausing when your phone is jostled and the headphone cord shifts. I added this because it was annoying when my phone was in my pocket and would auto-pause because the headphone cable was bumped.
- Plays .mp3, .m4p, .m4a, .aac, and many more types of audio files.
- Accepts play, pause, previous, and next commands from Bluetooth headsets.
- When you select 'All songs' for a given artist, the songs are ordered by album, not alphabetically. 
- Automatically pauses and resumes when another app (e.g. GPS) needs audio.
- If another app needs audio for more than 30 seconds (e.g. a phone call), it does not automatically resume. 
- "Repeat All" is always on.
- If you click "back" too quickly, it double checks that you want to leave before quitting.

Expected Folder Layout:
It's designed to work with artist folders copied from iTunes. That means it expects directory structure:

music/
artist/
/album1
/01 - First Song.mp3
/02 - Second Song.mp3
/03 - Third Song.mp3
/04 - Fourth Song.mp3
/album2
/01 - Another Song.mp3
....

Misc:
I've noticed that if I pause a song for an extended period of time, I need to press and hold the 'pause' button on my Bluetooth headset to get it to resume playing.

The full source code is available here: https://github.com/smithdtyler/prettygoodmusicplayer

The Pretty Good Music Player also has a Facebook page where I post information about coming releases and issues and enhancements:
https://www.facebook.com/PrettyGoodMusicPlayer

The awesome icon was designed by Emily Krueger of Vine Creative: http://www.vinecreativemn.com/

If you have any problems, please leave a comment with as much information as possible and I'll try to get a fix out ASAP.

Compatibility:
Bluetooth control tested with an LG Electronics Tone+ HBS-730 Bluetooth Headset

Legal Stuff:

Pretty Good Music Player
Copyright Tyler Smith 2014

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

See http://www.gnu.org/licenses/ for a full copy of the GNU General Public License.

Reddit Stuff:
I post on Reddit as https://www.reddit.com/user/MythsBusted
