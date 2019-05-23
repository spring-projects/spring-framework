/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.support;

import java.lang.management.ManagementFactory;
import java.util.Set;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LiveBeansView}
 *
 * @author Stephane Nicoll
 */
public class LiveBeansViewTests {

	@Rule
	public TestName name = new TestName();

	private final MockEnvironment environment = new MockEnvironment();

	@Test
	public void registerIgnoredIfPropertyIsNotSet() throws MalformedObjectNameException {
		ConfigurableApplicationContext context = createApplicationContext("app");
		assertThat(searchLiveBeansViewMeans().size()).isEqualTo(0);
		LiveBeansView.registerApplicationContext(context);
		assertThat(searchLiveBeansViewMeans().size()).isEqualTo(0);
		LiveBeansView.unregisterApplicationContext(context);
	}

	@Test
	public void registerUnregisterSingleContext() throws MalformedObjectNameException {
		this.environment.setProperty(LiveBeansView.MBEAN_DOMAIN_PROPERTY_NAME, this.name.getMethodName());
		ConfigurableApplicationContext context = createApplicationContext("app");
		assertThat(searchLiveBeansViewMeans().size()).isEqualTo(0);
		LiveBeansView.registerApplicationContext(context);
		assertSingleLiveBeansViewMbean("app");
		LiveBeansView.unregisterApplicationContext(context);
		assertThat(searchLiveBeansViewMeans().size()).isEqualTo(0);
	}

	@Test
	public void registerUnregisterSeveralContexts() throws MalformedObjectNameException {
		this.environment.setProperty(LiveBeansView.MBEAN_DOMAIN_PROPERTY_NAME, this.name.getMethodName());
		ConfigurableApplicationContext context = createApplicationContext("app");
		ConfigurableApplicationContext childContext = createApplicationContext("child");
		assertThat(searchLiveBeansViewMeans().size()).isEqualTo(0);
		LiveBeansView.registerApplicationContext(context);
		assertSingleLiveBeansViewMbean("app");
		LiveBeansView.registerApplicationContext(childContext);
		// Only one MBean
		assertThat(searchLiveBeansViewMeans().size()).isEqualTo(1);
		LiveBeansView.unregisterApplicationContext(childContext);
		assertSingleLiveBeansViewMbean("app"); // Root context removes it
		LiveBeansView.unregisterApplicationContext(context);
		assertThat(searchLiveBeansViewMeans().size()).isEqualTo(0);
	}

	@Test
	public void registerUnregisterSeveralContextsDifferentOrder() throws MalformedObjectNameException {
		this.environment.setProperty(LiveBeansView.MBEAN_DOMAIN_PROPERTY_NAME, this.name.getMethodName());
		ConfigurableApplicationContext context = createApplicationContext("app");
		ConfigurableApplicationContext childContext = createApplicationContext("child");
		assertThat(searchLiveBeansViewMeans().size()).isEqualTo(0);
		LiveBeansView.registerApplicationContext(context);
		assertSingleLiveBeansViewMbean("app");
		LiveBeansView.registerApplicationContext(childContext);
		assertSingleLiveBeansViewMbean("app"); // Only one MBean
		LiveBeansView.unregisterApplicationContext(context);
		LiveBeansView.unregisterApplicationContext(childContext);
		assertThat(searchLiveBeansViewMeans().size()).isEqualTo(0);
	}

	private ConfigurableApplicationContext createApplicationContext(String applicationName) {
		ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
		given(context.getEnvironment()).willReturn(this.environment);
		given(context.getApplicationName()).willReturn(applicationName);
		return context;
	}

	public void assertSingleLiveBeansViewMbean(String applicationName) throws MalformedObjectNameException {
		Set<ObjectName> objectNames = searchLiveBeansViewMeans();
		assertThat(objectNames.size()).isEqualTo(1);
		assertThat(objectNames.iterator().next().getCanonicalName()).as("Wrong MBean name").isEqualTo(String.format("%s:application=%s", this.name.getMethodName(), applicationName));

	}

	private Set<ObjectName> searchLiveBeansViewMeans()
			throws MalformedObjectNameException {
		String objectName = String.format("%s:*,%s=*", this.name.getMethodName(),
				LiveBeansView.MBEAN_APPLICATION_KEY);
		return ManagementFactory.getPlatformMBeanServer().queryNames(new ObjectName(objectName), null);
	}

}
