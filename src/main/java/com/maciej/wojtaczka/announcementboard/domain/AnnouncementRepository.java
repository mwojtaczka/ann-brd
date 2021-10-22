package com.maciej.wojtaczka.announcementboard.domain;

import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AnnouncementRepository {

	Announcement save(Announcement announcement);

	List<Announcement> fetchAll(Map<UUID, List<Instant>> authorIdToCreationTimes);
}
