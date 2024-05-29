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

package org.springframework.test.context.aot.samples.management;

import java.lang.reflect.Executable;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.RegisteredBean.InstantiationDescriptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * Configuration class that mimics Spring Boot's AOT support for child management
 * contexts in
 * {@code org.springframework.boot.actuate.autoconfigure.web.server.ChildManagementContextInitializer}.
 *
 * <p>See <a href="https://github.com/spring-projects/spring-framework/issues/30861">gh-30861</a>.
 *
 * @author Sam Brannen
 * @since 6.0.12
 */
@Configuration
public class ManagementConfiguration {

	@Bean
	static BeanRegistrationAotProcessor beanRegistrationAotProcessor() {
		return registeredBean -> {
			InstantiationDescriptor instantiationDescriptor = registeredBean.resolveInstantiationDescriptor();
			Executable factoryMethod = instantiationDescriptor.executable();
			// Make AOT contribution for @Managed @Bean methods.
			if (AnnotatedElementUtils.hasAnnotation(factoryMethod, Managed.class)) {
				return new AotContribution(createManagementContext());
			}
			return null;
		};
	}

	private static GenericApplicationContext createManagementContext() {
		GenericApplicationContext managementContext = new GenericApplicationContext();
		managementContext.registerBean(ManagementMessageService.class);
		return managementContext;
	}


	/**
	 * Mimics Spring Boot's AOT support for child management contexts in
	 * {@code org.springframework.boot.actuate.autoconfigure.web.server.ChildManagementContextInitializer.AotContribution}.
	 */
	private static class AotContribution implements BeanRegistrationAotContribution {

		private final GenericApplicationContext managementContext;

		AotContribution(GenericApplicationContext managementContext) {
			this.managementContext = managementContext;
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
			GenerationContext managementGenerationContext = generationContext.withName("Management");
			new ApplicationContextAotGenerator().processAheadOfTime(this.managementContext, managementGenerationContext);
		}

	}

}
