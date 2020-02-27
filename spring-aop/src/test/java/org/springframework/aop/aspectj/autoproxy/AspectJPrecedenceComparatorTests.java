/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.aop.aspectj.autoproxy;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.Advisor;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.BeforeAdvice;
import org.springframework.aop.aspectj.AbstractAspectJAdvice;
import org.springframework.aop.aspectj.AspectJAfterAdvice;
import org.springframework.aop.aspectj.AspectJAfterReturningAdvice;
import org.springframework.aop.aspectj.AspectJAfterThrowingAdvice;
import org.springframework.aop.aspectj.AspectJAroundAdvice;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.AspectJMethodBeforeAdvice;
import org.springframework.aop.aspectj.AspectJPointcutAdvisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Adrian Colyer
 * @author Chris Beams
 */
public class AspectJPrecedenceComparatorTests {

	private static final int HIGH_PRECEDENCE_ADVISOR_ORDER = 100;
	private static final int LOW_PRECEDENCE_ADVISOR_ORDER = 200;
	private static final int EARLY_ADVICE_DECLARATION_ORDER = 5;
	private static final int LATE_ADVICE_DECLARATION_ORDER = 10;


	private AspectJPrecedenceComparator comparator;

	private Method anyOldMethod;

	private AspectJExpressionPointcut anyOldPointcut;


	@BeforeEach
	public void setUp() throws Exception {
		this.comparator = new AspectJPrecedenceComparator();
		this.anyOldMethod = getClass().getMethods()[0];
		this.anyOldPointcut = new AspectJExpressionPointcut();
		this.anyOldPointcut.setExpression("execution(* *(..))");
	}


	@Test
	public void testSameAspectNoAfterAdvice() {
		Advisor advisor1 = createAspectJBeforeAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someAspect");
		Advisor advisor2 = createAspectJBeforeAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someAspect");
		assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor1 sorted before advisor2").isEqualTo(-1);

		advisor1 = createAspectJBeforeAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someAspect");
		advisor2 = createAspectJAroundAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someAspect");
		assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor2 sorted before advisor1").isEqualTo(1);
	}

	@Test
	public void testSameAspectAfterAdvice() {
		Advisor advisor1 = createAspectJAfterAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someAspect");
		Advisor advisor2 = createAspectJAroundAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someAspect");
		assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor2 sorted before advisor1").isEqualTo(1);

		advisor1 = createAspectJAfterReturningAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someAspect");
		advisor2 = createAspectJAfterThrowingAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someAspect");
		assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor1 sorted before advisor2").isEqualTo(-1);
	}

	@Test
	public void testSameAspectOneOfEach() {
		Advisor advisor1 = createAspectJAfterAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someAspect");
		Advisor advisor2 = createAspectJBeforeAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someAspect");
		assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor1 and advisor2 not comparable").isEqualTo(1);
	}

	@Test
	public void testSameAdvisorPrecedenceDifferentAspectNoAfterAdvice() {
		Advisor advisor1 = createAspectJBeforeAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someAspect");
		Advisor advisor2 = createAspectJBeforeAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someOtherAspect");
		assertThat(this.comparator.compare(advisor1, advisor2)).as("nothing to say about order here").isEqualTo(0);

		advisor1 = createAspectJBeforeAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someAspect");
		advisor2 = createAspectJAroundAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someOtherAspect");
		assertThat(this.comparator.compare(advisor1, advisor2)).as("nothing to say about order here").isEqualTo(0);
	}

	@Test
	public void testSameAdvisorPrecedenceDifferentAspectAfterAdvice() {
		Advisor advisor1 = createAspectJAfterAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someAspect");
		Advisor advisor2 = createAspectJAroundAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someOtherAspect");
		assertThat(this.comparator.compare(advisor1, advisor2)).as("nothing to say about order here").isEqualTo(0);

		advisor1 = createAspectJAfterReturningAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someAspect");
		advisor2 = createAspectJAfterThrowingAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someOtherAspect");
		assertThat(this.comparator.compare(advisor1, advisor2)).as("nothing to say about order here").isEqualTo(0);
	}

	@Test
	public void testHigherAdvisorPrecedenceNoAfterAdvice() {
		Advisor advisor1 = createSpringAOPBeforeAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER);
		Advisor advisor2 = createAspectJBeforeAdvice(LOW_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someOtherAspect");
		assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor1 sorted before advisor2").isEqualTo(-1);

		advisor1 = createAspectJBeforeAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someAspect");
		advisor2 = createAspectJAroundAdvice(LOW_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someOtherAspect");
		assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor1 sorted before advisor2").isEqualTo(-1);
	}

	@Test
	public void testHigherAdvisorPrecedenceAfterAdvice() {
		Advisor advisor1 = createAspectJAfterAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someAspect");
		Advisor advisor2 = createAspectJAroundAdvice(LOW_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someOtherAspect");
		assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor1 sorted before advisor2").isEqualTo(-1);

		advisor1 = createAspectJAfterReturningAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someAspect");
		advisor2 = createAspectJAfterThrowingAdvice(LOW_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someOtherAspect");
		assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor2 sorted after advisor1").isEqualTo(-1);
	}

	@Test
	public void testLowerAdvisorPrecedenceNoAfterAdvice() {
		Advisor advisor1 = createAspectJBeforeAdvice(LOW_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someAspect");
		Advisor advisor2 = createAspectJBeforeAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someOtherAspect");
		assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor1 sorted after advisor2").isEqualTo(1);

		advisor1 = createAspectJBeforeAdvice(LOW_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someAspect");
		advisor2 = createAspectJAroundAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someOtherAspect");
		assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor1 sorted after advisor2").isEqualTo(1);
	}

	@Test
	public void testLowerAdvisorPrecedenceAfterAdvice() {
		Advisor advisor1 = createAspectJAfterAdvice(LOW_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someAspect");
		Advisor advisor2 = createAspectJAroundAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, LATE_ADVICE_DECLARATION_ORDER, "someOtherAspect");
		assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor1 sorted after advisor2").isEqualTo(1);

		advisor1 = createSpringAOPAfterAdvice(LOW_PRECEDENCE_ADVISOR_ORDER);
		advisor2 = createAspectJAfterThrowingAdvice(HIGH_PRECEDENCE_ADVISOR_ORDER, EARLY_ADVICE_DECLARATION_ORDER, "someOtherAspect");
		assertThat(this.comparator.compare(advisor1, advisor2)).as("advisor1 sorted after advisor2").isEqualTo(1);
	}


	private Advisor createAspectJBeforeAdvice(int advisorOrder, int adviceDeclarationOrder, String aspectName) {
		AspectJMethodBeforeAdvice advice = new AspectJMethodBeforeAdvice(this.anyOldMethod, this.anyOldPointcut, null);
		return createAspectJAdvice(advisorOrder, adviceDeclarationOrder, aspectName, advice);
	}

	private Advisor createAspectJAroundAdvice(int advisorOrder, int adviceDeclarationOrder, String aspectName) {
		AspectJAroundAdvice advice = new AspectJAroundAdvice(this.anyOldMethod, this.anyOldPointcut, null);
		return createAspectJAdvice(advisorOrder, adviceDeclarationOrder, aspectName, advice);
	}

	private Advisor createAspectJAfterAdvice(int advisorOrder, int adviceDeclarationOrder, String aspectName) {
		AspectJAfterAdvice advice = new AspectJAfterAdvice(this.anyOldMethod, this.anyOldPointcut, null);
		return createAspectJAdvice(advisorOrder, adviceDeclarationOrder, aspectName, advice);
	}

	private Advisor createAspectJAfterReturningAdvice(int advisorOrder, int adviceDeclarationOrder, String aspectName) {
		AspectJAfterReturningAdvice advice = new AspectJAfterReturningAdvice(this.anyOldMethod, this.anyOldPointcut, null);
		return createAspectJAdvice(advisorOrder, adviceDeclarationOrder, aspectName, advice);
	}

	private Advisor createAspectJAfterThrowingAdvice(int advisorOrder, int adviceDeclarationOrder, String aspectName) {
		AspectJAfterThrowingAdvice advice = new AspectJAfterThrowingAdvice(this.anyOldMethod, this.anyOldPointcut, null);
		return createAspectJAdvice(advisorOrder, adviceDeclarationOrder, aspectName, advice);
	}

	private Advisor createAspectJAdvice(int advisorOrder, int adviceDeclarationOrder, String aspectName, AbstractAspectJAdvice advice) {
		advice.setDeclarationOrder(adviceDeclarationOrder);
		advice.setAspectName(aspectName);
		AspectJPointcutAdvisor advisor = new AspectJPointcutAdvisor(advice);
		advisor.setOrder(advisorOrder);
		return advisor;
	}

	private Advisor createSpringAOPAfterAdvice(int order) {
		AfterReturningAdvice advice = (returnValue, method, args, target) -> {
		};
		DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(this.anyOldPointcut, advice);
		advisor.setOrder(order);
		return advisor;
	}

	private Advisor createSpringAOPBeforeAdvice(int order) {
		BeforeAdvice advice = new BeforeAdvice() {
		};
		DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(this.anyOldPointcut, advice);
		advisor.setOrder(order);
		return advisor;
	}

}
