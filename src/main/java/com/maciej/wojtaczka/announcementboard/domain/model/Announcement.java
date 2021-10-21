package com.maciej.wojtaczka.announcementboard.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
@Getter
@EqualsAndHashCode
public class Announcement {

    private final UUID authorId;
    private final String content;
    private final Instant creationTime;
    private List<Comment> comments;
    
}
