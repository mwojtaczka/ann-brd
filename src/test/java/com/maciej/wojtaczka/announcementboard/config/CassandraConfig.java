package com.maciej.wojtaczka.announcementboard.config;

import com.datastax.oss.driver.api.core.CqlSession;
import com.github.nosan.embedded.cassandra.Cassandra;
import com.github.nosan.embedded.cassandra.Settings;
import com.github.nosan.embedded.cassandra.cql.CqlScript;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import java.net.InetSocketAddress;

@TestConfiguration
@Import(CassandraConfig.EmbeddedCassandraInitializer.class)
public class CassandraConfig {

	@Bean
	public static EmbeddedCassandraInitializerPostProcessor embeddedCassandraInitializerPostProcessor() {
		return new EmbeddedCassandraInitializerPostProcessor();
	}

	public static class EmbeddedCassandraInitializer implements InitializingBean {

		private final Cassandra cassandra;

		public EmbeddedCassandraInitializer(Cassandra cassandra) {
			this.cassandra = cassandra;
		}

		@Override
		public void afterPropertiesSet() {
			// initialize cassandra
			Settings settings = cassandra.getSettings();
			try (CqlSession session = CqlSession.builder()
												.addContactPoint(new InetSocketAddress(settings.getAddress(), settings.getPort()))
												.withLocalDatacenter("datacenter1")
												.build()) {
				CqlScript.ofClassPath("schema.cql").forEachStatement(session::execute);
			}
		}

	}

	public static class EmbeddedCassandraInitializerPostProcessor implements BeanPostProcessor, Ordered, BeanFactoryAware {

		private BeanFactory beanFactory;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE + 1;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof Cassandra) {
				this.beanFactory.getBean(EmbeddedCassandraInitializer.class);
			}
			return bean;
		}

	}

}
