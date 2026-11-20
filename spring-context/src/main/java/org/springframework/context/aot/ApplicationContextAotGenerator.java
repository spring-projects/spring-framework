/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

		return withCglibClassHandler(new CglibClassHandler(generationContext), () -> {
			applicationContext.refreshForAotProcessing(generationContext.getRuntimeHints());
			ApplicationContextInitializationCodeGenerator codeGenerator =
					new ApplicationContextInitializationCodeGenerator(applicationContext, generationContext);
			DefaultListableBeanFactory beanFactory = applicationContext.getDefaultListableBeanFactory();
			new BeanFactoryInitializationAotContributions(beanFactory).applyTo(generationContext, codeGenerator);
			return codeGenerator.getClassName();
		});
	}

	private <T> T withCglibClassHandler(CglibClassHandler cglibClassHandler, Supplier<T> task) {
		try {
			ReflectUtils.setLoadedClassHandler(cglibClassHandler::handleLoadedClass);
			ReflectUtils.setGeneratedClassHandler(cglibClassHandler::handleGeneratedClass);
			return task.get();
		}
		finally {
			ReflectUtils.setLoadedClassHandler(null);
			ReflectUtils.setGeneratedClassHandler(null);
		}
	}

}
