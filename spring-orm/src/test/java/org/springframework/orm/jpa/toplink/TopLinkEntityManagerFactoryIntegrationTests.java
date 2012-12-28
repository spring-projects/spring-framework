/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.orm.jpa.toplink;

import org.springframework.orm.jpa.AbstractContainerEntityManagerFactoryIntegrationTests;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;

/**
 * TopLink-specific JPA tests.
 *
 * @author Costin Leau
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class TopLinkEntityManagerFactoryIntegrationTests extends AbstractContainerEntityManagerFactoryIntegrationTests {

	@Override
	protected String[] getConfigLocations() {
		return TOPLINK_CONFIG_LOCATIONS;
	}


	public void testCanCastNativeEntityManagerFactoryToTopLinkEntityManagerFactoryImpl() {
		EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) entityManagerFactory;
		assertTrue(emfi.getNativeEntityManagerFactory().getClass().getName().endsWith("EntityManagerFactoryImpl"));
	}

	public void testCanCastSharedEntityManagerProxyToTopLinkEntityManager() {
		assertTrue(sharedEntityManager instanceof oracle.toplink.essentials.ejb.cmp3.EntityManager);
		oracle.toplink.essentials.ejb.cmp3.EntityManager toplinkEntityManager =
				(oracle.toplink.essentials.ejb.cmp3.EntityManager) sharedEntityManager;
		assertNotNull(toplinkEntityManager.getActiveSession());
	}

}
