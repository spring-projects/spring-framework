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

package org.springframework.util.xml;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.util.xml.XmlValidationModeDetector.VALIDATION_DTD;
import static org.springframework.util.xml.XmlValidationModeDetector.VALIDATION_XSD;

/**
 * Tests for {@link XmlValidationModeDetector}.
 *
 * @author Sam Brannen
 * @since 5.1.10
 */
class XmlValidationModeDetectorTests {

	private final XmlValidationModeDetector xmlValidationModeDetector = new XmlValidationModeDetector();


	@ParameterizedTest
	@ValueSource(strings = {
		"dtdWithNoComments.xml",
		"dtdWithLeadingComment.xml",
		"dtdWithTrailingComment.xml",
		"dtdWithTrailingCommentAcrossMultipleLines.xml",
		"dtdWithCommentOnNextLine.xml",
		"dtdWithMultipleComments.xml"
	})
	void dtdDetection(String fileName) throws Exception {
		assertValidationMode(fileName, VALIDATION_DTD);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"xsdWithNoComments.xml",
		"xsdWithMultipleComments.xml",
		"xsdWithDoctypeInComment.xml",
		"xsdWithDoctypeInOpenCommentWithAdditionalCommentOnSameLine.xml"
	})
	void xsdDetection(String fileName) throws Exception {
		assertValidationMode(fileName, VALIDATION_XSD);
	}


	private void assertValidationMode(String fileName, int expectedValidationMode) throws IOException {
		try (InputStream inputStream = getClass().getResourceAsStream(fileName)) {
			assertThat(xmlValidationModeDetector.detectValidationMode(inputStream))
				.as("Validation Mode")
				.isEqualTo(expectedValidationMode);
		}
	}

}
