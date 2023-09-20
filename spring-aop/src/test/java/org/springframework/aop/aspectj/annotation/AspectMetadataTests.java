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

package org.springframework.aop.aspectj.annotation;

import org.aspectj.lang.reflect.PerClauseKind;
import org.junit.jupiter.api.Test;

import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactoryTests.ExceptionThrowingAspect;
import org.springframework.aop.testfixture.aspectj.PerTargetAspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rod Johnson
 * @author Chris Beams
 * @author Sam Brannen
 */
class AspectMetadataTests {

	@Test
	void notAnAspect() {
		assertThatIllegalArgumentException().isThrownBy(() -> new AspectMetadata(String.class, "someBean"));
	}

	@Test
	void singletonAspect() {
		AspectMetadata am = new AspectMetadata(ExceptionThrowingAspect.class, "someBean");
		assertThat(am.isPerThisOrPerTarget()).isFalse();
		assertThat(am.getPerClausePointcut()).isSameAs(Pointcut.TRUE);
		assertThat(am.getAjType().getPerClause().getKind()).isEqualTo(PerClauseKind.SINGLETON);
	}

	@Test
	void perTargetAspect() {
		AspectMetadata am = new AspectMetadata(PerTargetAspect.class, "someBean");
		assertThat(am.isPerThisOrPerTarget()).isTrue();
		assertThat(am.getPerClausePointcut()).isNotSameAs(Pointcut.TRUE);
		assertThat(am.getAjType().getPerClause().getKind()).isEqualTo(PerClauseKind.PERTARGET);
		assertThat(am.getPerClausePointcut()).isInstanceOf(AspectJExpressionPointcut.class);
		assertThat(((AspectJExpressionPointcut) am.getPerClausePointcut()).getExpression())
				.isEqualTo("execution(* *.getSpouse())");
	}

	@Test
	void perThisAspect() {
		AspectMetadata am = new AspectMetadata(PerThisAspect.class, "someBean");
		assertThat(am.isPerThisOrPerTarget()).isTrue();
		assertThat(am.getPerClausePointcut()).isNotSameAs(Pointcut.TRUE);
		assertThat(am.getAjType().getPerClause().getKind()).isEqualTo(PerClauseKind.PERTHIS);
		assertThat(am.getPerClausePointcut()).isInstanceOf(AspectJExpressionPointcut.class);
		assertThat(((AspectJExpressionPointcut) am.getPerClausePointcut()).getExpression())
				.isEqualTo("execution(* *.getSpouse())");
	}

}
