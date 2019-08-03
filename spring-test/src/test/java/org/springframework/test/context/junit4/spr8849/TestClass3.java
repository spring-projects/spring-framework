/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.context.junit4.spr8849;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This name of this class intentionally does not end with "Test" or "Tests"
 * since it should only be run as part of the test suite: {@link Spr8849Tests}.
 *
 * @author Sam Brannen
 * @since 4.2
 * @see Spr8849Tests
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class TestClass3 {

	@Configuration
	@ImportResource("classpath:/org/springframework/test/context/junit4/spr8849/datasource-config-with-auto-generated-db-name.xml")
	static class Config {
	}


	@Resource
	DataSource dataSource;


	@Test
	public void dummyTest() {
		assertThat(dataSource).isNotNull();
	}

}
