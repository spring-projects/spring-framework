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
import java.util.List;
import java.util.function.BiConsumer;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.util.ReflectionUtils;

/**
 * {@code TestExecutionListener} that enables Bean Override support in tests,
 * injecting overridden beans in appropriate fields of the test instance.
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
	 * Process the test instance and make sure that fields flagged for bean
	 * overriding are processed.
	 * <p>Each field's value will be updated with the overridden bean instance.
	 */
	protected void injectFields(TestContext testContext) {
		postProcessFields(testContext, (testMetadata, overrideRegistrar) -> overrideRegistrar.inject(
				testMetadata.testInstance, testMetadata.overrideMetadata));
	}

	/**
	 * Process the test instance and make sure that fields flagged for bean
	 * overriding are processed.
	 * <p>If a fresh instance is required, the field is nulled out and then
	 * re-injected with the overridden bean instance.
	 * <p>This method does nothing if the
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE}
	 * attribute is not present in the {@code TestContext}.
	 */
	protected void reinjectFieldsIfConfigured(TestContext testContext) throws Exception {
		if (Boolean.TRUE.equals(
				testContext.getAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE))) {

			postProcessFields(testContext, (testMetadata, registrar) -> {
				Object testInstance = testMetadata.testInstance;
				Field field = testMetadata.overrideMetadata.getField();
				ReflectionUtils.makeAccessible(field);
				ReflectionUtils.setField(field, testInstance, null);
				registrar.inject(testInstance, testMetadata.overrideMetadata);
			});
		}
	}

	private void postProcessFields(TestContext testContext, BiConsumer<TestContextOverrideMetadata,
			BeanOverrideRegistrar> consumer) {

		Class<?> testClass = testContext.getTestClass();
		Object testInstance = testContext.getTestInstance();

		List<OverrideMetadata> metadataForFields = OverrideMetadata.forTestClass(testClass);
		if (!metadataForFields.isEmpty()) {
			BeanOverrideRegistrar registrar =
					testContext.getApplicationContext().getBean(BeanOverrideRegistrar.class);
			for (OverrideMetadata metadata : metadataForFields) {
				consumer.accept(new TestContextOverrideMetadata(testInstance, metadata), registrar);
			}
		}
	}

	private record TestContextOverrideMetadata(Object testInstance, OverrideMetadata overrideMetadata) {}

}
