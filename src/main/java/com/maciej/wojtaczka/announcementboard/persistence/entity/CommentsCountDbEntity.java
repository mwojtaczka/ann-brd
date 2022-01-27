package com.maciej.wojtaczka.announcementboard.persistence.entity;

import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("comments_count")
@Builder
@Value
public class CommentsCountDbEntity {

	@PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, name = "announcement_author_id")
	UUID announcementAuthorId;
	@PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, name = "announcement_creation_time")
	Instant announcementCreationTime;
	@Column("comments_count")
	Long commentsCount;

	public static CommentsCountDbEntity newFrom(Announcement announcement) {
		return builder()
				.announcementAuthorId(announcement.getAuthorId())
				.announcementCreationTime(announcement.getCreationTime())
				.build();
	}

	public String getKey() {
		return announcementAuthorId.toString() + ":" + announcementCreationTime.toEpochMilli();
	}

	public boolean isEmpty() {
		return announcementAuthorId == null || announcementCreationTime == null || commentsCount == null;
	}
}
