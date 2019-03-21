/*
 * Copyright 2002-2018 the original author or authors.
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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.ParameterMode;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TransactionRequiredException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

/**
 * Unit tests for {@link SharedEntityManagerCreator}.
 *
 * @author Oliver Gierke
 * @author Juergen Hoeller
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class SharedEntityManagerCreatorTests {

	@Test
	public void proxyingWorksIfInfoReturnsNullEntityManagerInterface() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class,
				withSettings().extraInterfaces(EntityManagerFactoryInfo.class));
		// EntityManagerFactoryInfo.getEntityManagerInterface returns null
		assertThat(SharedEntityManagerCreator.createSharedEntityManager(emf), is(notNullValue()));
	}

	@Test(expected = TransactionRequiredException.class)
	public void transactionRequiredExceptionOnJoinTransaction() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(emf);
		em.joinTransaction();
	}

	@Test(expected = TransactionRequiredException.class)
	public void transactionRequiredExceptionOnFlush() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(emf);
		em.flush();
	}

	@Test(expected = TransactionRequiredException.class)
	public void transactionRequiredExceptionOnPersist() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(emf);
		em.persist(new Object());
	}

	@Test(expected = TransactionRequiredException.class)
	public void transactionRequiredExceptionOnMerge() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(emf);
		em.merge(new Object());
	}

	@Test(expected = TransactionRequiredException.class)
	public void transactionRequiredExceptionOnRemove() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(emf);
		em.remove(new Object());
	}

	@Test(expected = TransactionRequiredException.class)
	public void transactionRequiredExceptionOnRefresh() {
		EntityManagerFactory emf = mock(EntityManagerFactory.class);
		EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(emf);
		em.refresh(new Object());
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
		assertEquals("y", spq.getOutputParameterValue(0));
		try {
			spq.getOutputParameterValue(1);
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
		assertEquals("z", spq.getOutputParameterValue(2));

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
		assertEquals("y", spq.getOutputParameterValue("a"));
		try {
			spq.getOutputParameterValue("b");
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
		assertEquals("z", spq.getOutputParameterValue("c"));

		verify(query).registerStoredProcedureParameter("a", String.class, ParameterMode.OUT);
		verify(query).registerStoredProcedureParameter("b", Number.class, ParameterMode.IN);
		verify(query).registerStoredProcedureParameter("c", Object.class, ParameterMode.INOUT);
		verify(query).execute();
		verify(targetEm).close();
		verifyNoMoreInteractions(query);
		verifyNoMoreInteractions(targetEm);
	}

}
