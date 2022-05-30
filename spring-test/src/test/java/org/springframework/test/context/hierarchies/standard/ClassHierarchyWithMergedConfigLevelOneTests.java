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

package org.springframework.test.context.hierarchies.standard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 3.2.2
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy({
//
	@ContextConfiguration(name = "parent", classes = ClassHierarchyWithMergedConfigLevelOneTests.AppConfig.class),//
	@ContextConfiguration(name = "child", classes = ClassHierarchyWithMergedConfigLevelOneTests.UserConfig.class) //
})
class ClassHierarchyWithMergedConfigLevelOneTests {

	@Configuration
	static class AppConfig {

		@Bean
		String parent() {
			return "parent";
		}
	}

	@Configuration
	static class UserConfig {

		@Autowired
		private AppConfig appConfig;


		@Bean
		String user() {
			return appConfig.parent() + " + user";
		}

		@Bean
		String beanFromUserConfig() {
			return "from UserConfig";
		}
	}


	@Autowired
	protected String parent;

	@Autowired
	protected String user;

	@Autowired(required = false)
	@Qualifier("beanFromUserConfig")
	protected String beanFromUserConfig;

	@Autowired
	protected ApplicationContext context;


	@Test
	void loadContextHierarchy() {
		assertThat(context).as("child ApplicationContext").isNotNull();
		assertThat(context.getParent()).as("parent ApplicationContext").isNotNull();
		assertThat(context.getParent().getParent()).as("grandparent ApplicationContext").isNull();
		assertThat(parent).isEqualTo("parent");
		assertThat(user).isEqualTo("parent + user");
		assertThat(beanFromUserConfig).isEqualTo("from UserConfig");
	}

}
