package com.smeanox.apps.vacationnotify;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class MainActivity extends AppCompatActivity {

	private String url = "", username = "", password = "";
	private List<Message> messages;

	private final String PREFS_URL = "url", PREFS_USERNAME = "username", PREFS_PASSWORD = "password";
	private final String PREFS_MSGS_LENGTH = "messages_length", PREFS_MSGS_MSG_PREFIX = "msg_msg_",
			PREFS_MSGS_TIME_PREFIX = "msg_time_", PREFS_MSGS_CODE_PREFIX = "msg_code_";

	private final String ENDPOINT_CHECK = "check.php", ENDPOINT_DELETE = "received.php";

	private volatile boolean isUpdating = false, isDeleting = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		messages = new ArrayList<>();

		loadSettings();

		//mockSomeData();

		createMessageEntries();
	}

	private void mockSomeData(){
		messages.clear();
		for (int i = 0; i < 100; i++) {
			messages.add(new Message("Msg" + i, i, i + 1));
		}
	}

	private void setEditTextStrings() {
		EditText editTextUrl = (EditText) findViewById(R.id.editTextUrl);
		EditText editTextUsername = (EditText) findViewById(R.id.editTextUsername);
		EditText editTextPassword = (EditText) findViewById(R.id.editTextPassword);
		assert editTextUrl != null && editTextUsername != null && editTextPassword != null;

		editTextUrl.setText(url);
		editTextUsername.setText(username);
		editTextPassword.setText(password);
	}

	private void getEditTextStrings() {
		EditText editTextUrl = (EditText) findViewById(R.id.editTextUrl);
		EditText editTextUsername = (EditText) findViewById(R.id.editTextUsername);
		EditText editTextPassword = (EditText) findViewById(R.id.editTextPassword);
		assert editTextUrl != null && editTextUsername != null && editTextPassword != null;

		url = editTextUrl.getText().toString();
		if(!url.endsWith("/")){
			url += "/";
		}
		if (!url.startsWith("http://") && !url.startsWith("https://")) {
			url = "http://" + url;
		}
		username = editTextUsername.getText().toString();
		password = editTextPassword.getText().toString();
	}

	private void createMessageEntries(){
		LinearLayout messageList = (LinearLayout) findViewById(R.id.messagesList);

		assert messageList != null;

		messageList.removeAllViews();

		LayoutInflater layoutInflater = getLayoutInflater();

		for (Message message : messages) {
			LinearLayout messageEntry = (LinearLayout) layoutInflater.inflate(R.layout.message_entry, null);
			TextView textViewMessage = (TextView) messageEntry.findViewById(R.id.textViewMessage);
			Button buttonDelete = (Button) messageEntry.findViewById(R.id.buttonDelete);
			assert textViewMessage != null && buttonDelete != null;
			textViewMessage.setText(message.getMessage());
			buttonDelete.setTag(message);
			messageList.addView(messageEntry);
		}
	}

	private void loadSettings(){
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		url = preferences.getString(PREFS_URL, url);
		username = preferences.getString(PREFS_USERNAME, username);
		password = preferences.getString(PREFS_PASSWORD, password);

		messages.clear();
		int messageLength = preferences.getInt(PREFS_MSGS_LENGTH, 0);
		for (int i = 0; i < messageLength; i++) {
			String message = preferences.getString(PREFS_MSGS_MSG_PREFIX + i, "");
			long time = preferences.getLong(PREFS_MSGS_TIME_PREFIX + i, 0);
			long code = preferences.getLong(PREFS_MSGS_CODE_PREFIX + i, 0);
			messages.add(new Message(message, time, code));
		}

		setEditTextStrings();
	}

	public void saveSettings() {
		getEditTextStrings();

		SharedPreferences preferences = getPreferences(MODE_PRIVATE);

		int oldMessagesLength = preferences.getInt(PREFS_MSGS_LENGTH, 0);

		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(PREFS_URL, url);
		editor.putString(PREFS_USERNAME, username);
		editor.putString(PREFS_PASSWORD, password);
		editor.putInt(PREFS_MSGS_LENGTH, messages.size());
		for (int i = 0; i < messages.size(); i++) {
			Message message = messages.get(i);
			editor.putString(PREFS_MSGS_MSG_PREFIX + i, message.getMessage());
			editor.putLong(PREFS_MSGS_TIME_PREFIX + i, message.getTime());
			editor.putLong(PREFS_MSGS_CODE_PREFIX + i, message.getCode());
		}
		for(int i = messages.size(); i < oldMessagesLength; i++){
			editor.remove(PREFS_MSGS_MSG_PREFIX + i);
			editor.remove(PREFS_MSGS_TIME_PREFIX + i);
			editor.remove(PREFS_MSGS_CODE_PREFIX + i);
		}
		editor.apply();
	}

	public void downloadMessages(){
		if(isUpdating){
			return;
		}
		isUpdating = true;
		makeRequestAsync(ENDPOINT_CHECK, null, new RequestCallback() {
			@Override
			public void run(String response) {
				if (response == null) {
					System.out.println("Check: Response was null");
					return;
				}
				messages.clear();
				String[] lines = response.split("\n");
				for (String line : lines) {
					String[] parts = line.split(" ", 3);
					if (parts.length != 3) {
						System.out.println("Line has wrong length: " + line);
						continue;
					}
					messages.add(new Message(parts[2], Long.parseLong(parts[0]), Long.parseLong(parts[1])));
				}
				isUpdating = false;
				createMessageEntries();
				saveSettings();
				Toast.makeText(MainActivity.this, "updated", Toast.LENGTH_SHORT).show();
			}
		}, new RequestCallback() {
			@Override
			public void run(String response) {
				isUpdating = false;
			}
		});
	}

	public void deleteMessage(final View button, Message message){
		if(isDeleting){
			return;
		}
		makeRequestAsync(ENDPOINT_DELETE, "time=" + message.getTime() + "&code=" + message.getCode(), new RequestCallback() {
			@Override
			public void run(String response) {
				if (response == null) {
					System.out.println("Delete: Response was null");
				} else if ("done\n".equals(response)) {
					((ViewGroup) button.getParent().getParent()).removeView((View) button.getParent());
					Toast.makeText(MainActivity.this, "deleted", Toast.LENGTH_SHORT).show();
				}
				isDeleting = false;
			}
		}, new RequestCallback() {
			@Override
			public void run(String response) {
				isDeleting = false;
			}
		});
	}

	private void makeRequestAsync(final String endpoint, final String args, final RequestCallback callback, final RequestCallback callbackError){
		AsyncTask.execute(new Runnable() {
			@Override
			public void run() {
				final String response;
				try {
					response = makeRequest(endpoint, args);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							callback.run(response);
						}
					});
				} catch (final IOException e) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (callbackError != null) {
								callbackError.run(e.getMessage());
							}
							AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
							builder.setTitle("Error").setMessage(e.getMessage()).setIcon(android.R.drawable.ic_dialog_alert);
							builder.setPositiveButton("k", null);
							builder.create().show();
						}
					});
				}
			}
		});
	}

	private String makeRequest(String endpoint, String args) throws IOException {
		try {
			URL connUrl;
			if (args != null && args.length() > 0) {
				connUrl = new URL(url + endpoint + "?" + args);
			} else {
				connUrl = new URL(url + endpoint);
			}

			String authString = "";
			if (username.length() > 0) {
				authString = username + ":" + password;
				byte[] authEncBytes = Base64.encode(authString.getBytes(), Base64.DEFAULT);
				authString = new String(authEncBytes);
			}

			HttpURLConnection conn = (HttpURLConnection) connUrl.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("User-Agent", "VacationNotify on Android");
			if (authString.length() > 0) {
				conn.setRequestProperty("Authorization", "Basic " + authString);
			}

			int responseCode = conn.getResponseCode();

			if(responseCode == 200) {
				String response = readInputStream(conn.getInputStream());
				System.out.println(response);
				return response;
			} else {
				String error = readInputStream(conn.getErrorStream());
				System.out.println(responseCode + " " + error);
				throw new IOException(responseCode + " " + error);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new IOException(e);
		}
	}

	private String readInputStream(InputStream inputStream) throws IOException {
		if (inputStream == null) {
			return "";
		}
		BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
		String inputLine;
		StringBuilder response = new StringBuilder();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
			response.append("\n");
		}
		in.close();
		return response.toString();
	}

	public void onUpdateSettings(View view) {
		saveSettings();
		downloadMessages();
	}

	public void onDeleteMessage(View view) {
		Message message = (Message) view.getTag();
		assert message != null;
		deleteMessage(view, message);
	}
}
