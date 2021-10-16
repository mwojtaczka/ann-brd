package com.maciej.wojtaczka.announcementboard.util;

import com.maciej.wojtaczka.announcementboard.persistence.entity.UserDbEntity;
import lombok.SneakyThrows;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Component;

import java.util.UUID;


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

		@SneakyThrows
		public void exists() {
			UserDbEntity entity = build();
			cassandraOperations.insert(entity);
		}

		private UserDbEntity build() {
			return userBuilder.build();
		}
	}


}
