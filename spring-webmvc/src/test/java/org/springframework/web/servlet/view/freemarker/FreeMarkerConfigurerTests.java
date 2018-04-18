/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet.view.freemarker;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.ui.freemarker.SpringTemplateLoader;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @since 14.03.2004
 */
public class FreeMarkerConfigurerTests {

	@Test(expected = IOException.class)
	public void freeMarkerConfigurationFactoryBeanWithConfigLocation() throws Exception {
		FreeMarkerConfigurationFactoryBean fcfb = new FreeMarkerConfigurationFactoryBean();
		fcfb.setConfigLocation(new FileSystemResource("myprops.properties"));
		Properties props = new Properties();
		props.setProperty("myprop", "/mydir");
		fcfb.setFreemarkerSettings(props);
		fcfb.afterPropertiesSet();
	}

	@Test
	public void freeMarkerConfigurationFactoryBeanWithResourceLoaderPath() throws Exception {
		FreeMarkerConfigurationFactoryBean fcfb = new FreeMarkerConfigurationFactoryBean();
		fcfb.setTemplateLoaderPath("file:/mydir");
		fcfb.afterPropertiesSet();
		Configuration cfg = fcfb.getObject();
		assertTrue(cfg.getTemplateLoader() instanceof SpringTemplateLoader);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void freeMarkerConfigurationFactoryBeanWithNonFileResourceLoaderPath() throws Exception {
		FreeMarkerConfigurationFactoryBean fcfb = new FreeMarkerConfigurationFactoryBean();
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
		assertThat(fcfb.getObject(), instanceOf(Configuration.class));
		Configuration fc = fcfb.getObject();
		Template ft = fc.getTemplate("test");
		assertEquals("test", FreeMarkerTemplateUtils.processTemplateIntoString(ft, new HashMap()));
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
		beanFactory.getBean(Configuration.class);
	}

}
