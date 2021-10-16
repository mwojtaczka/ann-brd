package com.maciej.wojtaczka.announcementboard.domain;

import com.maciej.wojtaczka.announcementboard.domain.exception.UserException;
import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import com.maciej.wojtaczka.announcementboard.domain.model.User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AnnouncementBoardService {

	private final UserService userService;
	private final DomainEventPublisher domainEventPublisher;
	private final AnnouncementRepository repository;

	public AnnouncementBoardService(UserService userService, DomainEventPublisher domainEventPublisher,
									AnnouncementRepository repository) {
		this.userService = userService;
		this.domainEventPublisher = domainEventPublisher;
		this.repository = repository;
	}

	public Announcement publishAnnouncement(UUID announcerId, String content) {

		User announcer = userService.fetchUser(announcerId)
									.orElseThrow(() -> UserException.userNotFound(announcerId));

		Announcement announcement = announcer.publishAnnouncement(content);

		Announcement savedAnnouncement = repository.save(announcement);

		announcer.getDomainEvents()
				 .forEach(domainEventPublisher::publish);

		return savedAnnouncement;
	}
}
