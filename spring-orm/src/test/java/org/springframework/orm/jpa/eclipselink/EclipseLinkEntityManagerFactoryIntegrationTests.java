/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.orm.jpa.eclipselink;

import org.eclipse.persistence.jpa.JpaEntityManager;

import org.springframework.orm.jpa.AbstractContainerEntityManagerFactoryIntegrationTests;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;

/**
 * EclipseLink-specific JPA tests.
 *
 * @author Juergen Hoeller
 */
@SuppressWarnings("deprecation")
public class EclipseLinkEntityManagerFactoryIntegrationTests extends AbstractContainerEntityManagerFactoryIntegrationTests {

	@Override
	protected String[] getConfigPaths() {
		return ECLIPSELINK_CONFIG_LOCATIONS;
	}


	public void testCanCastNativeEntityManagerFactoryToEclipseLinkEntityManagerFactoryImpl() {
		EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) entityManagerFactory;
		assertTrue(emfi.getNativeEntityManagerFactory().getClass().getName().endsWith("EntityManagerFactoryImpl"));
	}

	public void testCanCastSharedEntityManagerProxyToEclipseLinkEntityManager() {
		assertTrue(sharedEntityManager instanceof JpaEntityManager);
		JpaEntityManager eclipselinkEntityManager = (JpaEntityManager) sharedEntityManager;
		assertNotNull(eclipselinkEntityManager.getActiveSession());
	}

}
