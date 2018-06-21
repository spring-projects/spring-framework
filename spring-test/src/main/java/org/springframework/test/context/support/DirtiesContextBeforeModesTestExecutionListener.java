/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.context.support;

import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;

import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;
import static org.springframework.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD;

/**
 * {@code TestExecutionListener} which provides support for marking the
 * {@code ApplicationContext} associated with a test as <em>dirty</em> for
 * both test classes and test methods annotated with the
 * {@link DirtiesContext @DirtiesContext} annotation.
 *
 * <p>This listener supports test methods with the
 * {@linkplain DirtiesContext#methodMode method mode} set to
 * {@link MethodMode#BEFORE_METHOD BEFORE_METHOD} and test classes with the
 * {@linkplain DirtiesContext#classMode() class mode} set to
 * {@link ClassMode#BEFORE_EACH_TEST_METHOD BEFORE_EACH_TEST_METHOD} or
 * {@link ClassMode#BEFORE_CLASS BEFORE_CLASS}. For support for <em>AFTER</em>
 * modes, see {@link DirtiesContextTestExecutionListener}.
 *
 * <p>When {@linkplain TestExecutionListeners#mergeMode merging}
 * {@code TestExecutionListeners} with the defaults, this listener will
 * automatically be ordered before the {@link DependencyInjectionTestExecutionListener};
 * otherwise, this listener must be manually configured to execute before the
 * {@code DependencyInjectionTestExecutionListener}.
 *
 * @author Sam Brannen
 * @since 4.2
 * @see DirtiesContext
 * @see DirtiesContextTestExecutionListener
 */
public class DirtiesContextBeforeModesTestExecutionListener extends AbstractDirtiesContextTestExecutionListener {

	/**
	 * Returns {@code 1500}.
	 */
	@Override
	public final int getOrder() {
		return 1500;
	}

	/**
	 * If the test class of the supplied {@linkplain TestContext test context}
	 * is annotated with {@code @DirtiesContext} and the {@linkplain
	 * DirtiesContext#classMode() class mode} is set to {@link
	 * ClassMode#BEFORE_CLASS BEFORE_CLASS}, the {@linkplain ApplicationContext
	 * application context} of the test context will be
	 * {@linkplain TestContext#markApplicationContextDirty marked as dirty}, and the
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
	 * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context will be set to
	 * {@code true}.
	 */
	@Override
	public void beforeTestClass(TestContext testContext) throws Exception {
		beforeOrAfterTestClass(testContext, BEFORE_CLASS);
	}

	/**
	 * If the current test method of the supplied {@linkplain TestContext test
	 * context} is annotated with {@code @DirtiesContext} and the {@linkplain
	 * DirtiesContext#methodMode() method mode} is set to {@link
	 * MethodMode#BEFORE_METHOD BEFORE_METHOD}, or if the test class is
	 * annotated with {@code @DirtiesContext} and the {@linkplain
	 * DirtiesContext#classMode() class mode} is set to {@link
	 * ClassMode#BEFORE_EACH_TEST_METHOD BEFORE_EACH_TEST_METHOD}, the
	 * {@linkplain ApplicationContext application context} of the test context
	 * will be {@linkplain TestContext#markApplicationContextDirty marked as dirty} and the
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
	 * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context will be set to {@code true}.
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		beforeOrAfterTestMethod(testContext, BEFORE_METHOD, BEFORE_EACH_TEST_METHOD);
	}

}
