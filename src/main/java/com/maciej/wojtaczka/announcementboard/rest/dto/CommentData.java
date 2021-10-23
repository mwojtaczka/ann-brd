package com.maciej.wojtaczka.announcementboard.rest.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CommentData {

	private final UUID authorId;
	private final String content;
}
