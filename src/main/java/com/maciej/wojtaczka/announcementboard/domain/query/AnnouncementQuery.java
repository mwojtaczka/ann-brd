package com.maciej.wojtaczka.announcementboard.domain.query;

import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Value
@Builder
public class AnnouncementQuery {

	UUID authorId;
	Instant creationTime;

	@Value
	@Builder
	public static class Result {
		UUID authorId;
		List<Announcement> announcements;

		public static Result of(Map.Entry<UUID, List<Announcement>> entry) {
			List<Announcement> value = entry.getValue();
			value.sort(Comparator.comparing(Announcement::getCreationTime));
			return new Result(entry.getKey(), value);
		}
	}
}
