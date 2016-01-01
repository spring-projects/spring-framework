/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.jmx.export.annotation;

import org.springframework.jmx.IJmxTestBean;
import org.springframework.jmx.support.MetricType;
import org.springframework.stereotype.Service;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
@Service("testBean")
@ManagedResource(objectName = "bean:name=testBean4", description = "My Managed Bean", log = true,
		logFile = "jmx.log", currencyTimeLimit = 15, persistPolicy = "OnUpdate", persistPeriod = 200,
		persistLocation = "./foo", persistName = "bar.jmx")
@ManagedNotification(name="My Notification", notificationTypes={"type.foo", "type.bar"})
public class AnnotationTestBean implements IJmxTestBean {

	private String name;

	private String nickName;

	private int age;

	private boolean isSuperman;


	@Override
	@ManagedAttribute(description = "The Age Attribute", currencyTimeLimit = 15)
	public int getAge() {
		return age;
	}

	@Override
	public void setAge(int age) {
		this.age = age;
	}

	@Override
	@ManagedOperation(currencyTimeLimit = 30)
	public long myOperation() {
		return 1L;
	}

	@Override
	@ManagedAttribute(description = "The Name Attribute",
			currencyTimeLimit = 20,
			defaultValue = "bar",
			persistPolicy = "OnUpdate")
	public void setName(String name) {
		this.name = name;
	}

	@Override
	@ManagedAttribute(defaultValue = "foo", persistPeriod = 300)
	public String getName() {
		return name;
	}

	@ManagedAttribute(description = "The Nick Name Attribute")
	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public String getNickName() {
		return this.nickName;
	}

	public void setSuperman(boolean superman) {
		this.isSuperman = superman;
	}

	@ManagedAttribute(description = "The Is Superman Attribute")
	public boolean isSuperman() {
		return isSuperman;
	}

	@Override
	@ManagedOperation(description = "Add Two Numbers Together")
	@ManagedOperationParameter(name="x", description="Left operand")
	@ManagedOperationParameter(name="y", description="Right operand")
	public int add(int x, int y) {
		return x + y;
	}

	/**
	 * Test method that is not exposed by the MetadataAssembler.
	 */
	@Override
	public void dontExposeMe() {
		throw new RuntimeException();
	}

	@ManagedMetric(description="The QueueSize metric", currencyTimeLimit = 20, persistPolicy="OnUpdate", persistPeriod=300,
			category="utilization", metricType = MetricType.COUNTER, displayName="Queue Size", unit="messages")
	public long getQueueSize() {
		return 100l;
	}

	@ManagedMetric
	public int getCacheEntries() {
		return 3;
	}

}
