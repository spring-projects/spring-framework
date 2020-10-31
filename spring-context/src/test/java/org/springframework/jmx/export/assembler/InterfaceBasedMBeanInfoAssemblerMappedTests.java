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

package org.springframework.jmx.export.assembler;

import java.util.Properties;

import javax.management.MBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rob Harrop
 * @author Chris Beams
 */
public class InterfaceBasedMBeanInfoAssemblerMappedTests extends AbstractJmxAssemblerTests {

	protected static final String OBJECT_NAME = "bean:name=testBean4";

	@Test
	public void testGetAgeIsReadOnly() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		ModelMBeanAttributeInfo attr = info.getAttribute(AGE_ATTRIBUTE);

		assertThat(attr.isReadable()).as("Age is not readable").isTrue();
		assertThat(attr.isWritable()).as("Age is not writable").isFalse();
	}

	@Test
	public void testWithUnknownClass() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				getWithMapping("com.foo.bar.Unknown"));
	}

	@Test
	public void testWithNonInterface() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				getWithMapping("JmxTestBean"));
	}

	@Test
	public void testWithFallThrough() throws Exception {
		InterfaceBasedMBeanInfoAssembler assembler =
				getWithMapping("foobar", "org.springframework.jmx.export.assembler.ICustomJmxBean");
		assembler.setManagedInterfaces(new Class<?>[] {IAdditionalTestMethods.class});

		ModelMBeanInfo inf = assembler.getMBeanInfo(getBean(), getObjectName());
		MBeanAttributeInfo attr = inf.getAttribute("NickName");

		assertNickName(attr);
	}

	@Test
	public void testNickNameIsExposed() throws Exception {
		ModelMBeanInfo inf = (ModelMBeanInfo) getMBeanInfo();
		MBeanAttributeInfo attr = inf.getAttribute("NickName");

		assertNickName(attr);
	}

	@Override
	protected String getObjectName() {
		return OBJECT_NAME;
	}

	@Override
	protected int getExpectedOperationCount() {
		return 7;
	}

	@Override
	protected int getExpectedAttributeCount() {
		return 3;
	}

	@Override
	protected MBeanInfoAssembler getAssembler() throws Exception {
		return getWithMapping(
				"org.springframework.jmx.export.assembler.IAdditionalTestMethods, " +
				"org.springframework.jmx.export.assembler.ICustomJmxBean");
	}

	@Override
	protected String getApplicationContextPath() {
		return "org/springframework/jmx/export/assembler/interfaceAssemblerMapped.xml";
	}

	private InterfaceBasedMBeanInfoAssembler getWithMapping(String mapping) {
		return getWithMapping(OBJECT_NAME, mapping);
	}

	private InterfaceBasedMBeanInfoAssembler getWithMapping(String name, String mapping) {
		InterfaceBasedMBeanInfoAssembler assembler = new InterfaceBasedMBeanInfoAssembler();
		Properties props = new Properties();
		props.setProperty(name, mapping);
		assembler.setInterfaceMappings(props);
		assembler.afterPropertiesSet();
		return assembler;
	}

	private void assertNickName(MBeanAttributeInfo attr) {
		assertThat(attr).as("Nick Name should not be null").isNotNull();
		assertThat(attr.isWritable()).as("Nick Name should be writable").isTrue();
		assertThat(attr.isReadable()).as("Nick Name should be readable").isTrue();
	}

}
