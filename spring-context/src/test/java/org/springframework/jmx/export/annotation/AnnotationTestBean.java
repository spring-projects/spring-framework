/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.annotation;

import org.springframework.jmx.ITestBean;
import org.springframework.jmx.support.MetricType;
import org.springframework.stereotype.Service;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
@Service("testBean")
@ManagedResource(value = "bean:name=testBean4", description = "My Managed Bean", log = true,
		logFile = "build/jmx.log", currencyTimeLimit = 15, persistPolicy = "OnUpdate", persistPeriod = 200,
		persistLocation = "./foo", persistName = "bar.jmx")
@ManagedNotification(name = "My Notification 1", notificationTypes = { "type.foo", "type.bar" })
@ManagedNotification(name = "My Notification 2", notificationTypes = "type.enigma")
public class AnnotationTestBean implements ITestBean {

	private String name;

	private String nickName;

	private int age;

	private boolean isSuperman;


	public void setAge(int age) {
		this.age = age;
	}

	@ManagedAttribute(description = "The Age Attribute", currencyTimeLimit = 15)
	public int getAge() {
		return this.age;
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
		return this.name;
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

	@ManagedOperation(currencyTimeLimit = 30)
	public long myOperation() {
		return 1L;
	}

	@ManagedOperation(description = "Add Two Numbers Together")
	@ManagedOperationParameter(name="x", description="Left operand")
	@ManagedOperationParameter(name="y", description="Right operand")
	public int add(int x, int y) {
		return x + y;
	}

	/**
	 * Method that is not exposed by the MetadataAssembler.
	 */
	public void dontExposeMe() {
		throw new RuntimeException();
	}

	@ManagedMetric(description="The QueueSize metric", currencyTimeLimit = 20, persistPolicy="OnUpdate", persistPeriod=300,
			category="utilization", metricType = MetricType.COUNTER, displayName="Queue Size", unit="messages")
	public long getQueueSize() {
		return 100L;
	}

	@ManagedMetric
	public int getCacheEntries() {
		return 3;
	}

}
