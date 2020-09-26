/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.context.annotation.spr12233;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.type.AnnotatedTypeMetadata;


/**
 * Tests cornering the regression reported in SPR-12233.
 *
 * @author Phillip Webb
 */
public class Spr12233Tests {

	@Test
	public void spr12233() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(PropertySourcesPlaceholderConfigurer.class);
		ctx.register(ImportConfiguration.class);
		ctx.refresh();
		ctx.close();
	}

	static class NeverConfigurationCondition implements ConfigurationCondition {
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return false;
		}

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.REGISTER_BEAN;
		}
	}

	@Import(ComponentScanningConfiguration.class)
	static class ImportConfiguration {

	}

	@Configuration
	@ComponentScan
	static class ComponentScanningConfiguration {

	}


	@Configuration
	@Conditional(NeverConfigurationCondition.class)
	static class ConditionWithPropertyValueInjection {

		@Value("${idontexist}")
		private String property;
	}
}
