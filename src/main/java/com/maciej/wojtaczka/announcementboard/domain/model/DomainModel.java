package com.maciej.wojtaczka.announcementboard.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.maciej.wojtaczka.announcementboard.domain.DomainEvent;

import java.util.ArrayList;
import java.util.List;


abstract class DomainModel {

	private final transient List<DomainEvent<?>> eventsToPublish = new ArrayList<>();


	@JsonIgnore
	public List<DomainEvent<?>> getDomainEvents() {
		return List.copyOf(eventsToPublish);
	}

	protected void addEventToPublish(DomainEvent<?> event) {
		eventsToPublish.add(event);
	}
}
