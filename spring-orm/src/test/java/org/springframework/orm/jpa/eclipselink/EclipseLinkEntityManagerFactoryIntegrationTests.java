/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.eclipselink;

import org.eclipse.persistence.jpa.JpaEntityManager;
import org.junit.jupiter.api.Test;

import org.springframework.orm.jpa.AbstractContainerEntityManagerFactoryIntegrationTests;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EclipseLink-specific JPA tests.
 *
 * @author Juergen Hoeller
 */
class EclipseLinkEntityManagerFactoryIntegrationTests extends AbstractContainerEntityManagerFactoryIntegrationTests {

	@Test
	void canCastNativeEntityManagerFactoryToEclipseLinkEntityManagerFactoryImpl() {
		EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) entityManagerFactory;
		assertThat(emfi.getNativeEntityManagerFactory().getClass().getName()).endsWith("EntityManagerFactoryImpl");
	}

	@Test
	void canCastSharedEntityManagerProxyToEclipseLinkEntityManager() {
		assertThat(sharedEntityManager).isInstanceOf(JpaEntityManager.class);
		JpaEntityManager eclipselinkEntityManager = (JpaEntityManager) sharedEntityManager;
		assertThat(eclipselinkEntityManager.getActiveSession()).isNotNull();
	}

}
