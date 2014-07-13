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

package org.springframework.web.servlet.config.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.util.List;

/**
 * This class creates a FreeMarkerConfigurer bean.
 * It is typically imported by adding {@link EnableWebMvc @EnableWebMvc} to an
 * application {@link Configuration @Configuration} class when FreeMarker is
 * in the classpath.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 * @see org.springframework.web.servlet.config.annotation.ViewConfigurationsImportSelector
 */
@Configuration
public class FreeMarkerConfigurerConfigurationSupport {

	private List<WebMvcConfigurationSupport> webMvcConfigurationSupports;

	@Autowired(required = false)
	public void setWebMvcConfigurationSupports(List<WebMvcConfigurationSupport> webMvcConfigurationSupports) {
		this.webMvcConfigurationSupports = webMvcConfigurationSupports;
	}

	@Bean
	public FreeMarkerConfigurer freeMarkerConfigurer() {
		FreeMarkerConfigurer configurer = null;
		if(webMvcConfigurationSupports != null) {
			for(WebMvcConfigurationSupport configurationSupport : webMvcConfigurationSupports) {
				configurer = configurationSupport.getViewResolutionRegistry().getFreeMarkerConfigurer();
				if(configurer != null) {
					break;
				}
			}
		}
		return configurer;
	}
}
