package com.maciej.wojtaczka.announcementboard.persistence;

import com.maciej.wojtaczka.announcementboard.domain.AnnouncementRepository;
import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import com.maciej.wojtaczka.announcementboard.persistence.entity.AnnouncementDbEntity;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Repository;

@Repository
public class AnnouncementRepositoryAdapter implements AnnouncementRepository {

	private final CassandraOperations cassandraOperations;

	public AnnouncementRepositoryAdapter(CassandraOperations cassandraOperations) {
		this.cassandraOperations = cassandraOperations;
	}

	@Override
	public Announcement save(Announcement announcement) {

		AnnouncementDbEntity announcementEntity = AnnouncementDbEntity.builder()
																	  .announcerId(announcement.getAnnouncerId())
																	  .creationTime(announcement.getCreationTime())
																	  .content(announcement.getContent())
																	  .comments(announcement.getComments())
																	  .build();

		AnnouncementDbEntity savedEntity = cassandraOperations.insert(announcementEntity);

		return Announcement.builder()
						   .announcerId(savedEntity.getAnnouncerId())
						   .creationTime(savedEntity.getCreationTime())
						   .content(savedEntity.getContent())
						   .comments(savedEntity.getComments())
						   .build();
	}
}
