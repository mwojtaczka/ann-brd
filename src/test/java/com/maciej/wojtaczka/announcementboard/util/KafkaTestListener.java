package com.maciej.wojtaczka.announcementboard.util;

import com.maciej.wojtaczka.announcementboard.domain.model.User;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@ConditionalOnBean(EmbeddedKafkaBroker.class)
public class KafkaTestListener implements DisposableBean {

	private final EmbeddedKafkaBroker broker;

	private final Map<String, ConcurrentLinkedQueue<ConsumerRecord<String, String>>> recordsPerTopic;
	private Map<String, CountDownLatch> latchPerTopic;
	private final Set<KafkaMessageListenerContainer<String, String>> containers = new HashSet<>();


	public KafkaTestListener(EmbeddedKafkaBroker broker) {
		this.broker = broker;
		recordsPerTopic = new HashMap<>();
		latchPerTopic = new HashMap<>();
		prepareForTopics(User.DomainEvents.ANNOUNCEMENT_PUBLISHED);
	}

	private void prepareForTopics(String... topics) {
		for (String topic : topics) {
			recordsPerTopic.put(topic, new ConcurrentLinkedQueue<>());
			latchPerTopic.put(topic, new CountDownLatch(1));
			setupContainer(topic);
		}
	}

	private void setupContainer(String topic) {
		ContainerProperties containerProperties = new ContainerProperties(topic);
		Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), "false", broker);
		DefaultKafkaConsumerFactory<String, String> consumer = new DefaultKafkaConsumerFactory<>(consumerProperties);
		var container = new KafkaMessageListenerContainer<>(consumer, containerProperties);
		container.setupMessageListener((MessageListener<String, String>) record -> consume(record, topic));
		container.start();
		containers.add(container);
		ContainerTestUtils.waitForAssignment(container, broker.getPartitionsPerTopic());
	}

	public void reset() {
		recordsPerTopic.forEach((k, v) -> v.clear());
		latchPerTopic = latchPerTopic.entrySet().stream()
									 .collect(Collectors.toMap(Map.Entry::getKey, entry -> new CountDownLatch(1)));
	}

	void consume(ConsumerRecord<String, String> consumerRecord, String topic) {
		ConcurrentLinkedQueue<ConsumerRecord<String, String>> records = recordsPerTopic.get(topic);
		records.add(consumerRecord);
		latchPerTopic.get(topic).countDown();
	}

	@SneakyThrows
	public Optional<String> receiveFirstContentFromTopic(String topic) {
		latchPerTopic.get(topic).await(2000, TimeUnit.MILLISECONDS);
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

	@Override
	public void destroy() {
		containers.forEach(KafkaMessageListenerContainer::stop);
	}
}
