/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jdbc.core.support;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Juergen Hoeller
 * @since 30.07.2003
 */
public class JdbcDaoSupportTests {

	@Test
	public void testJdbcDaoSupportWithDataSource() throws Exception {
		DataSource ds = mock();
		final List<String> test = new ArrayList<>();
		JdbcDaoSupport dao = new JdbcDaoSupport() {
			@Override
			protected void initDao() {
				test.add("test");
			}
		};
		dao.setDataSource(ds);
		dao.afterPropertiesSet();
		assertThat(dao.getDataSource()).as("Correct DataSource").isEqualTo(ds);
		assertThat(dao.getJdbcTemplate().getDataSource()).as("Correct JdbcTemplate").isEqualTo(ds);
		assertThat(test).as("initDao called").hasSize(1);
	}

	@Test
	public void testJdbcDaoSupportWithJdbcTemplate() throws Exception {
		JdbcTemplate template = new JdbcTemplate();
		final List<String> test = new ArrayList<>();
		JdbcDaoSupport dao = new JdbcDaoSupport() {
			@Override
			protected void initDao() {
				test.add("test");
			}
		};
		dao.setJdbcTemplate(template);
		dao.afterPropertiesSet();
		assertThat(template).as("Correct JdbcTemplate").isEqualTo(dao.getJdbcTemplate());
		assertThat(test).as("initDao called").hasSize(1);
	}

}
