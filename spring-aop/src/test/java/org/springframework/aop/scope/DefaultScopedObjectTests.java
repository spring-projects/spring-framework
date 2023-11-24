/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aop.scope;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the {@link DefaultScopedObject} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public class DefaultScopedObjectTests {

	private static final String GOOD_BEAN_NAME = "foo";


	@Test
	public void testCtorWithNullBeanFactory() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
			new DefaultScopedObject(null, GOOD_BEAN_NAME));
	}

	@Test
	public void testCtorWithNullTargetBeanName() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				testBadTargetBeanName(null));
	}

	@Test
	public void testCtorWithEmptyTargetBeanName() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				testBadTargetBeanName(""));
	}

	@Test
	public void testCtorWithJustWhitespacedTargetBeanName() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				testBadTargetBeanName("   "));
	}

	private static void testBadTargetBeanName(final String badTargetBeanName) {
		ConfigurableBeanFactory factory = mock();
		new DefaultScopedObject(factory, badTargetBeanName);
	}

}
