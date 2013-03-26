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

package org.springframework.test.context.hierarchies.standard;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Sam Brannen
 * @since 3.2.2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
//
	@ContextConfiguration(name = "parent", classes = ClassHierarchyWithMergedConfigLevelOneTests.AppConfig.class),//
	@ContextConfiguration(name = "child", classes = ClassHierarchyWithMergedConfigLevelOneTests.UserConfig.class) //
})
public class ClassHierarchyWithMergedConfigLevelOneTests {

	@Configuration
	static class AppConfig {

		@Bean
		public String parent() {
			return "parent";
		}
	}

	@Configuration
	static class UserConfig {

		@Autowired
		private AppConfig appConfig;


		@Bean
		public String user() {
			return appConfig.parent() + " + user";
		}

		@Bean
		public String beanFromUserConfig() {
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
	public void loadContextHierarchy() {
		assertNotNull("child ApplicationContext", context);
		assertNotNull("parent ApplicationContext", context.getParent());
		assertNull("grandparent ApplicationContext", context.getParent().getParent());
		assertEquals("parent", parent);
		assertEquals("parent + user", user);
		assertEquals("from UserConfig", beanFromUserConfig);
	}

}
