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

package org.springframework.orm.jdo.support;

import java.util.ArrayList;
import java.util.List;

import javax.jdo.PersistenceManagerFactory;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.orm.jdo.JdoTemplate;

/**
 * @author Juergen Hoeller
 * @since 30.07.2003
 */
public class JdoDaoSupportTests extends TestCase {

	public void testJdoDaoSupportWithPersistenceManagerFactory() throws Exception {
		MockControl pmfControl = MockControl.createControl(PersistenceManagerFactory.class);
		PersistenceManagerFactory pmf = (PersistenceManagerFactory) pmfControl.getMock();
		pmf.getConnectionFactory();
		pmfControl.setReturnValue(null, 1);
		pmfControl.replay();
		final List test = new ArrayList();
		JdoDaoSupport dao = new JdoDaoSupport() {
			@Override
			protected void initDao() {
				test.add("test");
			}
		};
		dao.setPersistenceManagerFactory(pmf);
		dao.afterPropertiesSet();
		assertEquals("Correct PersistenceManagerFactory", pmf, dao.getPersistenceManagerFactory());
		assertEquals("Correct JdoTemplate", pmf, dao.getJdoTemplate().getPersistenceManagerFactory());
		assertEquals("initDao called", test.size(), 1);
		pmfControl.verify();
	}

	public void testJdoDaoSupportWithJdoTemplate() throws Exception {
		JdoTemplate template = new JdoTemplate();
		final List test = new ArrayList();
		JdoDaoSupport dao = new JdoDaoSupport() {
			@Override
			protected void initDao() {
				test.add("test");
			}
		};
		dao.setJdoTemplate(template);
		dao.afterPropertiesSet();
		assertEquals("Correct JdoTemplate", template, dao.getJdoTemplate());
		assertEquals("initDao called", test.size(), 1);
	}

}
