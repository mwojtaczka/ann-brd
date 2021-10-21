package com.maciej.wojtaczka.announcementboard.util;

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

	public UserFixtures(CassandraOperations cassandraOperations) {
		this.cassandraOperations = cassandraOperations;
	}

	public UserBuilder user() {
		return new UserBuilder();
	}

	public class UserBuilder {

		private final UserDbEntity.UserDbEntityBuilder userBuilder;
		private final Set<AnnouncementBuilder> userAnnouncements = new HashSet<>();

		public UserBuilder() {
			userBuilder = UserDbEntity.builder()
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
			UserDbEntity entity = build();
			cassandraOperations.insert(entity);

			List<AnnouncementDbEntity> announcements = userAnnouncements.stream()
																		.map(announcementBuilder -> announcementBuilder.build(entity.getId()))
																		.map(cassandraOperations::insert)
																		.collect(Collectors.toList());

			return new GivenUser(entity, announcements);
		}

		private UserDbEntity build() {
			return userBuilder.build();
		}
	}

	public class AnnouncementBuilder {

		private final AnnouncementDbEntity.AnnouncementDbEntityBuilder entityBuilder;
		private final UserBuilder owner;

		public AnnouncementBuilder(UserBuilder owner) {
			this.owner = owner;
			owner.userAnnouncements.add(this);
			this.entityBuilder = AnnouncementDbEntity.builder()
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

		public AnnouncementBuilder andAnnouncement() {
			return new AnnouncementBuilder(owner);
		}

		public UserBuilder andThisUser() {
			return owner;
		}

		private AnnouncementDbEntity build(UUID announcerId) {
			return entityBuilder.authorId(announcerId)
								.build();
		}
	}

	public static class GivenUser {

		public static final int FIRST = 0;
		public static final int SECOND = 1;
		public static final int THIRD = 2;

		private final UserDbEntity userDbEntity;
		private final List<AnnouncementDbEntity> announcements;

		public GivenUser(UserDbEntity userDbEntity,
						 List<AnnouncementDbEntity> announcements) {
			this.userDbEntity = userDbEntity;
			this.announcements = announcements.stream()
											  .sorted(Comparator.comparing(AnnouncementDbEntity::getCreationTime))
											  .collect(Collectors.toList());
		}

		public UUID getUserId() {
			return userDbEntity.getId();
		}

		public AnnouncementQuery getQueryForAnnouncement(int index) {
			return AnnouncementQuery.builder()
									.authorId(userDbEntity.getId())
									.creationTime(announcements.get(index).getCreationTime())
									.build();
		}
	}

}
