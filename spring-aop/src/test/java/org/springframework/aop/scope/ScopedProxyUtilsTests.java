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

package org.springframework.aop.scope;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ScopedProxyUtils}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 5.1.10
 */
class ScopedProxyUtilsTests {

	@Test
	void getTargetBeanNameAndIsScopedTarget() {
		String originalBeanName = "myBean";
		String targetBeanName = ScopedProxyUtils.getTargetBeanName(originalBeanName);

		assertThat(targetBeanName).isNotEqualTo(originalBeanName).endsWith(originalBeanName);
		assertThat(ScopedProxyUtils.isScopedTarget(targetBeanName)).isTrue();
		assertThat(ScopedProxyUtils.isScopedTarget(originalBeanName)).isFalse();
	}

	@Test
	void getOriginalBeanNameAndIsScopedTarget() {
		String originalBeanName = "myBean";
		String targetBeanName = ScopedProxyUtils.getTargetBeanName(originalBeanName);
		String parsedOriginalBeanName = ScopedProxyUtils.getOriginalBeanName(targetBeanName);

		assertThat(parsedOriginalBeanName).isNotEqualTo(targetBeanName).isEqualTo(originalBeanName);
		assertThat(ScopedProxyUtils.isScopedTarget(targetBeanName)).isTrue();
		assertThat(ScopedProxyUtils.isScopedTarget(parsedOriginalBeanName)).isFalse();
	}

	@Test
	void getOriginalBeanNameForNullTargetBean() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> ScopedProxyUtils.getOriginalBeanName(null))
				.withMessage("bean name 'null' does not refer to the target of a scoped proxy");
	}

	@Test
	void getOriginalBeanNameForNonScopedTarget() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> ScopedProxyUtils.getOriginalBeanName("myBean"))
				.withMessage("bean name 'myBean' does not refer to the target of a scoped proxy");
	}

	@Test
	void createScopedProxyTargetAppliesAutowireSettingsToProxyBeanDefinition() {
		AbstractBeanDefinition targetDefinition = new GenericBeanDefinition();
		// Opposite of defaults
		targetDefinition.setAutowireCandidate(false);
		targetDefinition.setDefaultCandidate(false);
		targetDefinition.setPrimary(true);
		targetDefinition.setFallback(true);

		BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		BeanDefinitionHolder proxyHolder = ScopedProxyUtils.createScopedProxy(
				new BeanDefinitionHolder(targetDefinition, "myBean"), registry, false);
		AbstractBeanDefinition proxyBeanDefinition = (AbstractBeanDefinition) proxyHolder.getBeanDefinition();

		assertThat(proxyBeanDefinition.isAutowireCandidate()).isFalse();
		assertThat(proxyBeanDefinition.isDefaultCandidate()).isFalse();
		assertThat(proxyBeanDefinition.isPrimary()).isTrue();
		assertThat(proxyBeanDefinition.isFallback()).isTrue();
	}

	@Test
	void createScopedProxyTargetAppliesBeanAttributesToProxyBeanDefinition() {
		GenericBeanDefinition targetDefinition = new GenericBeanDefinition();
		// Opposite of defaults
		targetDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		targetDefinition.setSource("theSource");
		targetDefinition.addQualifier(new AutowireCandidateQualifier("myQualifier"));

		BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		BeanDefinitionHolder proxyHolder = ScopedProxyUtils.createScopedProxy(
				new BeanDefinitionHolder(targetDefinition, "myBean"), registry, false);
		BeanDefinition proxyBeanDefinition = proxyHolder.getBeanDefinition();

		assertThat(proxyBeanDefinition.getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
		assertThat(proxyBeanDefinition).isInstanceOf(RootBeanDefinition.class);
		assertThat(proxyBeanDefinition.getPropertyValues()).hasSize(2);
		assertThat(proxyBeanDefinition.getPropertyValues().get("proxyTargetClass")).isEqualTo(false);
		assertThat(proxyBeanDefinition.getPropertyValues().get("targetBeanName")).isEqualTo(
				ScopedProxyUtils.getTargetBeanName("myBean"));

		RootBeanDefinition rootBeanDefinition = (RootBeanDefinition) proxyBeanDefinition;
		assertThat(rootBeanDefinition.getQualifiers()).hasSize(1);
		assertThat(rootBeanDefinition.hasQualifier("myQualifier")).isTrue();
		assertThat(rootBeanDefinition.getSource()).isEqualTo("theSource");
	}

	@Test
	void createScopedProxyTargetCleansAutowireSettingsInTargetDefinition() {
		AbstractBeanDefinition targetDefinition = new GenericBeanDefinition();
		targetDefinition.setAutowireCandidate(true);
		targetDefinition.setDefaultCandidate(true);
		targetDefinition.setPrimary(true);
		targetDefinition.setFallback(true);

		BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
		ScopedProxyUtils.createScopedProxy(
				new BeanDefinitionHolder(targetDefinition, "myBean"), registry, false);

		assertThat(targetDefinition.isAutowireCandidate()).isFalse();
		assertThat(targetDefinition.isDefaultCandidate()).isFalse();
		assertThat(targetDefinition.isPrimary()).isFalse();
		assertThat(targetDefinition.isFallback()).isFalse();
	}

}
