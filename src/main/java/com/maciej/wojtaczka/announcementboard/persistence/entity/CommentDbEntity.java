package com.maciej.wojtaczka.announcementboard.persistence.entity;

import com.maciej.wojtaczka.announcementboard.domain.model.Comment;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("comment")
@Builder
@Value
public class CommentDbEntity {

	@PrimaryKeyColumn(name = "announcement_author_id", type = PrimaryKeyType.PARTITIONED)
	UUID announcementAuthorId;
	@PrimaryKeyColumn(name = "announcement_creation_time", type = PrimaryKeyType.PARTITIONED)
	Instant announcementCreationTime;

	@PrimaryKeyColumn(name = "author_id", type = PrimaryKeyType.CLUSTERED)
	UUID authorId;
	@PrimaryKeyColumn(name = "creation_time", type = PrimaryKeyType.CLUSTERED)
	Instant creationTime;
	@Column("author_nickname")
	String authorNickname;
	@Column("content")
	String content;

	public static CommentDbEntity from(Comment comment) {
		return CommentDbEntity.builder()
							  .announcementAuthorId(comment.getAnnouncementAuthorId())
							  .announcementCreationTime(comment.getAnnouncementCreationTime())
							  .authorId(comment.getAuthorId())
							  .authorNickname(comment.getAuthorNickname())
							  .content(comment.getContent())
							  .creationTime(comment.getCreationTime())
							  .build();
	}

	Comment toModel() {
		return Comment.builder()
					  .announcementAuthorId(announcementAuthorId)
					  .announcementCreationTime(announcementCreationTime)
					  .authorId(getAuthorId())
					  .authorNickname(getAuthorNickname())
					  .content(getContent())
					  .creationTime(getCreationTime())
					  .build();
	}
}
