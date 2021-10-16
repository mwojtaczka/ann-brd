package com.maciej.wojtaczka.announcementboard.domain.model;

import com.maciej.wojtaczka.announcementboard.domain.DomainEvent;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
@Builder
@Value
public class User extends DomainModel {

	UUID id;
	String nickname;
	String name;
	String surname;


	public Announcement publishAnnouncement(String content) {

		Announcement announcement = Announcement.builder()
												.announcerId(id)
												.content(content)
												.creationTime(Instant.now())
												.build();

		addEventToPublish(DomainEvents.postPublished(announcement));
		
		return announcement;
	}

	public static class DomainEvents {
		public static final String ANNOUNCEMENT_PUBLISHED = "announcement-published";

		static DomainEvent<Announcement> postPublished(Announcement announcement) {
			return new DomainEvent<>(ANNOUNCEMENT_PUBLISHED, announcement);
		}
	}
}
