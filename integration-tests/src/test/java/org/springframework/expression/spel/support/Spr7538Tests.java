/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.expression.spel.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.MethodExecutor;

class Spr7538Tests {

	@Test
	void repro() throws Exception {
		AlwaysTrueReleaseStrategy target = new AlwaysTrueReleaseStrategy();
		BeanFactoryTypeConverter converter = new BeanFactoryTypeConverter();

		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setTypeConverter(converter);

		List<Foo> arguments = Collections.emptyList();

		List<TypeDescriptor> paramDescriptors = new ArrayList<>();
		Method method = AlwaysTrueReleaseStrategy.class.getMethod("checkCompleteness", List.class);
		paramDescriptors.add(new TypeDescriptor(new MethodParameter(method, 0)));


		List<TypeDescriptor> argumentTypes = new ArrayList<>();
		argumentTypes.add(TypeDescriptor.forObject(arguments));
		ReflectiveMethodResolver resolver = new ReflectiveMethodResolver();
		MethodExecutor executor = resolver.resolve(context, target, "checkCompleteness", argumentTypes);

		Object result = executor.execute(context, target, arguments);
		System.out.println("Result: " + result);
	}

	static class AlwaysTrueReleaseStrategy {
		public boolean checkCompleteness(List<Foo> messages) {
			return true;
		}
	}

	static class Foo{}
}
