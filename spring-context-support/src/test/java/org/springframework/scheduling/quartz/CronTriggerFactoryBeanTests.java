/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.scheduling.quartz;

import java.text.ParseException;

import org.junit.Test;
import org.quartz.CronTrigger;

import static org.junit.Assert.*;

/**
 * @author Stephane Nicoll
 */
public class CronTriggerFactoryBeanTests {

	@Test
	public void createWithoutJobDetail() throws ParseException {
		CronTriggerFactoryBean factory = new CronTriggerFactoryBean();
		factory.setName("myTrigger");
		factory.setCronExpression("0 15 10 ? * *");
		factory.afterPropertiesSet();
		CronTrigger trigger = factory.getObject();
		assertEquals("0 15 10 ? * *", trigger.getCronExpression());
	}

}
