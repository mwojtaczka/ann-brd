package com.maciej.wojtaczka.announcementboard.domain.dto;

import lombok.Value;

import java.util.Set;
import java.util.UUID;

@Value
public class Envelope <T> {

	Set<UUID> recipients;
	T payload;
}
