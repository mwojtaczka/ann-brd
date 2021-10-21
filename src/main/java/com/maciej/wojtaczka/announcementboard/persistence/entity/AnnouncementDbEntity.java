package com.maciej.wojtaczka.announcementboard.persistence.entity;

import com.maciej.wojtaczka.announcementboard.domain.model.Comment;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Table("announcement")
@Builder
@Value
public class AnnouncementDbEntity {

	@PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, name = "author_id")
	UUID authorId;
	@PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING, name = "creation_time")
	Instant creationTime;
	String content;
	List<Comment> comments;
}
