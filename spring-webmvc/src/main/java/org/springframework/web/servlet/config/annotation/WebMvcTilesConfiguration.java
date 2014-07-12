/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.config.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.view.tiles3.TilesConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration class that declares a
 * {@link org.springframework.web.servlet.view.tiles3.TilesConfigurer
 * TilesConfigurer} bean. The configuration is conditional and applies
 * only if there is no {@code TilesConfigurer} bean already declared.
 *
 * <p>This configuration is imported when using {@link EnableWebMvc} if Tiles 3
 * is available on the classpath. It can be customized by implementing
 * {@link TilesWebMvcConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
@Configuration
@Conditional(WebMvcTilesConfiguration.TilesConfigurerNotPresentCondition.class)
public class WebMvcTilesConfiguration implements ResourceLoaderAware {

	private final List<TilesWebMvcConfigurer> webMvcConfigurers = new ArrayList<TilesWebMvcConfigurer>(1);

	private ResourceLoader resourceLoader;


	@Autowired(required = false)
	public void setWebMvcConfigurers(List<TilesWebMvcConfigurer> webMvcConfigurers) {
		if (!CollectionUtils.isEmpty(webMvcConfigurers)) {
			this.webMvcConfigurers.addAll(webMvcConfigurers);
		}
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}


	@Bean
	public TilesConfigurer tilesConfigurer() {
		TilesConfigurer configurer = new TilesConfigurer();
		if (!this.webMvcConfigurers.isEmpty()) {
			for (TilesWebMvcConfigurer webMvcConfigurer : this.webMvcConfigurers) {
				webMvcConfigurer.configureTiles(configurer);
			}
		}
		else {
			Resource resource = this.resourceLoader.getResource("/WEB-INF/tiles.xml");
			if (!resource.exists()) {
				String[] noTilesDefinitions = new String[0];
				configurer.setDefinitions(noTilesDefinitions);
			}
		}
		return configurer;
	}


	static class TilesConfigurerNotPresentCondition extends BeanTypeNotPresentCondition {

		private TilesConfigurerNotPresentCondition() {
			super(TilesConfigurer.class);
		}
	}

}
