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

package org.springframework.aop.aspectj;

import java.io.Serializable;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Adrian Colyer
 * @author Chris Beams
 */
class DeclarationOrderIndependenceTests {

	private ClassPathXmlApplicationContext ctx;

	private TopsyTurvyAspect aspect;

	private TopsyTurvyTarget target;


	@BeforeEach
	void setup() {
		this.ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		aspect = ctx.getBean(TopsyTurvyAspect.class);
		target = ctx.getBean(TopsyTurvyTarget.class);
	}

	@AfterEach
	void tearDown() {
		this.ctx.close();
	}


	@Test
	void targetIsSerializable() {
		assertThat(this.target).isInstanceOf(Serializable.class);
	}

	@Test
	void targetIsBeanNameAware() {
		assertThat(this.target).isInstanceOf(BeanNameAware.class);
	}

	@Test
	void beforeAdviceFiringOk() {
		AspectCollaborator collab = new AspectCollaborator();
		this.aspect.setCollaborator(collab);
		this.target.doSomething();
		assertThat(collab.beforeFired).as("before advice fired").isTrue();
	}

	@Test
	void aroundAdviceFiringOk() {
		AspectCollaborator collab = new AspectCollaborator();
		this.aspect.setCollaborator(collab);
		this.target.getX();
		assertThat(collab.aroundFired).as("around advice fired").isTrue();
	}

	@Test
	void afterReturningFiringOk() {
		AspectCollaborator collab = new AspectCollaborator();
		this.aspect.setCollaborator(collab);
		this.target.getX();
		assertThat(collab.afterReturningFired).as("after returning advice fired").isTrue();
	}


	/** public visibility is required */
	public static class BeanNameAwareMixin implements BeanNameAware {

		@SuppressWarnings("unused")
		private String beanName;

		@Override
		public void setBeanName(String name) {
			this.beanName = name;
		}

	}

	/** public visibility is required */
	@SuppressWarnings("serial")
	public static class SerializableMixin implements Serializable {
	}

}


class TopsyTurvyAspect {

	interface Collaborator {
		void beforeAdviceFired();
		void afterReturningAdviceFired();
		void aroundAdviceFired();
	}

	private Collaborator collaborator;

	public void setCollaborator(Collaborator collaborator) {
		this.collaborator = collaborator;
	}

	public void before() {
		this.collaborator.beforeAdviceFired();
	}

	public void afterReturning() {
		this.collaborator.afterReturningAdviceFired();
	}

	public Object around(ProceedingJoinPoint pjp) throws Throwable {
		Object ret = pjp.proceed();
		this.collaborator.aroundAdviceFired();
		return ret;
	}
}


interface TopsyTurvyTarget {

	void doSomething();

	int getX();
}


class TopsyTurvyTargetImpl implements TopsyTurvyTarget {

	private int x = 5;

	@Override
	public void doSomething() {
		this.x = 10;
	}

	@Override
	public int getX() {
		return x;
	}
}


class AspectCollaborator implements TopsyTurvyAspect.Collaborator {

	public boolean afterReturningFired = false;
	public boolean aroundFired = false;
	public boolean beforeFired = false;

	@Override
	public void afterReturningAdviceFired() {
		this.afterReturningFired = true;
	}

	@Override
	public void aroundAdviceFired() {
		this.aroundFired = true;
	}

	@Override
	public void beforeAdviceFired() {
		this.beforeFired = true;
	}
}
