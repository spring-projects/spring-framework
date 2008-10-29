/*
 * Copyright 2002-2005 the original author or authors.
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

import junit.framework.TestCase;
import org.hibernate.SessionFactory;
import org.easymock.MockControl;

import org.springframework.orm.hibernate3.HibernateTemplate;

/**
 * @author Juergen Hoeller
 * @since 05.03.2005
 */
public class HibernateDaoSupportTests extends TestCase {

	public void testHibernateDaoSupportWithSessionFactory() throws Exception {
		MockControl sfControl = MockControl.createControl(SessionFactory.class);
		SessionFactory sf = (SessionFactory) sfControl.getMock();
		sfControl.replay();
		final List test = new ArrayList();
		HibernateDaoSupport dao = new HibernateDaoSupport() {
			protected void initDao() {
				test.add("test");
			}
		};
		dao.setSessionFactory(sf);
		dao.afterPropertiesSet();
		assertEquals("Correct SessionFactory", sf, dao.getSessionFactory());
		assertEquals("Correct HibernateTemplate", sf, dao.getHibernateTemplate().getSessionFactory());
		assertEquals("initDao called", test.size(), 1);
		sfControl.verify();
	}

	public void testHibernateDaoSupportWithHibernateTemplate() throws Exception {
		HibernateTemplate template = new HibernateTemplate();
		final List test = new ArrayList();
		HibernateDaoSupport dao = new HibernateDaoSupport() {
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
