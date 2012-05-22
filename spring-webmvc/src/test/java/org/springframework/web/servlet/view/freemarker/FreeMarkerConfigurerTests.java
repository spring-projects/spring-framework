/*
 * Copyright 2002-2012 the original author or authors.
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

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import junit.framework.TestCase;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.ui.freemarker.SpringTemplateLoader;

/**
 * @author Juergen Hoeller
 * @since 14.03.2004
 */
public class FreeMarkerConfigurerTests extends TestCase {

	public void testTemplateLoaders() throws Exception {
		FreeMarkerConfigurer fc = new FreeMarkerConfigurer();
		fc.setTemplateLoaders(new TemplateLoader[] {});
		fc.afterPropertiesSet();
		assertTrue(fc.getConfiguration().getTemplateLoader() instanceof ClassTemplateLoader);

		fc = new FreeMarkerConfigurer();
		fc.setTemplateLoaders(new TemplateLoader[] {new ClassTemplateLoader()});
		fc.afterPropertiesSet();
		assertTrue(fc.getConfiguration().getTemplateLoader() instanceof MultiTemplateLoader);
	}

	public void testFreemarkerConfigurationFactoryBeanWithConfigLocation() throws TemplateException {
		FreeMarkerConfigurationFactoryBean fcfb = new FreeMarkerConfigurationFactoryBean();
		fcfb.setConfigLocation(new FileSystemResource("myprops.properties"));
		Properties props = new Properties();
		props.setProperty("myprop", "/mydir");
		fcfb.setFreemarkerSettings(props);
		try {
			fcfb.afterPropertiesSet();
			fail("Should have thrown IOException");
		}
		catch (IOException ex) {
			// expected
		}
	}

	public void testFreeMarkerConfigurationFactoryBeanWithResourceLoaderPath() throws Exception {
		FreeMarkerConfigurationFactoryBean fcfb = new FreeMarkerConfigurationFactoryBean();
		fcfb.setTemplateLoaderPath("file:/mydir");
		fcfb.afterPropertiesSet();
		Configuration cfg = fcfb.getObject();
		assertTrue(cfg.getTemplateLoader() instanceof SpringTemplateLoader);
	}

	public void testFreemarkerConfigurationFactoryBeanWithNonFileResourceLoaderPath()
			throws IOException, TemplateException {
		FreeMarkerConfigurationFactoryBean fcfb = new FreeMarkerConfigurationFactoryBean();
		fcfb.setTemplateLoaderPath("file:/mydir");
		Properties settings = new Properties();
		settings.setProperty("localized_lookup", "false");
		fcfb.setFreemarkerSettings(settings);
		fcfb.setResourceLoader(new ResourceLoader() {
			public Resource getResource(String location) {
				if (!("file:/mydir".equals(location) || "file:/mydir/test".equals(location))) {
					throw new IllegalArgumentException(location);
				}
				return new ByteArrayResource("test".getBytes(), "test");
			}
			public ClassLoader getClassLoader() {
				return getClass().getClassLoader();
			}
		});
		fcfb.afterPropertiesSet();
		assertTrue(fcfb.getObject() instanceof Configuration);
		Configuration fc = fcfb.getObject();
		Template ft = fc.getTemplate("test");
		assertEquals("test", FreeMarkerTemplateUtils.processTemplateIntoString(ft, new HashMap()));
	}

}
