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

package org.springframework.context.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Yanming Zhou
 */
class AnnotatedBeanDefinitionReaderTests {

	@Test
	@SuppressWarnings("unchecked")
	void registerBeanWithQualifiers() {
		GenericApplicationContext context = new GenericApplicationContext();
		AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(context);

		reader.registerBean(TestBean.class, "primary", Primary.class);
		assertThat(context.getBeanDefinition("primary").isPrimary()).isTrue();

		reader.registerBean(TestBean.class, "fallback", Fallback.class);
		assertThat(context.getBeanDefinition("fallback").isFallback()).isTrue();

		reader.registerBean(TestBean.class, "lazy", Lazy.class);
		assertThat(context.getBeanDefinition("lazy").isLazyInit()).isTrue();

		reader.registerBean(TestBean.class, "customQualifier", CustomQualifier.class);
		assertThat(context.getBeanDefinition("customQualifier"))
				.isInstanceOfSatisfying(AbstractBeanDefinition.class, abd ->
					assertThat(abd.hasQualifier(CustomQualifier.class.getTypeName())).isTrue());
	}

	@Lazy(false)
	static class TestBean {
	}

	@Target({ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@Qualifier
	@interface CustomQualifier {
	}

}
