package com.maciej.wojtaczka.announcementboard.util;

import com.maciej.wojtaczka.announcementboard.cache.AnnouncementRedisCache;
import com.maciej.wojtaczka.announcementboard.cache.entry.AnnouncementEntry;
import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import com.maciej.wojtaczka.announcementboard.domain.model.User;
import com.maciej.wojtaczka.announcementboard.domain.query.AnnouncementQuery;
import com.maciej.wojtaczka.announcementboard.persistence.entity.AnnouncementDbEntity;
import com.maciej.wojtaczka.announcementboard.persistence.entity.UserDbEntity;
import lombok.SneakyThrows;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Component;

import java.time.Instant;
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

	public UserBuilder user() {
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

		private final Announcement.AnnouncementBuilder entityBuilder;
		private final UserBuilder owner;
		private boolean shouldCache;

		public AnnouncementBuilder(UserBuilder owner) {
			this.owner = owner;
			owner.userAnnouncements.add(this);
			this.entityBuilder = Announcement.builder()
											 .content("Default content")
											 .creationTime(Instant.now());
		}

		public AnnouncementBuilder atTime(Instant instant) {
			entityBuilder.creationTime(instant);
			return this;
		}

		public AnnouncementBuilder withContent(String content) {
			entityBuilder.content(content);
			return this;
		}

		public AnnouncementBuilder thatHasBeenCached() {
			shouldCache = true;
			return this;
		}

		public AnnouncementBuilder andAnnouncement() {
			return new AnnouncementBuilder(owner);
		}

		public UserBuilder andThisUser() {
			return owner;
		}

		private Announcement build(UUID announcerId) {
			Announcement announcement = entityBuilder.authorId(announcerId)
													 .build();

			cassandraOperations.insert(AnnouncementDbEntity.from(announcement));

			if (shouldCache) {
				redisRepository.save(AnnouncementEntry.from(announcement));
			}

			return announcement;
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

		public AnnouncementQuery getQueryForAnnouncement(int index) {
			return AnnouncementQuery.builder()
									.authorId(user.getId())
									.creationTime(announcements.get(index).getCreationTime())
									.build();
		}
	}

}
