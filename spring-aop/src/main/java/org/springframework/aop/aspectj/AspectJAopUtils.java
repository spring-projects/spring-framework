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

import org.aopalliance.aop.Advice;

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
	 * Return <code>true</code> if the advisor is a form of before advice.
	 */
	public static boolean isBeforeAdvice(Advisor anAdvisor) {
		AspectJPrecedenceInformation precedenceInfo = getAspectJPrecedenceInformationFor(anAdvisor);
		if (precedenceInfo != null) {
			return precedenceInfo.isBeforeAdvice();
		}
		return (anAdvisor.getAdvice() instanceof BeforeAdvice);
	}

	/**
	 * Return <code>true</code> if the advisor is a form of after advice.
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
	 * will return <code>null</code>.
	 */
	public static AspectJPrecedenceInformation getAspectJPrecedenceInformationFor(Advisor anAdvisor) {
		if (anAdvisor instanceof AspectJPrecedenceInformation) {
			return (AspectJPrecedenceInformation) anAdvisor;
		}
		Advice advice = anAdvisor.getAdvice();
		if (advice instanceof AspectJPrecedenceInformation) {
			return (AspectJPrecedenceInformation) advice;
		}
		return null;
	}

}
