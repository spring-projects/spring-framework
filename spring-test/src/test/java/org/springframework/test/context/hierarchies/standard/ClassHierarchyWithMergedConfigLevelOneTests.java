/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
import org.springframework.test.context.aot.DisabledInAotMode;
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
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
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
