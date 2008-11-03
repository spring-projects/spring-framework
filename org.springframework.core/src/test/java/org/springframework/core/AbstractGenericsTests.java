/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.core;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import junit.framework.TestCase;

/**
 * @author Serge Bogatyrjov
 */
public abstract class AbstractGenericsTests extends TestCase {

	protected Class<?> targetClass;

	protected String methods[];

	protected Type expectedResults[];

	protected void executeTest() throws NoSuchMethodException {
		String methodName = getName().substring(4);
		methodName = methodName.substring(0, 1).toLowerCase() + methodName.substring(1);
		for (int i = 0; i < this.methods.length; i++) {
			if (methodName.equals(this.methods[i])) {
				Method method = this.targetClass.getMethod(methodName);
				Type type = getType(method);
				assertEquals(this.expectedResults[i], type);
				return;
			}
		}
		throw new IllegalStateException("Bad test data");
	}

	protected abstract Type getType(Method method);

}