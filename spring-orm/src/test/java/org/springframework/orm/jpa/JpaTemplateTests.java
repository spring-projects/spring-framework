/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.orm.jpa;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Costin Leau
 * @author Phillip Webb
 */
public class JpaTemplateTests {

	private JpaTemplate template;

	private EntityManager manager;

	private EntityManagerFactory factory;

	@Before
	public void setUp() throws Exception {
		template = new JpaTemplate();

		factory = mock(EntityManagerFactory.class);
		manager = mock(EntityManager.class);

		template.setEntityManager(manager);
		template.afterPropertiesSet();

	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.JpaTemplate(EntityManagerFactory)'
	 */
	@Test
	public void testJpaTemplateEntityManagerFactory() {

	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.JpaTemplate(EntityManager)'
	 */
	@Test
	public void testJpaTemplateEntityManager() {

	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.execute(JpaCallback)'
	 */
	@Test
	public void testExecuteJpaCallback() {
		template.setExposeNativeEntityManager(true);
		template.setEntityManager(manager);
		template.afterPropertiesSet();

		template.execute(new JpaCallback() {

			@Override
			public Object doInJpa(EntityManager em) throws PersistenceException {
				assertSame(em, manager);
				return null;
			}
		});

		template.setExposeNativeEntityManager(false);
		template.execute(new JpaCallback() {

			@Override
			public Object doInJpa(EntityManager em) throws PersistenceException {
				assertNotSame(em, manager);
				return null;
			}
		});
	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.executeFind(JpaCallback)'
	 */
	@Test
	public void testExecuteFind() {
		template.setEntityManager(manager);
		template.setExposeNativeEntityManager(true);
		template.afterPropertiesSet();

		try {
			template.executeFind(new JpaCallback() {

				@Override
				public Object doInJpa(EntityManager em) throws PersistenceException {
					assertSame(em, manager);
					return new Object();
				}
			});
			fail("should have thrown exception");
		}
		catch (DataAccessException e) {
			// expected
		}
	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.execute(JpaCallback, boolean)'
	 */
	@Test
	public void testExecuteJpaCallbackBoolean() {
		template = new JpaTemplate();
		template.setExposeNativeEntityManager(false);
		template.setEntityManagerFactory(factory);
		template.afterPropertiesSet();

		given(factory.createEntityManager()).willReturn(manager);
		given(manager.isOpen()).willReturn(true);
		manager.close();

		template.execute(new JpaCallback() {
			@Override
			public Object doInJpa(EntityManager em) throws PersistenceException {
				assertSame(em, manager);
				return null;
			}
		}, true);
	}

	@Test
	public void testExecuteJpaCallbackBooleanWithPrebound() {
		template.setExposeNativeEntityManager(false);
		template.setEntityManagerFactory(factory);
		template.afterPropertiesSet();

		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));

		try {
			template.execute(new JpaCallback() {

				@Override
				public Object doInJpa(EntityManager em) throws PersistenceException {
					assertSame(em, manager);
					return null;
				}
			}, true);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}
	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.createSharedEntityManager(EntityManager)'
	 */
	@Test
	public void testCreateEntityManagerProxy() {

		EntityManager proxy = template.createEntityManagerProxy(manager);
		assertNotSame(manager, proxy);
		assertFalse(manager.equals(proxy));
		assertFalse(manager.hashCode() == proxy.hashCode());
		// close call not propagated to the em
		proxy.close();
		proxy.clear();

		verify(manager).clear();
	}

	/*
	 * Test method for 'org.springframework.orm.jpa.JpaTemplate.find(Class<T>,
	 * Object) <T>'
	 */
	@Test
	public void testFindClassOfTObject() {
		Integer result = new Integer(1);
		Object id = new Object();
		given(manager.find(Number.class, id)).willReturn(result);

		assertSame(result, template.find(Number.class, id));
	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.getReference(Class<T>, Object)
	 * <T>'
	 */
	@Test
	public void testGetReference() {
		Integer reference = new Integer(1);
		Object id = new Object();
		given(manager.getReference(Number.class, id)).willReturn(reference);

		assertSame(reference, template.getReference(Number.class, id));
	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.contains(Object)'
	 */
	@Test
	public void testContains() {
		boolean result = true;
		Object entity = new Object();
		given(manager.contains(entity)).willReturn(result);

		assertSame(result, template.contains(entity));
	}

	/*
	 * Test method for 'org.springframework.orm.jpa.JpaTemplate.refresh(Object)'
	 */
	@Test
	public void testRefresh() {
		Object entity = new Object();
		template.refresh(entity);
		verify(manager).refresh(entity);
	}

	/*
	 * Test method for 'org.springframework.orm.jpa.JpaTemplate.persist(Object)'
	 */
	@Test
	public void testPersist() {
		Object entity = new Object();
		template.persist(entity);
		verify(manager).persist(entity);
	}

	/*
	 * Test method for 'org.springframework.orm.jpa.JpaTemplate.merge(T) <T>'
	 */
	@Test
	public void testMerge() {
		Object result = new Object();
		Object entity = new Object();
		given(manager.merge(entity)).willReturn(result);
		assertSame(result, template.merge(entity));
	}

	/*
	 * Test method for 'org.springframework.orm.jpa.JpaTemplate.remove(Object)'
	 */
	@Test
	public void testRemove() {
		Object entity = new Object();
		template.remove(entity);
		verify(manager).remove(entity);
	}

	/*
	 * Test method for 'org.springframework.orm.jpa.JpaTemplate.flush()'
	 */
	@Test
	public void testFlush() {
		template.flush();
		verify(manager).flush();
	}

	/*
	 * Test method for 'org.springframework.orm.jpa.JpaTemplate.find(String)'
	 */
	@Test
	public void testFindString() {
		String queryString = "some query";
		Query query = mock(Query.class);
		List result = new ArrayList();

		given(manager.createQuery(queryString)).willReturn(query);
		given(query.getResultList()).willReturn(result);
		assertSame(result, template.find(queryString));
	}

	/*
	 * Test method for 'org.springframework.orm.jpa.JpaTemplate.find(String,
	 * Object...)'
	 */
	@Test
	public void testFindStringObjectArray() {
		String queryString = "some query";
		Query query = mock(Query.class);
		List result = new ArrayList();
		Object param1 = new Object();
		Object param2 = new Object();
		Object[] params = new Object[] { param1, param2 };

		given(manager.createQuery(queryString)).willReturn(query);
		given(query.getResultList()).willReturn(result);

		assertSame(result, template.find(queryString, params));

		verify(query).setParameter(1, param1);
		verify(query).setParameter(2, param2);
	}

	/*
	 * Test method for 'org.springframework.orm.jpa.JpaTemplate.find(String, Map<String,
	 * Object>)'
	 */
	@Test
	public void testFindStringMapOfStringObject() {
		String queryString = "some query";
		Query query = mock(Query.class);
		List result = new ArrayList();
		Object param1 = new Object();
		Object param2 = new Object();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("param1", param1);
		params.put("param2", param2);

		given(manager.createQuery(queryString)).willReturn(query);
		given(query.getResultList()).willReturn(result);

		assertSame(result, template.findByNamedParams(queryString, params));

		verify(query).setParameter("param1", param1);
		verify(query).setParameter("param2", param2);
	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.findByNamedQuery(String)'
	 */
	@Test
	public void testFindByNamedQueryString() {
		String queryName = "some query name";
		Query query = mock(Query.class);
		List result = new ArrayList();

		given(manager.createNamedQuery(queryName)).willReturn(query);
		given(query.getResultList()).willReturn(result);

		assertSame(result, template.findByNamedQuery(queryName));
	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.findByNamedQuery(String,
	 * Object...)'
	 */
	@Test
	public void testFindByNamedQueryStringObjectArray() {
		String queryName = "some query name";
		Query query = mock(Query.class);
		List result = new ArrayList();
		Object param1 = new Object();
		Object param2 = new Object();
		Object[] params = new Object[] { param1, param2 };

		given(manager.createNamedQuery(queryName)).willReturn(query);
		given(query.getResultList()).willReturn(result);

		assertSame(result, template.findByNamedQuery(queryName, params));

		verify(query).setParameter(1, param1);
		verify(query).setParameter(2, param2);
	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.findByNamedQuery(String, Map<String,
	 * Object>)'
	 */
	@Test
	public void testFindByNamedQueryStringMapOfStringObject() {
		String queryName = "some query name";
		Query query = mock(Query.class);
		List result = new ArrayList();
		Object param1 = new Object();
		Object param2 = new Object();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("param1", param1);
		params.put("param2", param2);

		given(manager.createNamedQuery(queryName)).willReturn(query);
		given(query.getResultList()).willReturn(result);

		assertSame(result, template.findByNamedQueryAndNamedParams(queryName, params));
		verify(query).setParameter("param1", param1);
		verify(query).setParameter("param2", param2);
	}
}
