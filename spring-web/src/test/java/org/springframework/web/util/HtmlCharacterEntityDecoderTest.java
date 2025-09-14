package org.springframework.web.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HtmlCharacterEntityDecoderTest {

	@Test
	@DisplayName("Should correctly unescape Unicode supplementary characters")
	void unescapeHandlesSupplementaryCharactersCorrectly() {
		// Arrange: Prepare test cases with the 'grinning face' emoji (😀, U+1F600).
		String expectedCharacter = "😀";
		String decimalEntity = "&#128512;";
		String hexEntity = "&#x1F600;";

		// Act: Call the HtmlUtils.htmlUnescape method to get the actual results.
		String actualResultFromDecimal = HtmlUtils.htmlUnescape(decimalEntity);
		String actualResultFromHex = HtmlUtils.htmlUnescape(hexEntity);

		// Assert: Verify that the actual results match the expected character.
		assertEquals(expectedCharacter, actualResultFromDecimal, "Decimal entity was not converted correctly.");
		assertEquals(expectedCharacter, actualResultFromHex, "Hexadecimal entity was not converted correctly.");
	}

	@Test
	@DisplayName("Should correctly unescape basic and named HTML entities")
	void unescapeHandlesBasicEntities() {
		// Arrange
		String input = "&lt;p&gt;Tom &amp; Jerry&#39;s &quot;Show&quot;&lt;/p&gt;";
		String expectedOutput = "<p>Tom & Jerry's \"Show\"</p>";

		// Act
		String actualOutput = HtmlUtils.htmlUnescape(input);

		// Assert
		assertEquals(expectedOutput, actualOutput, "Basic HTML entities were not unescaped correctly.");
	}
}
