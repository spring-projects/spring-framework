/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj;

import org.aspectj.lang.JoinPoint;

/**
 * Aspect used as part of before advice binding tests and
 * serves as base class for a number of more specialized test aspects.
 *
 * @author Adrian Colyer
 * @author Chris Beams
 */
class AdviceBindingTestAspect {

	protected AdviceBindingCollaborator collaborator;


	public void setCollaborator(AdviceBindingCollaborator aCollaborator) {
		this.collaborator = aCollaborator;
	}


	// "advice" methods

	public void oneIntArg(int age) {
		this.collaborator.oneIntArg(age);
	}

	public void oneObjectArg(Object bean) {
		this.collaborator.oneObjectArg(bean);
	}

	public void oneIntAndOneObject(int x, Object o) {
		this.collaborator.oneIntAndOneObject(x,o);
	}

	public void needsJoinPoint(JoinPoint tjp) {
		this.collaborator.needsJoinPoint(tjp.getSignature().getName());
	}

	public void needsJoinPointStaticPart(JoinPoint.StaticPart tjpsp) {
		this.collaborator.needsJoinPointStaticPart(tjpsp.getSignature().getName());
	}


	/**
	 * Collaborator interface that makes it easy to test this aspect is
	 * working as expected through mocking.
	 */
	public interface AdviceBindingCollaborator {

		void oneIntArg(int x);

		void oneObjectArg(Object o);

		void oneIntAndOneObject(int x, Object o);

		void needsJoinPoint(String s);

		void needsJoinPointStaticPart(String s);
	}

}
