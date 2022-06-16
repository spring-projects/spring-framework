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

package org.springframework.beans.factory.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generator.CodeContribution;
import org.springframework.aot.generator.DefaultCodeContribution;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessorTests.ResourceInjectionBean;
import org.springframework.beans.factory.generator.BeanInstantiationContribution;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.env.Environment;
import org.springframework.javapoet.support.CodeSnippet;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for code contribution of {@link AutowiredAnnotationBeanPostProcessor}.
 *
 * @author Stephane Nicoll
 */
class AutowiredAnnotationBeanInstantiationContributionTests {

	@Test
	void contributeWithPackageProtectedFieldInjection() {
		CodeContribution contribution = contribute(PackageProtectedFieldInjectionSample.class);
		assertThat(CodeSnippet.process(contribution.statements().toCodeBlock())).isEqualTo("""
				instanceContext.field("environment")
						.invoke(beanFactory, (attributes) -> bean.environment = attributes.get(0))""");
		assertThat(contribution.runtimeHints().reflection().typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(PackageProtectedFieldInjectionSample.class));
			assertThat(typeHint.fields()).singleElement().satisfies(fieldHint -> {
				assertThat(fieldHint.getName()).isEqualTo("environment");
				assertThat(fieldHint.isAllowWrite()).isTrue();
				assertThat(fieldHint.isAllowUnsafeAccess()).isFalse();
			});
		});
		assertThat(contribution.protectedAccess().getPrivilegedPackageName("com.example"))
				.isEqualTo(PackageProtectedFieldInjectionSample.class.getPackageName());
	}

	@Test
	void contributeWithPrivateFieldInjection() {
		CodeContribution contribution = contribute(PrivateFieldInjectionSample.class);
		assertThat(CodeSnippet.process(contribution.statements().toCodeBlock())).isEqualTo("""
				instanceContext.field("environment")
						.invoke(beanFactory, (attributes) -> {
							Field environmentField = ReflectionUtils.findField(AutowiredAnnotationBeanInstantiationContributionTests.PrivateFieldInjectionSample.class, "environment");
							ReflectionUtils.makeAccessible(environmentField);
							ReflectionUtils.setField(environmentField, bean, attributes.get(0));
						})""");
		assertThat(contribution.runtimeHints().reflection().typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(PrivateFieldInjectionSample.class));
			assertThat(typeHint.fields()).singleElement().satisfies(fieldHint -> {
				assertThat(fieldHint.getName()).isEqualTo("environment");
				assertThat(fieldHint.isAllowWrite()).isTrue();
				assertThat(fieldHint.isAllowUnsafeAccess()).isFalse();
			});
		});
		assertThat(contribution.protectedAccess().isAccessible("com.example")).isTrue();
	}

	@Test
	void contributeWithPublicMethodInjection() {
		CodeContribution contribution = contribute(PublicMethodInjectionSample.class);
		assertThat(CodeSnippet.process(contribution.statements().toCodeBlock())).isEqualTo("""
				instanceContext.method("setTestBean", TestBean.class)
						.invoke(beanFactory, (attributes) -> bean.setTestBean(attributes.get(0)))""");
		assertThat(contribution.runtimeHints().reflection().typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(PublicMethodInjectionSample.class));
			assertThat(typeHint.methods()).singleElement().satisfies(methodHint -> {
				assertThat(methodHint.getName()).isEqualTo("setTestBean");
				assertThat(methodHint.getModes()).contains(ExecutableMode.INTROSPECT);
			});
		});
		assertThat(contribution.protectedAccess().isAccessible("com.example")).isTrue();
	}

	@Test
	void contributeWithInjectionPoints() {
		CodeContribution contribution = contribute(ResourceInjectionBean.class);
		assertThat(CodeSnippet.process(contribution.statements().toCodeBlock())).isEqualTo("""
				instanceContext.field("testBean")
						.resolve(beanFactory, false).ifResolved((attributes) -> {
							Field testBeanField = ReflectionUtils.findField(AutowiredAnnotationBeanPostProcessorTests.ResourceInjectionBean.class, "testBean");
							ReflectionUtils.makeAccessible(testBeanField);
							ReflectionUtils.setField(testBeanField, bean, attributes.get(0));
						});
				instanceContext.method("setTestBean2", TestBean.class)
						.invoke(beanFactory, (attributes) -> bean.setTestBean2(attributes.get(0)));""");
		assertThat(contribution.runtimeHints().reflection().typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.fields()).singleElement().satisfies(fieldHint ->
					assertThat(fieldHint.getName()).isEqualTo("testBean"));
			assertThat(typeHint.methods()).singleElement().satisfies(methodHint ->
					assertThat(methodHint.getName()).isEqualTo("setTestBean2"));
		});
		assertThat(contribution.protectedAccess().isAccessible("com.example")).isTrue();
	}

	@Test
	void contributeWithoutInjectionPoints() {
		BeanInstantiationContribution contributor = createContribution(String.class);
		assertThat(contributor).isNull();
	}

	private DefaultCodeContribution contribute(Class<?> type) {
		BeanInstantiationContribution contributor = createContribution(type);
		assertThat(contributor).isNotNull();
		DefaultCodeContribution contribution = new DefaultCodeContribution(new RuntimeHints());
		contributor.applyTo(contribution);
		return contribution;
	}

	@Nullable
	private BeanInstantiationContribution createContribution(Class<?> type) {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(type);
		return bpp.contribute(beanDefinition, type, "test");
	}


	public static class PackageProtectedFieldInjectionSample {

		@Autowired
		Environment environment;

	}

	public static class PrivateFieldInjectionSample {

		@Autowired
		@SuppressWarnings("unused")
		private Environment environment;

	}

	public static class PublicMethodInjectionSample {

		@Autowired
		public void setTestBean(TestBean testBean) {

		}

		public void setUnrelated(String unrelated) {

		}

	}


}
