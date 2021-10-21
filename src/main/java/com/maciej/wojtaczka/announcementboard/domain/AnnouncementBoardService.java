package com.maciej.wojtaczka.announcementboard.domain;

import com.maciej.wojtaczka.announcementboard.domain.exception.UserException;
import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import com.maciej.wojtaczka.announcementboard.domain.model.User;
import com.maciej.wojtaczka.announcementboard.domain.query.AnnouncementQuery;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;

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

	public Announcement publishAnnouncement(UUID authorId, String content) {

		User announcer = userService.fetchUser(authorId)
									.orElseThrow(() -> UserException.userNotFound(authorId));

		Announcement announcement = announcer.publishAnnouncement(content);

		Announcement savedAnnouncement = repository.save(announcement);

		announcer.getDomainEvents()
				 .forEach(domainEventPublisher::publish);

		return savedAnnouncement;
	}

	public List<AnnouncementQuery.Result> fetchAll(List<AnnouncementQuery> queries) {

		Map<UUID, Instant> authorIdToFromTime =
				queries.stream()
					   .collect(groupingBy(AnnouncementQuery::getAuthorId,
										   reducing(Instant.now(), AnnouncementQuery::getCreationTime, this::getOlder)));

		return repository.fetchAll(authorIdToFromTime).stream()
						 .collect(groupingBy(Announcement::getAuthorId))
						 .entrySet().stream()
						 .map(AnnouncementQuery.Result::of)
						 .collect(Collectors.toList());
	}

	private Instant getOlder(Instant a, Instant b) {
		return a.isBefore(b) ? a : b;
	}
}
