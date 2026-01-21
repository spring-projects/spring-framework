/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.docs.core.beans.java.beansjavaprogrammaticregistration;

import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

// tag::snippet[]
class MyBeanRegistrar implements BeanRegistrar {

	@Override
	public void register(BeanRegistry registry, Environment env) {
		registry.registerBean("foo", Foo.class);
		registry.registerBean("bar", Bar.class, spec -> spec
				.prototype()
				.lazyInit()
				.description("Custom description")
				.supplier(context -> new Bar(context.bean(Foo.class))));
		if (env.matchesProfiles("baz")) {
			registry.registerBean(Baz.class, spec -> spec
					.supplier(context -> new Baz("Hello World!")));
		}
		registry.registerBean(MyRepository.class);
		registry.registerBean(RouterFunction.class, spec ->
				spec.supplier(context -> router(context.bean(MyRepository.class))));
	}

	RouterFunction<ServerResponse> router(MyRepository myRepository) {
		return RouterFunctions.route()
				// ...
				.build();
	}

}
// end::snippet[]
