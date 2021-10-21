package com.maciej.wojtaczka.announcementboard.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maciej.wojtaczka.announcementboard.config.CassandraConfig;
import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import com.maciej.wojtaczka.announcementboard.domain.model.User;
import com.maciej.wojtaczka.announcementboard.persistence.entity.AnnouncementDbEntity;
import com.maciej.wojtaczka.announcementboard.rest.dto.AnnouncementData;
import com.maciej.wojtaczka.announcementboard.util.KafkaTestListener;
import com.maciej.wojtaczka.announcementboard.util.UserFixtures;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static com.maciej.wojtaczka.announcementboard.rest.AnnouncementBoardController.ANNOUNCEMENTS_URL;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@WebAppConfiguration
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@Import({ CassandraConfig.class })
class AnnouncementBoardControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CassandraOperations cassandraOperations;

	@Autowired
	private KafkaTestListener kafkaTestListener;

	@Autowired
	private UserFixtures $;

	@BeforeEach
	void setupEach() {
		kafkaTestListener.reset();
	}

	@Test
	void shouldCreateAnnouncementAndEmitEvent() throws Exception {
		//given
		UUID announcerId = UUID.randomUUID();
		$.user().withId(announcerId).exists();
		String announcementContent = "Hello world";

		AnnouncementData requestBody = AnnouncementData.builder()
													   .author(announcerId)
													   .content(announcementContent)
													   .build();

		//when
		ResultActions result = mockMvc.perform(post(ANNOUNCEMENTS_URL)
													   .content(asJsonString(requestBody))
													   .contentType(APPLICATION_JSON)
													   .accept(APPLICATION_JSON));

		//then
		//verify response
		String jsonAnnouncement = result.andExpect(status().isCreated())
										.andExpect(jsonPath("$.authorId", equalTo(announcerId.toString())))
										.andExpect(jsonPath("$.content", equalTo(announcementContent)))
										.andExpect(jsonPath("$.creationTime", notNullValue()))
										.andReturn().getResponse().getContentAsString();

		//verify persistence
		Announcement announcement = objectMapper.readValue(jsonAnnouncement, Announcement.class);

		AnnouncementDbEntity announcementDbEntity = cassandraOperations.selectOne(
				String.format("select * from announcement_board.announcement where author_id = %s and creation_time = %s",
							  announcerId,
							  announcement.getCreationTime().toEpochMilli()),
				AnnouncementDbEntity.class
		);

		assertThat(announcementDbEntity).isNotNull();
		assertAll(
				() -> assertThat(announcementDbEntity.getAuthorId()).isEqualTo(announcerId),
				() -> assertThat(announcementDbEntity.getCreationTime()).isEqualTo(announcement.getCreationTime().truncatedTo(MILLIS)),
				() -> assertThat(announcementDbEntity.getContent()).isEqualTo(announcementContent)
		);

		//verify publishing event
		String capturedEvent = kafkaTestListener.receiveFirstContentFromTopic(User.DomainEvents.ANNOUNCEMENT_PUBLISHED)
												.orElseThrow(() -> new RuntimeException("No event"));
		JSONAssert.assertEquals(jsonAnnouncement, capturedEvent, false);
		assertThat(kafkaTestListener.noMoreMessagesOnTopic(User.DomainEvents.ANNOUNCEMENT_PUBLISHED, 50)).isTrue();
	}

	@SneakyThrows
	private String asJsonString(final Object obj) {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(obj);
	}
}
