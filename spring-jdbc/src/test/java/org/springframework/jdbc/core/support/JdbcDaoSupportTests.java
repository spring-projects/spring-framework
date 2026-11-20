/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
@SuppressWarnings("removal")
class JdbcDaoSupportTests {

	@Test
	void jdbcDaoSupportWithDataSource() {
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
	void jdbcDaoSupportWithJdbcTemplate() {
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
