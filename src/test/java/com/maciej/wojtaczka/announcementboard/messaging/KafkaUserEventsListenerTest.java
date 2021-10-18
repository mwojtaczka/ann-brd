package com.maciej.wojtaczka.announcementboard.messaging;

import com.maciej.wojtaczka.announcementboard.config.CassandraConfig;
import com.maciej.wojtaczka.announcementboard.persistence.entity.UserDbEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@WebAppConfiguration
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@Import({ CassandraConfig.class })
class KafkaUserEventsListenerTest {

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	private CassandraOperations cassandraOperations;

	@Test
	void shouldPersistNewUser() throws Exception {
		//given
		String jsonPayload = "{\n" +
				"   \"id\":\"ab91aac7-e4a3-4801-950b-7b4dc3405313\",\n" +
				"   \"nickname\":\"DefaultNickname\",\n" +
				"   \"name\":\"DefaultName\",\n" +
				"   \"surname\":\"DefaultSurname\",\n" +
				"   \"contactDetails\":{\n" +
				"      \"id\":\"ab91aac7-e4a3-4801-950b-7b4dc3405313\",\n" +
				"      \"email\":\"default@email.com\",\n" +
				"      \"phoneNumber\":\"999999999\"\n" +
				"   }\n" +
				"}";

		//when
		ListenableFuture<SendResult<String, String>> sent = kafkaTemplate.send(KafkaUserEventsListener.USER_CREATED, jsonPayload);
		sent.get();

		//then
		Thread.sleep(200);
		UserDbEntity userDbEntity = cassandraOperations.selectOne(
				"select * from announcement_board.user where id = ab91aac7-e4a3-4801-950b-7b4dc3405313",
				UserDbEntity.class);

		assertThat(userDbEntity).isNotNull();
		assertAll(
				() -> assertThat(userDbEntity.getId()).isEqualTo(UUID.fromString( "ab91aac7-e4a3-4801-950b-7b4dc3405313")),
				() -> assertThat(userDbEntity.getName()).isEqualTo("DefaultName"),
				() -> assertThat(userDbEntity.getSurname()).isEqualTo("DefaultSurname"),
				() -> assertThat(userDbEntity.getNickname()).isEqualTo("DefaultNickname")
		);
	}

}
