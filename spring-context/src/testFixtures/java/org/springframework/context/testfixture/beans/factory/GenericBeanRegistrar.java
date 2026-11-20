/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
		registry.registerBean("fooSupplier", new ParameterizedTypeReference<Supplier<Foo>>() {}, spec ->
				spec.supplier(context-> (Supplier<Foo>) Foo::new));
	}

	public record Foo() {}

}
