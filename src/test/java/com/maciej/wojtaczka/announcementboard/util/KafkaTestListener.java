package com.maciej.wojtaczka.announcementboard.util;

import com.maciej.wojtaczka.announcementboard.domain.model.User;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class KafkaTestListener {

	private Map<String, ConcurrentLinkedQueue<ConsumerRecord<String, String>>> recordsPerTopic;
	private Map<String, CountDownLatch> latchPerTopic;

	public KafkaTestListener() {
		recordsPerTopic = new HashMap<>();
		latchPerTopic = new HashMap<>();
		prepareForTopics(User.DomainEvents.ANNOUNCEMENT_PUBLISHED);
	}

	private void prepareForTopics(String... topics) {
		for (String topic : topics) {
			recordsPerTopic.put(topic, new ConcurrentLinkedQueue<>());
			latchPerTopic.put(topic, new CountDownLatch(1));
		}
	}

	public void reset() {
		recordsPerTopic.forEach((k, v) -> v.clear());
		latchPerTopic = latchPerTopic.entrySet().stream()
									 .collect(Collectors.toMap(Map.Entry::getKey, entry -> new CountDownLatch(1)));
	}

	@KafkaListener(topics = User.DomainEvents.ANNOUNCEMENT_PUBLISHED, groupId = "test")
	void receiveAnnouncementPublished(ConsumerRecord<String, String> consumerRecord) {

		ConcurrentLinkedQueue<ConsumerRecord<String, String>> announcementPublishedRecords =
				recordsPerTopic.get(User.DomainEvents.ANNOUNCEMENT_PUBLISHED);

		announcementPublishedRecords.add(consumerRecord);

		latchPerTopic.get(User.DomainEvents.ANNOUNCEMENT_PUBLISHED).countDown();
	}

	@SneakyThrows
	public Optional<String> receiveFirstContentFromTopic(String topic) {
		latchPerTopic.get(topic).await(200, TimeUnit.MILLISECONDS);
		ConsumerRecord<String, String> firstMessage = recordsPerTopic.get(topic).poll();
		if (firstMessage == null) {
			return Optional.empty();
		}
		return Optional.of(firstMessage.value());
	}

	@SneakyThrows
	public boolean noMoreMessagesOnTopic(String topic, long awaitTimeMillis) {
		Thread.sleep(awaitTimeMillis);
		return recordsPerTopic.get(topic).isEmpty();
	}
}
