/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.assembler;

import java.lang.reflect.Method;
import java.util.Properties;

import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;

import org.junit.jupiter.api.Test;

import org.springframework.jmx.JmxTestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Chris Beams
 */
class MethodExclusionMBeanInfoAssemblerTests extends AbstractJmxAssemblerTests {

	private static final String OBJECT_NAME = "bean:name=testBean5";


	@Override
	protected String getObjectName() {
		return OBJECT_NAME;
	}

	@Override
	protected int getExpectedOperationCount() {
		return 9;
	}

	@Override
	protected int getExpectedAttributeCount() {
		return 4;
	}

	@Override
	protected String getApplicationContextPath() {
		return "org/springframework/jmx/export/assembler/methodExclusionAssembler.xml";
	}

	@Override
	protected MBeanInfoAssembler getAssembler() {
		MethodExclusionMBeanInfoAssembler assembler = new MethodExclusionMBeanInfoAssembler();
		assembler.setIgnoredMethods(new String[] {"dontExposeMe", "setSuperman"});
		return assembler;
	}

	@Test
	void supermanIsReadOnly() throws Exception {
		ModelMBeanInfo info = getMBeanInfoFromAssembler();
		ModelMBeanAttributeInfo attr = info.getAttribute("Superman");

		assertThat(attr.isReadable()).isTrue();
		assertThat(attr.isWritable()).isFalse();
	}

	/*
	 * https://opensource.atlassian.com/projects/spring/browse/SPR-2754
	 */
	@Test
	void isNotIgnoredDoesntIgnoreUnspecifiedBeanMethods() throws Exception {
		final String beanKey = "myTestBean";
		MethodExclusionMBeanInfoAssembler assembler = new MethodExclusionMBeanInfoAssembler();
		Properties ignored = new Properties();
		ignored.setProperty(beanKey, "dontExposeMe,setSuperman");
		assembler.setIgnoredMethodMappings(ignored);
		Method method = JmxTestBean.class.getMethod("dontExposeMe");
		assertThat(assembler.isNotIgnored(method, beanKey)).isFalse();
		// this bean does not have any ignored methods on it, so must obviously not be ignored...
		assertThat(assembler.isNotIgnored(method, "someOtherBeanKey")).isTrue();
	}

}
