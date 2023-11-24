/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.expression.spel.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.expression.BeanResolver;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypeLocator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StandardEvaluationContext}.
 *
 * @author Stephane Nicoll
 */
class StandardEvaluationContextTests {

	private final StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

	@Test
	void applyDelegatesToSetDelegatesToTarget() {
		StandardEvaluationContext target = new StandardEvaluationContext();
		this.evaluationContext.applyDelegatesTo(target);
		assertThat(target).hasFieldOrProperty("reflectiveMethodResolver").isNotNull();
		assertThat(target.getBeanResolver()).isSameAs(this.evaluationContext.getBeanResolver());
		assertThat(target.getTypeLocator()).isSameAs(this.evaluationContext.getTypeLocator());
		assertThat(target.getTypeConverter()).isSameAs(this.evaluationContext.getTypeConverter());
		assertThat(target.getOperatorOverloader()).isSameAs(this.evaluationContext.getOperatorOverloader());
		assertThat(target.getPropertyAccessors()).satisfies(hasSameElements(
				this.evaluationContext.getPropertyAccessors()));
		assertThat(target.getConstructorResolvers()).satisfies(hasSameElements(
				this.evaluationContext.getConstructorResolvers()));
		assertThat(target.getMethodResolvers()).satisfies(hasSameElements(
				this.evaluationContext.getMethodResolvers()));
	}

	@Test
	void applyDelegatesToSetOverrideDelegatesInTarget() {
		StandardEvaluationContext target = new StandardEvaluationContext();
		target.setBeanResolver(mock(BeanResolver.class));
		target.setTypeLocator(mock(TypeLocator.class));
		target.setTypeConverter(mock(TypeConverter.class));
		target.setOperatorOverloader(mock(OperatorOverloader.class));
		target.setPropertyAccessors(new ArrayList<>());
		target.setConstructorResolvers(new ArrayList<>());
		target.setMethodResolvers(new ArrayList<>());
		this.evaluationContext.applyDelegatesTo(target);
		assertThat(target).hasFieldOrProperty("reflectiveMethodResolver").isNotNull();
		assertThat(target.getBeanResolver()).isSameAs(this.evaluationContext.getBeanResolver());
		assertThat(target.getTypeLocator()).isSameAs(this.evaluationContext.getTypeLocator());
		assertThat(target.getTypeConverter()).isSameAs(this.evaluationContext.getTypeConverter());
		assertThat(target.getOperatorOverloader()).isSameAs(this.evaluationContext.getOperatorOverloader());
		assertThat(target.getPropertyAccessors()).satisfies(hasSameElements(
				this.evaluationContext.getPropertyAccessors()));
		assertThat(target.getConstructorResolvers()).satisfies(hasSameElements(
				this.evaluationContext.getConstructorResolvers()));
		assertThat(target.getMethodResolvers()).satisfies(hasSameElements(
				this.evaluationContext.getMethodResolvers()));
	}

	@Test
	void applyDelegatesToMakesACopyOfPropertyAccessors() {
		StandardEvaluationContext target = new StandardEvaluationContext();
		this.evaluationContext.applyDelegatesTo(target);
		PropertyAccessor propertyAccessor = mock(PropertyAccessor.class);
		this.evaluationContext.getPropertyAccessors().add(propertyAccessor);
		assertThat(target.getPropertyAccessors()).doesNotContain(propertyAccessor);
	}

	@Test
	void applyDelegatesToMakesACopyOfConstructorResolvers() {
		StandardEvaluationContext target = new StandardEvaluationContext();
		this.evaluationContext.applyDelegatesTo(target);
		ConstructorResolver methodResolver = mock(ConstructorResolver.class);
		this.evaluationContext.getConstructorResolvers().add(methodResolver);
		assertThat(target.getConstructorResolvers()).doesNotContain(methodResolver);
	}

	@Test
	void applyDelegatesToMakesACopyOfMethodResolvers() {
		StandardEvaluationContext target = new StandardEvaluationContext();
		this.evaluationContext.applyDelegatesTo(target);
		MethodResolver methodResolver = mock(MethodResolver.class);
		this.evaluationContext.getMethodResolvers().add(methodResolver);
		assertThat(target.getMethodResolvers()).doesNotContain(methodResolver);
	}

	private Consumer<List<?>> hasSameElements(List<?> candidates) {
		return actual -> {
			assertThat(actual.size()).isEqualTo(candidates.size());
			for (int i = 0; i < candidates.size(); i++) {
				assertThat(candidates.get(i)).isSameAs(actual.get(i));
			}
		};
	}

}
