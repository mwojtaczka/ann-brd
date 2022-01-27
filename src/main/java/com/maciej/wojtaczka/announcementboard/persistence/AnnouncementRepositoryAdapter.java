package com.maciej.wojtaczka.announcementboard.persistence;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.maciej.wojtaczka.announcementboard.domain.AnnouncementRepository;
import com.maciej.wojtaczka.announcementboard.domain.model.Announcement;
import com.maciej.wojtaczka.announcementboard.domain.model.Comment;
import com.maciej.wojtaczka.announcementboard.persistence.entity.AnnouncementDbEntity;
import com.maciej.wojtaczka.announcementboard.persistence.entity.CommentDbEntity;
import com.maciej.wojtaczka.announcementboard.persistence.entity.CommentsCountDbEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.cassandra.core.AsyncCassandraOperations;
import org.springframework.data.cassandra.core.CassandraOperations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;

@Slf4j
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
	public void saveAnnouncementComment(Comment comment) {
		SimpleStatement incrementCounter = QueryBuilder.update("announcement_board", "comments_count")
													.increment("comments_count")
													.whereColumn("announcement_author_id").isEqualTo( literal(comment.getAnnouncementAuthorId()))
													.whereColumn("announcement_creation_time").isEqualTo( literal(comment.getAnnouncementCreationTime()))
													.build();
		ResultSet result = cassandraOperations.execute(incrementCounter);

		if (result.wasApplied()) {
			try {
				cassandraOperations.insert(CommentDbEntity.from(comment));
			} catch (DataAccessException e) {
				log.warn("Could not insert comment: ", e);
				SimpleStatement decrementCounter = QueryBuilder.update("announcement_board", "comments_count")
															   .decrement("comments_count")
															   .whereColumn("announcement_author_id").isEqualTo( literal(comment.getAnnouncementAuthorId()))
															   .whereColumn("announcement_creation_time").isEqualTo( literal(comment.getAnnouncementCreationTime()))
															   .build();
				ResultSet decrementResult = cassandraOperations.execute(decrementCounter);
				if (!decrementResult.wasApplied()) {
					//handle inconsistency
				}
			}
		}

	}

	@Override
	public List<Announcement> fetchAll(Map<UUID, List<Instant>> authorIdToCreationTimes) {

		if (authorIdToCreationTimes.isEmpty()) {
			return List.of();
		}

		List<GetAnnouncementsAndCommentsCountStatement> selects =
				authorIdToCreationTimes.entrySet().stream()
									   .map(announcerToTimeEntry -> buildSelectStatement(announcerToTimeEntry.getKey(),
																						 announcerToTimeEntry.getValue()))
									   .collect(Collectors.toList());

		Queue<CompletableFuture<List<Announcement>>> futureResults = new LinkedList<>();
		List<Announcement> allAnnouncements = new ArrayList<>();

		for (GetAnnouncementsAndCommentsCountStatement statement : selects) {
			CompletableFuture<List<AnnouncementDbEntity>> futureDbAnnouncements =
					asyncCassandraOperations.select(statement.getAnnouncements, AnnouncementDbEntity.class)
											.completable();

			CompletableFuture<Map<String, CommentsCountDbEntity>> futureDbCommentsCount =
					asyncCassandraOperations.select(statement.getCommentsCount, CommentsCountDbEntity.class)
											.completable()
											.thenApply(this::toMap);

			CompletableFuture<List<Announcement>> futureAnnouncements =
					futureDbAnnouncements.thenCombine(futureDbCommentsCount, this::combineAnnouncements);
			futureResults.add(futureAnnouncements);

			while (futureResults.size() >= maxSimultaneousSelects ||
					(!futureResults.isEmpty() && futureResults.peek().isDone())) {
				CompletableFuture<List<Announcement>> nextToGet = futureResults.remove();
				allAnnouncements.addAll(nextToGet.join());
			}
		}

		while (!futureResults.isEmpty()) {
			CompletableFuture<List<Announcement>> nextToGet = futureResults.remove();
			allAnnouncements.addAll(nextToGet.join());
		}

		return List.copyOf(allAnnouncements);
	}

	private Map<String, CommentsCountDbEntity> toMap(List<CommentsCountDbEntity> commentsCounts) {
		return commentsCounts.stream()
							 .filter(obj -> !obj.isEmpty())
							 .collect(Collectors.toMap(
									 CommentsCountDbEntity::getKey,
									 Function.identity()));
	}

	private List<Announcement> combineAnnouncements(List<AnnouncementDbEntity> announcementDbEntities,
													Map<String, CommentsCountDbEntity> commentsCount) {
		return announcementDbEntities.stream()
									 .map(announcementDbEntity -> announcementDbEntity.toModel(commentsCount))
									 .collect(Collectors.toList());
	}

	private GetAnnouncementsAndCommentsCountStatement buildSelectStatement(UUID authorId, List<Instant> creationTimes) {
		creationTimes.sort(Instant::compareTo);
		Instant oldest = creationTimes.get(0);

		SimpleStatement getCommentCounts = QueryBuilder.selectFrom("announcement_board", "comments_count")
													   .all()
													   .whereColumn("announcement_author_id").isEqualTo(literal(authorId))
													   .whereColumn("announcement_creation_time").isGreaterThanOrEqualTo(literal(oldest))
													   .build();

		SimpleStatement getAnnouncements = QueryBuilder.selectFrom("announcement_board", "announcement")
													   .all()
													   .whereColumn("author_id").isEqualTo(literal(authorId))
													   .whereColumn("creation_time").isGreaterThanOrEqualTo(literal(oldest))
													   .build();
		return new GetAnnouncementsAndCommentsCountStatement(getAnnouncements, getCommentCounts);
	}

	@Override
	public Optional<Announcement> fetchOne(UUID authorId, Instant creationTime) {

		SimpleStatement selectAnnouncement = QueryBuilder.selectFrom("announcement_board", "announcement")
														 .all()
														 .whereColumn("author_id").isEqualTo(literal(authorId))
														 .whereColumn("creation_time").isEqualTo(literal(creationTime))
														 .build();

		SimpleStatement countComments = QueryBuilder.selectFrom("announcement_board", "comments_count")
													.column("comments_count")
													.whereColumn("announcement_author_id").isEqualTo(literal(authorId))
													.whereColumn("announcement_creation_time").isEqualTo(literal(creationTime))
													.build();

		AnnouncementDbEntity entity = cassandraOperations.selectOne(selectAnnouncement, AnnouncementDbEntity.class);
		Long commentsCount = Optional.ofNullable(cassandraOperations.selectOne(countComments, Long.class))
									 .orElse(0L);
		return Optional.ofNullable(entity)
					   .map(announcementDb -> announcementDb.toModel(commentsCount));
	}

	private static class GetAnnouncementsAndCommentsCountStatement {
		SimpleStatement getAnnouncements;
		SimpleStatement getCommentsCount;

		GetAnnouncementsAndCommentsCountStatement(SimpleStatement getAnnouncements, SimpleStatement getCommentsCount) {
			this.getAnnouncements = getAnnouncements;
			this.getCommentsCount = getCommentsCount;
		}
	}
}
