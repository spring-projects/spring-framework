/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.beans.factory;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.core.env.Environment;

public class CircularBeanRegistrar implements BeanRegistrar {

	@Override
	public void register(BeanRegistry registry, Environment env) {
		registry.registerBean("foo", Foo.class, spec -> spec.supplier(context -> new Foo(context.bean(Bar.class))));
		registry.registerAlias("foo", "fooAlias");
		registry.registerBean("bar", Bar.class, spec -> spec
				.prototype()
				.lazyInit()
				.description("Custom description")
				.supplier(context -> new Bar(context.bean(Foo.class))));
		if (env.matchesProfiles("baz")) {
			registry.registerBean(Baz.class, spec -> spec
					.supplier(context -> new Baz("Hello World!")));
		}
		registry.registerBean(Init.class);
	}

	public record Foo(Bar bar) {}

	public record Bar(Foo foo) {}

	public record Baz(String message) {}

	public static class Init {

		public boolean initialized = false;

		@PostConstruct
		public void postConstruct() {
			initialized = true;
		}
	}

}
