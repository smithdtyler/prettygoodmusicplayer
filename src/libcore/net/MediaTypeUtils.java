/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package libcore.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Utilities for dealing with MIME types.
 * Used to implement java.net.URLConnection and android.webkit.MimeTypeMap.
 */
public final class MediaTypeUtils {
	private static final Map<String, List<String>> mimeTypeToExtensionMap = new HashMap<>();
	private static final Map<String, List<String>> extensionToMimeTypeMap = new HashMap<>();

	static {
		// The following table contains media types following
		// https://developer.android.com/guide/appendix/media-formats.html
		// The "most popular" extension must come first, so that it's the one returned
		// by guessExtensionFromMimeType.
		add("audio/3gpp", "3gpp");
		add("audio/3gpp", "3gp");
		add("audio/amr-wb", "3gpp");
		add("audio/amr-wb", "3gp");

		// not sure about this one
		add("audio/dts", "ts");

		// no mkv/mka support for flac according to the docs :(
		add("audio/flac", "flac");

		add("audio/gsm", "gsm");

		add("audio/midi", "mid");
		add("audio/midi", "midi");
		add("audio/midi", "xmf");
		add("audio/midi", "mxmf");
		add("audio/midi", "rttl");
		add("audio/midi", "rtx");
		add("audio/midi", "ota");
		add("audio/midi", "imy");

		// audio/mpeg seems to be used exclusively by OMX.google.mp3.decoder
		add("audio/mpeg", "mp3");
		add("audio/mpeg3", "mp3");

		add("audio/mpeg-L2", "mp2");

		add("audio/mp4a-latm", "m4a");
		add("audio/mp4a-latm", "aac");
		add("audio/mp4a-latm", "mp4");
		add("audio/mp4a-latm", "m4p");

		add("audio/opus", "opus");
		add("audio/opus", "ogg");
		add("audio/opus", "oga");
		add("audio/opus", "mka");
		add("audio/opus", "mkv");

		add("audio/raw", "raw");
		add("audio/raw", "pcm");

		add("audio/vorbis", "ogg");
		add("audio/vorbis", "oga");
		add("audio/vorbis", "vorbis");
		add("audio/vorbis", "mka");
		add("audio/vorbis", "mkv");

		add("audio/wav", "wav");
		add("audio/wav", "wave");

		add("audio/x-ape", "ape");

		add("audio/x-ms-wma", "wma");

		add("audio/vnd.rn-realaudio", "ra");
		add("audio/vnd.rn-realaudio", "rm");
		applyOverrides();
	}

	private static void add(String mimeType, String extension) {
		//
		// if we have an existing x --> y mapping, we do not want to
		// override it with another mapping x --> ?
		// this is mostly because of the way the mime-type map below
		// is constructed (if a mime type maps to several extensions
		// the first extension is considered the most popular and is
		// added first; we do not want to overwrite it later).
		//
		if (!mimeTypeToExtensionMap.containsKey(mimeType)) {
			mimeTypeToExtensionMap.put(mimeType, new ArrayList<String>());
		}
		if (!mimeTypeToExtensionMap.get(mimeType).contains(extension)) {
			mimeTypeToExtensionMap.get(mimeType).add(extension);
		}

		if (!extensionToMimeTypeMap.containsKey(extension)) {
			extensionToMimeTypeMap.put(extension, new ArrayList<String>());
		}
		if (!extensionToMimeTypeMap.get(extension).contains(mimeType)) {
			extensionToMimeTypeMap.get(extension).add(mimeType);
		}
	}

	private static InputStream getContentTypesPropertiesStream() {
		// User override?
		String userTable = System.getProperty("content.types.user.table");
		if (userTable != null) {
			File f = new File(userTable);
			if (f.exists()) {
				try {
					return new FileInputStream(f);
				} catch (IOException ignored) {
				}
			}
		}
		// Standard location?
		File f = new File(System.getProperty("java.home"), "lib" + File.separator + "content-types.properties");
		if (f.exists()) {
			try {
				return new FileInputStream(f);
			} catch (IOException ignored) {
			}
		}
		return null;
	}

	/**
	 * This isn't what the RI does. The RI doesn't have hard-coded defaults, so supplying your
	 * own "content.types.user.table" means you don't get any of the built-ins, and the built-ins
	 * come from "$JAVA_HOME/lib/content-types.properties".
	 */
	private static void applyOverrides() {
		// Get the appropriate InputStream to read overrides from, if any.
		InputStream stream = getContentTypesPropertiesStream();
		if (stream == null) {
			return;
		}
		try {
			try {
				// Read the properties file...
				Properties overrides = new Properties();
				overrides.load(stream);
				// And translate its mapping to ours...
				for (Map.Entry<Object, Object> entry : overrides.entrySet()) {
					String extension = (String) entry.getKey();
					String mimeType = (String) entry.getValue();
					add(mimeType, extension);
				}
			} finally {
				stream.close();
			}
		} catch (IOException ignored) {
		}
	}

	private MediaTypeUtils() {
	}

	/**
	 * Returns true if the given MIME type has an entry in the map.
	 *
	 * @param mimeType A MIME type (i.e. text/plain)
	 * @return True iff there is a mimeType entry in the map.
	 */
	public static boolean hasMimeType(String mimeType) {
		if (mimeType == null || mimeType.isEmpty()) {
			return false;
		}
		return mimeTypeToExtensionMap.containsKey(mimeType);
	}

	/**
	 * Returns the MIME type for the given extension.
	 *
	 * @param extension A file extension without the leading '.'
	 * @return The MIME type for the given extension or null iff there is none.
	 */
	public static String guessMimeTypeFromExtension(String extension) {
		if (extension == null || extension.isEmpty()) {
			return null;
		}
		return extensionToMimeTypeMap.get(extension).get(0);
	}

	public static List<String> getMimeTypesFromExtension(String extension) {
		if (extension == null) {
			return Collections.emptyList();
		}
		return extensionToMimeTypeMap.get(extension);
	}

	/**
	 * Returns true if the given extension has a registered MIME type.
	 *
	 * @param extension A file extension without the leading '.'
	 * @return True iff there is an extension entry in the map.
	 */
	public static boolean hasExtension(String extension) {
		if (extension == null || extension.isEmpty()) {
			return false;
		}
		return extensionToMimeTypeMap.containsKey(extension);
	}

	/**
	 * Returns the registered extension for the given MIME type. Note that some
	 * MIME types map to multiple extensions. This call will return the most
	 * common extension for the given MIME type.
	 *
	 * @param mimeType A MIME type (i.e. text/plain)
	 * @return The extension for the given MIME type or null iff there is none.
	 */
	public static String guessExtensionFromMimeType(String mimeType) {
		if (mimeType == null || mimeType.isEmpty()) {
			return null;
		}
		return getExtensionsFromMimeType(mimeType).get(0);
	}

	public static List<String> getExtensionsFromMimeType(String mimeType) {
		if (mimeType == null) {
			return Collections.emptyList();
		}
		return mimeTypeToExtensionMap.get(mimeType);
	}
}