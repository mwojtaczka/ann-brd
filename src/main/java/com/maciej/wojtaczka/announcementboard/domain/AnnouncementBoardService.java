package com.maciej.wojtaczka.announcementboard.domain;

import com.maciej.wojtaczka.announcementboard.domain.exception.AnnouncementException;
import com.maciej.wojtaczka.announcementboard.domain.exception.UserException;
import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import com.maciej.wojtaczka.announcementboard.domain.model.User;
import com.maciej.wojtaczka.announcementboard.domain.query.AnnouncementQuery;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Service
public class AnnouncementBoardService {

	private final UserService userService;
	private final DomainEventPublisher domainEventPublisher;
	private final AnnouncementRepository repository;
	private final AnnouncementCache cache;

	public AnnouncementBoardService(UserService userService,
									DomainEventPublisher domainEventPublisher,
									AnnouncementRepository repository,
									AnnouncementCache cache) {
		this.userService = userService;
		this.domainEventPublisher = domainEventPublisher;
		this.repository = repository;
		this.cache = cache;
	}

	public Announcement publishAnnouncement(UUID authorId, String content) {

		User announcer = userService.fetchUser(authorId)
									.orElseThrow(() -> UserException.notFound(authorId));

		Announcement announcement = announcer.publishAnnouncement(content);

		Announcement savedAnnouncement = repository.save(announcement);

		announcer.getDomainEvents()
				 .forEach(domainEventPublisher::publish);

		return savedAnnouncement;
	}

	public void placeComment(UUID commentAuthorId,
							 String commentContent,
							 UUID announcementAuthorId,
							 Instant announcementCreationTime) {

		User commenter = userService.fetchUser(commentAuthorId)
									.orElseThrow(() -> UserException.notFound(commentAuthorId));

		Announcement announcement = repository.fetchOne(announcementAuthorId, announcementCreationTime)
											  .orElseThrow(() -> AnnouncementException.notFound(announcementAuthorId, announcementCreationTime));

		commenter.commentAnnouncement(commentContent, announcement);

		repository.save(announcement);

		commenter.getDomainEvents()
				 .forEach(domainEventPublisher::publish);
	}

	public List<AnnouncementQuery.Result> fetchAll(List<AnnouncementQuery> queries) {

		Map<AnnouncementQuery, Announcement> cached = cacheLookup(queries);

		List<AnnouncementQuery> queriesForDb = queries.stream()
													  .filter(q -> !cached.containsKey(q))
													  .collect(toList());

		Map<UUID, List<Instant>> authorIdToCreationTimes =
				queriesForDb.stream()
							.collect(groupingBy(AnnouncementQuery::getAuthorId,
												mapping(AnnouncementQuery::getCreationTime, toList())));

		List<Announcement> fromRepo = repository.fetchAll(authorIdToCreationTimes);
		cache.saveAll(fromRepo);

		return Stream.concat(
				cached.values().stream(), fromRepo.stream())
					 .distinct()
					 .collect(groupingBy(Announcement::getAuthorId))
					 .entrySet().stream()
					 .map(AnnouncementQuery.Result::of)
					 .collect(toList());
	}

	private Map<AnnouncementQuery, Announcement> cacheLookup(List<AnnouncementQuery> queries) {
		return cache.get(queries).stream()
					.collect(Collectors.toMap(
							announcement -> AnnouncementQuery.builder()
															 .authorId(announcement.getAuthorId())
															 .creationTime(announcement.getCreationTime())
															 .build(),
							Function.identity()
					));
	}
}
