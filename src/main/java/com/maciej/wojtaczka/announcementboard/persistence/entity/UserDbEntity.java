package com.maciej.wojtaczka.announcementboard.persistence.entity;

import com.maciej.wojtaczka.announcementboard.domain.model.User;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

@Table("user")
@Builder
@Value
public class UserDbEntity {

	@PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, name = "id")
	UUID id;
	String nickname;
	String name;
	String surname;

	public static UserDbEntity from(User user) {
		return UserDbEntity.builder()
						   .id(user.getId())
						   .nickname(user.getNickname())
						   .name(user.getName())
						   .surname(user.getSurname())
						   .build();
	}

	public User toModel() {
		return User.builder()
				   .id(id)
				   .name(name)
				   .surname(surname)
				   .nickname(nickname)
				   .build();
	}

}
