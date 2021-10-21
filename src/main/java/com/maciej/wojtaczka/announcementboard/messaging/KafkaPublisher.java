package com.maciej.wojtaczka.announcementboard.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maciej.wojtaczka.announcementboard.domain.DomainEvent;
import com.maciej.wojtaczka.announcementboard.domain.DomainEventPublisher;
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

	@Override
	public void publish(DomainEvent<?> domainEvent) {
		try {
			String jsonPayload = objectMapper.writeValueAsString(domainEvent.getPayload());
			kafkaTemplate.send(domainEvent.getDestination(), jsonPayload);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Exception during json marshaling", e);
		}
	}
}
