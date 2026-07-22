package org.springframework.beans.factory.support;

import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;

import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractBeanFactory#isTypeMatch} with scoped proxy beans that use generic types.
 */
class ScopedProxyGenericTypeMatchTests {

	@Test
	void scopedProxyBeanTypeMatching() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

		String proxyBeanName = "wordBean-" + UUID.randomUUID();
		String targetBeanName = "scopedTarget." + proxyBeanName;

		RootBeanDefinition targetDef = new RootBeanDefinition(SomeGenericSupplier.class);
		targetDef.setScope("request");
		factory.registerBeanDefinition(targetBeanName, targetDef);

		RootBeanDefinition proxyDef = new RootBeanDefinition();
		proxyDef.setScope("singleton");
		proxyDef.setTargetType(ResolvableType.forClassWithGenerics(Supplier.class, String.class));
		proxyDef.setAttribute("targetBeanName", targetBeanName);
		factory.registerBeanDefinition(proxyBeanName, proxyDef);

		ResolvableType supplierType = ResolvableType.forClassWithGenerics(Supplier.class, String.class);

		assertThat(factory.isTypeMatch(proxyBeanName, supplierType)).isTrue();

		assertThat(factory.getBeanNamesForType(supplierType)).contains(proxyBeanName);
	}

	static class SomeGenericSupplier implements Supplier<String> {
		@Override
		public String get() {
			return "value";
		}
	}
}