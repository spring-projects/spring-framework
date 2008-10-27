/*
 * Copyright 2002-2008 the original author or authors.
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

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestContext;
import org.springframework.util.Assert;

/**
 * <code>TestExecutionListener</code> which processes test methods configured
 * with the {@link DirtiesContext @DirtiesContext} annotation.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see DirtiesContext
 */
public class DirtiesContextTestExecutionListener extends AbstractTestExecutionListener {

	private static final Log logger = LogFactory.getLog(DirtiesContextTestExecutionListener.class);


	/**
	 * If the current test method of the supplied
	 * {@link TestContext test context} has been annotated with
	 * {@link DirtiesContext @DirtiesContext}, the
	 * {@link ApplicationContext application context} of the test context will
	 * be {@link TestContext#markApplicationContextDirty() marked as dirty},
	 * and the
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE}
	 * will be set to <code>true</code> in the test context.
	 */
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		Method testMethod = testContext.getTestMethod();
		Assert.notNull(testMethod, "The test method of the supplied TestContext must not be null");

		boolean dirtiesContext = testMethod.isAnnotationPresent(DirtiesContext.class);
		if (logger.isDebugEnabled()) {
			logger.debug("After test method: context [" + testContext + "], dirtiesContext [" + dirtiesContext + "].");
		}

		if (dirtiesContext) {
			testContext.markApplicationContextDirty();
			testContext.setAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE,
					Boolean.TRUE);
		}
	}

}
