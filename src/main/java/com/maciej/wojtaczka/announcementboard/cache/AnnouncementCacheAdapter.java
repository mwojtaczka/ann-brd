package com.maciej.wojtaczka.announcementboard.cache;

import com.maciej.wojtaczka.announcementboard.cache.entry.AnnouncementEntry;
import com.maciej.wojtaczka.announcementboard.domain.AnnouncementCache;
import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import com.maciej.wojtaczka.announcementboard.domain.query.AnnouncementQuery;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.StreamSupport;

import static com.maciej.wojtaczka.announcementboard.cache.entry.AnnouncementEntry.createId;
import static java.util.stream.Collectors.toList;

@Component
public class AnnouncementCacheAdapter implements AnnouncementCache {

	private final AnnouncementRedisCache repository;

	public AnnouncementCacheAdapter(AnnouncementRedisCache repository) {
		this.repository = repository;
	}

	@Override
	public void saveAll(List<Announcement> announcements) {

		List<AnnouncementEntry> entries = announcements.stream()
													   .map(AnnouncementEntry::from)
													   .collect(toList());

		repository.saveAll(entries);
	}

	@Override
	public List<Announcement> get(List<AnnouncementQuery> queries) {

		List<String> ids = queries.stream()
								  .map(q -> createId(q.getAuthorId(), q.getCreationTime()))
								  .collect(toList());

		return StreamSupport.stream(repository.findAllById(ids).spliterator(), false)
							.map(AnnouncementEntry::toModel)
							.collect(toList());
	}

}
