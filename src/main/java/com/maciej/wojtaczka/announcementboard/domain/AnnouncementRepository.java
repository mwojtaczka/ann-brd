package com.maciej.wojtaczka.announcementboard.domain;

import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;

public interface AnnouncementRepository {

	Announcement save(Announcement announcement);
}
