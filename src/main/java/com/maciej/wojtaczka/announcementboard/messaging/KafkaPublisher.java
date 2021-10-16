package com.maciej.wojtaczka.announcementboard.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maciej.wojtaczka.announcementboard.domain.DomainEvent;
import com.maciej.wojtaczka.announcementboard.domain.DomainEventPublisher;
import lombok.SneakyThrows;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class KafkaPublisher implements DomainEventPublisher {

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	KafkaPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
		this.kafkaTemplate = kafkaTemplate;
		this.objectMapper = objectMapper;
	}

	@SneakyThrows
	@Override
	public void publish(DomainEvent<?> domainEvent) {
		String jsonPayload = objectMapper.writeValueAsString(domainEvent.getPayload());

		kafkaTemplate.send(domainEvent.getDestination(), jsonPayload);
	}
}
