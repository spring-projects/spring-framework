/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.context.aot;

import java.util.function.Supplier;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;

/**
 * Process an {@link ApplicationContext} and its {@link BeanFactory} to generate
 * code that represents the state of the bean factory, as well as the necessary
 * hints that can be used at runtime in a constrained environment.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 */
public class ApplicationContextAotGenerator {

	/**
	 * Process the specified {@link GenericApplicationContext} ahead-of-time using
	 * the specified {@link GenerationContext}.
	 * <p>Return the {@link ClassName} of the {@link ApplicationContextInitializer}
	 * to use to restore an optimized state of the application context.
	 * @param applicationContext the non-refreshed application context to process
	 * @param generationContext the generation context to use
	 * @return the {@code ClassName} of the {@code ApplicationContextInitializer}
	 * entry point
	 */
	public ClassName processAheadOfTime(GenericApplicationContext applicationContext,
			GenerationContext generationContext) {
		return withGeneratedClassHandler(new GeneratedClassHandler(generationContext), () -> {
			applicationContext.refreshForAotProcessing(generationContext.getRuntimeHints());
			DefaultListableBeanFactory beanFactory = applicationContext.getDefaultListableBeanFactory();
			ApplicationContextInitializationCodeGenerator codeGenerator =
					new ApplicationContextInitializationCodeGenerator(generationContext);
			new BeanFactoryInitializationAotContributions(beanFactory).applyTo(generationContext, codeGenerator);
			return codeGenerator.getGeneratedClass().getName();
		});
	}

	private <T> T withGeneratedClassHandler(GeneratedClassHandler generatedClassHandler, Supplier<T> task) {
		try {
			ReflectUtils.setGeneratedClassHandler(generatedClassHandler);
			return task.get();
		}
		finally {
			ReflectUtils.setGeneratedClassHandler(null);
		}
	}

}
