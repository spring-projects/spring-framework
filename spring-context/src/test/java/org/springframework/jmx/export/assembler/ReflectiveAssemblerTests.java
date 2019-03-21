/*
 * Copyright 2002-2016 the original author or authors.
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

/**
 * @author Rob Harrop
 */
public class ReflectiveAssemblerTests extends AbstractJmxAssemblerTests {

	protected static final String OBJECT_NAME = "bean:name=testBean1";


	@Override
	protected String getObjectName() {
		return OBJECT_NAME;
	}

	@Override
	protected int getExpectedOperationCount() {
		return 11;
	}

	@Override
	protected int getExpectedAttributeCount() {
		return 4;
	}

	@Override
	protected MBeanInfoAssembler getAssembler() {
		return new SimpleReflectiveMBeanInfoAssembler();
	}

	@Override
	protected String getApplicationContextPath() {
		return "org/springframework/jmx/export/assembler/reflectiveAssembler.xml";
	}

}
