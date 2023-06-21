/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.context.annotation;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.lifecyclemethods.InitDestroyBean;
import org.springframework.context.annotation.lifecyclemethods.PackagePrivateInitDestroyBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests which verify expected <em>init</em> and <em>destroy</em> bean lifecycle
 * behavior as requested in
 * <a href="https://github.com/spring-projects/spring-framework/issues/8455" target="_blank">SPR-3775</a>.
 *
 * <p>Specifically, combinations of the following are tested:
 * <ul>
 * <li>{@link InitializingBean} &amp; {@link DisposableBean} interfaces</li>
 * <li>Custom {@link RootBeanDefinition#getInitMethodName() init} &amp;
 * {@link RootBeanDefinition#getDestroyMethodName() destroy} methods</li>
 * <li>JSR 250's {@link jakarta.annotation.PostConstruct @PostConstruct} &amp;
 * {@link jakarta.annotation.PreDestroy @PreDestroy} annotations</li>
 * </ul>
 *
 * @author Sam Brannen
 * @since 2.5
 */
class InitDestroyMethodLifecycleTests {

	@Test
	void initDestroyMethods() {
		Class<?> beanClass = InitDestroyBean.class;
		DefaultListableBeanFactory beanFactory = createBeanFactoryAndRegisterBean(beanClass, "initMethod", "destroyMethod");
		InitDestroyBean bean = beanFactory.getBean(InitDestroyBean.class);
		assertThat(bean.initMethods).as("init-methods").containsExactly("initMethod");
		beanFactory.destroySingletons();
		assertThat(bean.destroyMethods).as("destroy-methods").containsExactly("destroyMethod");
	}

	@Test
	void initializingDisposableInterfaces() {
		Class<?> beanClass = CustomInitializingDisposableBean.class;
		DefaultListableBeanFactory beanFactory = createBeanFactoryAndRegisterBean(beanClass, "customInit", "customDestroy");
		CustomInitializingDisposableBean bean = beanFactory.getBean(CustomInitializingDisposableBean.class);
		assertThat(bean.initMethods).as("init-methods").containsExactly("afterPropertiesSet", "customInit");
		beanFactory.destroySingletons();
		assertThat(bean.destroyMethods).as("destroy-methods").containsExactly("destroy", "customDestroy");
	}

	@Test
	void initializingDisposableInterfacesWithShadowedMethods() {
		Class<?> beanClass = InitializingDisposableWithShadowedMethodsBean.class;
		DefaultListableBeanFactory beanFactory = createBeanFactoryAndRegisterBean(beanClass, "afterPropertiesSet", "destroy");
		InitializingDisposableWithShadowedMethodsBean bean = beanFactory.getBean(InitializingDisposableWithShadowedMethodsBean.class);
		assertThat(bean.initMethods).as("init-methods").containsExactly("InitializingBean.afterPropertiesSet");
		beanFactory.destroySingletons();
		assertThat(bean.destroyMethods).as("destroy-methods").containsExactly("DisposableBean.destroy");
	}

	@Test
	void jakartaAnnotations() {
		Class<?> beanClass = CustomAnnotatedInitDestroyBean.class;
		DefaultListableBeanFactory beanFactory = createBeanFactoryAndRegisterBean(beanClass, "customInit", "customDestroy");
		CustomAnnotatedInitDestroyBean bean = beanFactory.getBean(CustomAnnotatedInitDestroyBean.class);
		assertThat(bean.initMethods).as("init-methods").containsExactly("postConstruct", "afterPropertiesSet", "customInit");
		beanFactory.destroySingletons();
		assertThat(bean.destroyMethods).as("destroy-methods").containsExactly("preDestroy", "destroy", "customDestroy");
	}

	@Test
	void jakartaAnnotationsWithShadowedMethods() {
		Class<?> beanClass = CustomAnnotatedInitDestroyWithShadowedMethodsBean.class;
		DefaultListableBeanFactory beanFactory = createBeanFactoryAndRegisterBean(beanClass, "customInit", "customDestroy");
		CustomAnnotatedInitDestroyWithShadowedMethodsBean bean = beanFactory.getBean(CustomAnnotatedInitDestroyWithShadowedMethodsBean.class);
		assertThat(bean.initMethods).as("init-methods").containsExactly("@PostConstruct.afterPropertiesSet", "customInit");
		beanFactory.destroySingletons();
		assertThat(bean.destroyMethods).as("destroy-methods").containsExactly("@PreDestroy.destroy", "customDestroy");
	}

	@Test
	void jakartaAnnotationsWithCustomPrivateInitDestroyMethods() {
		Class<?> beanClass = CustomAnnotatedPrivateInitDestroyBean.class;
		DefaultListableBeanFactory beanFactory = createBeanFactoryAndRegisterBean(beanClass, "customInit1", "customDestroy1");
		CustomAnnotatedPrivateInitDestroyBean bean = beanFactory.getBean(CustomAnnotatedPrivateInitDestroyBean.class);
		assertThat(bean.initMethods).as("init-methods").containsExactly("@PostConstruct.privateCustomInit1", "afterPropertiesSet");
		beanFactory.destroySingletons();
		assertThat(bean.destroyMethods).as("destroy-methods").containsExactly("@PreDestroy.privateCustomDestroy1", "destroy");
	}

	@Test
	void jakartaAnnotationsCustomPrivateInitDestroyMethodsWithTheSameMethodNames() {
		Class<?> beanClass = CustomAnnotatedPrivateSameNameInitDestroyBean.class;
		DefaultListableBeanFactory beanFactory = createBeanFactoryAndRegisterBean(beanClass, "customInit", "customDestroy");
		CustomAnnotatedPrivateSameNameInitDestroyBean bean = beanFactory.getBean(CustomAnnotatedPrivateSameNameInitDestroyBean.class);

		assertThat(bean.initMethods).as("init-methods").containsExactly(
				"@PostConstruct.privateCustomInit1",
				"@PostConstruct.sameNameCustomInit1",
				"afterPropertiesSet",
				"customInit"
			);

		beanFactory.destroySingletons();
		assertThat(bean.destroyMethods).as("destroy-methods").containsExactly(
				"@PreDestroy.sameNameCustomDestroy1",
				"@PreDestroy.privateCustomDestroy1",
				"destroy",
				"customDestroy"
			);
	}

	@Test
	void jakartaAnnotationsCustomPackagePrivateInitDestroyMethodsWithTheSameMethodNames() {
		Class<?> beanClass = SubPackagePrivateInitDestroyBean.class;
		DefaultListableBeanFactory beanFactory = createBeanFactoryAndRegisterBean(beanClass, "initMethod", "destroyMethod");
		SubPackagePrivateInitDestroyBean bean = beanFactory.getBean(SubPackagePrivateInitDestroyBean.class);

		assertThat(bean.initMethods).as("init-methods").containsExactly(
				"PackagePrivateInitDestroyBean.postConstruct",
				"SubPackagePrivateInitDestroyBean.postConstruct",
				"initMethod"
			);

		beanFactory.destroySingletons();
		assertThat(bean.destroyMethods).as("destroy-methods").containsExactly(
				"SubPackagePrivateInitDestroyBean.preDestroy",
				"PackagePrivateInitDestroyBean.preDestroy",
				"destroyMethod"
			);
	}

	@Test
	void allLifecycleMechanismsAtOnce() {
		Class<?> beanClass = AllInOneBean.class;
		DefaultListableBeanFactory beanFactory = createBeanFactoryAndRegisterBean(beanClass, "afterPropertiesSet", "destroy");
		AllInOneBean bean = beanFactory.getBean(AllInOneBean.class);
		assertThat(bean.initMethods).as("init-methods").containsExactly("afterPropertiesSet");
		beanFactory.destroySingletons();
		assertThat(bean.destroyMethods).as("destroy-methods").containsExactly("destroy");
	}


	private static DefaultListableBeanFactory createBeanFactoryAndRegisterBean(Class<?> beanClass,
			String initMethodName, String destroyMethodName) {

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.addBeanPostProcessor(new CommonAnnotationBeanPostProcessor());

		// Configure and register an InitDestroyAnnotationBeanPostProcessor as
		// done in AnnotationConfigUtils.registerAnnotationConfigProcessors()
		// for an ApplicatonContext.
		InitDestroyAnnotationBeanPostProcessor initDestroyBpp = new InitDestroyAnnotationBeanPostProcessor();
		initDestroyBpp.setInitAnnotationType(javax.annotation.PostConstruct.class);
		initDestroyBpp.setDestroyAnnotationType(javax.annotation.PreDestroy.class);
		beanFactory.addBeanPostProcessor(initDestroyBpp);

		RootBeanDefinition beanDefinition = new RootBeanDefinition(beanClass);
		beanDefinition.setInitMethodName(initMethodName);
		beanDefinition.setDestroyMethodName(destroyMethodName);
		beanFactory.registerBeanDefinition("lifecycleTestBean", beanDefinition);
		return beanFactory;
	}


	static class InitializingDisposableWithShadowedMethodsBean extends InitDestroyBean implements
			InitializingBean, DisposableBean {

		@Override
		public void afterPropertiesSet() throws Exception {
			this.initMethods.add("InitializingBean.afterPropertiesSet");
		}

		@Override
		public void destroy() throws Exception {
			this.destroyMethods.add("DisposableBean.destroy");
		}
	}

	static class CustomInitDestroyBean {

		final List<String> initMethods = new ArrayList<>();
		final List<String> destroyMethods = new ArrayList<>();

		public void customInit() throws Exception {
			this.initMethods.add("customInit");
		}

		public void customDestroy() throws Exception {
			this.destroyMethods.add("customDestroy");
		}
	}

	static class CustomAnnotatedPrivateInitDestroyBean extends CustomInitializingDisposableBean {

		@PostConstruct
		private void customInit1() throws Exception {
			this.initMethods.add("@PostConstruct.privateCustomInit1");
		}

		@PreDestroy
		private void customDestroy1() throws Exception {
			this.destroyMethods.add("@PreDestroy.privateCustomDestroy1");
		}
	}

	static class CustomAnnotatedPrivateSameNameInitDestroyBean extends CustomAnnotatedPrivateInitDestroyBean {

		@PostConstruct
		@SuppressWarnings("unused")
		private void customInit1() throws Exception {
			this.initMethods.add("@PostConstruct.sameNameCustomInit1");
		}

		@PreDestroy
		@SuppressWarnings("unused")
		private void customDestroy1() throws Exception {
			this.destroyMethods.add("@PreDestroy.sameNameCustomDestroy1");
		}
	}

	static class CustomInitializingDisposableBean extends CustomInitDestroyBean
			implements InitializingBean, DisposableBean {

		@Override
		public void afterPropertiesSet() throws Exception {
			this.initMethods.add("afterPropertiesSet");
		}

		@Override
		public void destroy() throws Exception {
			this.destroyMethods.add("destroy");
		}
	}

	static class CustomAnnotatedInitDestroyBean extends CustomInitializingDisposableBean {

		@PostConstruct
		public void postConstruct() throws Exception {
			this.initMethods.add("postConstruct");
		}

		@PreDestroy
		public void preDestroy() throws Exception {
			this.destroyMethods.add("preDestroy");
		}
	}

	static class CustomAnnotatedInitDestroyWithShadowedMethodsBean extends CustomInitializingDisposableBean {

		@PostConstruct
		@Override
		public void afterPropertiesSet() throws Exception {
			this.initMethods.add("@PostConstruct.afterPropertiesSet");
		}

		@PreDestroy
		@Override
		public void destroy() throws Exception {
			this.destroyMethods.add("@PreDestroy.destroy");
		}
	}

	static class AllInOneBean implements InitializingBean, DisposableBean {

		final List<String> initMethods = new ArrayList<>();
		final List<String> destroyMethods = new ArrayList<>();

		@PostConstruct
		@Override
		public void afterPropertiesSet() throws Exception {
			this.initMethods.add("afterPropertiesSet");
		}

		@PreDestroy
		@Override
		public void destroy() throws Exception {
			this.destroyMethods.add("destroy");
		}
	}

	static class SubPackagePrivateInitDestroyBean extends PackagePrivateInitDestroyBean {

		@PostConstruct
		void postConstruct() {
			this.initMethods.add("SubPackagePrivateInitDestroyBean.postConstruct");
		}

		@PreDestroy
		void preDestroy() {
			this.destroyMethods.add("SubPackagePrivateInitDestroyBean.preDestroy");
		}

	}

}
