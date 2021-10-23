package com.maciej.wojtaczka.announcementboard.rest;

import com.maciej.wojtaczka.announcementboard.domain.AnnouncementBoardService;
import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import com.maciej.wojtaczka.announcementboard.domain.query.AnnouncementQuery;
import com.maciej.wojtaczka.announcementboard.rest.dto.AnnouncementData;
import com.maciej.wojtaczka.announcementboard.rest.dto.CommentData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
public class AnnouncementBoardController {

	public final static String ANNOUNCEMENTS_URL = "/v1/announcements";
	public final static String ANNOUNCEMENTS_FETCH_URL = "/v1/announcements/fetch";

	private final AnnouncementBoardService announcementBoardService;

	public AnnouncementBoardController(AnnouncementBoardService announcementBoardService) {
		this.announcementBoardService = announcementBoardService;
	}

	@PostMapping(ANNOUNCEMENTS_URL)
	ResponseEntity<Announcement> publishAnnouncement(@RequestBody AnnouncementData announcementData) {

		Announcement announcement = announcementBoardService.publishAnnouncement(announcementData.getAuthor(), announcementData.getContent());

		String resourceLocation = ANNOUNCEMENTS_URL + "/" + announcement.getAuthorId() + "/" + announcement.getCreationTime().toEpochMilli();

		return ResponseEntity.created(URI.create(resourceLocation))
							 .body(announcement);
	}

	@PostMapping(ANNOUNCEMENTS_URL + "/{announcementAuthorId}/{announcementCreationTimeMillis}")
	ResponseEntity<Void> placeComment(@PathVariable UUID announcementAuthorId,
											  @PathVariable Long announcementCreationTimeMillis,
											  @RequestBody CommentData commentData) {

		announcementBoardService.placeComment(
				commentData.getAuthorId(),
				commentData.getContent(),
				announcementAuthorId,
				Instant.ofEpochMilli(announcementCreationTimeMillis));

		return ResponseEntity.ok().build();
	}

	//TODO: Replace with gRPC
	@PostMapping(ANNOUNCEMENTS_FETCH_URL)
	ResponseEntity<List<AnnouncementQuery.Result>> fetchAllAnnouncement(@RequestBody List<AnnouncementQuery> announcementQueries) {
		List<AnnouncementQuery.Result> announcements = announcementBoardService.fetchAll(announcementQueries);

		return ResponseEntity.ok(announcements);
	}

}
