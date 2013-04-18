/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.junit4;

import java.util.Comparator;
import java.util.List;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.springframework.test.context.junit4.InternalSpringJUnit4ClassRunner;

/**
 * @since 3.2
 */
public class OrderedMethodsSpringJUnit4ClassRunner extends InternalSpringJUnit4ClassRunner {

	public OrderedMethodsSpringJUnit4ClassRunner(Class<?> clazz) throws InitializationError {
		super(clazz);
	}

	@Override
	protected List<FrameworkMethod> computeTestMethods() {
		List<FrameworkMethod> testMethods = super.computeTestMethods();

		java.util.Collections.sort(testMethods, new Comparator<FrameworkMethod>() {

			@Override
			public int compare(FrameworkMethod method1, FrameworkMethod method2) {
				return method1.getName().compareTo(method2.getName());
			}
		});

		return testMethods;
	}

}