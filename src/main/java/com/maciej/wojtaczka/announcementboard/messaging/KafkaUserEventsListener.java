package com.maciej.wojtaczka.announcementboard.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maciej.wojtaczka.announcementboard.domain.model.User;
import com.maciej.wojtaczka.announcementboard.persistence.UserLocalRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class KafkaUserEventsListener {

	public final static String USER_REGISTERED = "user-registered";
	private final String GROUP_ID = "announcement_board";

	private final UserLocalRepository userLocalRepository;
	private final ObjectMapper objectMapper;

	public KafkaUserEventsListener(UserLocalRepository userLocalRepository, ObjectMapper objectMapper) {
		this.userLocalRepository = userLocalRepository;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = USER_REGISTERED, groupId = GROUP_ID)
	void listenToUserCreated(ConsumerRecord<String, String> consumerRecord) {
		log.debug(USER_REGISTERED + " event received:\n {}", consumerRecord.value());
		String jsonUser = consumerRecord.value();

		try {
			User user = objectMapper.readValue(jsonUser, User.class);
			userLocalRepository.saveUser(user);
			log.info("User {} saved", user.getId());
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
