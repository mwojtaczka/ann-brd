package com.maciej.wojtaczka.announcementboard.persistence;

import com.maciej.wojtaczka.announcementboard.domain.UserService;
import com.maciej.wojtaczka.announcementboard.domain.model.User;
import com.maciej.wojtaczka.announcementboard.persistence.entity.UserDbEntity;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserLocalRepository implements UserService {

	private final CassandraOperations cassandraOperations;

	public UserLocalRepository(CassandraOperations cassandraOperations) {
		this.cassandraOperations = cassandraOperations;
	}

	public void saveUser(User user) {
		UserDbEntity entity = UserDbEntity.from(user);

		cassandraOperations.insert(entity);
	}

	@Override
	public Optional<User> fetchUser(UUID userId) {

		UserDbEntity userDbEntity = cassandraOperations.selectOne("select * from announcement_board.user where id = " + userId, UserDbEntity.class);

		if (userDbEntity == null) {
			return Optional.empty();
		}

		return Optional.of(userDbEntity.toModel());
	}
}
