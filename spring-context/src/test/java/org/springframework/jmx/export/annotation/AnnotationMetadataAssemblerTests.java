/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.jmx.export.annotation;

import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.junit.jupiter.api.Test;

import org.springframework.jmx.IJmxTestBean;
import org.springframework.jmx.export.assembler.AbstractMetadataAssemblerTests;
import org.springframework.jmx.export.metadata.JmxAttributeSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Chris Beams
 */
class AnnotationMetadataAssemblerTests extends AbstractMetadataAssemblerTests {

	private static final String OBJECT_NAME = "bean:name=testBean4";


	@Test
	void testAttributeFromInterface() throws Exception {
		ModelMBeanInfo inf = getMBeanInfoFromAssembler();
		ModelMBeanAttributeInfo attr = inf.getAttribute("Colour");
		assertThat(attr.isWritable()).as("The name attribute should be writable").isTrue();
		assertThat(attr.isReadable()).as("The name attribute should be readable").isTrue();
	}

	@Test
	void testOperationFromInterface() throws Exception {
		ModelMBeanInfo inf = getMBeanInfoFromAssembler();
		ModelMBeanOperationInfo op = inf.getOperation("fromInterface");
		assertThat(op).isNotNull();
	}

	@Test
	void testOperationOnGetter() throws Exception {
		ModelMBeanInfo inf = getMBeanInfoFromAssembler();
		ModelMBeanOperationInfo op = inf.getOperation("getExpensiveToCalculate");
		assertThat(op).isNotNull();
	}

	@Test
	void testRegistrationOnInterface() throws Exception {
		Object bean = getContext().getBean("testInterfaceBean");
		ModelMBeanInfo inf = getAssembler().getMBeanInfo(bean, "bean:name=interfaceTestBean");
		assertThat(inf).isNotNull();
		assertThat(inf.getDescription()).isEqualTo("My Managed Bean");

		ModelMBeanOperationInfo op = inf.getOperation("foo");
		assertThat(op).as("foo operation not exposed").isNotNull();
		assertThat(op.getDescription()).isEqualTo("invoke foo");

		assertThat(inf.getOperation("doNotExpose")).as("doNotExpose operation should not be exposed").isNull();

		ModelMBeanAttributeInfo attr = inf.getAttribute("Bar");
		assertThat(attr).as("bar attribute not exposed").isNotNull();
		assertThat(attr.getDescription()).isEqualTo("Bar description");

		ModelMBeanAttributeInfo attr2 = inf.getAttribute("CacheEntries");
		assertThat(attr2).as("cacheEntries attribute not exposed").isNotNull();
		assertThat(attr2.getDescriptor().getFieldValue("metricType")).as("Metric Type should be COUNTER").isEqualTo("COUNTER");
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
