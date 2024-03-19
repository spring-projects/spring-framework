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

package org.springframework.test.context.bean.override;

import java.lang.reflect.Field;
import java.util.function.BiConsumer;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.util.ReflectionUtils;

/**
 * {@code TestExecutionListener} that enables Bean Override support in tests,
 * injecting overridden beans in appropriate fields of the test instance.
 *
 * <p>Some Bean Override implementations might additionally require the use of
 * additional listeners, which should be mentioned in the javadoc for the
 * corresponding annotations.
 *
 * @author Simon Baslé
 * @since 6.2
 */
public class BeanOverrideTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * Executes almost last ({@code LOWEST_PRECEDENCE - 50}).
	 */
	@Override
	public int getOrder() {
		return LOWEST_PRECEDENCE - 50;
	}

	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		injectFields(testContext);
	}

	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		reinjectFieldsIfConfigured(testContext);
	}

	/**
	 * Using a registered {@link BeanOverrideBeanPostProcessor}, find metadata
	 * associated with the current test class and ensure fields are injected
	 * with the overridden bean instance.
	 */
	protected void injectFields(TestContext testContext) {
		postProcessFields(testContext, (testMetadata, postProcessor) -> postProcessor.inject(
				testMetadata.overrideMetadata.field(), testMetadata.testInstance, testMetadata.overrideMetadata));
	}

	/**
	 * Using a registered {@link BeanOverrideBeanPostProcessor}, find metadata
	 * associated with the current test class and ensure fields are nulled out
	 * and then re-injected with the overridden bean instance.
	 * <p>This method does nothing if the
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE}
	 * attribute is not present in the {@code TestContext}.
	 */
	protected void reinjectFieldsIfConfigured(TestContext testContext) throws Exception {
		if (Boolean.TRUE.equals(
				testContext.getAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE))) {

			postProcessFields(testContext, (testMetadata, postProcessor) -> {
				Object testInstance = testMetadata.testInstance;
				Field field = testMetadata.overrideMetadata.field();
				ReflectionUtils.makeAccessible(field);
				ReflectionUtils.setField(field, testInstance, null);
				postProcessor.inject(field, testInstance, testMetadata.overrideMetadata);
			});
		}
	}

	private void postProcessFields(TestContext testContext, BiConsumer<TestContextOverrideMetadata,
			BeanOverrideBeanPostProcessor> consumer) {

		Class<?> testClass = testContext.getTestClass();
		Object testInstance = testContext.getTestInstance();
		BeanOverrideParser parser = new BeanOverrideParser();

		// Avoid full parsing, but validate that this particular class has some bean override field(s).
		if (parser.hasBeanOverride(testClass)) {
			BeanOverrideBeanPostProcessor postProcessor =
					testContext.getApplicationContext().getBean(BeanOverrideBeanPostProcessor.class);
			// The class should have already been parsed by the context customizer.
			for (OverrideMetadata metadata : postProcessor.getOverrideMetadata()) {
				if (!metadata.field().getDeclaringClass().equals(testClass)) {
					continue;
				}
				consumer.accept(new TestContextOverrideMetadata(testInstance, metadata), postProcessor);
			}
		}
	}

	private record TestContextOverrideMetadata(Object testInstance, OverrideMetadata overrideMetadata) {}

}
