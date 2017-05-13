/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.context.support;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
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
		assertEquals(0, searchLiveBeansViewMeans().size());
		LiveBeansView.registerApplicationContext(context);
		assertEquals(0, searchLiveBeansViewMeans().size());
		LiveBeansView.unregisterApplicationContext(context);
	}

	@Test
	public void registerUnregisterSingleContext() throws MalformedObjectNameException {
		this.environment.setProperty(LiveBeansView.MBEAN_DOMAIN_PROPERTY_NAME, this.name.getMethodName());
		ConfigurableApplicationContext context = createApplicationContext("app");
		assertEquals(0, searchLiveBeansViewMeans().size());
		LiveBeansView.registerApplicationContext(context);
		assertSingleLiveBeansViewMbean("app");
		LiveBeansView.unregisterApplicationContext(context);
		assertEquals(0, searchLiveBeansViewMeans().size());
	}

	@Test
	public void registerUnregisterServeralContexts() throws MalformedObjectNameException {
		this.environment.setProperty(LiveBeansView.MBEAN_DOMAIN_PROPERTY_NAME, this.name.getMethodName());
		ConfigurableApplicationContext context = createApplicationContext("app");
		ConfigurableApplicationContext childContext = createApplicationContext("child");
		assertEquals(0, searchLiveBeansViewMeans().size());
		LiveBeansView.registerApplicationContext(context);
		assertSingleLiveBeansViewMbean("app");
		LiveBeansView.registerApplicationContext(childContext);
		assertEquals(1, searchLiveBeansViewMeans().size()); // Only one MBean
		LiveBeansView.unregisterApplicationContext(childContext);
		assertSingleLiveBeansViewMbean("app"); // Root context removes it
		LiveBeansView.unregisterApplicationContext(context);
		assertEquals(0, searchLiveBeansViewMeans().size());
	}

	@Test
	public void registerUnregisterServeralContextsDifferentOrder() throws MalformedObjectNameException {
		this.environment.setProperty(LiveBeansView.MBEAN_DOMAIN_PROPERTY_NAME, this.name.getMethodName());
		ConfigurableApplicationContext context = createApplicationContext("app");
		ConfigurableApplicationContext childContext = createApplicationContext("child");
		assertEquals(0, searchLiveBeansViewMeans().size());
		LiveBeansView.registerApplicationContext(context);
		assertSingleLiveBeansViewMbean("app");
		LiveBeansView.registerApplicationContext(childContext);
		assertSingleLiveBeansViewMbean("app"); // Only one MBean
		LiveBeansView.unregisterApplicationContext(context);
		LiveBeansView.unregisterApplicationContext(childContext);
		assertEquals(0, searchLiveBeansViewMeans().size());
	}

	@Test
	public void orderOfApplicationContextsShouldBeRootFirst() {
		ConfigurableApplicationContext root = createApplicationContext("root");
		ConfigurableApplicationContext childCtx1 = createApplicationContext("ctx1");
		ConfigurableApplicationContext childCtx11 = createApplicationContext("ctx11");
		ConfigurableApplicationContext childCtx12 = createApplicationContext("ctx12");
		ConfigurableApplicationContext childCtx123 = createApplicationContext("ctx123");
		ConfigurableApplicationContext childCtx2 = createApplicationContext("ctx2");
		ConfigurableApplicationContext childCtx21 = createApplicationContext("ctx21");
		ConfigurableApplicationContext childCtx22 = createApplicationContext("ctx21");
		ConfigurableApplicationContext childCtx223 = createApplicationContext("ctx21");
		setParent(childCtx1, root);
		setParent(childCtx2, root);
		setParent(childCtx11, childCtx1);
		setParent(childCtx12, childCtx1);
		setParent(childCtx123, childCtx12);
		setParent(childCtx21, childCtx2);
		setParent(childCtx22, childCtx2);
		setParent(childCtx223, childCtx22);


		Set<ConfigurableApplicationContext> contextsSet = new HashSet<>();
		contextsSet.add(childCtx1);
		contextsSet.add(childCtx2);
		contextsSet.add(root);
		contextsSet.add(childCtx11);
		contextsSet.add(childCtx12);
		contextsSet.add(childCtx123);
		contextsSet.add(childCtx21);
		contextsSet.add(childCtx22);
		contextsSet.add(childCtx223);


		LiveBeansView liveBeansView = new LiveBeansView();
		List<ConfigurableApplicationContext> orderedCtx = liveBeansView.orderedApplicationContexts(contextsSet);
		assertEquals(root, orderedCtx.get(0));
		assertTrue((orderedCtx.indexOf(childCtx1) < orderedCtx.indexOf(childCtx11)) &&
				(orderedCtx.indexOf(childCtx1) < orderedCtx.indexOf(childCtx12)));

		assertTrue((orderedCtx.indexOf(childCtx2) < orderedCtx.indexOf(childCtx21)) &&
				(orderedCtx.indexOf(childCtx2) < orderedCtx.indexOf(childCtx22)));

		assertTrue((orderedCtx.indexOf(childCtx12) < orderedCtx.indexOf(childCtx123)));
		assertTrue((orderedCtx.indexOf(childCtx22) < orderedCtx.indexOf(childCtx223)));
	}


	private ConfigurableApplicationContext createApplicationContext(String applicationName) {
		ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
		given(context.getEnvironment()).willReturn(this.environment);
		given(context.getApplicationName()).willReturn(applicationName);
		return context;
	}

	private void setParent(ConfigurableApplicationContext child, ConfigurableApplicationContext parent) {
		given(child.getParent()).willReturn(parent);
	}

	public void assertSingleLiveBeansViewMbean(String applicationName) throws MalformedObjectNameException {
		Set<ObjectName> objectNames = searchLiveBeansViewMeans();
		assertEquals(1, objectNames.size());
		assertEquals("Wrong MBean name",
				String.format("%s:application=%s", this.name.getMethodName(), applicationName),
				objectNames.iterator().next().getCanonicalName());

	}

	private Set<ObjectName> searchLiveBeansViewMeans()
			throws MalformedObjectNameException {
		String objectName = String.format("%s:*,%s=*", this.name.getMethodName(),
				LiveBeansView.MBEAN_APPLICATION_KEY);
		return ManagementFactory.getPlatformMBeanServer().queryNames(new ObjectName(objectName), null);
	}

}