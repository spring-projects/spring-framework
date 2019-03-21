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

package org.springframework.test.context.support;

import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

/**
 * Abstract implementation of the {@link TestExecutionListener} interface which
 * provides empty method stubs. Subclasses can extend this class and override
 * only those methods suitable for the task at hand.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 */
public abstract class AbstractTestExecutionListener implements TestExecutionListener, Ordered {

	/**
	 * The default implementation is <em>empty</em>. Can be overridden by
	 * subclasses as necessary.
	 */
	@Override
	public void beforeTestClass(TestContext testContext) throws Exception {
		/* no-op */
	}

	/**
	 * The default implementation is <em>empty</em>. Can be overridden by
	 * subclasses as necessary.
	 */
	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		/* no-op */
	}

	/**
	 * The default implementation is <em>empty</em>. Can be overridden by
	 * subclasses as necessary.
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		/* no-op */
	}

	/**
	 * The default implementation is <em>empty</em>. Can be overridden by
	 * subclasses as necessary.
	 */
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		/* no-op */
	}

	/**
	 * The default implementation is <em>empty</em>. Can be overridden by
	 * subclasses as necessary.
	 */
	@Override
	public void afterTestClass(TestContext testContext) throws Exception {
		/* no-op */
	}

	/**
	 * The default implementation returns {@link Ordered#LOWEST_PRECEDENCE},
	 * thereby ensuring that custom listeners are ordered after default
	 * listeners supplied by the framework. Can be overridden by subclasses
	 * as necessary.
	 * @since 4.1
	 */
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
