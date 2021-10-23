package com.maciej.wojtaczka.announcementboard.domain.exception;

import java.util.UUID;

public class UserException extends RuntimeException {

	public UserException(String message) {
		super(message);
	}

	public static UserException notFound(UUID userId) {
		return new UserException(String.format("User with id: %s not found", userId));
	}
}
