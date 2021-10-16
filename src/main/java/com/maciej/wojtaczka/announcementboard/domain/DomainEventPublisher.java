package com.maciej.wojtaczka.announcementboard.domain;

public interface DomainEventPublisher {

	void publish(DomainEvent<?> domainEvent);
}
