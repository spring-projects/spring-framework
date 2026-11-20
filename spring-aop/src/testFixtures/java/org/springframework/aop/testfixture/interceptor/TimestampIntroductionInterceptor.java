/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.testfixture.interceptor;

import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.core.testfixture.TimeStamped;

@SuppressWarnings("serial")
public class TimestampIntroductionInterceptor extends DelegatingIntroductionInterceptor
		implements TimeStamped {

	private long ts;

	public TimestampIntroductionInterceptor() {
	}

	public TimestampIntroductionInterceptor(long ts) {
		this.ts = ts;
	}

	public void setTime(long ts) {
		this.ts = ts;
	}

	@Override
	public long getTimeStamp() {
		return ts;
	}

}
