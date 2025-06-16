/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.context.litemode;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This set of tests refutes the claims made in
 * <a href="https://github.com/spring-projects/spring-framework/issues/13690" target="_blank">gh-13690</a>.
 *
 * <p><b>The Claims</b>:
 *
 * <blockquote>
 * When a {@code @ContextConfiguration} test class references a config class
 * missing an {@code @Configuration} annotation, {@code @Bean} dependencies are
 * wired successfully but the bean lifecycle is not applied (no init methods are
 * invoked, for example). Adding the missing {@code @Configuration} annotation
 * solves the problem, however the problem and solution isn't obvious since
 * wiring/injection appeared to work.
 * </blockquote>
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 3.2
 */
@SpringJUnitConfig(AnnotatedConfigClassesWithoutAtConfigurationTests.AnnotatedFactoryBeans.class)
class AnnotatedConfigClassesWithoutAtConfigurationTests {

	/**
	 * This is intentionally <b>not</b> annotated with {@code @Configuration}.
	 * Consequently, this class contains what we call <i>annotated factory bean
	 * methods</i> instead of standard bean definition methods.
	 */
	static class AnnotatedFactoryBeans {

		static final AtomicInteger enigmaCallCount = new AtomicInteger();


		@Bean
		String enigma() {
			return "enigma #" + enigmaCallCount.incrementAndGet();
		}

		@Bean
		LifecycleBean lifecycleBean() {
			// The following call to enigma() literally invokes the local
			// enigma() method, not a CGLIB proxied version, since these methods
			// are essentially factory bean methods.
			LifecycleBean bean = new LifecycleBean(enigma());
			assertThat(bean.isInitialized()).isFalse();
			return bean;
		}
	}


	@Autowired
	private String enigma;

	@Autowired
	private LifecycleBean lifecycleBean;


	@Test
	void testSPR_9051() {
		assertThat(enigma).isNotNull();
		assertThat(lifecycleBean).isNotNull();
		assertThat(lifecycleBean.isInitialized()).isTrue();
		Set<String> names = Set.of(enigma, lifecycleBean.getName());
		assertThat(names).containsExactlyInAnyOrder("enigma #1", "enigma #2");
	}

}
