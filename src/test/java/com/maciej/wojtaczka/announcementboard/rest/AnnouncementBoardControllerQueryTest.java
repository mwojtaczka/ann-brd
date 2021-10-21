package com.maciej.wojtaczka.announcementboard.rest;

import com.datastax.oss.driver.api.core.CqlSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import com.maciej.wojtaczka.announcementboard.domain.query.AnnouncementQuery;
import com.maciej.wojtaczka.announcementboard.util.UserFixtures;
import lombok.SneakyThrows;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.maciej.wojtaczka.announcementboard.rest.AnnouncementBoardController.ANNOUNCEMENTS_FETCH_URL;
import static com.maciej.wojtaczka.announcementboard.util.UserFixtures.GivenUser.FIRST;
import static com.maciej.wojtaczka.announcementboard.util.UserFixtures.GivenUser.SECOND;
import static com.maciej.wojtaczka.announcementboard.util.UserFixtures.GivenUser.THIRD;
import static java.time.Instant.parse;
import static org.assertj.core.api.Assertions.assertThat;
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

	@Test
	void shouldFetchAnnouncements() throws Exception {
		//given
		var user1 = $.user()
					 .publishedAnnouncement().withContent("Hello 1").atTime(parse("2007-12-03T10:15:30.00Z"))
					 .andAnnouncement().withContent("Hello 2").atTime(parse("2007-12-04T10:15:30.00Z"))
					 .andAnnouncement().withContent("Hello 3").atTime(parse("2007-12-05T10:15:30.00Z"))
					 .andThisUser()
					 .exists();

		var user2 = $.user()
					 .publishedAnnouncement().withContent("Hello 1").atTime(parse("2007-12-01T10:16:30.00Z"))
					 .andAnnouncement().withContent("Hello 2").atTime(parse("2007-12-04T10:16:30.00Z"))
					 .andAnnouncement().withContent("Hello 3").atTime(parse("2007-12-05T10:16:30.00Z"))
					 .andThisUser()
					 .exists();

		var user3 = $.user()
					 .publishedAnnouncement().withContent("Hello 1").atTime(parse("2007-12-03T10:15:30.00Z"))
					 .andAnnouncement().withContent("Hello 2").atTime(parse("2007-12-04T10:15:30.00Z"))
					 .andThisUser()
					 .exists();

		AnnouncementQuery q1 = user1.getQueryForAnnouncement(FIRST);
		AnnouncementQuery q2 = user1.getQueryForAnnouncement(SECOND);
		AnnouncementQuery q3 = user1.getQueryForAnnouncement(THIRD);
		AnnouncementQuery q4 = user2.getQueryForAnnouncement(SECOND);
		AnnouncementQuery q5 = user2.getQueryForAnnouncement(THIRD);

		var queries = List.of(q1, q2, q3, q4, q5);

		//when
		ResultActions result = mockMvc.perform(post(ANNOUNCEMENTS_FETCH_URL)
													   .content(asJsonString(queries))
													   .contentType(APPLICATION_JSON)
													   .accept(APPLICATION_JSON));

		//then
		String jsonResponseBody = result.andExpect(status().isOk())
										.andExpect(jsonPath("$", Matchers.hasSize(2)))
										.andReturn()
										.getResponse().getContentAsString();

		AnnouncementQuery.Result[] queryResults = objectMapper.readValue(jsonResponseBody, AnnouncementQuery.Result[].class);

		List<Announcement> announcementsFromUser1 = getAnnouncementsFor(user1, queryResults).orElseThrow();
		assertThat(announcementsFromUser1).hasSize(3);
		assertThat(announcementsFromUser1.get(0).getContent()).isEqualTo("Hello 1");
		assertThat(announcementsFromUser1.get(1).getContent()).isEqualTo("Hello 2");
		assertThat(announcementsFromUser1.get(2).getContent()).isEqualTo("Hello 3");

		List<Announcement> announcementsFromUser2 = getAnnouncementsFor(user2, queryResults).orElseThrow();
		assertThat(announcementsFromUser2).hasSize(2);
		assertThat(announcementsFromUser2.get(0).getContent()).isEqualTo("Hello 2");
		assertThat(announcementsFromUser2.get(1).getContent()).isEqualTo("Hello 3");

		Optional<List<Announcement>> announcementsFromUser3 = getAnnouncementsFor(user3, queryResults);
		assertThat(announcementsFromUser3).isEmpty();
	}

	private Optional<List<Announcement>> getAnnouncementsFor(UserFixtures.GivenUser user3, AnnouncementQuery.Result[] queryResults) {
		return Stream.of(queryResults)
					 .filter(r -> r.getAuthorId().equals(user3.getUserId()))
					 .map(AnnouncementQuery.Result::getAnnouncements)
					 .findFirst();
	}

	@SneakyThrows
	private String asJsonString(final Object obj) {
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		return objectMapper.writeValueAsString(obj);
	}
}
