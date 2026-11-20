/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj;

import org.aopalliance.aop.Advice;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.Advisor;
import org.springframework.aop.AfterAdvice;
import org.springframework.aop.BeforeAdvice;

/**
 * Utility methods for dealing with AspectJ advisors.
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract class AspectJAopUtils {

	/**
	 * Return {@code true} if the advisor is a form of before advice.
	 */
	public static boolean isBeforeAdvice(Advisor anAdvisor) {
		AspectJPrecedenceInformation precedenceInfo = getAspectJPrecedenceInformationFor(anAdvisor);
		if (precedenceInfo != null) {
			return precedenceInfo.isBeforeAdvice();
		}
		return (anAdvisor.getAdvice() instanceof BeforeAdvice);
	}

	/**
	 * Return {@code true} if the advisor is a form of after advice.
	 */
	public static boolean isAfterAdvice(Advisor anAdvisor) {
		AspectJPrecedenceInformation precedenceInfo = getAspectJPrecedenceInformationFor(anAdvisor);
		if (precedenceInfo != null) {
			return precedenceInfo.isAfterAdvice();
		}
		return (anAdvisor.getAdvice() instanceof AfterAdvice);
	}

	/**
	 * Return the AspectJPrecedenceInformation provided by this advisor or its advice.
	 * If neither the advisor nor the advice have precedence information, this method
	 * will return {@code null}.
	 */
	public static @Nullable AspectJPrecedenceInformation getAspectJPrecedenceInformationFor(Advisor anAdvisor) {
		if (anAdvisor instanceof AspectJPrecedenceInformation ajpi) {
			return ajpi;
		}
		Advice advice = anAdvisor.getAdvice();
		if (advice instanceof AspectJPrecedenceInformation ajpi) {
			return ajpi;
		}
		return null;
	}

}
