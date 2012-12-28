/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.scheduling.quartz;

import java.util.Date;
import java.util.Calendar;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** @author Rob Harrop */
public class CronTriggerBeanTests {

	@Test(expected = IllegalStateException.class)
	public void testInvalidStartDelay() {
		createTriggerBean().setStartDelay(-1);
	}

	@Test
	public void testStartTime() throws Exception {
		CronTriggerBean bean = createTriggerBean();
		Date startTime = new Date(System.currentTimeMillis() + 1234L);
		bean.setStartTime(startTime);
		bean.afterPropertiesSet();
		assertTimeEquals(startTime, bean.getStartTime());
	}

	@Test
	public void testStartDelay() throws Exception {
		CronTriggerBean bean = createTriggerBean();
		long startDelay = 1234L;
		Date startTime = new Date(System.currentTimeMillis() + startDelay);
		bean.setStartDelay(startDelay);
		bean.afterPropertiesSet();
		assertTimeEquals(startTime, bean.getStartTime());
	}

	@Test
	public void testStartDelayOverridesStartTime() throws Exception {
		CronTriggerBean bean = createTriggerBean();
		long startDelay = 1234L;
		Date startTime = new Date(System.currentTimeMillis() + startDelay);
		bean.setStartTime(new Date(System.currentTimeMillis() + 123456789L));
		bean.setStartDelay(startDelay);
		bean.afterPropertiesSet();
		assertTimeEquals(startTime, bean.getStartTime());
	}

	private CronTriggerBean createTriggerBean() {
		CronTriggerBean triggerBean = new CronTriggerBean();
		triggerBean.setName("test");
		return triggerBean;
	}

	private void assertTimeEquals(Date a, Date b) {
		Calendar ca = Calendar.getInstance();
		ca.setTime(a);
		ca.set(Calendar.MILLISECOND, 0);
		Calendar cb = Calendar.getInstance();
		cb.setTime(b);
		cb.set(Calendar.MILLISECOND, 0);
		assertEquals(ca, cb);
	}

}
