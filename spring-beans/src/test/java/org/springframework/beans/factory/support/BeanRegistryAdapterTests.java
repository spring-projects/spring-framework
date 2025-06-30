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

package org.springframework.beans.factory.support;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanRegistryAdapter}.
 *
 * @author Sebastien Deleuze
 */
public class BeanRegistryAdapterTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final Environment env = new StandardEnvironment();

	@Test
	void defaultBackgroundInit() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, DefaultBeanRegistrar.class);
		new DefaultBeanRegistrar().register(adapter, env);
		AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition) this.beanFactory.getBeanDefinition("foo");
		assertThat(beanDefinition.isBackgroundInit()).isFalse();
	}

	@Test
	void enableBackgroundInit() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, BackgroundInitBeanRegistrar.class);
		new BackgroundInitBeanRegistrar().register(adapter, env);
		AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition) this.beanFactory.getBeanDefinition("foo");
		assertThat(beanDefinition.isBackgroundInit()).isTrue();
	}

	@Test
	void defaultDescription() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, DefaultBeanRegistrar.class);
		new DefaultBeanRegistrar().register(adapter, env);
		BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition("foo");
		assertThat(beanDefinition.getDescription()).isNull();
	}

	@Test
	void customDescription() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, CustomDescriptionBeanRegistrar.class);
		new CustomDescriptionBeanRegistrar().register(adapter, env);
		BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition("foo");
		assertThat(beanDefinition.getDescription()).isEqualTo("custom");
	}

	@Test
	void defaultFallback() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, DefaultBeanRegistrar.class);
		new DefaultBeanRegistrar().register(adapter, env);
		BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition("foo");
		assertThat(beanDefinition.isFallback()).isFalse();
	}

	@Test
	void enableFallback() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, FallbackBeanRegistrar.class);
		new FallbackBeanRegistrar().register(adapter, env);
		BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition("foo");
		assertThat(beanDefinition.isFallback()).isTrue();
	}

	@Test
	void defaultRole() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, DefaultBeanRegistrar.class);
		new DefaultBeanRegistrar().register(adapter, env);
		BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition("foo");
		assertThat(beanDefinition.getRole()).isEqualTo(AbstractBeanDefinition.ROLE_APPLICATION);
	}

	@Test
	void infrastructureRole() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, InfrastructureBeanRegistrar.class);
		new InfrastructureBeanRegistrar().register(adapter, env);
		BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition("foo");
		assertThat(beanDefinition.getRole()).isEqualTo(AbstractBeanDefinition.ROLE_INFRASTRUCTURE);
	}

	@Test
	void defaultLazyInit() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, DefaultBeanRegistrar.class);
		new DefaultBeanRegistrar().register(adapter, env);
		AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition) this.beanFactory.getBeanDefinition("foo");
		assertThat(beanDefinition.isLazyInit()).isFalse();
	}

	@Test
	void enableLazyInit() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, LazyInitBeanRegistrar.class);
		new LazyInitBeanRegistrar().register(adapter, env);
		AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition) this.beanFactory.getBeanDefinition("foo");
		assertThat(beanDefinition.isLazyInit()).isTrue();
	}

	@Test
	void defaultAutowirable() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, DefaultBeanRegistrar.class);
		new DefaultBeanRegistrar().register(adapter, env);
		AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition) this.beanFactory.getBeanDefinition("foo");
		assertThat(beanDefinition.isAutowireCandidate()).isTrue();
	}

	@Test
	void notAutowirable() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, NotAutowirableBeanRegistrar.class);
		new NotAutowirableBeanRegistrar().register(adapter, env);
		AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition) this.beanFactory.getBeanDefinition("foo");
		assertThat(beanDefinition.isAutowireCandidate()).isFalse();
	}

	@Test
	void defaultOrder() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, DefaultBeanRegistrar.class);
		new DefaultBeanRegistrar().register(adapter, env);
		AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition) this.beanFactory.getBeanDefinition("foo");
		Integer order = (Integer)beanDefinition.getAttribute(AbstractBeanDefinition.ORDER_ATTRIBUTE);
		assertThat(order).isNull();
	}

	@Test
	void customOrder() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, CustomOrderBeanRegistrar.class);
		new CustomOrderBeanRegistrar().register(adapter, env);
		AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition) this.beanFactory.getBeanDefinition("foo");
		Integer order = (Integer)beanDefinition.getAttribute(AbstractBeanDefinition.ORDER_ATTRIBUTE);
		assertThat(order).isEqualTo(1);
	}

	@Test
	void defaultPrimary() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, DefaultBeanRegistrar.class);
		new DefaultBeanRegistrar().register(adapter, env);
		BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition("foo");
		assertThat(beanDefinition.isPrimary()).isFalse();
	}

	@Test
	void enablePrimary() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, PrimaryBeanRegistrar.class);
		new PrimaryBeanRegistrar().register(adapter, env);
		BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition("foo");
		assertThat(beanDefinition.isPrimary()).isTrue();
	}

	@Test
	void defaultScope() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, DefaultBeanRegistrar.class);
		new DefaultBeanRegistrar().register(adapter, env);
		BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition("foo");
		assertThat(beanDefinition.getScope()).isEqualTo(AbstractBeanDefinition.SCOPE_DEFAULT);
	}

	@Test
	void prototypeScope() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, PrototypeBeanRegistrar.class);
		new PrototypeBeanRegistrar().register(adapter, env);
		BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition("foo");
		assertThat(beanDefinition.getScope()).isEqualTo(AbstractBeanDefinition.SCOPE_PROTOTYPE);
	}

	@Test
	void defaultSupplier() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, DefaultBeanRegistrar.class);
		new DefaultBeanRegistrar().register(adapter, env);
		AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition)this.beanFactory.getBeanDefinition("foo");
		assertThat(beanDefinition.getInstanceSupplier()).isNull();
	}

	@Test
	void customSupplier() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, SupplierBeanRegistrar.class);
		new SupplierBeanRegistrar().register(adapter, env);
		AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition)this.beanFactory.getBeanDefinition("foo");
		Supplier<?> supplier = beanDefinition.getInstanceSupplier();
		assertThat(supplier).isNotNull();
		assertThat(supplier.get()).isNotNull().isInstanceOf(Foo.class);
	}

	@Test
	void customTargetTypeFromResolvableType() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, env, TargetTypeBeanRegistrar.class);
		new TargetTypeBeanRegistrar().register(adapter, env);
		RootBeanDefinition beanDefinition = (RootBeanDefinition)this.beanFactory.getBeanDefinition("fooSupplierFromResolvableType");
		assertThat(beanDefinition.getResolvableType().resolveGeneric(0)).isEqualTo(Foo.class);
	}

	@Test
	void customTargetTypeFromTypeReference() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, env, TargetTypeBeanRegistrar.class);
		new TargetTypeBeanRegistrar().register(adapter, env);
		RootBeanDefinition beanDefinition = (RootBeanDefinition)this.beanFactory.getBeanDefinition("fooSupplierFromTypeReference");
		assertThat(beanDefinition.getResolvableType().resolveGeneric(0)).isEqualTo(Foo.class);
	}

	@Test
	void registerViaAnotherRegistrar() {
		BeanRegistryAdapter adapter = new BeanRegistryAdapter(this.beanFactory, this.beanFactory, this.env, ChainedBeanRegistrar.class);
		new ChainedBeanRegistrar().register(adapter, env);
		assertThat(this.beanFactory.getBeanDefinition("foo")).isNotNull();
	}


	private static class ChainedBeanRegistrar implements BeanRegistrar {

		@Override
		public void register(BeanRegistry registry, Environment env) {
			registry.register(new DefaultBeanRegistrar());
		}
	}

	private static class DefaultBeanRegistrar implements BeanRegistrar {

		@Override
		public void register(BeanRegistry registry, Environment env) {
			registry.registerBean("foo", Foo.class);
		}
	}

	private static class BackgroundInitBeanRegistrar implements BeanRegistrar {

		@Override
		public void register(BeanRegistry registry, Environment env) {
			registry.registerBean("foo", Foo.class, BeanRegistry.Spec::backgroundInit);
		}
	}

	private static class CustomDescriptionBeanRegistrar implements BeanRegistrar {

		@Override
		public void register(BeanRegistry registry, Environment env) {
			registry.registerBean("foo", Foo.class, spec -> spec.description("custom"));
		}
	}

	private static class FallbackBeanRegistrar implements BeanRegistrar {

		@Override
		public void register(BeanRegistry registry, Environment env) {
			registry.registerBean("foo", Foo.class, BeanRegistry.Spec::fallback);
		}
	}

	private static class InfrastructureBeanRegistrar implements BeanRegistrar {

		@Override
		public void register(BeanRegistry registry, Environment env) {
			registry.registerBean("foo", Foo.class, BeanRegistry.Spec::infrastructure);
		}
	}

	private static class LazyInitBeanRegistrar implements BeanRegistrar {

		@Override
		public void register(BeanRegistry registry, Environment env) {
			registry.registerBean("foo", Foo.class, BeanRegistry.Spec::lazyInit);
		}
	}

	private static class NotAutowirableBeanRegistrar implements BeanRegistrar {

		@Override
		public void register(BeanRegistry registry, Environment env) {
			registry.registerBean("foo", Foo.class, BeanRegistry.Spec::notAutowirable);
		}
	}

	private static class CustomOrderBeanRegistrar implements BeanRegistrar {

		@Override
		public void register(BeanRegistry registry, Environment env) {
			registry.registerBean("foo", Foo.class, spec -> spec.order(1));
		}
	}

	private static class PrimaryBeanRegistrar implements BeanRegistrar {

		@Override
		public void register(BeanRegistry registry, Environment env) {
			registry.registerBean("foo", Foo.class, BeanRegistry.Spec::primary);
		}
	}

	private static class PrototypeBeanRegistrar implements BeanRegistrar {

		@Override
		public void register(BeanRegistry registry, Environment env) {
			registry.registerBean("foo", Foo.class, BeanRegistry.Spec::prototype);
		}
	}

	private static class SupplierBeanRegistrar implements BeanRegistrar {

		@Override
		public void register(BeanRegistry registry, Environment env) {
			registry.registerBean("foo", Foo.class, spec -> spec.supplier(context -> new Foo()));
		}
	}

	private static class TargetTypeBeanRegistrar implements BeanRegistrar {

		@Override
		public void register(BeanRegistry registry, Environment env) {
			registry.registerBean("fooSupplierFromResolvableType", Foo.class,
					spec -> spec.targetType(ResolvableType.forClassWithGenerics(Supplier.class, Foo.class)));
			ParameterizedTypeReference<Supplier<Foo>> type = new ParameterizedTypeReference<>() {};
			registry.registerBean("fooSupplierFromTypeReference", Supplier.class,
					spec -> spec.targetType(type));
		}
	}

	private static class Foo {}

}
