package com.maciej.wojtaczka.announcementboard.domain.model;

import com.maciej.wojtaczka.announcementboard.domain.DomainEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class UserTest {

	@Test
	void shouldCreateAnnouncement() {
		//given
		UUID announcerId = UUID.randomUUID();
		User announcer = User.builder()
							 .id(announcerId)
							 .build();

		//when
		Announcement announcement = announcer.publishAnnouncement("Hello world");

		//then
		assertAll(
				() -> assertThat(announcement.getAuthorId()).isEqualTo(announcerId),
				() -> assertThat(announcement.getCreationTime()).isNotNull()
		);
	}

	@Test
	void shouldCreateAnnouncementPublishedEvent() {
		//given
		UUID announcerId = UUID.randomUUID();
		User announcer = User.builder()
							 .id(announcerId)
							 .build();

		//when
		Announcement announcement = announcer.publishAnnouncement("Hello world");

		//then
		List<DomainEvent<?>> domainEvents = announcer.getDomainEvents();
		assertThat(domainEvents).hasSize(1);
		assertAll(
				() -> assertThat(domainEvents.get(0).getDestination()).isEqualTo("announcement-published"),
				() -> assertThat(domainEvents.get(0).getPayload()).isEqualTo(announcement)
		);
	}

	@Test
	void shouldCommentAnnouncement() {
		//given
		UUID announcerId = UUID.randomUUID();
		User commenter = User.builder()
							 .id(UUID.randomUUID())
							 .nickname("Nick")
							 .build();
		Announcement announcement = Announcement.builder()
												.authorId(announcerId)
												.content("Hello world")
												.creationTime(Instant.parse("2007-12-03T10:15:30.00Z"))
												.build();

		//when
		Comment comment = commenter.commentAnnouncement("Hello", announcement);

		//then
		assertThat(announcement.getCommentsCount()).isEqualTo(1);
		assertAll(
				() -> assertThat(comment.getAnnouncementAuthorId()).isEqualTo(announcerId),
				() -> assertThat(comment.getAnnouncementCreationTime()).isEqualTo(Instant.parse("2007-12-03T10:15:30.00Z")),
				() -> assertThat(comment.getAuthorId()).isEqualTo(commenter.getId()),
				() -> assertThat(comment.getAuthorNickname()).isEqualTo(commenter.getNickname()),
				() -> assertThat(comment.getContent()).isEqualTo("Hello"),
				() -> assertThat(comment.getCreationTime()).isNotNull()
		);
	}

	@Test
	void shouldCreateAnnouncementCommentedEvent() {
		//given
		UUID announcerId = UUID.randomUUID();
		User commenter = User.builder()
							 .id(UUID.randomUUID())
							 .nickname("Nick")
							 .build();
		Announcement announcement = Announcement.builder()
												.authorId(announcerId)
												.content("Hello world")
												.creationTime(Instant.parse("2007-12-03T10:15:30.00Z"))
												.build();

		//when
		Comment placedComment = commenter.commentAnnouncement("Hello", announcement);

		//then
		List<DomainEvent<?>> domainEvents = commenter.getDomainEvents();
		assertThat(domainEvents).hasSize(1);
		assertThat(domainEvents.get(0).getDestination()).isEqualTo("announcement-commented");

		User.AnnouncementCommented payload = (User.AnnouncementCommented) domainEvents.get(0).getPayload();
		assertThat(payload.getAnnouncementAuthorId()).isEqualTo(announcerId);
		assertThat(payload.getAnnouncementCreationTime()).isEqualTo(Instant.parse("2007-12-03T10:15:30.00Z"));
		assertThat(payload.getComment()).isEqualTo(placedComment);
	}

}
