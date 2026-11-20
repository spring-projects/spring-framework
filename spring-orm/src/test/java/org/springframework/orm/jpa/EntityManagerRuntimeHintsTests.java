/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EntityManagerRuntimeHints}.
 *
 * @author Sebastien Deleuze
 */
class EntityManagerRuntimeHintsTests {

	private final RuntimeHints hints = new RuntimeHints();

	@BeforeEach
	void setup() {
		AotServices.factories().load(RuntimeHintsRegistrar.class)
				.forEach(registrar -> registrar.registerHints(this.hints, ClassUtils.getDefaultClassLoader()));
	}

	@Test
	void entityManagerFactoryInfoHasHibernateHints() {
		assertThat(RuntimeHintsPredicates.proxies().forInterfaces(SessionFactory.class, EntityManagerFactoryInfo.class))
				.accepts(this.hints);
	}

	@Test
	void entityManagerProxyHasHibernateHints() {
		assertThat(RuntimeHintsPredicates.proxies().forInterfaces(Session.class, EntityManagerProxy.class))
				.accepts(this.hints);
		assertThat(RuntimeHintsPredicates.proxies().forInterfaces(StatelessSession.class)).accepts(this.hints);
	}

	@Test
	void entityManagerFactoryHasFallbackQueryProxyHints() {
		assertThat(RuntimeHintsPredicates.proxies().forInterfaces(Query.class)).accepts(this.hints);
	}

	@Test
	void entityManagerFactoryHasReflectionHints() {
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(EntityManagerFactory.class, "getCriteriaBuilder")).accepts(this.hints);
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(EntityManagerFactory.class, "getMetamodel")).accepts(this.hints);
	}

	@Test
	void persistenceUnitInfoDescriptorHasReflectionHints() {
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(PersistenceUnitInfoDescriptor.class, "getName"))
				.accepts(this.hints);
	}

}
