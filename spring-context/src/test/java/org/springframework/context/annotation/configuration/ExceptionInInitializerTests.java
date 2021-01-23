/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Liu Dongmiao
 */
public class ExceptionInInitializerTests {

	@Test
	public void checkXml() {
		try (ClassPathXmlApplicationContext ignored = new ClassPathXmlApplicationContext("ExceptionInInitializerTests.xml", getClass())) {
			fail("shouldn't happen");
		} catch (BeanCreationException ex) {
			checkBeanCreationException(ex);
		}
	}

	@Test
	public void checkAnnotation() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.register(BeanMethodConfiguration.class);
			context.refresh();
			fail("shouldn't happen");
		} catch (BeanCreationException ex) {
			checkBeanCreationException(ex);
		}
	}

	private static void checkBeanCreationException(BeanCreationException ex) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);
		ex.printStackTrace(pw);
		pw.flush();
		String stackTrace = baos.toString();
		assertThat(stackTrace.contains(".<clinit>")).isTrue();
		assertThat(stackTrace.contains("java.lang.NoClassDefFoundError")).isFalse();
	}


	@Configuration
	static class BeanMethodConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}

		@Bean
		public AutowiredBean autowiredBean() {
			return new AutowiredBean();
		}

		@Bean
		public SimpleFactoryBean simpleFactoryBean() {
			SimpleFactoryBean bean = new SimpleFactoryBean();
			bean.setObjectType(SimpleExceptionInInitializer2.class);
			return bean;
		}
	}


	static class AutowiredBean {

		protected String foo;

		@Autowired
		public void setFoo(String foo) {
			this.foo = foo;
		}
	}


	static class SimpleBean {

		protected Class<?> objectType;

		protected Object object;

		public void setObjectType(Class<?> objectType) {
			this.objectType = objectType;
			try {
				this.object = objectType.newInstance();
			} catch (ReflectiveOperationException ex) {
				throw new RuntimeException(ex);
			}
		}
	}


	static class SimpleFactoryBean extends SimpleBean implements FactoryBean<Object> {

		@Override
		public Object getObject() {
			return object;
		}

		@Override
		public Class<?> getObjectType() {
			return objectType;
		}
	}


	static class SimpleExceptionInInitializer1 {

		private static final int ERROR = callInClinit();

		private static int callInClinit() {
			throw new UnsupportedOperationException();
		}
	}


	static class SimpleExceptionInInitializer2 {

		private static final int ERROR = callInClinit();

		private static int callInClinit() {
			throw new UnsupportedOperationException();
		}
	}

}
