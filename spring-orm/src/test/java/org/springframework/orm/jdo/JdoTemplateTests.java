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

package org.springframework.orm.jdo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.jdo.JDODataStoreException;
import javax.jdo.JDOException;
import javax.jdo.JDOFatalDataStoreException;
import javax.jdo.JDOFatalUserException;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.JDOOptimisticVerificationException;
import javax.jdo.JDOUserException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;

import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 03.06.2003
 */
public class JdoTemplateTests {

	private PersistenceManagerFactory pmf;
	private PersistenceManager pm;

	@Before
	public void setUp() {
		pmf = mock(PersistenceManagerFactory.class);
		pm = mock(PersistenceManager.class);
	}

	@Test
	public void testTemplateExecuteWithNotAllowCreate() {
		JdoTemplate jt = new JdoTemplate();
		jt.setPersistenceManagerFactory(pmf);
		jt.setAllowCreate(false);
		try {
			jt.execute(new JdoCallback() {
				@Override
				public Object doInJdo(PersistenceManager pm) {
					return null;
				}
			});
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	public void testTemplateExecuteWithNotAllowCreateAndThreadBound() {
		JdoTemplate jt = new JdoTemplate(pmf);
		jt.setAllowCreate(false);
		TransactionSynchronizationManager.bindResource(pmf, new PersistenceManagerHolder(pm));
		final List l = new ArrayList();
		l.add("test");
		List result = (List) jt.execute(new JdoCallback() {
			@Override
			public Object doInJdo(PersistenceManager pm) {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);
		TransactionSynchronizationManager.unbindResource(pmf);
	}

	@Test
	public void testTemplateExecuteWithNewPersistenceManager() {
		given(pmf.getPersistenceManager()).willReturn(pm);

		JdoTemplate jt = new JdoTemplate(pmf);
		final List l = new ArrayList();
		l.add("test");
		List result = (List) jt.execute(new JdoCallback() {
			@Override
			public Object doInJdo(PersistenceManager pm) {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);
		verify(pm).close();
	}

	@Test
	public void testTemplateExecuteWithThreadBoundAndFlushEager() {
		JdoTemplate jt = new JdoTemplate(pmf);
		jt.setFlushEager(true);
		jt.setAllowCreate(false);
		TransactionSynchronizationManager.bindResource(pmf, new PersistenceManagerHolder(pm));
		final List l = new ArrayList();
		l.add("test");
		List result = (List) jt.execute(new JdoCallback() {
			@Override
			public Object doInJdo(PersistenceManager pm) {
				return l;
			}
		});
		assertTrue("Correct result list", result == l);
		TransactionSynchronizationManager.unbindResource(pmf);
		verify(pm).flush();
	}

	@Test
	public void testGetObjectById() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.getObjectById("0", true)).willReturn("A");
		JdoTemplate jt = new JdoTemplate(pmf);
		assertEquals("A", jt.getObjectById("0"));
		verify(pm).close();
	}

	@Test
	public void testGetObjectByIdWithClassAndValue() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.getObjectById(String.class, "0")).willReturn("A");
		JdoTemplate jt = new JdoTemplate(pmf);
		assertEquals("A", jt.getObjectById(String.class, "0"));
		verify(pm).close();
	}

	@Test
	public void testEvict() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		JdoTemplate jt = new JdoTemplate(pmf);
		jt.evict("0");
		verify(pm).evict("0");
		verify(pm).close();
	}

	@Test
	public void testEvictAllWithCollection() {
		Collection coll = new HashSet();
		given(pmf.getPersistenceManager()).willReturn(pm);
		JdoTemplate jt = new JdoTemplate(pmf);
		jt.evictAll(coll);
		verify(pm).evictAll(coll);
		verify(pm).close();
	}

	@Test
	public void testEvictAll() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		JdoTemplate jt = new JdoTemplate(pmf);
		jt.evictAll();
		verify(pm).evictAll();
		verify(pm).close();
	}

	@Test
	public void testRefresh() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		JdoTemplate jt = new JdoTemplate(pmf);
		jt.refresh("0");
		verify(pm).refresh("0");
		verify(pm).close();
	}

	@Test
	public void testRefreshAllWithCollection() {
		Collection coll = new HashSet();
		given(pmf.getPersistenceManager()).willReturn(pm);
		JdoTemplate jt = new JdoTemplate(pmf);
		jt.refreshAll(coll);
		verify(pm).refreshAll(coll);
		verify(pm).close();
	}

	@Test
	public void testRefreshAll() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		JdoTemplate jt = new JdoTemplate(pmf);
		jt.refreshAll();
		verify(pm).refreshAll();
		verify(pm).close();
	}

	@Test
	public void testMakePersistent() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		JdoTemplate jt = new JdoTemplate(pmf);
		jt.makePersistent("0");
		verify(pm).makePersistent("0");
		verify(pm).close();
	}

	@Test
	public void testMakePersistentAll() {
		Collection coll = new HashSet();
		given(pmf.getPersistenceManager()).willReturn(pm);
		JdoTemplate jt = new JdoTemplate(pmf);
		jt.makePersistentAll(coll);
		verify(pm).makePersistentAll(coll);
		verify(pm).close();
	}

	@Test
	public void testDeletePersistent() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		JdoTemplate jt = new JdoTemplate(pmf);
		jt.deletePersistent("0");
		verify(pm).deletePersistent("0");
		verify(pm).close();
	}

	@Test
	public void testDeletePersistentAll() {
		Collection coll = new HashSet();
		given(pmf.getPersistenceManager()).willReturn(pm);
		JdoTemplate jt = new JdoTemplate(pmf);
		jt.deletePersistentAll(coll);
		verify(pm).deletePersistentAll(coll);
		verify(pm).close();
	}

	@Test
	public void testDetachCopy() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.detachCopy("0")).willReturn("0x");
		JdoTemplate jt = new JdoTemplate(pmf);
		assertEquals("0x", jt.detachCopy("0"));
		verify(pm).close();
	}

	@Test
	public void testDetachCopyAll() {
		Collection attached = new HashSet();
		Collection detached = new HashSet();
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.detachCopyAll(attached)).willReturn(detached);
		JdoTemplate jt = new JdoTemplate(pmf);
		assertEquals(detached, jt.detachCopyAll(attached));
		verify(pm).close();
	}

	@Test
	public void testFlush() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		JdoTemplate jt = new JdoTemplate(pmf);
		jt.flush();
		verify(pm).flush();
		verify(pm).close();
	}

	@Test
	public void testFlushWithDialect() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		JdoTemplate jt = new JdoTemplate(pmf);
		jt.flush();
		verify(pm).flush();
		verify(pm).close();
	}

	@Test
	public void testFind() {
		Query query = mock(Query.class);
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.newQuery(String.class)).willReturn(query);
		Collection coll = new HashSet();
		given(query.execute()).willReturn(coll);
		JdoTemplate jt = new JdoTemplate(pmf);
		assertEquals(coll, jt.find(String.class));
		verify(pm).close();
	}

	@Test
	public void testFindWithFilter() {
		Query query = mock(Query.class);
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.newQuery(String.class, "a == b")).willReturn(query);
		Collection coll = new HashSet();
		given(query.execute()).willReturn(coll);
		JdoTemplate jt = new JdoTemplate(pmf);
		assertEquals(coll, jt.find(String.class, "a == b"));
		verify(pm).close();
	}

	@Test
	public void testFindWithFilterAndOrdering() {
		Query query = mock(Query.class);
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.newQuery(String.class, "a == b")).willReturn(query);
		Collection coll = new HashSet();
		given(query.execute()).willReturn(coll);
		JdoTemplate jt = new JdoTemplate(pmf);
		assertEquals(coll, jt.find(String.class, "a == b", "c asc"));
		verify(query).setOrdering("c asc");
		verify(pm).close();
	}

	@Test
	public void testFindWithParameterArray() {
		Query query = mock(Query.class);
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.newQuery(String.class, "a == b")).willReturn(query);
		Object[] values = new Object[0];
		Collection coll = new HashSet();
		given(query.executeWithArray(values)).willReturn(coll);
		JdoTemplate jt = new JdoTemplate(pmf);
		assertEquals(coll, jt.find(String.class, "a == b", "params", values));
		verify(query).declareParameters("params");
		verify(pm).close();
	}

	@Test
	public void testFindWithParameterArrayAndOrdering() {
		Query query = mock(Query.class);
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.newQuery(String.class, "a == b")).willReturn(query);
		Object[] values = new Object[0];
		Collection coll = new HashSet();
		given(query.executeWithArray(values)).willReturn(coll);
		JdoTemplate jt = new JdoTemplate(pmf);
		assertEquals(coll, jt.find(String.class, "a == b", "params", values, "c asc"));
		verify(query).declareParameters("params");
		verify(query).setOrdering("c asc");
		verify(pm).close();
	}

	@Test
	public void testFindWithParameterMap() {
		Query query = mock(Query.class);
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.newQuery(String.class, "a == b")).willReturn(query);
		Map values = new HashMap();
		Collection coll = new HashSet();
		given(query.executeWithMap(values)).willReturn(coll);
		JdoTemplate jt = new JdoTemplate(pmf);
		assertEquals(coll, jt.find(String.class, "a == b", "params", values));
		verify(query).declareParameters("params");
		verify(pm).close();
	}

	@Test
	public void testFindWithParameterMapAndOrdering() {
		Query query = mock(Query.class);
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.newQuery(String.class, "a == b")).willReturn(query);
		Map values = new HashMap();
		Collection coll = new HashSet();
		given(query.executeWithMap(values)).willReturn(coll);
		JdoTemplate jt = new JdoTemplate(pmf);
		assertEquals(coll, jt.find(String.class, "a == b", "params", values, "c asc"));
		verify(query).declareParameters("params");
		verify(query).setOrdering("c asc");
		verify(pm).close();
	}

	@Test
	public void testFindWithLanguageAndQueryObject() {
		Query query = mock(Query.class);
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.newQuery(Query.SQL, "some SQL")).willReturn(query);
		Collection coll = new HashSet();
		given(query.execute()).willReturn(coll);
		JdoTemplate jt = new JdoTemplate(pmf);
		assertEquals(coll, jt.find(Query.SQL, "some SQL"));
		verify(pm).close();
	}

	@Test
	public void testFindWithQueryString() {
		Query query = mock(Query.class);
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.newQuery("single string query")).willReturn(query);
		Collection coll = new HashSet();
		given(query.execute()).willReturn(coll);
		JdoTemplate jt = new JdoTemplate(pmf);
		assertEquals(coll, jt.find("single string query"));
		verify(pm).close();
	}

	@Test
	public void testFindByNamedQuery() {
		Query query = mock(Query.class);
		given(pmf.getPersistenceManager()).willReturn(pm);
		given(pm.newNamedQuery(String.class, "some query name")).willReturn(query);
		Collection coll = new HashSet();
		given(query.execute()).willReturn(coll);
		JdoTemplate jt = new JdoTemplate(pmf);
		assertEquals(coll, jt.findByNamedQuery(String.class, "some query name"));
		verify(pm).close();
	}

	@Test
	public void testTemplateExceptions() {
		try {
			createTemplate().execute(new JdoCallback() {
				@Override
				public Object doInJdo(PersistenceManager pm) {
					throw new JDOObjectNotFoundException();
				}
			});
			fail("Should have thrown JdoObjectRetrievalFailureException");
		}
		catch (JdoObjectRetrievalFailureException ex) {
			// expected
		}

		try {
			createTemplate().execute(new JdoCallback() {
				@Override
				public Object doInJdo(PersistenceManager pm) {
					throw new JDOOptimisticVerificationException();
				}
			});
			fail("Should have thrown JdoOptimisticLockingFailureException");
		}
		catch (JdoOptimisticLockingFailureException ex) {
			// expected
		}

		try {
			createTemplate().execute(new JdoCallback() {
				@Override
				public Object doInJdo(PersistenceManager pm) {
					throw new JDODataStoreException();
				}
			});
			fail("Should have thrown JdoResourceFailureException");
		}
		catch (JdoResourceFailureException ex) {
			// expected
		}

		try {
			createTemplate().execute(new JdoCallback() {
				@Override
				public Object doInJdo(PersistenceManager pm) {
					throw new JDOFatalDataStoreException();
				}
			});
			fail("Should have thrown JdoResourceFailureException");
		}
		catch (JdoResourceFailureException ex) {
			// expected
		}

		try {
			createTemplate().execute(new JdoCallback() {
				@Override
				public Object doInJdo(PersistenceManager pm) {
					throw new JDOUserException();
				}
			});
			fail("Should have thrown JdoUsageException");
		}
		catch (JdoUsageException ex) {
			// expected
		}

		try {
			createTemplate().execute(new JdoCallback() {
				@Override
				public Object doInJdo(PersistenceManager pm) {
					throw new JDOFatalUserException();
				}
			});
			fail("Should have thrown JdoUsageException");
		}
		catch (JdoUsageException ex) {
			// expected
		}

		try {
			createTemplate().execute(new JdoCallback() {
				@Override
				public Object doInJdo(PersistenceManager pm) {
					throw new JDOException();
				}
			});
			fail("Should have thrown JdoSystemException");
		}
		catch (JdoSystemException ex) {
			// expected
		}
	}

	@Test
	public void testTranslateException() {
		JdoDialect dialect = mock(JdoDialect.class);
		final JDOException ex = new JDOException();
		given(dialect.translateException(ex)).willReturn(new DataIntegrityViolationException("test", ex));
		try {
			JdoTemplate template = createTemplate();
			template.setJdoDialect(dialect);
			template.execute(new JdoCallback() {
				@Override
				public Object doInJdo(PersistenceManager pm) {
					throw ex;
				}
			});
			fail("Should have thrown DataIntegrityViolationException");
		}
		catch (DataIntegrityViolationException dive) {
			// expected
		}
	}

	private JdoTemplate createTemplate() {
		given(pmf.getPersistenceManager()).willReturn(pm);
		return new JdoTemplate(pmf);
	}

}
