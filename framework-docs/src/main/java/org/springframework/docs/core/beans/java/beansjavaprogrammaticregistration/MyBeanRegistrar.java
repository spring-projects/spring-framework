/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
