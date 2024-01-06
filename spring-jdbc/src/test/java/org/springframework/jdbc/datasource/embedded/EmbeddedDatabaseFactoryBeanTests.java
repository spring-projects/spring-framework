/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.jdbc.datasource.embedded;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Keith Donald
 */
class EmbeddedDatabaseFactoryBeanTests {

	private final ClassRelativeResourceLoader resourceLoader = new ClassRelativeResourceLoader(getClass());


	Resource resource(String path) {
		return resourceLoader.getResource(path);
	}

	@Test
	void testFactoryBeanLifecycle() {
		EmbeddedDatabaseFactoryBean bean = new EmbeddedDatabaseFactoryBean();
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator(resource("db-schema.sql"),
			resource("db-test-data.sql"));
		bean.setDatabasePopulator(populator);
		bean.afterPropertiesSet();
		DataSource ds = bean.getObject();
		JdbcTemplate template = new JdbcTemplate(ds);
		assertThat(template.queryForObject("select NAME from T_TEST", String.class)).isEqualTo("Keith");
		bean.destroy();
	}

}
