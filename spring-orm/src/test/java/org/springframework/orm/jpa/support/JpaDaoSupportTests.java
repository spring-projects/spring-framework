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

package org.springframework.orm.jpa.support;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.Test;
import org.springframework.orm.jpa.JpaTemplate;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Costin Leau
 * @author Phillip Webb
 */
public class JpaDaoSupportTests {

	@Test
	public void testJpaDaoSupportWithEntityManager() throws Exception {
		EntityManager entityManager = mock(EntityManager.class);
		final List test = new ArrayList();
		JpaDaoSupport dao = new JpaDaoSupport() {
			@Override
			protected void initDao() {
				test.add("test");
			}
		};
		dao.setEntityManager(entityManager);
		dao.afterPropertiesSet();
		assertNotNull("jpa template not created", dao.getJpaTemplate());
		assertEquals("incorrect entity manager", entityManager, dao.getJpaTemplate().getEntityManager());
		assertEquals("initDao not called", test.size(), 1);
	}

	@Test
	public void testJpaDaoSupportWithEntityManagerFactory() throws Exception {
		EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
		final List test = new ArrayList();
		JpaDaoSupport dao = new JpaDaoSupport() {
			@Override
			protected void initDao() {
				test.add("test");
			}
		};
		dao.setEntityManagerFactory(entityManagerFactory);
		dao.afterPropertiesSet();
		assertNotNull("jpa template not created", dao.getJpaTemplate());
		assertEquals("incorrect entity manager factory", entityManagerFactory,
				dao.getJpaTemplate().getEntityManagerFactory());
		assertEquals("initDao not called", test.size(), 1);
	}

	@Test
	public void testJpaDaoSupportWithJpaTemplate() throws Exception {
		JpaTemplate template = new JpaTemplate();
		final List test = new ArrayList();
		JpaDaoSupport dao = new JpaDaoSupport() {
			@Override
			protected void initDao() {
				test.add("test");
			}
		};
		dao.setJpaTemplate(template);
		dao.afterPropertiesSet();
		assertNotNull("jpa template not created", dao.getJpaTemplate());
		assertEquals("incorrect JpaTemplate", template, dao.getJpaTemplate());
		assertEquals("initDao not called", test.size(), 1);
	}

	@Test
	public void testInvalidJpaTemplate() throws Exception {
		JpaDaoSupport dao = new JpaDaoSupport() {
		};
		try {
			dao.afterPropertiesSet();
			fail("expected exception");
		}
		catch (IllegalArgumentException iae) {
			// okay
		}
	}
}
