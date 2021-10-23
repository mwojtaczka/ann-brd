package com.maciej.wojtaczka.announcementboard.domain.model;

import com.maciej.wojtaczka.announcementboard.domain.DomainEvent;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.time.Instant;
import java.util.ArrayList;
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
												.authorId(id)
												.content(content)
												.creationTime(Instant.now())
												.comments(new ArrayList<>())
												.build();

		addEventToPublish(DomainEvents.announcementPublished(announcement));

		return announcement;
	}

	public void commentAnnouncement(String commentContent, Announcement announcement) {

		Comment comment = Comment.builder()
								 .authorId(id)
								 .authorNickname(nickname)
								 .content(commentContent)
								 .creationTime(Instant.now())
								 .build();

		announcement.placeComment(comment);

		addEventToPublish(DomainEvents.announcementCommented(announcement, comment));
	}

	public static class DomainEvents {
		public static final String ANNOUNCEMENT_PUBLISHED = "announcement-published";
		public static final String ANNOUNCEMENT_COMMENTED = "announcement-commented";

		static DomainEvent<Announcement> announcementPublished(Announcement announcement) {
			return new DomainEvent<>(ANNOUNCEMENT_PUBLISHED, announcement);
		}

		static DomainEvent<AnnouncementCommented> announcementCommented(Announcement announcement, Comment comment) {
			AnnouncementCommented event = AnnouncementCommented.builder()
															   .announcementAuthorId(announcement.getAuthorId())
															   .announcementCreationTime(announcement.getCreationTime())
															   .comment(comment)
															   .build();
			return new DomainEvent<>(ANNOUNCEMENT_COMMENTED, event);
		}
	}

	@Builder
	@Value
	public static class AnnouncementCommented {
		UUID announcementAuthorId;
		Instant announcementCreationTime;
		Comment comment;
	}

}
