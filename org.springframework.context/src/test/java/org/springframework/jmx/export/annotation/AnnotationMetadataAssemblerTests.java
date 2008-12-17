/*
 * Copyright 2002-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.jmx.export.annotation;

import org.springframework.jmx.IJmxTestBean;
import org.springframework.jmx.export.assembler.AbstractMetadataAssemblerTests;
import org.springframework.jmx.export.metadata.JmxAttributeSource;

/**
 * @author Rob Harrop
 */
public class AnnotationMetadataAssemblerTests extends AbstractMetadataAssemblerTests {

	private static final String OBJECT_NAME = "bean:name=testBean4";

	protected JmxAttributeSource getAttributeSource() {
		return new AnnotationJmxAttributeSource();
	}

	protected String getObjectName() {
		return OBJECT_NAME;
	}

	protected IJmxTestBean createJmxTestBean() {
		return new AnnotationTestBean();
	}

	protected String getApplicationContextPath() {
		return "org/springframework/jmx/export/annotation/annotations.xml";
	}

}
