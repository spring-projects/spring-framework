/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.support;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.DerivedTestBean;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 04.07.2006
 */
class DefaultSingletonBeanRegistryTests {

	private final DefaultSingletonBeanRegistry beanRegistry = new DefaultSingletonBeanRegistry();


	@Test
	void singletons() {
		AtomicBoolean tbFlag = new AtomicBoolean();
		beanRegistry.addSingletonCallback("tb", instance -> tbFlag.set(true));
		TestBean tb = new TestBean();
		beanRegistry.registerSingleton("tb", tb);
		assertThat(beanRegistry.getSingleton("tb")).isSameAs(tb);
		assertThat(tbFlag.get()).isTrue();

		AtomicBoolean tb2Flag = new AtomicBoolean();
		beanRegistry.addSingletonCallback("tb2", instance -> tb2Flag.set(true));
		TestBean tb2 = (TestBean) beanRegistry.getSingleton("tb2", TestBean::new);
		assertThat(beanRegistry.getSingleton("tb2")).isSameAs(tb2);
		assertThat(tb2Flag.get()).isTrue();

		TestBean tb3 = (TestBean) beanRegistry.getSingleton("tb3", () -> {
			TestBean newTb = new TestBean();
			beanRegistry.registerSingleton("tb3", newTb);
			return newTb;
		});
		assertThat(beanRegistry.getSingleton("tb3")).isSameAs(tb3);

		assertThat(beanRegistry.getSingletonCount()).isEqualTo(3);
		assertThat(beanRegistry.getSingletonNames()).containsExactly("tb", "tb2", "tb3");

		beanRegistry.destroySingletons();
		assertThat(beanRegistry.getSingletonCount()).isZero();
		assertThat(beanRegistry.getSingletonNames()).isEmpty();
	}

	@Test
	void disposableBean() {
		DerivedTestBean tb = new DerivedTestBean();
		beanRegistry.registerSingleton("tb", tb);
		beanRegistry.registerDisposableBean("tb", tb);
		assertThat(beanRegistry.getSingleton("tb")).isSameAs(tb);

		assertThat(beanRegistry.getSingleton("tb")).isSameAs(tb);
		assertThat(beanRegistry.getSingletonCount()).isEqualTo(1);
		assertThat(beanRegistry.getSingletonNames()).containsExactly("tb");
		assertThat(tb.wasDestroyed()).isFalse();

		beanRegistry.destroySingletons();
		assertThat(beanRegistry.getSingletonCount()).isZero();
		assertThat(beanRegistry.getSingletonNames()).isEmpty();
		assertThat(tb.wasDestroyed()).isTrue();
	}

	@Test
	void dependentRegistration() {
		beanRegistry.registerDependentBean("a", "b");
		beanRegistry.registerDependentBean("b", "c");
		beanRegistry.registerDependentBean("c", "b");
		assertThat(beanRegistry.isDependent("a", "b")).isTrue();
		assertThat(beanRegistry.isDependent("b", "c")).isTrue();
		assertThat(beanRegistry.isDependent("c", "b")).isTrue();
		assertThat(beanRegistry.isDependent("a", "c")).isTrue();
		assertThat(beanRegistry.isDependent("c", "a")).isFalse();
		assertThat(beanRegistry.isDependent("b", "a")).isFalse();
		assertThat(beanRegistry.isDependent("a", "a")).isFalse();
		assertThat(beanRegistry.isDependent("b", "b")).isTrue();
		assertThat(beanRegistry.isDependent("c", "c")).isTrue();
	}

}
