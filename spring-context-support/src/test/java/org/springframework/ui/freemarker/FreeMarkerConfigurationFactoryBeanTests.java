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

package org.springframework.ui.freemarker;

import java.util.HashMap;
import java.util.Properties;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * @author Juergen Hoeller
 * @author Issam El-atif
 * @author Sam Brannen
 */
class FreeMarkerConfigurationFactoryBeanTests {

	private final FreeMarkerConfigurationFactoryBean fcfb = new FreeMarkerConfigurationFactoryBean();

	@Test
	void freeMarkerConfigurationFactoryBeanWithConfigLocation() {
		fcfb.setConfigLocation(new FileSystemResource("myprops.properties"));
		Properties props = new Properties();
		props.setProperty("myprop", "/mydir");
		fcfb.setFreemarkerSettings(props);
		assertThatIOException().isThrownBy(fcfb::afterPropertiesSet);
	}

	@Test
	void freeMarkerConfigurationFactoryBeanWithResourceLoaderPath() throws Exception {
		fcfb.setTemplateLoaderPath("file:/mydir");
		fcfb.afterPropertiesSet();
		Configuration cfg = fcfb.getObject();
		assertThat(cfg.getTemplateLoader()).isInstanceOf(SpringTemplateLoader.class);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void freeMarkerConfigurationFactoryBeanWithNonFileResourceLoaderPath() throws Exception {
		fcfb.setTemplateLoaderPath("file:/mydir");
		Properties settings = new Properties();
		settings.setProperty("localized_lookup", "false");
		fcfb.setFreemarkerSettings(settings);
		fcfb.setResourceLoader(new ResourceLoader() {
			@Override
			public Resource getResource(String location) {
				if (!("file:/mydir".equals(location) || "file:/mydir/test".equals(location))) {
					throw new IllegalArgumentException(location);
				}
				return new ByteArrayResource("test".getBytes(), "test");
			}
			@Override
			public ClassLoader getClassLoader() {
				return getClass().getClassLoader();
			}
		});
		fcfb.afterPropertiesSet();
		assertThat(fcfb.getObject()).isInstanceOf(Configuration.class);
		Configuration fc = fcfb.getObject();
		Template ft = fc.getTemplate("test");
		assertThat(FreeMarkerTemplateUtils.processTemplateIntoString(ft, new HashMap())).isEqualTo("test");
	}

	@Test  // SPR-12448
	public void freeMarkerConfigurationAsBean() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition loaderDef = new RootBeanDefinition(SpringTemplateLoader.class);
		loaderDef.getConstructorArgumentValues().addGenericArgumentValue(new DefaultResourceLoader());
		loaderDef.getConstructorArgumentValues().addGenericArgumentValue("/freemarker");
		RootBeanDefinition configDef = new RootBeanDefinition(Configuration.class);
		configDef.getPropertyValues().add("templateLoader", loaderDef);
		beanFactory.registerBeanDefinition("freeMarkerConfig", configDef);
		assertThat(beanFactory.getBean(Configuration.class)).isNotNull();
	}

}
