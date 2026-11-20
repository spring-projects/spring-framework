/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.core;

import io.r2dbc.h2.CloseableConnectionFactory;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.R2dbcNonTransientResourceException;
import org.junit.jupiter.api.AfterEach;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 * @since 6.1
 */
class H2DatabaseClientContextIntegrationTests extends H2DatabaseClientIntegrationTests {

	AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);

	CloseableConnectionFactory connectionFactory = context.getBean(CloseableConnectionFactory.class);


	@Override
	protected ConnectionFactory createConnectionFactory() {
		return connectionFactory;
	}

	@AfterEach
	void tearDown() {
		context.close();
		assertThatExceptionOfType(R2dbcNonTransientResourceException.class).isThrownBy(
				() -> connectionFactory.create().block());
	}


	@Configuration
	static class Config {

		@Bean
		ConnectionFactory connectionFactory() {
			return H2ConnectionFactory.inMemory("r2dbc-context");
		}
	}

}
