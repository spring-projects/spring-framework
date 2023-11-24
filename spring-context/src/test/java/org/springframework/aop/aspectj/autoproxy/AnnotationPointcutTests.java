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

package org.springframework.aop.aspectj.autoproxy;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class AnnotationPointcutTests {

	private ClassPathXmlApplicationContext ctx;

	private AnnotatedTestBean testBean;


	@BeforeEach
	void setup() {
		this.ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());
		this.testBean = ctx.getBean("testBean", AnnotatedTestBean.class);
	}

	@AfterEach
	void tearDown() {
		this.ctx.close();
	}


	@Test
	void annotationBindingInAroundAdvice() {
		assertThat(testBean.doThis()).isEqualTo("this value");
	}

	@Test
	void noMatchingWithoutAnnotationPresent() {
		assertThat(testBean.doTheOther()).isEqualTo("doTheOther");
	}

}


class TestMethodInterceptor implements MethodInterceptor {

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		return "this value";
	}
}
