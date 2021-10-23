package com.maciej.wojtaczka.announcementboard.domain.exception;

import java.time.Instant;
import java.util.UUID;

public class AnnouncementException extends RuntimeException {

	public AnnouncementException(String message) {
		super(message);
	}

	public static AnnouncementException notFound(UUID userId, Instant instant) {
		return new AnnouncementException(String.format("Announcement from user with id: %s created at %s not found", userId, instant));
	}
}
