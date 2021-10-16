package com.maciej.wojtaczka.announcementboard.rest.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AnnouncementData {

	UUID author;
	String content;
}
