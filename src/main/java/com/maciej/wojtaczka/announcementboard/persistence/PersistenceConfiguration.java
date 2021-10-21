package com.maciej.wojtaczka.announcementboard.persistence;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.AsyncCassandraOperations;
import org.springframework.data.cassandra.core.AsyncCassandraTemplate;
import org.springframework.data.cassandra.core.CassandraOperations;

@Configuration
public class PersistenceConfiguration {

	@Bean
	AsyncCassandraOperations asyncCassandraOperations(CqlSession session) {
		return new AsyncCassandraTemplate(session);
	}

	@Bean
	AnnouncementRepositoryAdapter repository(CassandraOperations cassandraOperations, AsyncCassandraOperations asyncCassandraOperations) {
		return new AnnouncementRepositoryAdapter(cassandraOperations, asyncCassandraOperations, 100);
	}
}
