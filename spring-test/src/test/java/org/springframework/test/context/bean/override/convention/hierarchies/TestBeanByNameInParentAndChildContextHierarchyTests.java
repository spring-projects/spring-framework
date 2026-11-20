/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.convention.hierarchies;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.convention.hierarchies.TestBeanByNameInParentAndChildContextHierarchyTests.Config1;
import org.springframework.test.context.bean.override.convention.hierarchies.TestBeanByNameInParentAndChildContextHierarchyTests.Config2;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.ExampleServiceCaller;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link TestBean @TestBean} can be used within a
 * {@link ContextHierarchy @ContextHierarchy} with named context levels, when
 * identical beans are overridden "by name" in the parent and in the child.
 *
 * @author Sam Brannen
 * @since 6.2.6
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy({
	@ContextConfiguration(classes = Config1.class, name = "parent"),
	@ContextConfiguration(classes = Config2.class, name = "child")
})
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
class TestBeanByNameInParentAndChildContextHierarchyTests {

	@TestBean(name = "service", contextName = "parent")
	ExampleService serviceInParent;

	@TestBean(name = "service", contextName = "child")
	ExampleService serviceInChild;

	@Autowired
	ExampleServiceCaller serviceCaller1;

	@Autowired
	ExampleServiceCaller serviceCaller2;


	static ExampleService serviceInParent() {
		return () -> "@TestBean 1";
	}

	static ExampleService serviceInChild() {
		return () -> "@TestBean 2";
	}


	@Test
	void test() {
		assertThat(serviceInParent.greeting()).isEqualTo("@TestBean 1");
		assertThat(serviceInChild.greeting()).isEqualTo("@TestBean 2");
		assertThat(serviceCaller1.getService()).isSameAs(serviceInParent);
		assertThat(serviceCaller2.getService()).isSameAs(serviceInChild);
		assertThat(serviceCaller1.sayGreeting()).isEqualTo("I say @TestBean 1");
		assertThat(serviceCaller2.sayGreeting()).isEqualTo("I say @TestBean 2");
	}


	@Configuration
	static class Config1 {

		@Bean
		ExampleService service() {
			return () -> "Service 1";
		}

		@Bean
		ExampleServiceCaller serviceCaller1(ExampleService service) {
			return new ExampleServiceCaller(service);
		}
	}

	@Configuration
	static class Config2 {

		@Bean
		ExampleService service() {
			return () -> "Service 2";
		}

		@Bean
		ExampleServiceCaller serviceCaller2(ExampleService service) {
			return new ExampleServiceCaller(service);
		}
	}

}
