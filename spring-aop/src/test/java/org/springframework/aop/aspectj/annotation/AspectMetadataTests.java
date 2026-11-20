/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
