/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.connection;

import jakarta.jms.Connection;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueSession;
import jakarta.jms.TopicConnection;
import jakarta.jms.TopicSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConnectionFactoriesRuntimeHints}.
 */
class ConnectionFactoriesRuntimeHintsTests {

	private RuntimeHints hints;

	@BeforeEach
	void setup() {
		this.hints = new RuntimeHints();
		SpringFactoriesLoader.forResourceLocation("META-INF/spring/aot.factories")
				.load(RuntimeHintsRegistrar.class).forEach(registrar -> registrar
						.registerHints(this.hints, ClassUtils.getDefaultClassLoader()));
	}

	@Test
	void shouldRegisterProxyHints() throws Exception {
		assertThat(RuntimeHintsPredicates.proxies().forInterfaces(SessionProxy.class, TopicSession.class, QueueSession.class)).accepts(this.hints);
		assertThat(RuntimeHintsPredicates.proxies().forInterfaces(Connection.class, QueueConnection.class, TopicConnection.class)).accepts(this.hints);
	}

}
