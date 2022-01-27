package com.maciej.wojtaczka.announcementboard.domain;

import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import com.maciej.wojtaczka.announcementboard.domain.query.AnnouncementQuery;

import java.util.List;
import java.util.Optional;

public interface AnnouncementCache {

	void saveAll(List<Announcement> announcements);

	List<Announcement> get(List<AnnouncementQuery> queries);

	Optional<Announcement> getOne(AnnouncementQuery query);
}
