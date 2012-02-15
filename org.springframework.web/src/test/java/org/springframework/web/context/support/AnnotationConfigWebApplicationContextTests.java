/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.context.support;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.CoreMatchers.*;

import static org.junit.Assert.*;

/**
 * @author Chris Beams
 */
public class AnnotationConfigWebApplicationContextTests {

	@Test
	public void registerSingleClass() {
		AnnotationConfigWebApplicationContext ctx =
			new AnnotationConfigWebApplicationContext();
		ctx.register(Config.class);
		ctx.refresh();

		TestBean bean = ctx.getBean(TestBean.class);
		assertNotNull(bean);
	}

	@Test
	public void configLocationWithSingleClass() {
		AnnotationConfigWebApplicationContext ctx =
			new AnnotationConfigWebApplicationContext();
		ctx.setConfigLocation(Config.class.getName());
		ctx.refresh();

		TestBean bean = ctx.getBean(TestBean.class);
		assertNotNull(bean);
	}

	@Test
	public void withBeanNameGenerator() {
		AnnotationConfigWebApplicationContext ctx =
			new AnnotationConfigWebApplicationContext();
		ctx.setBeanNameGenerator(new AnnotationBeanNameGenerator() {
			@Override
			public String generateBeanName(BeanDefinition definition,
					BeanDefinitionRegistry registry) {
				return "custom-" + super.generateBeanName(definition, registry);
			}
		});
		ctx.setConfigLocation(Config.class.getName());
		ctx.refresh();
		assertThat(ctx.containsBean("custom-myConfig"), is(true));
	}


	@Configuration("myConfig")
	static class Config {
		@Bean
		public TestBean myTestBean() {
			return new TestBean();
		}
	}

	static class NotConfigurationAnnotated { }

	static class TestBean { }

}
