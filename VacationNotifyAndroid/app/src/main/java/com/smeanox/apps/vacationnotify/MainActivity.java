package com.smeanox.apps.vacationnotify;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
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
				for(String line : lines) {
					String[] parts = line.split(" ");
					if (parts.length != 3) {
						System.out.println("Line has wrong length: " + line);
						continue;
					}
					messages.add(new Message(parts[2], Long.parseLong(parts[0]), Long.parseLong(parts[1])));
				}
				isUpdating = false;
				createMessageEntries();
				saveSettings();
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
				} else if("done\n".equals(response)){
					((ViewGroup) button.getParent().getParent()).removeView((View) button.getParent());
				}
				isDeleting = false;
			}
		});
	}

	private void makeRequestAsync(final String endpoint, final String args, final RequestCallback callback){
		AsyncTask.execute(new Runnable() {
			@Override
			public void run() {
				final String response = makeRequest(endpoint, args);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						callback.run(response);
					}
				});
			}
		});
	}

	private String makeRequest(String endpoint, String args) {
		try {
			URL connUrl;
			if (args != null && args.length() > 0) {
				connUrl = new URL(url + endpoint + "?" + args);
			} else {
				connUrl = new URL(url + endpoint);
			}
			HttpURLConnection conn = (HttpURLConnection) connUrl.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("User-Agent", "VacationNotify on Android");

			int responseCode = conn.getResponseCode();
			System.out.println(endpoint + ": " + responseCode);
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
				response.append("\n");
			}
			in.close();
			System.out.println(response.toString());
			return response.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
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
