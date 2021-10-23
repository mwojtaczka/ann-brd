package com.maciej.wojtaczka.announcementboard.persistence.entity;

import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import com.maciej.wojtaczka.announcementboard.domain.model.Comment;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Table("announcement")
@Builder
@Value
public class AnnouncementDbEntity {

	@PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, name = "author_id")
	UUID authorId;
	@PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING, name = "creation_time")
	Instant creationTime;
	String content;
	List<CommentDbEntity> comments;

	public static AnnouncementDbEntity from(Announcement announcement) {
		List<CommentDbEntity> commentDbEntities = announcement.getComments().stream()
															  .map(CommentDbEntity::from)
															  .collect(Collectors.toList());
		return AnnouncementDbEntity.builder()
								   .authorId(announcement.getAuthorId())
								   .creationTime(announcement.getCreationTime())
								   .content(announcement.getContent())
								   .comments(commentDbEntities)
								   .build();
	}

	public Announcement toModel() {
		List<Comment> commentModels;
		if (comments == null) {
			commentModels = new ArrayList<>();
		} else {
			commentModels = this.comments.stream()
										 .map(CommentDbEntity::toModel)
										 .collect(Collectors.toList());
		}
		return Announcement.builder()
						   .authorId(authorId)
						   .creationTime(creationTime)
						   .content(content)
						   .comments(commentModels)
						   .build();
	}
}

