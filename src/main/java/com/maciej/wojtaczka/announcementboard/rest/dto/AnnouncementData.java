package com.maciej.wojtaczka.announcementboard.rest.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AnnouncementData {

	private final UUID author;
	private final String content;
}
