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

package org.springframework.jmx.export.annotation;

import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.junit.Test;

import org.springframework.jmx.IJmxTestBean;
import org.springframework.jmx.export.assembler.AbstractMetadataAssemblerTests;
import org.springframework.jmx.export.metadata.JmxAttributeSource;

import static org.junit.Assert.*;

/**
 * @author Rob Harrop
 * @author Chris Beams
 */
public class AnnotationMetadataAssemblerTests extends AbstractMetadataAssemblerTests {

	private static final String OBJECT_NAME = "bean:name=testBean4";

	@Test
	public void testAttributeFromInterface() throws Exception {
		ModelMBeanInfo inf = getMBeanInfoFromAssembler();
		ModelMBeanAttributeInfo attr = inf.getAttribute("Colour");
		assertTrue("The name attribute should be writable", attr.isWritable());
		assertTrue("The name attribute should be readable", attr.isReadable());
	}

	@Test
	public void testOperationFromInterface() throws Exception {
		ModelMBeanInfo inf = getMBeanInfoFromAssembler();
		ModelMBeanOperationInfo op = inf.getOperation("fromInterface");
		assertNotNull(op);
	}

	@Test
	public void testOperationOnGetter() throws Exception {
		ModelMBeanInfo inf = getMBeanInfoFromAssembler();
		ModelMBeanOperationInfo op = inf.getOperation("getExpensiveToCalculate");
		assertNotNull(op);
	}

	@Test
	public void testRegistrationOnInterface() throws Exception {
		Object bean = getContext().getBean("testInterfaceBean");
		ModelMBeanInfo inf = getAssembler().getMBeanInfo(bean, "bean:name=interfaceTestBean");
		assertNotNull(inf);
		assertEquals("My Managed Bean", inf.getDescription());

		ModelMBeanOperationInfo op = inf.getOperation("foo");
		assertNotNull("foo operation not exposed", op);
		assertEquals("invoke foo", op.getDescription());

		assertNull("doNotExpose operation should not be exposed", inf.getOperation("doNotExpose"));

		ModelMBeanAttributeInfo attr = inf.getAttribute("Bar");
		assertNotNull("bar attribute not exposed", attr);
		assertEquals("Bar description", attr.getDescription());

		ModelMBeanAttributeInfo attr2 = inf.getAttribute("CacheEntries");
		assertNotNull("cacheEntries attribute not exposed", attr2);
		assertEquals("Metric Type should be COUNTER", "COUNTER",
				attr2.getDescriptor().getFieldValue("metricType"));
	}


	@Override
	protected JmxAttributeSource getAttributeSource() {
		return new AnnotationJmxAttributeSource();
	}

	@Override
	protected String getObjectName() {
		return OBJECT_NAME;
	}

	@Override
	protected IJmxTestBean createJmxTestBean() {
		return new AnnotationTestSubBean();
	}

	@Override
	protected String getApplicationContextPath() {
		return "org/springframework/jmx/export/annotation/annotations.xml";
	}

	@Override
	protected int getExpectedAttributeCount() {
		return super.getExpectedAttributeCount() + 1;
	}

	@Override
	protected int getExpectedOperationCount() {
		return super.getExpectedOperationCount() + 4;
	}
}
