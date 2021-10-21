package com.maciej.wojtaczka.announcementboard.domain.model;

import com.maciej.wojtaczka.announcementboard.domain.DomainEvent;
import org.junit.jupiter.api.Test;

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
		assertAll(
				() -> assertThat(domainEvents).hasSize(1),
				() -> assertThat(domainEvents.get(0).getDestination()).isEqualTo("announcement-published"),
				() -> assertThat(domainEvents.get(0).getPayload()).isEqualTo(announcement)
		);
	}

}
