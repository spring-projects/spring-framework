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

package org.springframework.orm.hibernate3.support;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 05.03.2005
 */
public class HibernateDaoSupportTests {

	@Test
	public void testHibernateDaoSupportWithSessionFactory() throws Exception {
		SessionFactory sf = mock(SessionFactory.class);
		final List test = new ArrayList();
		HibernateDaoSupport dao = new HibernateDaoSupport() {
			@Override
			protected void initDao() {
				test.add("test");
			}
		};
		dao.setSessionFactory(sf);
		dao.afterPropertiesSet();
		assertEquals("Correct SessionFactory", sf, dao.getSessionFactory());
		assertEquals("Correct HibernateTemplate", sf, dao.getHibernateTemplate().getSessionFactory());
		assertEquals("initDao called", test.size(), 1);
	}

	@Test
	public void testHibernateDaoSupportWithHibernateTemplate() throws Exception {
		HibernateTemplate template = new HibernateTemplate();
		final List test = new ArrayList();
		HibernateDaoSupport dao = new HibernateDaoSupport() {
			@Override
			protected void initDao() {
				test.add("test");
			}
		};
		dao.setHibernateTemplate(template);
		dao.afterPropertiesSet();
		assertEquals("Correct HibernateTemplate", template, dao.getHibernateTemplate());
		assertEquals("initDao called", test.size(), 1);
	}

}
