/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.testfixture.advice;

import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.aop.testfixture.interceptor.TimestampIntroductionInterceptor;

/**
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class TimestampIntroductionAdvisor extends DefaultIntroductionAdvisor {

	public TimestampIntroductionAdvisor() {
		super(new DelegatingIntroductionInterceptor(new TimestampIntroductionInterceptor()));
	}

}
