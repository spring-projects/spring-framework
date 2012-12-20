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

package org.springframework.jdbc.core.support;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Juergen Hoeller
 * @since 30.07.2003
 */
public class JdbcDaoSupportTests extends TestCase {

	public void testJdbcDaoSupportWithDataSource() throws Exception {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		DataSource ds = (DataSource) dsControl.getMock();
		final List<Object> test = new ArrayList<Object>();
		JdbcDaoSupport dao = new JdbcDaoSupport() {
			protected void initDao() {
				test.add("test");
			}
		};
		dao.setDataSource(ds);
		dao.afterPropertiesSet();
		assertEquals("Correct DataSource", ds, dao.getDataSource());
		assertEquals("Correct JdbcTemplate", ds, dao.getJdbcTemplate().getDataSource());
		assertEquals("initDao called", test.size(), 1);
	}

	public void testJdbcDaoSupportWithJdbcTemplate() throws Exception {
		JdbcTemplate template = new JdbcTemplate();
		final List<Object> test = new ArrayList<Object>();
		JdbcDaoSupport dao = new JdbcDaoSupport() {
			protected void initDao() {
				test.add("test");
			}
		};
		dao.setJdbcTemplate(template);
		dao.afterPropertiesSet();
		assertEquals("Correct JdbcTemplate", dao.getJdbcTemplate(), template);
		assertEquals("initDao called", test.size(), 1);
	}

}
