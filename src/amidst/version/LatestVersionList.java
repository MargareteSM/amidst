package amidst.version;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import amidst.logging.Log;
import amidst.resources.ResourceLoader;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class LatestVersionList {
	public enum LoadState {
		LOADED, LOADING, FAILED, IDLE
	}

	private static final String REMOTE_VERSION_LIST_URL = "https://s3.amazonaws.com/Minecraft.Download/versions/versions.json";
	private static final Gson GSON = new Gson();

	private LoadState loadState = LoadState.IDLE;

	private VersionList profile;

	public Map<String, String>[] getVersions() {
		return profile.versions;
	}

	public void load() {
		Log.i("Beginning latest version list load.");
		loadState = LoadState.LOADING;
		if (!attemptRemoteLoad() && !attemptLocalLoad()) {
			Log.w("Failed to load both remote and local version list.");
			loadState = LoadState.FAILED;
		}
		loadState = LoadState.LOADED;
	}

	private boolean attemptLocalLoad() {
		Log.i("Attempting to download local version list...");
		URL versionUrl = ResourceLoader.getResourceURL("versions.json");
		return attemptLoad(versionUrl);
	}

	private boolean attemptRemoteLoad() {
		Log.i("Attempting to download remote version list...");
		URL versionUrl = null;
		try {
			versionUrl = new URL(REMOTE_VERSION_LIST_URL);
		} catch (MalformedURLException e) {
			Log.w("MalformedURLException on remote version list. Aborting load. This should never be possible.");
			Log.printTraceStack(e);
			Log.w("Aborting remote version list load.");
			return false;
		}

		return attemptLoad(versionUrl);
	}

	private boolean attemptLoad(URL versionUrl) {
		URLConnection urlConnection = null;
		try {
			urlConnection = versionUrl.openConnection();
		} catch (IOException e) {
			Log.w("IOException when attempting to open connection to version list.");
			Log.printTraceStack(e);
			Log.w("Aborting version list load. URL: " + versionUrl);
			return false;
		}

		int contentLength = urlConnection.getContentLength();
		if (contentLength == -1) {
			Log.w("Content length of version list returned -1.");
			Log.w("Aborting version list load. URL: " + versionUrl);
			return false;
		}

		InputStream inputStream = null;
		try {
			inputStream = urlConnection.getInputStream();
		} catch (IOException e) {
			Log.w("IOException on opening input stream to version list.");
			Log.printTraceStack(e);
			Log.w("Aborting version list load. URL: " + versionUrl);
			return false;
		}

		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(inputStream));
		try {
			profile = GSON.fromJson(bufferedReader, VersionList.class);
		} catch (JsonSyntaxException e) {
			Log.w("Unable to parse version list.");
			Log.printTraceStack(e);
			Log.w("Aborting version list load. URL: " + versionUrl);
			return false;
		} finally {
			try {
				bufferedReader.close();
			} catch (IOException e) {
				Log.w("IOException thrown when attempting to close stream for version list.");
				Log.printTraceStack(e);
			}
		}

		Log.i("Successfully loaded version list. URL: " + versionUrl);
		return true;
	}

	public LoadState getLoadState() {
		return loadState;
	}

	private class VersionList {
		public Map<String, String>[] versions;
	}
}
