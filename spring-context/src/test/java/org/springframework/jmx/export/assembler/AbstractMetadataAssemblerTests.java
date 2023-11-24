/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jmx.export.assembler;

import java.util.HashMap;
import java.util.Map;

import javax.management.Descriptor;
import javax.management.MBeanInfo;
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.testfixture.interceptor.NopInterceptor;
import org.springframework.jmx.IJmxTestBean;
import org.springframework.jmx.JmxTestBean;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.metadata.JmxAttributeSource;
import org.springframework.jmx.support.ObjectNameManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Chris Beams
 */
public abstract class AbstractMetadataAssemblerTests extends AbstractJmxAssemblerTests {

	protected static final String QUEUE_SIZE_METRIC = "QueueSize";

	protected static final String CACHE_ENTRIES_METRIC = "CacheEntries";

	@Test
	public void testDescription() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		assertThat(info.getDescription()).as("The descriptions are not the same").isEqualTo("My Managed Bean");
	}

	@Test
	public void testAttributeDescriptionOnSetter() throws Exception {
		ModelMBeanInfo inf = getMBeanInfoFromAssembler();
		ModelMBeanAttributeInfo attr = inf.getAttribute(AGE_ATTRIBUTE);
		assertThat(attr.getDescription()).as("The description for the age attribute is incorrect").isEqualTo("The Age Attribute");
	}

	@Test
	public void testAttributeDescriptionOnGetter() throws Exception {
		ModelMBeanInfo inf = getMBeanInfoFromAssembler();
		ModelMBeanAttributeInfo attr = inf.getAttribute(NAME_ATTRIBUTE);
		assertThat(attr.getDescription()).as("The description for the name attribute is incorrect").isEqualTo("The Name Attribute");
	}

	/**
	 * Tests the situation where the attribute is only defined on the getter.
	 */
	@Test
	public void testReadOnlyAttribute() throws Exception {
		ModelMBeanInfo inf = getMBeanInfoFromAssembler();
		ModelMBeanAttributeInfo attr = inf.getAttribute(AGE_ATTRIBUTE);
		assertThat(attr.isWritable()).as("The age attribute should not be writable").isFalse();
	}

	@Test
	public void testReadWriteAttribute() throws Exception {
		ModelMBeanInfo inf = getMBeanInfoFromAssembler();
		ModelMBeanAttributeInfo attr = inf.getAttribute(NAME_ATTRIBUTE);
		assertThat(attr.isWritable()).as("The name attribute should be writable").isTrue();
		assertThat(attr.isReadable()).as("The name attribute should be readable").isTrue();
	}

	/**
	 * Tests the situation where the property only has a getter.
	 */
	@Test
	public void testWithOnlySetter() throws Exception {
		ModelMBeanInfo inf = getMBeanInfoFromAssembler();
		ModelMBeanAttributeInfo attr = inf.getAttribute("NickName");
		assertThat(attr).as("Attribute should not be null").isNotNull();
	}

	/**
	 * Tests the situation where the property only has a setter.
	 */
	@Test
	public void testWithOnlyGetter() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		ModelMBeanAttributeInfo attr = info.getAttribute("Superman");
		assertThat(attr).as("Attribute should not be null").isNotNull();
	}

	@Test
	public void testManagedResourceDescriptor() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		Descriptor desc = info.getMBeanDescriptor();

		assertThat(desc.getFieldValue("log")).as("Logging should be set to true").isEqualTo("true");
		assertThat(desc.getFieldValue("logFile")).as("Log file should be build/jmx.log").isEqualTo("build/jmx.log");
		assertThat(desc.getFieldValue("currencyTimeLimit")).as("Currency Time Limit should be 15").isEqualTo("15");
		assertThat(desc.getFieldValue("persistPolicy")).as("Persist Policy should be OnUpdate").isEqualTo("OnUpdate");
		assertThat(desc.getFieldValue("persistPeriod")).as("Persist Period should be 200").isEqualTo("200");
		assertThat(desc.getFieldValue("persistLocation")).as("Persist Location should be foo").isEqualTo("./foo");
		assertThat(desc.getFieldValue("persistName")).as("Persist Name should be bar").isEqualTo("bar.jmx");
	}

	@Test
	public void testAttributeDescriptor() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		Descriptor desc = info.getAttribute(NAME_ATTRIBUTE).getDescriptor();

		assertThat(desc.getFieldValue("default")).as("Default value should be foo").isEqualTo("foo");
		assertThat(desc.getFieldValue("currencyTimeLimit")).as("Currency Time Limit should be 20").isEqualTo("20");
		assertThat(desc.getFieldValue("persistPolicy")).as("Persist Policy should be OnUpdate").isEqualTo("OnUpdate");
		assertThat(desc.getFieldValue("persistPeriod")).as("Persist Period should be 300").isEqualTo("300");
	}

	@Test
	public void testOperationDescriptor() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		Descriptor desc = info.getOperation("myOperation").getDescriptor();

		assertThat(desc.getFieldValue("currencyTimeLimit")).as("Currency Time Limit should be 30").isEqualTo("30");
		assertThat(desc.getFieldValue("role")).as("Role should be \"operation\"").isEqualTo("operation");
	}

	@Test
	public void testOperationParameterMetadata() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		ModelMBeanOperationInfo oper = info.getOperation("add");
		MBeanParameterInfo[] params = oper.getSignature();

		assertThat(params).as("Invalid number of params").hasSize(2);
		assertThat(params[0].getName()).as("Incorrect name for x param").isEqualTo("x");
		assertThat(params[0].getType()).as("Incorrect type for x param").isEqualTo(int.class.getName());

		assertThat(params[1].getName()).as("Incorrect name for y param").isEqualTo("y");
		assertThat(params[1].getType()).as("Incorrect type for y param").isEqualTo(int.class.getName());
	}

	@Test
	public void testWithCglibProxy() throws Exception {
		IJmxTestBean tb = createJmxTestBean();
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(tb);
		pf.addAdvice(new NopInterceptor());
		Object proxy = pf.getProxy();

		MetadataMBeanInfoAssembler assembler = (MetadataMBeanInfoAssembler) getAssembler();

		MBeanExporter exporter = new MBeanExporter();
		exporter.setBeanFactory(getContext());
		exporter.setAssembler(assembler);

		String objectName = "spring:bean=test,proxy=true";

		Map<String, Object> beans = new HashMap<>();
		beans.put(objectName, proxy);
		exporter.setBeans(beans);
		start(exporter);

		MBeanInfo inf = getServer().getMBeanInfo(ObjectNameManager.getInstance(objectName));
		assertThat(inf.getOperations()).as("Incorrect number of operations").hasSize(getExpectedOperationCount());
		assertThat(inf.getAttributes()).as("Incorrect number of attributes").hasSize(getExpectedAttributeCount());

		assertThat(assembler.includeBean(proxy.getClass(), "some bean name")).as("Not included in autodetection").isTrue();
	}

	@Test
	public void testMetricDescription() throws Exception {
		ModelMBeanInfo inf = getMBeanInfoFromAssembler();
		ModelMBeanAttributeInfo metric = inf.getAttribute(QUEUE_SIZE_METRIC);
		ModelMBeanOperationInfo operation = inf.getOperation("getQueueSize");
		assertThat(metric.getDescription()).as("The description for the queue size metric is incorrect").isEqualTo("The QueueSize metric");
		assertThat(operation.getDescription()).as("The description for the getter operation of the queue size metric is incorrect").isEqualTo("The QueueSize metric");
	}

	@Test
	public void testMetricDescriptor() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		Descriptor desc = info.getAttribute(QUEUE_SIZE_METRIC).getDescriptor();
		assertThat(desc.getFieldValue("currencyTimeLimit")).as("Currency Time Limit should be 20").isEqualTo("20");
		assertThat(desc.getFieldValue("persistPolicy")).as("Persist Policy should be OnUpdate").isEqualTo("OnUpdate");
		assertThat(desc.getFieldValue("persistPeriod")).as("Persist Period should be 300").isEqualTo("300");
		assertThat(desc.getFieldValue("units")).as("Unit should be messages").isEqualTo("messages");
		assertThat(desc.getFieldValue("displayName")).as("Display Name should be Queue Size").isEqualTo("Queue Size");
		assertThat(desc.getFieldValue("metricType")).as("Metric Type should be COUNTER").isEqualTo("COUNTER");
		assertThat(desc.getFieldValue("metricCategory")).as("Metric Category should be utilization").isEqualTo("utilization");
	}

	@Test
	public void testMetricDescriptorDefaults() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		Descriptor desc = info.getAttribute(CACHE_ENTRIES_METRIC).getDescriptor();
		assertThat(desc.getFieldValue("currencyTimeLimit")).as("Currency Time Limit should not be populated").isNull();
		assertThat(desc.getFieldValue("persistPolicy")).as("Persist Policy should not be populated").isNull();
		assertThat(desc.getFieldValue("persistPeriod")).as("Persist Period should not be populated").isNull();
		assertThat(desc.getFieldValue("units")).as("Unit should not be populated").isNull();
		assertThat(desc.getFieldValue("displayName")).as("Display Name should be populated by default via JMX").isEqualTo(CACHE_ENTRIES_METRIC);
		assertThat(desc.getFieldValue("metricType")).as("Metric Type should be GAUGE").isEqualTo("GAUGE");
		assertThat(desc.getFieldValue("metricCategory")).as("Metric Category should not be populated").isNull();
	}

	@Override
	protected abstract String getObjectName();

	@Override
	protected int getExpectedAttributeCount() {
		return 6;
	}

	@Override
	protected int getExpectedOperationCount() {
		return 9;
	}

	protected IJmxTestBean createJmxTestBean() {
		return new JmxTestBean();
	}

	@Override
	protected MBeanInfoAssembler getAssembler() {
		MetadataMBeanInfoAssembler assembler = new MetadataMBeanInfoAssembler();
		assembler.setAttributeSource(getAttributeSource());
		return assembler;
	}

	protected abstract JmxAttributeSource getAttributeSource();

}
