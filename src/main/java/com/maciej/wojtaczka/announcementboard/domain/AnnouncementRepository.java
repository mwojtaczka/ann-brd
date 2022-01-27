package com.maciej.wojtaczka.announcementboard.domain;

import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import com.maciej.wojtaczka.announcementboard.domain.model.Comment;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface AnnouncementRepository {

	Announcement save(Announcement announcement);

	void saveAnnouncementComment(Comment comment);

	List<Announcement> fetchAll(Map<UUID, List<Instant>> authorIdToCreationTimes);

	Optional<Announcement> fetchOne(UUID authorId, Instant creationTime);
}
