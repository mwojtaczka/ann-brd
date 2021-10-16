package com.maciej.wojtaczka.announcementboard.config;

import com.datastax.oss.driver.api.core.CqlSession;
import com.github.nosan.embedded.cassandra.Cassandra;
import com.github.nosan.embedded.cassandra.CassandraBuilder;
import com.github.nosan.embedded.cassandra.Settings;
import com.github.nosan.embedded.cassandra.cql.CqlScript;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.net.InetSocketAddress;

@TestConfiguration
public class CassandraConfig {

	@Bean
	Cassandra cassandra() { //TODO add feature to clean cassandra for each test
		Cassandra cassandra = new CassandraBuilder()
				.build();
		cassandra.start();
		Settings settings = cassandra.getSettings();
		try (CqlSession session = CqlSession.builder()
											.addContactPoint(new InetSocketAddress(settings.getAddress(), settings.getPort()))
											.withLocalDatacenter("datacenter1")
											.build()) {
			CqlScript.ofClassPath("schema.cql").forEachStatement(session::execute);
		}
		return cassandra;
	}
}
