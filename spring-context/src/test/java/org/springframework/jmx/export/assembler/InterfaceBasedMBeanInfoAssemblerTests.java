/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.assembler;

/**
 * @author Rob Harrop
 */
class InterfaceBasedMBeanInfoAssemblerTests extends AbstractJmxAssemblerTests {

	@Override
	protected String getObjectName() {
		return "bean:name=testBean4";
	}

	@Override
	protected int getExpectedOperationCount() {
		return 7;
	}

	@Override
	protected int getExpectedAttributeCount() {
		return 2;
	}

	@Override
	protected MBeanInfoAssembler getAssembler() {
		return new InterfaceBasedMBeanInfoAssembler();
	}

	@Override
	protected String getApplicationContextPath() {
		return "org/springframework/jmx/export/assembler/interfaceAssembler.xml";
	}

}
