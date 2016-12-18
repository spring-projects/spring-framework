/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.configuration.method;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link ContextConfiguration} annotation presented on the method.
 * @author Sergei Ustimenko
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ContextConfigurationOnMethodTest.TypeContext.class)
public class ContextConfigurationOnMethodTest {

	@Autowired
	protected String bean;

	@Test
	@ContextConfiguration(classes = MethodContext.class)
	public void methodLevelAnnotationShouldOverrideTypeAnnotation() {
		assertEquals("method", bean);
	}

	@Test
	public void typeLevelAnnotationShouldInjectRightValue() {
		assertEquals("type", bean);
	}

	// ---------------------------------------------------------------

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ContextConfiguration(classes = MetaParentConfig.class)
	public @interface MetaOnParent {}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ContextConfiguration(classes = MetaParentParentConfig.class)
	public @interface MetaOnParentParent {}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ContextConfiguration(classes = MetaParentInterfaceConfig.class)
	public @interface MetaOnParentInterface {}

	public interface ParentContextConfigurationInterface {
		@ContextConfiguration(classes = ContextConfigurationOnMethodTest.ParentInterfaceConfig.class)
		void shouldPickUpConfigurationFromParentInterface();
	}

	public interface ContextConfigurationInterface extends ParentContextConfigurationInterface {}

	public interface ParentContextConfigurationMetaInterface {
		@ContextConfigurationOnMethodTest.MetaOnParentInterface
		void shouldPickUpMetaFromParentInterface();
	}

	public interface ContextConfigurationMetaInterface extends ParentContextConfigurationMetaInterface {}

	public static class TypeContext {
		@Bean
		public String testBean() {
			return "type";
		}
	}

	public static class MethodContext {
		@Bean
		public String testBean() {
			return "method";
		}
	}

	public static class ParentConfig {
		@Bean
		public String testBean() {
			return "parent";
		}
	}

	public static class ParentParentConfig {
		@Bean
		public String testBean() {
			return "parentParent";
		}
	}

	public static class MetaParentConfig {
		@Bean
		public String testBean() {
			return "metaOnParent";
		}
	}

	public static class MetaParentParentConfig {
		@Bean
		public String testBean() {
			return "metaOnParentParent";
		}
	}

	public static class ParentInterfaceConfig {
		@Bean
		public String testBean() {
			return "parentInterface";
		}
	}

	public static class MetaParentInterfaceConfig {
		@Bean
		public String testBean() {
			return "metaOnParentInterface";
		}
	}

	public static class ContextConfigurationOnMethodParentParent {
		@ContextConfiguration(classes = ContextConfigurationOnMethodTest.ParentConfig.class)
		public void shouldPickUpParentBean() {
			// for test purposes
		}

		@ContextConfigurationOnMethodTest.MetaOnParent
		public void shouldPickUpMetaParent() {
			// for test purposes
		}
	}

	public static class ContextConfigurationOnMethodParent extends ContextConfigurationOnMethodParentParent {
		@ContextConfiguration(classes = ContextConfigurationOnMethodTest.ParentParentConfig.class)
		public void shouldPickUpParentParentBean() {
			// for test purposes
		}

		@ContextConfigurationOnMethodTest.MetaOnParentParent
		public void shouldPickUpMetaParentParent() {
			// for test purposes
		}
	}

}