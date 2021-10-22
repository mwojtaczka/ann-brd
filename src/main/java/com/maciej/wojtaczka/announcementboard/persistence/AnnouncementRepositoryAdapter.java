package com.maciej.wojtaczka.announcementboard.persistence;

import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.maciej.wojtaczka.announcementboard.domain.AnnouncementRepository;
import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import com.maciej.wojtaczka.announcementboard.persistence.entity.AnnouncementDbEntity;
import org.springframework.data.cassandra.core.AsyncCassandraOperations;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.util.concurrent.ListenableFuture;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class AnnouncementRepositoryAdapter implements AnnouncementRepository {

	private final int maxSimultaneousSelects;

	private final CassandraOperations cassandraOperations;
	private final AsyncCassandraOperations asyncCassandraOperations;

	public AnnouncementRepositoryAdapter(CassandraOperations cassandraOperations,
										 AsyncCassandraOperations asyncCassandraOperations,
										 int maxSimultaneousSelects) {
		this.cassandraOperations = cassandraOperations;
		this.asyncCassandraOperations = asyncCassandraOperations;
		this.maxSimultaneousSelects = maxSimultaneousSelects;
	}

	@Override
	public Announcement save(Announcement announcement) {

		AnnouncementDbEntity announcementEntity = AnnouncementDbEntity.from(announcement);

		AnnouncementDbEntity savedEntity = cassandraOperations.insert(announcementEntity);

		return savedEntity.toModel();
	}

	@Override
	public List<Announcement> fetchAll(Map<UUID, List<Instant>> authorIdToCreationTimes) {

		if (authorIdToCreationTimes.isEmpty()) {
			return List.of();
		}

		List<SimpleStatement> selects = authorIdToCreationTimes.entrySet().stream()
														  .map(announcerToTimeEntry -> buildSelectStatement(announcerToTimeEntry.getKey(),
																											announcerToTimeEntry.getValue()))
														  .collect(Collectors.toList());

		Queue<ListenableFuture<List<AnnouncementDbEntity>>> futureResults = new LinkedList<>();
		List<AnnouncementDbEntity> allAnnouncements = new ArrayList<>();

		for (SimpleStatement selectStatement : selects) {
			ListenableFuture<List<AnnouncementDbEntity>> futureAnnouncements = asyncCassandraOperations.select(selectStatement,
																											   AnnouncementDbEntity.class);
			futureResults.add(futureAnnouncements);

			while (futureResults.size() >= maxSimultaneousSelects ||
					(!futureResults.isEmpty() && futureResults.peek().isDone())) {
				getFutureAnnouncementsAndLoadThemToList(futureResults, allAnnouncements);
			}
		}

		while (!futureResults.isEmpty()) {
			getFutureAnnouncementsAndLoadThemToList(futureResults, allAnnouncements);
		}

		return allAnnouncements.stream()
							   .map(AnnouncementDbEntity::toModel)
							   .collect(Collectors.toList());
	}

	private SimpleStatement buildSelectStatement(UUID authorId, List<Instant> creationTimes) {
		creationTimes.sort(Instant::compareTo);
		Instant oldest = creationTimes.get(0);

		return QueryBuilder.selectFrom("announcement_board", "announcement")
						   .all()
						   .whereColumn("author_id").isEqualTo(literal(authorId))
						   .whereColumn("creation_time").isGreaterThanOrEqualTo(literal(oldest))
						   .build();
	}

	private void getFutureAnnouncementsAndLoadThemToList(Queue<ListenableFuture<List<AnnouncementDbEntity>>> futureResults,
														 List<AnnouncementDbEntity> allAnnouncements) {
		ListenableFuture<List<AnnouncementDbEntity>> nextToGet = futureResults.remove();
		List<AnnouncementDbEntity> announcements;
		try {
			announcements = nextToGet.get(3000, MILLISECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new RuntimeException(e);
		}

		allAnnouncements.addAll(announcements);
	}

}
