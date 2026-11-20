/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.view.freemarker;

import java.util.HashMap;
import java.util.Properties;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.ui.freemarker.SpringTemplateLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * @author Juergen Hoeller
 * @author Issam El-atif
 * @author Sam Brannen
 * @since 5.2
 */
class FreeMarkerConfigurerTests {

	private final FreeMarkerConfigurer freeMarkerConfigurer = new FreeMarkerConfigurer();

	@Test
	void freeMarkerConfigurerDefaultEncoding() throws Exception {
		freeMarkerConfigurer.afterPropertiesSet();
		Configuration cfg = freeMarkerConfigurer.getConfiguration();
		assertThat(cfg.getDefaultEncoding()).isEqualTo("UTF-8");
	}

	@Test
	void freeMarkerConfigurerWithConfigLocation() {
		freeMarkerConfigurer.setConfigLocation(new FileSystemResource("myprops.properties"));
		Properties props = new Properties();
		props.setProperty("myprop", "/mydir");
		freeMarkerConfigurer.setFreemarkerSettings(props);
		assertThatIOException().isThrownBy(freeMarkerConfigurer::afterPropertiesSet);
	}

	@Test
	void freeMarkerConfigurerWithResourceLoaderPath() throws Exception {
		freeMarkerConfigurer.setTemplateLoaderPath("file:/mydir");
		freeMarkerConfigurer.afterPropertiesSet();
		Configuration cfg = freeMarkerConfigurer.getConfiguration();
		assertThat(cfg.getTemplateLoader()).isInstanceOf(MultiTemplateLoader.class);
		MultiTemplateLoader multiTemplateLoader = (MultiTemplateLoader)cfg.getTemplateLoader();
		assertThat(multiTemplateLoader.getTemplateLoader(0)).isInstanceOf(SpringTemplateLoader.class);
		assertThat(multiTemplateLoader.getTemplateLoader(1)).isInstanceOf(ClassTemplateLoader.class);
	}

	@Test
	@SuppressWarnings("rawtypes")
	void freeMarkerConfigurerWithNonFileResourceLoaderPath() throws Exception {
		freeMarkerConfigurer.setTemplateLoaderPath("file:/mydir");
		Properties settings = new Properties();
		settings.setProperty("localized_lookup", "false");
		freeMarkerConfigurer.setFreemarkerSettings(settings);
		freeMarkerConfigurer.setResourceLoader(new ResourceLoader() {
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
		freeMarkerConfigurer.afterPropertiesSet();
		assertThat(freeMarkerConfigurer.getConfiguration()).isInstanceOf(Configuration.class);
		Configuration fc = freeMarkerConfigurer.getConfiguration();
		Template ft = fc.getTemplate("test");
		assertThat(FreeMarkerTemplateUtils.processTemplateIntoString(ft, new HashMap())).isEqualTo("test");
	}

}
