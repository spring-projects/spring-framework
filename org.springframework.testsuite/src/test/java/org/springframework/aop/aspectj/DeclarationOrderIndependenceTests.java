/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.aspectj;

import java.io.Serializable;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * @author Adrian Colyer
 */
public class DeclarationOrderIndependenceTests extends AbstractDependencyInjectionSpringContextTests {

	private TopsyTurvyAspect aspect;

	private TopsyTurvyTarget target;
	

	public DeclarationOrderIndependenceTests() {
		setAutowireMode(AUTOWIRE_BY_NAME);
	}
	
	public void setTopsyTurvyAspect(TopsyTurvyAspect aspect) {
		this.aspect = aspect;
	}

	public void setTopsyTurvyTarget(TopsyTurvyTarget target) {
		this.target = target;
	}

	protected String getConfigPath() {
		return "topsy-turvy-aspect.xml";
	}
	

	public void testTargetIsSerializable() {
		assertTrue("target bean is serializable",this.target instanceof Serializable);
	}
	
	public void testTargetIsBeanNameAware() {
		assertTrue("target bean is bean name aware",this.target instanceof BeanNameAware);
	}
	
	public void testBeforeAdviceFiringOk() {
		AspectCollaborator collab = new AspectCollaborator();
		this.aspect.setCollaborator(collab);
		this.target.doSomething();
		assertTrue("before advice fired",collab.beforeFired);
	}
	
	public void testAroundAdviceFiringOk() {
		AspectCollaborator collab = new AspectCollaborator();
		this.aspect.setCollaborator(collab);
		this.target.getX();
		assertTrue("around advice fired",collab.aroundFired);
	}
	
	public void testAfterReturningFiringOk() {
		AspectCollaborator collab = new AspectCollaborator();
		this.aspect.setCollaborator(collab);
		this.target.getX();
		assertTrue("after returning advice fired",collab.afterReturningFired);		
	}
	
	private static class AspectCollaborator implements TopsyTurvyAspect.Collaborator {

		public boolean afterReturningFired = false;
		public boolean aroundFired = false;
		public boolean beforeFired = false;
		
		/* (non-Javadoc)
		 * @see org.springframework.aop.aspectj.TopsyTurvyAspect.Collaborator#afterReturningAdviceFired()
		 */
		public void afterReturningAdviceFired() {
			this.afterReturningFired = true;
		}

		/* (non-Javadoc)
		 * @see org.springframework.aop.aspectj.TopsyTurvyAspect.Collaborator#aroundAdviceFired()
		 */
		public void aroundAdviceFired() {
			this.aroundFired = true;
		}

		/* (non-Javadoc)
		 * @see org.springframework.aop.aspectj.TopsyTurvyAspect.Collaborator#beforeAdviceFired()
		 */
		public void beforeAdviceFired() {
			this.beforeFired = true;
		}
		
	}
}


