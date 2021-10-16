package com.maciej.wojtaczka.announcementboard.rest;

import com.maciej.wojtaczka.announcementboard.domain.AnnouncementBoardService;
import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import com.maciej.wojtaczka.announcementboard.rest.dto.AnnouncementData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class AnnouncementBoardController {

	public final static String ANNOUNCEMENTS_URL = "/v1/announcements";

	private final AnnouncementBoardService announcementBoardService;

	public AnnouncementBoardController(AnnouncementBoardService announcementBoardService) {
		this.announcementBoardService = announcementBoardService;
	}

	@PostMapping(ANNOUNCEMENTS_URL)
	ResponseEntity<Announcement> publishAnnouncement(@RequestBody AnnouncementData announcementData) {

		Announcement announcement = announcementBoardService.publishAnnouncement(announcementData.getAuthor(), announcementData.getContent());

		String resourceLocation = ANNOUNCEMENTS_URL + "/" + announcement.getAnnouncerId() + "/" + announcement.getCreationTime().toEpochMilli();

		return ResponseEntity.created(URI.create(resourceLocation))
							 .body(announcement);
	}

}
