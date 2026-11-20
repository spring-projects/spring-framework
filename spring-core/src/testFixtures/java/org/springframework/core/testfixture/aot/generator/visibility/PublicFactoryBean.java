/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.testfixture.aot.generator.visibility;

import org.springframework.core.ResolvableType;

public class PublicFactoryBean<T> {

	PublicFactoryBean(Class<T> type) {
	}

	public static PublicFactoryBean<ProtectedType> protectedTypeFactoryBean() {
		return new PublicFactoryBean<>(ProtectedType.class);
	}

	public static ResolvableType resolveToProtectedGenericParameter() {
		return ResolvableType.forClassWithGenerics(PublicFactoryBean.class, ProtectedType.class);
	}

}
