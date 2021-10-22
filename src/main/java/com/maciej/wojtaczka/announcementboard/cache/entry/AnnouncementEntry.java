package com.maciej.wojtaczka.announcementboard.cache.entry;

import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.redis.core.RedisHash;

import java.time.Instant;
import java.util.UUID;

@RedisHash("Announcement")
@Builder
@Value
public class AnnouncementEntry {

	String id;
	UUID authorId;
	String content;
	Instant creationTime;

	public static AnnouncementEntry from(Announcement announcement) {
		return AnnouncementEntry.builder()
								.id(createId(announcement.getAuthorId(), announcement.getCreationTime()))
								.authorId(announcement.getAuthorId())
								.creationTime(announcement.getCreationTime())
								.content(announcement.getContent())
								.build();
	}

	public Announcement toModel() {
		return Announcement.builder()
						   .authorId(authorId)
						   .creationTime(creationTime)
						   .content(content)
						   .build();
	}

	public static String createId(UUID authorId, Instant creationTime) {
		return authorId.toString() + ":" + creationTime.toEpochMilli();
	}
}
