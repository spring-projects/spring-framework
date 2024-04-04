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

package org.springframework.context.annotation.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests that @Import may be used both as a locally declared and meta-declared
 * annotation, that all declarations are processed, and that any local declaration
 * is processed last.
 *
 * @author Chris Beams
 * @since 3.1
 */
@SuppressWarnings("resource")
public class ImportAnnotationDetectionTests {

	@Test
	void multipleMetaImportsAreProcessed() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MultiMetaImportConfig.class);
		ctx.refresh();
		assertThat(ctx.containsBean("testBean1")).isTrue();
		assertThat(ctx.containsBean("testBean2")).isTrue();
	}

	@Test
	void localAndMetaImportsAreProcessed() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MultiMetaImportConfigWithLocalImport.class);
		ctx.refresh();
		assertThat(ctx.containsBean("testBean1")).isTrue();
		assertThat(ctx.containsBean("testBean2")).isTrue();
		assertThat(ctx.containsBean("testBean3")).isTrue();
	}

	@Test
	void localImportIsProcessedLast() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.setAllowBeanDefinitionOverriding(true);
		ctx.register(MultiMetaImportConfigWithLocalImportWithBeanOverride.class);
		ctx.refresh();
		assertThat(ctx.containsBean("testBean1")).isTrue();
		assertThat(ctx.containsBean("testBean2")).isTrue();
		assertThat(ctx.getBean("testBean2", TestBean.class).getName()).isEqualTo("2a");
	}

	@Test
	void importFromBean() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ImportFromBean.class);
		ctx.refresh();
		assertThat(ctx.containsBean("importAnnotationDetectionTests.ImportFromBean")).isTrue();
		assertThat(ctx.containsBean("testBean1")).isTrue();
		assertThat(ctx.getBean("testBean1", TestBean.class).getName()).isEqualTo("1");
	}

	@Configuration
	@MetaImport1
	@MetaImport2
	static class MultiMetaImportConfig {
	}

	@Configuration
	@MetaImport1
	@MetaImport2
	@Import(Config3.class)
	static class MultiMetaImportConfigWithLocalImport {
	}

	@Configuration
	@MetaImport1
	@MetaImport2
	@Import(Config2a.class)
	static class MultiMetaImportConfigWithLocalImportWithBeanOverride {
	}


	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Import(Config1.class)
	@interface MetaImport1 {
	}


	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Import(Config2.class)
	@interface MetaImport2 {
	}


	@Configuration
	static class Config1 {
		@Bean
		TestBean testBean1() {
			return new TestBean("1");
		}
	}

	@Configuration
	static class Config2 {
		@Bean
		TestBean testBean2() {
			return new TestBean("2");
		}
	}

	@Configuration
	static class Config2a {
		@Bean
		TestBean testBean2() {
			return new TestBean("2a");
		}
	}

	@Configuration
	static class Config3 {
		@Bean
		TestBean testBean3() {
			return new TestBean("3");
		}
	}

	@MetaImport1
	static class ImportFromBean {

	}
}
