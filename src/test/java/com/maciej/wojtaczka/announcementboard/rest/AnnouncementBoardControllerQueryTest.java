package com.maciej.wojtaczka.announcementboard.rest;

import com.datastax.oss.driver.api.core.CqlSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.maciej.wojtaczka.announcementboard.cache.AnnouncementRedisCache;
import com.maciej.wojtaczka.announcementboard.cache.entry.AnnouncementEntry;
import com.maciej.wojtaczka.announcementboard.domain.AnnouncementRepository;
import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import com.maciej.wojtaczka.announcementboard.domain.model.Comment;
import com.maciej.wojtaczka.announcementboard.domain.query.AnnouncementQuery;
import com.maciej.wojtaczka.announcementboard.util.UserFixtures;
import lombok.SneakyThrows;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.maciej.wojtaczka.announcementboard.rest.AnnouncementBoardController.ANNOUNCEMENTS_FETCH_URL;
import static com.maciej.wojtaczka.announcementboard.util.UserFixtures.GivenUser.FIRST;
import static com.maciej.wojtaczka.announcementboard.util.UserFixtures.GivenUser.SECOND;
import static com.maciej.wojtaczka.announcementboard.util.UserFixtures.GivenUser.THIRD;
import static java.time.Instant.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@WebAppConfiguration
@AutoConfigureMockMvc
class AnnouncementBoardControllerQueryTest {

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
	private UserFixtures $;

	@SpyBean
	private AnnouncementRepository announcementRepository;

	@Autowired
	private AnnouncementRedisCache cache;

	@Captor
	private ArgumentCaptor<Map<UUID, List<Instant>>> announcementRepoArgCaptor;

	@Test
	void shouldFetchAnnouncements() throws Exception {
		//given
		UserFixtures.GivenUser user1 =
				$.givenUser()
				 .publishedAnnouncement().withContent("Hello 1 from User1").atTime(parse("2007-12-03T10:15:30.00Z"))
				 .andAnnouncement().withContent("Hello 2 from User1").atTime(parse("2007-12-04T10:15:30.00Z"))
				 .andAnnouncement().withContent("Hello 3 from User1").atTime(parse("2007-12-05T10:15:30.00Z"))
				 .andThisUser()
				 .exists();

		UUID commenterId1 = UUID.randomUUID();
		UserFixtures.GivenUser user2 =
				$.givenUser()
				 .publishedAnnouncement().withContent("Hello 1 from User2").atTime(parse("2007-12-01T10:16:30.00Z"))
				 .andAnnouncement().withContent("Hello 2 from User2").atTime(parse("2007-12-04T10:16:30.00Z"))
				 .thatHasBeenCommented().byUser(commenterId1, "commenter1")
				 .atTime(parse("2007-12-01T10:16:35.00Z")).withContent("Hello from Commenter1")
				 .andAlsoCommented()
				 .andTheGivenUser()
				 .publishedAnnouncement().withContent("Hello 3 from User2").atTime(parse("2007-12-05T10:16:30.00Z"))
				 .andThisUser()
				 .exists();

		UserFixtures.GivenUser user3 =
				$.givenUser()
				 .publishedAnnouncement().withContent("Hello 1 from User3").atTime(parse("2007-12-03T10:15:30.00Z"))
				 .thatHasBeenCached()
				 .andAnnouncement().withContent("Hello 2 from User3").atTime(parse("2007-12-04T10:15:30.00Z"))
				 .thatHasBeenCached()
				 .andThisUser()
				 .exists();

		UserFixtures.GivenUser user4 = $.givenUser()
										.publishedAnnouncement().withContent("Hello 1 from User4").atTime(parse("2007-12-03T10:15:30.00Z"))
										.andAnnouncement().withContent("Hello 2 from User4").atTime(parse("2007-12-04T10:15:30.00Z"))
										.andThisUser()
										.exists();

		AnnouncementQuery q1 = user1.getQueryForAnnouncement(FIRST);
		AnnouncementQuery q2 = user1.getQueryForAnnouncement(SECOND);
		AnnouncementQuery q3 = user1.getQueryForAnnouncement(THIRD);
		AnnouncementQuery q4 = user2.getQueryForAnnouncement(SECOND);
		AnnouncementQuery q5 = user2.getQueryForAnnouncement(THIRD);
		AnnouncementQuery q6 = user3.getQueryForAnnouncement(FIRST);
		AnnouncementQuery q7 = user3.getQueryForAnnouncement(SECOND);

		var queries = List.of(q1, q2, q3, q4, q5, q6, q7);

		//when
		ResultActions result = mockMvc.perform(post(ANNOUNCEMENTS_FETCH_URL)
													   .content(asJsonString(queries))
													   .contentType(APPLICATION_JSON)
													   .accept(APPLICATION_JSON));

		//then
		String jsonResponseBody = result.andExpect(status().isOk())
										.andExpect(jsonPath("$", Matchers.hasSize(3)))
										.andReturn()
										.getResponse().getContentAsString();

		AnnouncementQuery.Result[] queryResults = objectMapper.readValue(jsonResponseBody, AnnouncementQuery.Result[].class);

		List<Announcement> announcementsFromUser1 = getAnnouncementsFor(user1, queryResults).orElseThrow();
		assertThat(announcementsFromUser1).hasSize(3);
		assertThat(announcementsFromUser1.get(0).getContent()).isEqualTo("Hello 1 from User1");
		assertThat(announcementsFromUser1.get(1).getContent()).isEqualTo("Hello 2 from User1");
		assertThat(announcementsFromUser1.get(2).getContent()).isEqualTo("Hello 3 from User1");

		List<Announcement> announcementsFromUser2 = getAnnouncementsFor(user2, queryResults).orElseThrow();
		assertThat(announcementsFromUser2).hasSize(2);
		assertThat(announcementsFromUser2.get(0).getContent()).isEqualTo("Hello 2 from User2");
		assertThat(announcementsFromUser2.get(0).getComments()).hasSize(2);
		Comment firstComment = announcementsFromUser2.get(0).getComments().get(0);
		assertThat(firstComment.getAuthorId()).isEqualTo(commenterId1);
		assertThat(firstComment.getAuthorNickname()).isEqualTo("commenter1");
		assertThat(firstComment.getContent()).isEqualTo("Hello from Commenter1");
		assertThat(firstComment.getCreationTime()).isNotNull();
		assertThat(announcementsFromUser2.get(1).getContent()).isEqualTo("Hello 3 from User2");

		List<Announcement> announcementsFromUser3 = getAnnouncementsFor(user3, queryResults).orElseThrow();
		assertThat(announcementsFromUser3).hasSize(2);
		assertThat(announcementsFromUser3.get(0).getContent()).isEqualTo("Hello 1 from User3");
		assertThat(announcementsFromUser3.get(1).getContent()).isEqualTo("Hello 2 from User3");

		Optional<List<Announcement>> announcementsFromUser4 = getAnnouncementsFor(user4, queryResults);
		assertThat(announcementsFromUser4).isEmpty();

		//verify cache works - should not query repository for cached announcements
		verify(announcementRepository).fetchAll(announcementRepoArgCaptor.capture());
		Map<UUID, List<Instant>> repoArg = announcementRepoArgCaptor.getValue();
		assertThat(repoArg).hasSize(2);
		assertThat(repoArg).containsKey(user1.getUserId());
		assertThat(repoArg).containsKeys(user2.getUserId());
		assertThat(repoArg).doesNotContainKey(user3.getUserId());

		//should save in cache
		assertThat(isCached(announcementsFromUser1.get(0))).isTrue();
		assertThat(isCached(announcementsFromUser1.get(1))).isTrue();
		assertThat(isCached(announcementsFromUser1.get(2))).isTrue();
		assertThat(isCached(announcementsFromUser2.get(0))).isTrue();
		assertThat(isCached(announcementsFromUser2.get(1))).isTrue();
	}

	private boolean isCached(Announcement announcement) {
		return cache.existsById(AnnouncementEntry.createId(announcement.getAuthorId(), announcement.getCreationTime()));
	}

	private Optional<List<Announcement>> getAnnouncementsFor(UserFixtures.GivenUser user, AnnouncementQuery.Result[] queryResults) {
		return Stream.of(queryResults)
					 .filter(r -> r.getAuthorId().equals(user.getUserId()))
					 .map(AnnouncementQuery.Result::getAnnouncements)
					 .findFirst();
	}

	@SneakyThrows
	private String asJsonString(final Object obj) {
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		return objectMapper.writeValueAsString(obj);
	}
}
