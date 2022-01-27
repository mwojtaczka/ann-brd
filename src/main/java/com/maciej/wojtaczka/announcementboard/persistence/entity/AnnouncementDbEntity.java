package com.maciej.wojtaczka.announcementboard.persistence.entity;

import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
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

	public static AnnouncementDbEntity from(Announcement announcement) {
		return AnnouncementDbEntity.builder()
								   .authorId(announcement.getAuthorId())
								   .creationTime(announcement.getCreationTime())
								   .content(announcement.getContent())
								   .build();
	}

	public Announcement toModel(long commentsCount) {

		return Announcement.builder()
						   .authorId(authorId)
						   .creationTime(creationTime)
						   .content(content)
						   .commentsCount(commentsCount)
						   .build();
	}

	public Announcement toModel(Map<String, CommentsCountDbEntity> commentsCounts) {
		CommentsCountDbEntity commentsCountEntity = commentsCounts.get(authorId.toString() + ":" + creationTime.toEpochMilli());
		Long commentsCount = Optional.ofNullable(commentsCountEntity)
									 .map(CommentsCountDbEntity::getCommentsCount)
									 .orElse(0L);

		return Announcement.builder()
						   .authorId(authorId)
						   .creationTime(creationTime)
						   .content(content)
						   .commentsCount(commentsCount)
						   .build();
	}

	public Announcement toModel() {
		return toModel(0L);
	}
}

