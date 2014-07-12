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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.view.velocity.VelocityConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration class that declares a
 * {@link org.springframework.web.servlet.view.velocity.VelocityConfigurer
 * VelocityConfigurer} bean. The configuration is conditional and applies
 * only if there is no {@code VelocityConfigurer} bean already declared.
 *
 * <p>This configuration is imported when using {@link EnableWebMvc} if
 * Velocity is available on the classpath. It can be customized by
 * implementing {@link VelocityWebMvcConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
@Configuration
@Conditional(WebMvcVelocityConfiguration.VelocityConfigurerNotPresentCondition.class)
public class WebMvcVelocityConfiguration {

	private final List<VelocityWebMvcConfigurer> webMvcConfigurers = new ArrayList<VelocityWebMvcConfigurer>(1);


	@Autowired(required = false)
	public void setWebMvcConfigurers(List<VelocityWebMvcConfigurer> webMvcConfigurers) {
		if (!CollectionUtils.isEmpty(webMvcConfigurers)) {
			this.webMvcConfigurers.addAll(webMvcConfigurers);
		}
	}

	@Bean
	@Lazy
	public VelocityConfigurer velocityConfigurer() {
		VelocityConfigurer configurer = new VelocityConfigurer();
		configurer.setResourceLoaderPath("/WEB-INF/");
		for (VelocityWebMvcConfigurer webMvcConfigurer : this.webMvcConfigurers) {
			webMvcConfigurer.configureVelocity(configurer);
		}
		return configurer;
	}


	static class VelocityConfigurerNotPresentCondition extends BeanTypeNotPresentCondition {

		private VelocityConfigurerNotPresentCondition() {
			super(VelocityConfigurer.class);
		}
	}

}
