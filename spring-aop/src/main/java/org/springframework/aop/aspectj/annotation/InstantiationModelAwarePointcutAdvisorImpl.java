/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import java.lang.reflect.Method;

import org.aopalliance.aop.Advice;
import org.aspectj.lang.reflect.PerClauseKind;

import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.AspectJPrecedenceInformation;
import org.springframework.aop.aspectj.InstantiationModelAwarePointcutAdvisor;
import org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactory.AspectJAnnotation;
import org.springframework.aop.support.DynamicMethodMatcherPointcut;
import org.springframework.aop.support.Pointcuts;

/**
 * Internal implementation of AspectJPointcutAdvisor.
 * Note that there will be one instance of this advisor for each target method.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
class InstantiationModelAwarePointcutAdvisorImpl
		implements InstantiationModelAwarePointcutAdvisor, AspectJPrecedenceInformation {

	private final AspectJExpressionPointcut declaredPointcut;

	private Pointcut pointcut;

	private final MetadataAwareAspectInstanceFactory aspectInstanceFactory;

	private final Method method;

	private final boolean lazy;

	private final AspectJAdvisorFactory atAspectJAdvisorFactory;

	private Advice instantiatedAdvice;

	private int declarationOrder;

	private String aspectName;

	private Boolean isBeforeAdvice;

	private Boolean isAfterAdvice;


	public InstantiationModelAwarePointcutAdvisorImpl(AspectJAdvisorFactory af,  AspectJExpressionPointcut ajexp,
			MetadataAwareAspectInstanceFactory aif,  Method method, int declarationOrderInAspect, String aspectName) {

		this.declaredPointcut = ajexp;
		this.method = method;
		this.atAspectJAdvisorFactory = af;
		this.aspectInstanceFactory = aif;
		this.declarationOrder = declarationOrderInAspect;
		this.aspectName = aspectName;

		if (aif.getAspectMetadata().isLazilyInstantiated()) {
			// Static part of the pointcut is a lazy type.
			Pointcut preInstantiationPointcut =
					Pointcuts.union(aif.getAspectMetadata().getPerClausePointcut(), this.declaredPointcut);

			// Make it dynamic: must mutate from pre-instantiation to post-instantiation state.
			// If it's not a dynamic pointcut, it may be optimized out
			// by the Spring AOP infrastructure after the first evaluation.
			this.pointcut = new PerTargetInstantiationModelPointcut(this.declaredPointcut, preInstantiationPointcut, aif);
			this.lazy = true;
		}
		else {
			// A singleton aspect.
			this.instantiatedAdvice = instantiateAdvice(this.declaredPointcut);
			this.pointcut = declaredPointcut;
			this.lazy = false;
		}
	}


	/**
	 * The pointcut for Spring AOP to use. Actual behaviour of the pointcut will change
	 * depending on the state of the advice.
	 */
	public Pointcut getPointcut() {
		return this.pointcut;
	}

	/**
	 * This is only of interest for Spring AOP: AspectJ instantiation semantics
	 * are much richer. In AspectJ terminology, all a return of <code>true</code>
	 * means here is that the aspect is not a SINGLETON.
	 */
	public boolean isPerInstance() {
		return (getAspectMetadata().getAjType().getPerClause().getKind() != PerClauseKind.SINGLETON);
	}

	/**
	 * Return the AspectJ AspectMetadata for this advisor.
	 */
	public AspectMetadata getAspectMetadata() {
		return this.aspectInstanceFactory.getAspectMetadata();
	}

	/**
	 * Lazily instantiate advice if necessary.
	 */
	public synchronized Advice getAdvice() {
		if (this.instantiatedAdvice == null) {
			this.instantiatedAdvice = instantiateAdvice(this.declaredPointcut);
		}
		return this.instantiatedAdvice;
	}

	public boolean isLazy() {
		return this.lazy;
	}

	public synchronized boolean isAdviceInstantiated() {
		return (this.instantiatedAdvice != null);
	}


	private Advice instantiateAdvice(AspectJExpressionPointcut pcut) {
		return this.atAspectJAdvisorFactory.getAdvice(
				this.method, pcut, this.aspectInstanceFactory, this.declarationOrder, this.aspectName);
	}

	public MetadataAwareAspectInstanceFactory getAspectInstanceFactory() {
		return this.aspectInstanceFactory;
	}

	public AspectJExpressionPointcut getDeclaredPointcut() {
		return this.declaredPointcut;
	}

	public int getOrder() {
		return this.aspectInstanceFactory.getOrder();
	}

	public String getAspectName() {
		return this.aspectName;
	}

	public int getDeclarationOrder() {
		return this.declarationOrder;
	}

	public boolean isBeforeAdvice() {
		if (this.isBeforeAdvice == null) {
			determineAdviceType();
		}
		return this.isBeforeAdvice;
	}

	public boolean isAfterAdvice() {
		if (this.isAfterAdvice == null) {
			determineAdviceType();
		}
		return this.isAfterAdvice;
	}

	/**
	 * Duplicates some logic from getAdvice, but importantly does not force
	 * creation of the advice.
	 */
	private void determineAdviceType() {
		AspectJAnnotation<?> aspectJAnnotation =
				AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(this.method);
		if (aspectJAnnotation == null) {
			this.isBeforeAdvice = false;
			this.isAfterAdvice = false;
		}
		else {
			switch (aspectJAnnotation.getAnnotationType()) {
				case AtAfter:
				case AtAfterReturning:
				case AtAfterThrowing:
					this.isAfterAdvice = true;
					this.isBeforeAdvice = false;
					break;
				case AtAround:
				case AtPointcut:
					this.isAfterAdvice = false;
					this.isBeforeAdvice = false;
					break;
				case AtBefore:
					this.isAfterAdvice = false;
					this.isBeforeAdvice = true;
			}
		}
	}


	@Override
	public String toString() {
		return "InstantiationModelAwarePointcutAdvisor: expression [" + getDeclaredPointcut().getExpression() +
			"]; advice method [" + this.method + "]; perClauseKind=" +
			this.aspectInstanceFactory.getAspectMetadata().getAjType().getPerClause().getKind();

	}


	/**
	 * Pointcut implementation that changes its behaviour when the advice is instantiated.
	 * Note that this is a <i>dynamic</i> pointcut. Otherwise it might
	 * be optimized out if it does not at first match statically.
	 */
	private class PerTargetInstantiationModelPointcut extends DynamicMethodMatcherPointcut {

		private final AspectJExpressionPointcut declaredPointcut;

		private final Pointcut preInstantiationPointcut;

		private LazySingletonAspectInstanceFactoryDecorator aspectInstanceFactory;

		private PerTargetInstantiationModelPointcut(AspectJExpressionPointcut declaredPointcut,
				Pointcut preInstantiationPointcut, MetadataAwareAspectInstanceFactory aspectInstanceFactory) {
			this.declaredPointcut = declaredPointcut;
			this.preInstantiationPointcut = preInstantiationPointcut;
			if (aspectInstanceFactory instanceof LazySingletonAspectInstanceFactoryDecorator) {
				this.aspectInstanceFactory = (LazySingletonAspectInstanceFactoryDecorator) aspectInstanceFactory;
			}
		}

		@Override
		public boolean matches(Method method, Class targetClass) {
			// We're either instantiated and matching on declared pointcut, or uninstantiated matching on either pointcut
			return (isAspectMaterialized() && this.declaredPointcut.matches(method, targetClass)) ||
					this.preInstantiationPointcut.getMethodMatcher().matches(method, targetClass);
		}

		public boolean matches(Method method, Class targetClass, Object[] args) {
			// This can match only on declared pointcut.
			return (isAspectMaterialized() && this.declaredPointcut.matches(method, targetClass));
		}

		private boolean isAspectMaterialized() {
			return (this.aspectInstanceFactory == null || this.aspectInstanceFactory.isMaterialized());
		}
	}

}
