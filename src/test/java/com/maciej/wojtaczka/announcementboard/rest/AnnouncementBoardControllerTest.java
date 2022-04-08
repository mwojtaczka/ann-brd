package com.maciej.wojtaczka.announcementboard.rest;

import com.datastax.oss.driver.api.core.CqlSession;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maciej.wojtaczka.announcementboard.domain.AnnouncementCache;
import com.maciej.wojtaczka.announcementboard.domain.dto.Envelope;
import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import com.maciej.wojtaczka.announcementboard.domain.model.User;
import com.maciej.wojtaczka.announcementboard.domain.query.AnnouncementQuery;
import com.maciej.wojtaczka.announcementboard.persistence.entity.AnnouncementDbEntity;
import com.maciej.wojtaczka.announcementboard.persistence.entity.CommentDbEntity;
import com.maciej.wojtaczka.announcementboard.persistence.entity.CommentsCountDbEntity;
import com.maciej.wojtaczka.announcementboard.rest.dto.AnnouncementData;
import com.maciej.wojtaczka.announcementboard.rest.dto.CommentData;
import com.maciej.wojtaczka.announcementboard.util.KafkaTestListener;
import com.maciej.wojtaczka.announcementboard.util.UserFixtures;
import lombok.SneakyThrows;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.maciej.wojtaczka.announcementboard.rest.AnnouncementBoardController.ANNOUNCEMENTS_URL;
import static java.time.Instant.parse;
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
@DirtiesContext
class AnnouncementBoardControllerTest {

	@BeforeAll
	static void startCassandra() throws IOException, InterruptedException {
		EmbeddedCassandraServerHelper.startEmbeddedCassandra();
		CqlSession session = EmbeddedCassandraServerHelper.getSession();
		new CQLDataLoader(session).load(new ClassPathCQLDataSet("schema.cql"));
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CassandraOperations cassandraOperations;

	@Autowired
	private KafkaTestListener kafkaTestListener;

	@Autowired
	private AnnouncementCache cache;

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
		$.givenUser().withId(announcerId).exists();
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

	@Test
	void shouldPlaceCommentUnderAnnouncement() throws Exception {
		//given
		UUID announcerId = UUID.randomUUID();
		Instant announcementCreationTime = parse("2007-12-03T10:15:30.00Z");
		$.givenUser()
		 .withId(announcerId)
		 .publishedAnnouncement().atTime(announcementCreationTime).thatHasBeenCached()
		 .andThisUser()
		 .exists();

		UserFixtures.GivenUser commenter = $.givenUser()
											.exists();
		String commentContent = "Nice";

		CommentData commentData = CommentData.builder()
											 .authorId(commenter.getUserId())
											 .content(commentContent)
											 .build();

		//when
		ResultActions result = mockMvc.perform(post(ANNOUNCEMENTS_URL + "/" + announcerId.toString() + "/" + announcementCreationTime.toEpochMilli())
													   .content(asJsonString(commentData))
													   .contentType(APPLICATION_JSON)
													   .accept(APPLICATION_JSON));

		//then
		//verify response
		result.andExpect(status().isOk());

		//verify comment persistence
		CommentDbEntity commentDbEntity = cassandraOperations.selectOne(
				String.format("select * from announcement_board.comment where announcement_author_id = %s and announcement_creation_time = %s",
							  announcerId,
							  announcementCreationTime.toEpochMilli()),
				CommentDbEntity.class
		);

		assertThat(commentDbEntity).isNotNull();
		assertThat(commentDbEntity.getAuthorId()).isEqualTo(commenter.getUserId());
		assertThat(commentDbEntity.getAuthorNickname()).isEqualTo(commenter.getUserNickName());
		assertThat(commentDbEntity.getContent()).isEqualTo("Nice");
		assertThat(commentDbEntity.getCreationTime()).isNotNull();

		//verify comment counter increment
		CommentsCountDbEntity commentsCount = cassandraOperations.selectOne(
				String.format("select * from announcement_board.comments_count where announcement_author_id = %s and announcement_creation_time = %s",
							  announcerId,
							  announcementCreationTime.toEpochMilli()),
				CommentsCountDbEntity.class
		);
		assertThat(commentsCount).isNotNull();
		assertThat(commentsCount.getCommentsCount()).isEqualTo(1);

		//verify cached announcement update
		AnnouncementQuery query = AnnouncementQuery.builder().authorId(announcerId).creationTime(announcementCreationTime).build();
		Announcement announcementFromCache = cache.getOne(query).orElseThrow();
		assertThat(announcementFromCache.getCommentsCount()).isEqualTo(1);

		//verify publishing event
		String capturedEvent = kafkaTestListener.receiveFirstContentFromTopic(User.DomainEvents.ANNOUNCEMENT_COMMENTED)
												.orElseThrow(() -> new RuntimeException("No event"));
		Envelope<User.AnnouncementCommented> event = objectMapper.readValue(capturedEvent, new TypeReference<>() {
		});
		assertThat(event.getRecipients()).containsExactly(announcerId);
		var announcementCommented = event.getPayload();
		assertThat(announcementCommented.getAnnouncementAuthorId()).isEqualTo(announcerId);
		assertThat(announcementCommented.getAnnouncementCreationTime()).isEqualTo(announcementCreationTime);
		assertThat(announcementCommented.getComment().getAuthorId()).isEqualTo(commenter.getUserId());
		assertThat(announcementCommented.getComment().getAuthorNickname()).isEqualTo(commenter.getUserNickName());
		assertThat(announcementCommented.getComment().getContent()).isEqualTo("Nice");
		assertThat(announcementCommented.getComment().getCreationTime()).isNotNull();
		assertThat(kafkaTestListener.noMoreMessagesOnTopic(User.DomainEvents.ANNOUNCEMENT_PUBLISHED, 50)).isTrue();
	}

	@Test
	void shouldPlaceCommentsUnderAnnouncement_whenAllTheCommentsPlacedInCloseTime() throws Exception {
		//given
		int numberOfCommentsPlacedInCloseTime = 10;
		UUID announcerId = UUID.randomUUID();
		Instant announcementCreationTime = parse("2007-12-03T10:15:30.00Z");
		$.givenUser()
		 .withId(announcerId)
		 .publishedAnnouncement().atTime(announcementCreationTime)
		 .andThisUser()
		 .exists();

		String commentContent = "Nice";
		List<CommentData> commentsData = new ArrayList<>();
		for (int i = 0; i < numberOfCommentsPlacedInCloseTime; i++) {
			UserFixtures.GivenUser commenter = $.givenUser()
												.exists();
			CommentData commentData = CommentData.builder()
												 .authorId(commenter.getUserId())
												 .content(commentContent)
												 .build();
			commentsData.add(commentData);
		}
		List<CompletableFuture<Void>> jobs = new ArrayList<>();

		//when
		for (int i = 0; i < numberOfCommentsPlacedInCloseTime; i++) {
			CommentData commentData = commentsData.get(i);
			CompletableFuture<Void> asyncJob = CompletableFuture.runAsync(() -> {
				try {
					mockMvc.perform(post(ANNOUNCEMENTS_URL + "/" + announcerId.toString() + "/" + announcementCreationTime.toEpochMilli())
											.content(asJsonString(commentData))
											.contentType(APPLICATION_JSON)
											.accept(APPLICATION_JSON));
				} catch (Exception ignored) {
				}
			});
			jobs.add(asyncJob);
		}

		//then
		CompletableFuture.allOf(jobs.toArray(new CompletableFuture[numberOfCommentsPlacedInCloseTime])).join();
		List<CommentDbEntity> comments = cassandraOperations.select(
				String.format("select * from announcement_board.comment where announcement_author_id = %s and announcement_creation_time = %s",
							  announcerId,
							  announcementCreationTime.toEpochMilli()),
				CommentDbEntity.class
		);
		assertThat(comments).hasSize(numberOfCommentsPlacedInCloseTime);

		//verify comment counter increment
		CommentsCountDbEntity commentsCount = cassandraOperations.selectOne(
				String.format("select * from announcement_board.comments_count where announcement_author_id = %s and announcement_creation_time = %s",
							  announcerId,
							  announcementCreationTime.toEpochMilli()),
				CommentsCountDbEntity.class
		);
		assertThat(commentsCount).isNotNull();
		assertThat(commentsCount.getCommentsCount()).isEqualTo(numberOfCommentsPlacedInCloseTime);
	}

	@SneakyThrows
	private String asJsonString(final Object obj) {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(obj);
	}
}
