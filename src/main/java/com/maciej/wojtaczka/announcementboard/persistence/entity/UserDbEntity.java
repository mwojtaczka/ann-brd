package com.maciej.wojtaczka.announcementboard.persistence.entity;

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

}
