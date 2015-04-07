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

package org.springframework.jdbc.core.support;

import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import org.junit.Test;

import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Juergen Hoeller
 * @since 30.07.2003
 */
public class JdbcDaoSupportTests {

	@Test
	public void testJdbcDaoSupportWithDataSource() throws Exception {
		DataSource ds = mock(DataSource.class);
		final List<String> test = new ArrayList<String>();
		JdbcDaoSupport dao = new JdbcDaoSupport() {
			@Override
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

	@Test
	public void testJdbcDaoSupportWithJdbcTemplate() throws Exception {
		JdbcTemplate template = new JdbcTemplate();
		final List<String> test = new ArrayList<String>();
		JdbcDaoSupport dao = new JdbcDaoSupport() {
			@Override
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
