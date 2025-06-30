/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.context.testfixture.beans.factory;

import java.util.function.Supplier;

import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;

public class GenericBeanRegistrar implements BeanRegistrar {

	@Override
	public void register(BeanRegistry registry, Environment env) {
		ParameterizedTypeReference<Supplier<Foo>> type = new ParameterizedTypeReference<>() {};
		registry.registerBean("fooSupplier", Supplier.class, spec -> spec.targetType(type)
				.supplier(context-> (Supplier<Foo>) Foo::new));
	}

	public record Foo() {}
}
