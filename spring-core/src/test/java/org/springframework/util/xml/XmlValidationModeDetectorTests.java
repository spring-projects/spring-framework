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

package org.springframework.util.xml;

import java.io.InputStream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.springframework.util.xml.XmlValidationModeDetector.VALIDATION_DTD;

/**
 * Unit tests for {@link XmlValidationModeDetector}.
 *
 * @author Sam Brannen
 * @since 5.1.10
 */
public class XmlValidationModeDetectorTests {

	private final XmlValidationModeDetector xmlValidationModeDetector = new XmlValidationModeDetector();


	@Test
	public void dtdWithTrailingComment() throws Exception {
		dtdDetection("dtdWithTrailingComment.xml");
	}

	@Test
	public void dtdWithLeadingComment() throws Exception {
		dtdDetection("dtdWithLeadingComment.xml");
	}

	@Test
	public void dtdWithCommentOnNextLine() throws Exception {
		dtdDetection("dtdWithCommentOnNextLine.xml");
	}

	@Test
	public void dtdWithMultipleComments() throws Exception {
		dtdDetection("dtdWithMultipleComments.xml");
	}

	private void dtdDetection(String fileName) throws Exception {
		InputStream inputStream = getClass().getResourceAsStream(fileName);
		assertEquals(VALIDATION_DTD, xmlValidationModeDetector.detectValidationMode(inputStream));
	}

}
