/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.jmx.export.assembler;

import java.lang.reflect.Method;
import java.util.Properties;

import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;

import org.springframework.jmx.JmxTestBean;

/**
 * @author Rob Harrop
 * @author Rick Evans
 */
public class MethodExclusionMBeanInfoAssemblerTests extends AbstractJmxAssemblerTests {

	private static final String OBJECT_NAME = "bean:name=testBean5";


	protected String getObjectName() {
		return OBJECT_NAME;
	}

	protected int getExpectedOperationCount() {
		return 9;
	}

	protected int getExpectedAttributeCount() {
		return 4;
	}

	protected String getApplicationContextPath() {
		return "org/springframework/jmx/export/assembler/methodExclusionAssembler.xml";
	}

	protected MBeanInfoAssembler getAssembler() {
		MethodExclusionMBeanInfoAssembler assembler = new MethodExclusionMBeanInfoAssembler();
		assembler.setIgnoredMethods(new String[] {"dontExposeMe", "setSuperman"});
		return assembler;
	}

	public void testSupermanIsReadOnly() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		ModelMBeanAttributeInfo attr = info.getAttribute("Superman");

		assertTrue(attr.isReadable());
		assertFalse(attr.isWritable());
	}

	/*
	 * http://opensource.atlassian.com/projects/spring/browse/SPR-2754
	 */
	public void testIsNotIgnoredDoesntIgnoreUnspecifiedBeanMethods() throws Exception {
		final String beanKey = "myTestBean";
		MethodExclusionMBeanInfoAssembler assembler = new MethodExclusionMBeanInfoAssembler();
		Properties ignored = new Properties();
		ignored.setProperty(beanKey, "dontExposeMe,setSuperman");
		assembler.setIgnoredMethodMappings(ignored);
		Method method = JmxTestBean.class.getMethod("dontExposeMe");
		assertFalse(assembler.isNotIgnored(method, beanKey));
		// this bean does not have any ignored methods on it, so must obviously not be ignored...
		assertTrue(assembler.isNotIgnored(method, "someOtherBeanKey"));
	}

}
