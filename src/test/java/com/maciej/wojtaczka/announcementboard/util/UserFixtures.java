package com.maciej.wojtaczka.announcementboard.util;

import com.maciej.wojtaczka.announcementboard.cache.AnnouncementRedisCache;
import com.maciej.wojtaczka.announcementboard.cache.entry.AnnouncementEntry;
import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import com.maciej.wojtaczka.announcementboard.domain.model.Comment;
import com.maciej.wojtaczka.announcementboard.domain.model.User;
import com.maciej.wojtaczka.announcementboard.domain.query.AnnouncementQuery;
import com.maciej.wojtaczka.announcementboard.persistence.entity.AnnouncementDbEntity;
import com.maciej.wojtaczka.announcementboard.persistence.entity.UserDbEntity;
import lombok.SneakyThrows;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Component
public class UserFixtures {

	private final CassandraOperations cassandraOperations;
	private final AnnouncementRedisCache redisRepository;

	public UserFixtures(CassandraOperations cassandraOperations,
						AnnouncementRedisCache redisRepository) {
		this.cassandraOperations = cassandraOperations;
		this.redisRepository = redisRepository;
	}

	public UserBuilder givenUser() {
		return new UserBuilder();
	}

	public class UserBuilder {

		private final User.UserBuilder userBuilder;
		private final Set<AnnouncementBuilder> userAnnouncements = new HashSet<>();

		public UserBuilder() {
			userBuilder = User.builder()
							  .id(UUID.randomUUID())
							  .name("DefaultName")
							  .surname("DefaultSurname")
							  .nickname("DefaultNickname");
		}

		public UserBuilder withId(UUID id) {
			userBuilder.id(id);
			return this;
		}

		public AnnouncementBuilder publishedAnnouncement() {
			return new AnnouncementBuilder(this);
		}

		@SneakyThrows
		public GivenUser exists() {
			User user = build();
			cassandraOperations.insert(UserDbEntity.from(user));

			List<Announcement> announcements = userAnnouncements.stream()
																.map(announcementBuilder -> announcementBuilder.build(user.getId()))
																.collect(Collectors.toList());

			return new GivenUser(user, announcements);
		}

		private User build() {
			return userBuilder.build();
		}
	}

	public class AnnouncementBuilder {

		private final Announcement.AnnouncementBuilder builder;
		private final UserBuilder owner;
		private final List<CommentBuilder> comments = new ArrayList<>();
		private boolean shouldCache;

		public AnnouncementBuilder(UserBuilder owner) {
			this.owner = owner;
			owner.userAnnouncements.add(this);
			this.builder = Announcement.builder()
									   .content("Default content")
									   .comments(new ArrayList<>())
									   .creationTime(Instant.now());
		}

		public AnnouncementBuilder atTime(Instant instant) {
			builder.creationTime(instant);
			return this;
		}

		public AnnouncementBuilder withContent(String content) {
			builder.content(content);
			return this;
		}

		public AnnouncementBuilder thatHasBeenCached() {
			shouldCache = true;
			return this;
		}

		public CommentBuilder thatHasBeenCommented() {
			return new CommentBuilder(owner, this);
		}

		public AnnouncementBuilder andAnnouncement() {
			return new AnnouncementBuilder(owner);
		}

		public UserBuilder andThisUser() {
			return owner;
		}

		private Announcement build(UUID announcerId) {
			List<Comment> announcementComments = comments.stream()
														 .map(CommentBuilder::build)
														 .collect(Collectors.toList());

			Announcement announcement = builder.authorId(announcerId)
											   .comments(announcementComments)
											   .build();

			cassandraOperations.insert(AnnouncementDbEntity.from(announcement));

			if (shouldCache) {
				redisRepository.save(AnnouncementEntry.from(announcement));
			}

			return announcement;
		}
	}

	public static class CommentBuilder {
		private final UserBuilder announcementOwner;
		private final AnnouncementBuilder announcement;
		private final Comment.CommentBuilder builder;

		public CommentBuilder(UserBuilder announcementOwner, AnnouncementBuilder announcement) {
			this.announcementOwner = announcementOwner;
			this.announcement = announcement;
			builder = Comment.builder()
							 .authorId(UUID.randomUUID())
							 .authorNickname("Default nickname")
							 .content("Default content")
							 .creationTime(Instant.now());
			announcement.comments.add(this);
		}

		public CommentBuilder byUser(UUID userId, String nickname) {
			builder.authorId(userId).authorNickname(nickname);
			return this;
		}

		public CommentBuilder atTime(Instant commentTime) {
			builder.creationTime(commentTime);
			return this;
		}

		public CommentBuilder withContent(String content) {
			builder.content(content);
			return this;
		}

		public CommentBuilder andAlsoCommented() {
			return new CommentBuilder(announcementOwner, announcement);
		}

		public UserBuilder andTheGivenUser() {
			return announcementOwner;
		}

		private Comment build() {
			return builder.build();
		}

	}


	public static class GivenUser {

		public static final int FIRST = 0;
		public static final int SECOND = 1;
		public static final int THIRD = 2;

		private final User user;
		private final List<Announcement> announcements;

		public GivenUser(User user,
						 List<Announcement> announcements) {
			this.user = user;
			this.announcements = announcements.stream()
											  .sorted(Comparator.comparing(Announcement::getCreationTime))
											  .collect(Collectors.toList());
		}

		public UUID getUserId() {
			return user.getId();
		}

		public String getUserNickName() {
			return user.getNickname();
		}

		public AnnouncementQuery getQueryForAnnouncement(int index) {
			return AnnouncementQuery.builder()
									.authorId(user.getId())
									.creationTime(announcements.get(index).getCreationTime())
									.build();
		}
	}

}
