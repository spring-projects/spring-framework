/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.orm.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.TransactionRequiredException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.withSettings;

/**
 * Unit tests for {@link SharedEntityManagerCreator}.
 *
 * @author Oliver Gierke
 * @author Juergen Hoeller
 */
@ExtendWith(MockitoExtension.class)
public class SharedEntityManagerCreatorTests {

	@Test
	public void proxyingWorksIfInfoReturnsNullEntityManagerInterface() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class,
				withSettings().extraInterfaces(EntityManagerFactoryInfo.class));
		// EntityManagerFactoryInfo.getEntityManagerInterface returns null
		assertThat(SharedEntityManagerCreator.createSharedEntityManager(emf)).isNotNull();
	}

	@Test
	public void transactionRequiredExceptionOnJoinTransaction() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(emf);
		assertThatExceptionOfType(TransactionRequiredException.class).isThrownBy(
				em::joinTransaction);
	}

	@Test
	public void transactionRequiredExceptionOnFlush() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(emf);
		assertThatExceptionOfType(TransactionRequiredException.class).isThrownBy(
				em::flush);
	}

	@Test
	public void transactionRequiredExceptionOnPersist() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(emf);
		assertThatExceptionOfType(TransactionRequiredException.class).isThrownBy(() ->
				em.persist(new Object()));
	}

	@Test
	public void transactionRequiredExceptionOnMerge() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(emf);
		assertThatExceptionOfType(TransactionRequiredException.class).isThrownBy(() ->
				em.merge(new Object()));
	}

	@Test
	public void transactionRequiredExceptionOnRemove() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(emf);
		assertThatExceptionOfType(TransactionRequiredException.class).isThrownBy(() ->
				em.remove(new Object()));
	}

	@Test
	public void transactionRequiredExceptionOnRefresh() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(emf);
		assertThatExceptionOfType(TransactionRequiredException.class).isThrownBy(() ->
				em.refresh(new Object()));
	}

	@Test
	public void deferredQueryWithUpdate() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager targetEm = mock(EntityManager.class);
		Query query = mock(Query.class);
		given(emf.createEntityManager()).willReturn(targetEm);
		given(targetEm.createQuery("x")).willReturn(query);
		given(targetEm.isOpen()).willReturn(true);

		EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(emf);
		em.createQuery("x").executeUpdate();

		verify(query).executeUpdate();
		verify(targetEm).close();
	}

	@Test
	public void deferredQueryWithSingleResult() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager targetEm = mock(EntityManager.class);
		Query query = mock(Query.class);
		given(emf.createEntityManager()).willReturn(targetEm);
		given(targetEm.createQuery("x")).willReturn(query);
		given(targetEm.isOpen()).willReturn(true);

		EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(emf);
		em.createQuery("x").getSingleResult();

		verify(query).getSingleResult();
		verify(targetEm).close();
	}

	@Test
	public void deferredQueryWithResultList() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager targetEm = mock(EntityManager.class);
		Query query = mock(Query.class);
		given(emf.createEntityManager()).willReturn(targetEm);
		given(targetEm.createQuery("x")).willReturn(query);
		given(targetEm.isOpen()).willReturn(true);

		EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(emf);
		em.createQuery("x").getResultList();

		verify(query).getResultList();
		verify(targetEm).close();
	}

	@Test
	public void deferredQueryWithResultStream() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager targetEm = mock(EntityManager.class);
		Query query = mock(Query.class);
		given(emf.createEntityManager()).willReturn(targetEm);
		given(targetEm.createQuery("x")).willReturn(query);
		given(targetEm.isOpen()).willReturn(true);

		EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(emf);
		em.createQuery("x").getResultStream();

		verify(query).getResultStream();
		verify(targetEm).close();
	}

	@Test
	public void deferredStoredProcedureQueryWithIndexedParameters() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager targetEm = mock(EntityManager.class);
		StoredProcedureQuery query = mock(StoredProcedureQuery.class);
		given(emf.createEntityManager()).willReturn(targetEm);
		given(targetEm.createStoredProcedureQuery("x")).willReturn(query);
		willReturn("y").given(query).getOutputParameterValue(0);
		willReturn("z").given(query).getOutputParameterValue(2);
		given(targetEm.isOpen()).willReturn(true);

		EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(emf);
		StoredProcedureQuery spq = em.createStoredProcedureQuery("x");
		spq.registerStoredProcedureParameter(0, String.class, ParameterMode.OUT);
		spq.registerStoredProcedureParameter(1, Number.class, ParameterMode.IN);
		spq.registerStoredProcedureParameter(2, Object.class, ParameterMode.INOUT);
		spq.execute();
		assertThat(spq.getOutputParameterValue(0)).isEqualTo("y");
		assertThatIllegalArgumentException().isThrownBy(() ->
				spq.getOutputParameterValue(1));
		assertThat(spq.getOutputParameterValue(2)).isEqualTo("z");

		verify(query).registerStoredProcedureParameter(0, String.class, ParameterMode.OUT);
		verify(query).registerStoredProcedureParameter(1, Number.class, ParameterMode.IN);
		verify(query).registerStoredProcedureParameter(2, Object.class, ParameterMode.INOUT);
		verify(query).execute();
		verify(targetEm).close();
		verifyNoMoreInteractions(query);
		verifyNoMoreInteractions(targetEm);
	}

	@Test
	public void deferredStoredProcedureQueryWithNamedParameters() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager targetEm = mock(EntityManager.class);
		StoredProcedureQuery query = mock(StoredProcedureQuery.class);
		given(emf.createEntityManager()).willReturn(targetEm);
		given(targetEm.createStoredProcedureQuery("x")).willReturn(query);
		willReturn("y").given(query).getOutputParameterValue("a");
		willReturn("z").given(query).getOutputParameterValue("c");
		given(targetEm.isOpen()).willReturn(true);

		EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(emf);
		StoredProcedureQuery spq = em.createStoredProcedureQuery("x");
		spq.registerStoredProcedureParameter("a", String.class, ParameterMode.OUT);
		spq.registerStoredProcedureParameter("b", Number.class, ParameterMode.IN);
		spq.registerStoredProcedureParameter("c", Object.class, ParameterMode.INOUT);
		spq.execute();
		assertThat(spq.getOutputParameterValue("a")).isEqualTo("y");
		assertThatIllegalArgumentException().isThrownBy(() ->
				spq.getOutputParameterValue("b"));
		assertThat(spq.getOutputParameterValue("c")).isEqualTo("z");

		verify(query).registerStoredProcedureParameter("a", String.class, ParameterMode.OUT);
		verify(query).registerStoredProcedureParameter("b", Number.class, ParameterMode.IN);
		verify(query).registerStoredProcedureParameter("c", Object.class, ParameterMode.INOUT);
		verify(query).execute();
		verify(targetEm).close();
		verifyNoMoreInteractions(query);
		verifyNoMoreInteractions(targetEm);
	}

}
