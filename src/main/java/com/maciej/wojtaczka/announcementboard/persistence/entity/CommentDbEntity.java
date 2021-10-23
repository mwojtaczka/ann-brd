package com.maciej.wojtaczka.announcementboard.persistence.entity;

import com.maciej.wojtaczka.announcementboard.domain.model.Comment;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

import java.time.Instant;
import java.util.UUID;

@UserDefinedType("comment")
@Builder
@Value
public class CommentDbEntity {

	@Column("author_id")
	UUID authorId;
	@Column("author_nickname")
	String authorNickname;
	String content;
	@Column("creation_time")
	Instant creationTime;

	static CommentDbEntity from(Comment comment) {
		return CommentDbEntity.builder()
							  .authorId(comment.getAuthorId())
							  .authorNickname(comment.getAuthorNickname())
							  .content(comment.getContent())
							  .creationTime(comment.getCreationTime())
							  .build();
	}

	Comment toModel() {
		return Comment.builder()
					  .authorId(getAuthorId())
					  .authorNickname(getAuthorNickname())
					  .content(getContent())
					  .creationTime(getCreationTime())
					  .build();
	}
}
