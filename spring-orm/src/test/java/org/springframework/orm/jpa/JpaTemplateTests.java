/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.dao.DataAccessException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Costin Leau
 */
public class JpaTemplateTests extends TestCase {

	private JpaTemplate template;

	private MockControl factoryControl, managerControl;

	private EntityManager manager;

	private EntityManagerFactory factory;

	protected void setUp() throws Exception {
		template = new JpaTemplate();

		factoryControl = MockControl.createControl(EntityManagerFactory.class);
		factory = (EntityManagerFactory) factoryControl.getMock();
		managerControl = MockControl.createControl(EntityManager.class);
		manager = (EntityManager) managerControl.getMock();

		template.setEntityManager(manager);
		template.afterPropertiesSet();

	}

	protected void tearDown() throws Exception {
		template = null;
		factoryControl = null;
		managerControl = null;
		manager = null;
		factory = null;
	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.JpaTemplate(EntityManagerFactory)'
	 */
	public void testJpaTemplateEntityManagerFactory() {

	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.JpaTemplate(EntityManager)'
	 */
	public void testJpaTemplateEntityManager() {

	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.execute(JpaCallback)'
	 */
	public void testExecuteJpaCallback() {
		template.setExposeNativeEntityManager(true);
		template.setEntityManager(manager);
		template.afterPropertiesSet();

		managerControl.replay();
		factoryControl.replay();

		template.execute(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {
				assertSame(em, manager);
				return null;
			}
		});

		template.setExposeNativeEntityManager(false);
		template.execute(new JpaCallback() {

			public Object doInJpa(EntityManager em) throws PersistenceException {
				assertNotSame(em, manager);
				return null;
			}
		});
		managerControl.verify();
		factoryControl.verify();
	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.executeFind(JpaCallback)'
	 */
	public void testExecuteFind() {
		template.setEntityManager(manager);
		template.setExposeNativeEntityManager(true);
		template.afterPropertiesSet();

		managerControl.replay();
		factoryControl.replay();

		try {
			template.executeFind(new JpaCallback() {

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

		managerControl.verify();
		factoryControl.verify();
	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.execute(JpaCallback, boolean)'
	 */
	public void testExecuteJpaCallbackBoolean() {
		template = new JpaTemplate();
		template.setExposeNativeEntityManager(false);
		template.setEntityManagerFactory(factory);
		template.afterPropertiesSet();

		factoryControl.expectAndReturn(factory.createEntityManager(), manager);
		managerControl.expectAndReturn(manager.isOpen(), true);
		manager.close();

		managerControl.replay();
		factoryControl.replay();

		template.execute(new JpaCallback() {
			public Object doInJpa(EntityManager em) throws PersistenceException {
				assertSame(em, manager);
				return null;
			}
		}, true);

		managerControl.verify();
		factoryControl.verify();
	}

	public void testExecuteJpaCallbackBooleanWithPrebound() {
		template.setExposeNativeEntityManager(false);
		template.setEntityManagerFactory(factory);
		template.afterPropertiesSet();

		TransactionSynchronizationManager.bindResource(factory, new EntityManagerHolder(manager));
		managerControl.replay();
		factoryControl.replay();

		try {
			template.execute(new JpaCallback() {

				public Object doInJpa(EntityManager em) throws PersistenceException {
					assertSame(em, manager);
					return null;
				}
			}, true);

			managerControl.verify();
			factoryControl.verify();
		}
		finally {
			TransactionSynchronizationManager.unbindResource(factory);
		}
	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.createSharedEntityManager(EntityManager)'
	 */
	public void testCreateEntityManagerProxy() {
		manager.clear();
		managerControl.replay();

		EntityManager proxy = template.createEntityManagerProxy(manager);
		assertNotSame(manager, proxy);
		assertFalse(manager.equals(proxy));
		assertFalse(manager.hashCode() == proxy.hashCode());
		// close call not propagated to the em
		proxy.close();
		proxy.clear();

		managerControl.verify();
	}

	/*
	 * Test method for 'org.springframework.orm.jpa.JpaTemplate.find(Class<T>,
	 * Object) <T>'
	 */
	public void testFindClassOfTObject() {
		Integer result = new Integer(1);
		Object id = new Object();
		managerControl.expectAndReturn(manager.find(Number.class, id), result);
		managerControl.replay();
		factoryControl.replay();

		assertSame(result, template.find(Number.class, id));

		managerControl.verify();
		factoryControl.verify();
	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.getReference(Class<T>, Object)
	 * <T>'
	 */
	public void testGetReference() {
		Integer reference = new Integer(1);
		Object id = new Object();
		managerControl.expectAndReturn(manager.getReference(Number.class, id), reference);
		managerControl.replay();
		factoryControl.replay();

		assertSame(reference, template.getReference(Number.class, id));

		managerControl.verify();
		factoryControl.verify();
	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.contains(Object)'
	 */
	public void testContains() {
		boolean result = true;
		Object entity = new Object();
		managerControl.expectAndReturn(manager.contains(entity), result);
		managerControl.replay();
		factoryControl.replay();

		assertSame(result, template.contains(entity));

		managerControl.verify();
		factoryControl.verify();
	}

	/*
	 * Test method for 'org.springframework.orm.jpa.JpaTemplate.refresh(Object)'
	 */
	public void testRefresh() {
		Object entity = new Object();
		manager.refresh(entity);
		managerControl.replay();
		factoryControl.replay();

		template.refresh(entity);

		managerControl.verify();
		factoryControl.verify();
	}

	/*
	 * Test method for 'org.springframework.orm.jpa.JpaTemplate.persist(Object)'
	 */
	public void testPersist() {
		Object entity = new Object();
		manager.persist(entity);
		managerControl.replay();
		factoryControl.replay();

		template.persist(entity);

		managerControl.verify();
		factoryControl.verify();
	}

	/*
	 * Test method for 'org.springframework.orm.jpa.JpaTemplate.merge(T) <T>'
	 */
	public void testMerge() {
		Object result = new Object();
		Object entity = new Object();
		managerControl.expectAndReturn(manager.merge(entity), result);
		managerControl.replay();
		factoryControl.replay();

		assertSame(result, template.merge(entity));

		managerControl.verify();
		factoryControl.verify();
	}

	/*
	 * Test method for 'org.springframework.orm.jpa.JpaTemplate.remove(Object)'
	 */
	public void testRemove() {
		Object entity = new Object();
		manager.remove(entity);
		managerControl.replay();
		factoryControl.replay();

		template.remove(entity);

		managerControl.verify();
		factoryControl.verify();
	}

	/*
	 * Test method for 'org.springframework.orm.jpa.JpaTemplate.flush()'
	 */
	public void testFlush() {
		manager.flush();
		managerControl.replay();
		factoryControl.replay();

		template.flush();

		managerControl.verify();
		factoryControl.verify();
	}

	/*
	 * Test method for 'org.springframework.orm.jpa.JpaTemplate.find(String)'
	 */
	public void testFindString() {
		String queryString = "some query";
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();
		List result = new ArrayList();

		managerControl.expectAndReturn(manager.createQuery(queryString), query);
		queryControl.expectAndReturn(query.getResultList(), result);

		managerControl.replay();
		factoryControl.replay();
		queryControl.replay();

		assertSame(result, template.find(queryString));

		managerControl.verify();
		factoryControl.verify();
		queryControl.verify();
	}

	/*
	 * Test method for 'org.springframework.orm.jpa.JpaTemplate.find(String,
	 * Object...)'
	 */
	public void testFindStringObjectArray() {
		String queryString = "some query";
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();
		List result = new ArrayList();
		Object param1 = new Object();
		Object param2 = new Object();
		Object[] params = new Object[] { param1, param2 };

		managerControl.expectAndReturn(manager.createQuery(queryString), query);
		queryControl.expectAndReturn(query.setParameter(1, param1), null);
		queryControl.expectAndReturn(query.setParameter(2, param2), null);

		queryControl.expectAndReturn(query.getResultList(), result);

		managerControl.replay();
		factoryControl.replay();
		queryControl.replay();

		assertSame(result, template.find(queryString, params));

		managerControl.verify();
		factoryControl.verify();
		queryControl.verify();
	}

	/*
	 * Test method for 'org.springframework.orm.jpa.JpaTemplate.find(String, Map<String,
	 * Object>)'
	 */
	public void testFindStringMapOfStringObject() {
		String queryString = "some query";
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();
		List result = new ArrayList();
		Object param1 = new Object();
		Object param2 = new Object();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("param1", param1);
		params.put("param2", param2);

		managerControl.expectAndReturn(manager.createQuery(queryString), query);
		queryControl.expectAndReturn(query.setParameter("param1", param1), null);
		queryControl.expectAndReturn(query.setParameter("param2", param2), null);

		queryControl.expectAndReturn(query.getResultList(), result);

		managerControl.replay();
		factoryControl.replay();
		queryControl.replay();

		assertSame(result, template.findByNamedParams(queryString, params));

		managerControl.verify();
		factoryControl.verify();
		queryControl.verify();
	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.findByNamedQuery(String)'
	 */
	public void testFindByNamedQueryString() {
		String queryName = "some query name";
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();
		List result = new ArrayList();

		managerControl.expectAndReturn(manager.createNamedQuery(queryName), query);

		queryControl.expectAndReturn(query.getResultList(), result);

		managerControl.replay();
		factoryControl.replay();
		queryControl.replay();

		assertSame(result, template.findByNamedQuery(queryName));

		managerControl.verify();
		factoryControl.verify();
		queryControl.verify();
	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.findByNamedQuery(String,
	 * Object...)'
	 */
	public void testFindByNamedQueryStringObjectArray() {
		String queryName = "some query name";
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();
		List result = new ArrayList();
		Object param1 = new Object();
		Object param2 = new Object();
		Object[] params = new Object[] { param1, param2 };

		managerControl.expectAndReturn(manager.createNamedQuery(queryName), query);
		queryControl.expectAndReturn(query.setParameter(1, param1), null);
		queryControl.expectAndReturn(query.setParameter(2, param2), null);

		queryControl.expectAndReturn(query.getResultList(), result);

		managerControl.replay();
		factoryControl.replay();
		queryControl.replay();

		assertSame(result, template.findByNamedQuery(queryName, params));

		managerControl.verify();
		factoryControl.verify();
		queryControl.verify();
	}

	/*
	 * Test method for
	 * 'org.springframework.orm.jpa.JpaTemplate.findByNamedQuery(String, Map<String,
	 * Object>)'
	 */
	public void testFindByNamedQueryStringMapOfStringObject() {
		String queryName = "some query name";
		MockControl queryControl = MockControl.createControl(Query.class);
		Query query = (Query) queryControl.getMock();
		List result = new ArrayList();
		Object param1 = new Object();
		Object param2 = new Object();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("param1", param1);
		params.put("param2", param2);

		managerControl.expectAndReturn(manager.createNamedQuery(queryName), query);
		queryControl.expectAndReturn(query.setParameter("param1", param1), null);
		queryControl.expectAndReturn(query.setParameter("param2", param2), null);

		queryControl.expectAndReturn(query.getResultList(), result);

		managerControl.replay();
		factoryControl.replay();
		queryControl.replay();

		assertSame(result, template.findByNamedQueryAndNamedParams(queryName, params));

		managerControl.verify();
		factoryControl.verify();
		queryControl.verify();
	}

}
