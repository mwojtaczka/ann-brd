package com.maciej.wojtaczka.announcementboard.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maciej.wojtaczka.announcementboard.domain.model.User;
import com.maciej.wojtaczka.announcementboard.persistence.UserLocalRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaUserEventsListener {

	public final static String USER_CREATED = "user-created";
	private final String GROUP_ID = "announcement_board";

	private final UserLocalRepository userLocalRepository;
	private final ObjectMapper objectMapper;

	public KafkaUserEventsListener(UserLocalRepository userLocalRepository, ObjectMapper objectMapper) {
		this.userLocalRepository = userLocalRepository;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = USER_CREATED, groupId = GROUP_ID)
	void listenToUserCreated(ConsumerRecord<String, String> consumerRecord) {

		String jsonUser = consumerRecord.value();

		try {
			User user = objectMapper.readValue(jsonUser, User.class);
			userLocalRepository.saveUser(user);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
