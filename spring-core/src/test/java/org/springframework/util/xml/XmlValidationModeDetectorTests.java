/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
		"dtdWithMultipleComments.xml",
		"dtdWithDoctypeInMultiLineCommentBody.xml"
	})
	void dtdDetection(String fileName) throws Exception {
		assertValidationMode(fileName, VALIDATION_DTD);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"xsdWithNoComments.xml",
		"xsdWithMultipleComments.xml",
		"xsdWithDoctypeInComment.xml",
		"xsdWithDoctypeInOpenCommentWithAdditionalCommentOnSameLine.xml",
		"xsdWithDoctypeInMultiLineCommentBody.xml",
		"xsdWithMultipleDoctypesInMultiLineCommentBody.xml"
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
