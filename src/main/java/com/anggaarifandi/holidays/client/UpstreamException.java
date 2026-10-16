package com.anggaarifandi.holidays.client;

public class UpstreamException extends RuntimeException {

	public UpstreamException(String message) {
		super(message);
	}

	public UpstreamException(String message, Throwable cause) {
		super(message, cause);
	}

}
