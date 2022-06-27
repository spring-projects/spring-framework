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

import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
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
	 * Refresh the specified {@link GenericApplicationContext} and generate the
	 * necessary code to restore the state of its {@link BeanFactory}, using the
	 * specified {@link GenerationContext}.
	 * @param applicationContext the application context to handle
	 * @param generationContext the generation context to use
	 * @return the class name of the {@link ApplicationContextInitializer} entry point
	 */
	public ClassName generateApplicationContext(GenericApplicationContext applicationContext,
			GenerationContext generationContext) {
		applicationContext.refreshForAotProcessing();
		DefaultListableBeanFactory beanFactory = applicationContext
				.getDefaultListableBeanFactory();
		ApplicationContextInitializationCodeGenerator codeGenerator = new ApplicationContextInitializationCodeGenerator();
		new BeanFactoryInitializationAotContributions(beanFactory).applyTo(generationContext,
				codeGenerator);
		GeneratedClass applicationContextInitializer = generationContext.getGeneratedClasses()
				.forFeature("ApplicationContextInitializer")
				.generate(codeGenerator.generateJavaFile());
		return applicationContextInitializer.getName();
	}

}
