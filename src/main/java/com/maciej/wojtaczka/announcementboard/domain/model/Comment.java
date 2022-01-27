package com.maciej.wojtaczka.announcementboard.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Builder
@Value
public class Comment {

	UUID announcementAuthorId;
	Instant announcementCreationTime;

    UUID authorId;
	String authorNickname;
    String content;
    Instant creationTime;
}
