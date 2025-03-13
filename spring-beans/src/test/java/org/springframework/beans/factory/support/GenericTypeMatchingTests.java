package org.springframework.beans.factory.support;

import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractBeanFactory#isTypeMatch} with scoped proxy beans that use generic types.
 */
class ScopedProxyGenericTypeMatchTests {

	@Test
	void scopedProxyBeanTypeMatching() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

		String targetBeanName = "scopedTarget.wordBean";
		String proxyBeanName = "wordBean";

		RootBeanDefinition targetDef = new RootBeanDefinition(SomeGenericSupplier.class);
		targetDef.setScope("request");
		factory.registerBeanDefinition(targetBeanName, targetDef);

		RootBeanDefinition proxyDef = new RootBeanDefinition();
		proxyDef.setScope("singleton");
		proxyDef.setTargetType(ResolvableType.forClassWithGenerics(Supplier.class, String.class));
		proxyDef.setAttribute("targetBeanName", targetBeanName);
		factory.registerBeanDefinition(proxyBeanName, proxyDef);

		ResolvableType supplierType = ResolvableType.forClassWithGenerics(Supplier.class, String.class);

		boolean isMatch = factory.isTypeMatch(proxyBeanName, supplierType);
		assertThat(isMatch).isTrue();

		String[] names = factory.getBeanNamesForType(supplierType);
		assertThat(names).contains(proxyBeanName);
	}

	static class SomeGenericSupplier implements Supplier<String> {
		@Override
		public String get() {
			return "value";
		}
	}
}