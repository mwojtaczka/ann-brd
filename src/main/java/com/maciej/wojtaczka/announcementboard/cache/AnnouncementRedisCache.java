package com.maciej.wojtaczka.announcementboard.cache;

import com.maciej.wojtaczka.announcementboard.cache.entry.AnnouncementEntry;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnouncementRedisCache extends CrudRepository<AnnouncementEntry, String> {

}
