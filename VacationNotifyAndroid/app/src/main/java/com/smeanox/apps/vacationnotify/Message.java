package com.smeanox.apps.vacationnotify;

public class Message {
	private String message;
	private long time, code;

	public Message(String message, long time, long code) {
		this.message = message;
		this.time = time;
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getCode() {
		return code;
	}

	public void setCode(long code) {
		this.code = code;
	}
}
