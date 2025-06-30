/*
 * Copyright 2002-present the original author or authors.
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
