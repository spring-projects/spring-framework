/*
 * Copyright 2002-2023 the original author or authors.
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;

import org.springframework.orm.jpa.AbstractContainerEntityManagerFactoryIntegrationTests;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.JRE.JAVA_18;

/**
 * EclipseLink-specific JPA tests.
 *
 * @author Juergen Hoeller
 */
@DisabledForJreRange(min = JAVA_18, disabledReason = "These JPA tests don't pass on Java 18+")
public class EclipseLinkEntityManagerFactoryIntegrationTests extends AbstractContainerEntityManagerFactoryIntegrationTests {

	@Test
	public void testCanCastNativeEntityManagerFactoryToEclipseLinkEntityManagerFactoryImpl() {
		EntityManagerFactoryInfo emfi = (EntityManagerFactoryInfo) entityManagerFactory;
		assertThat(emfi.getNativeEntityManagerFactory().getClass().getName()).endsWith("EntityManagerFactoryImpl");
	}

	@Test
	public void testCanCastSharedEntityManagerProxyToEclipseLinkEntityManager() {
		boolean condition = sharedEntityManager instanceof JpaEntityManager;
		assertThat(condition).isTrue();
		JpaEntityManager eclipselinkEntityManager = (JpaEntityManager) sharedEntityManager;
		assertThat(eclipselinkEntityManager.getActiveSession()).isNotNull();
	}

}
