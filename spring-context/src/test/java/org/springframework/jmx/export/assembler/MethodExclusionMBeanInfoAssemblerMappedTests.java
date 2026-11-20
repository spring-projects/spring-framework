/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.assembler;

import java.util.Properties;

import javax.management.MBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Chris Beams
 */
class MethodExclusionMBeanInfoAssemblerMappedTests extends AbstractJmxAssemblerTests {

	protected static final String OBJECT_NAME = "bean:name=testBean4";

	@Test
	void getAgeIsReadOnly() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		ModelMBeanAttributeInfo attr = info.getAttribute(AGE_ATTRIBUTE);
		assertThat(attr.isReadable()).as("Age is not readable").isTrue();
		assertThat(attr.isWritable()).as("Age is not writable").isFalse();
	}

	@Test
	void nickNameIsExposed() throws Exception {
		ModelMBeanInfo inf = (ModelMBeanInfo) getMBeanInfo();
		MBeanAttributeInfo attr = inf.getAttribute("NickName");
		assertThat(attr).as("Nick Name should not be null").isNotNull();
		assertThat(attr.isWritable()).as("Nick Name should be writable").isTrue();
		assertThat(attr.isReadable()).as("Nick Name should be readable").isTrue();
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
	protected String getApplicationContextPath() {
		return "org/springframework/jmx/export/assembler/methodExclusionAssemblerMapped.xml";
	}

	@Override
	protected MBeanInfoAssembler getAssembler() {
		MethodExclusionMBeanInfoAssembler assembler = new MethodExclusionMBeanInfoAssembler();
		Properties props = new Properties();
		props.setProperty(OBJECT_NAME, "setAge,isSuperman,setSuperman,dontExposeMe");
		assembler.setIgnoredMethodMappings(props);
		return assembler;
	}

}
