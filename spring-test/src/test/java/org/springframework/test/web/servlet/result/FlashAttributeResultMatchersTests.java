/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.result;

import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.StubMvcResult;
import org.springframework.web.servlet.FlashMap;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Craig Walls
 */
class FlashAttributeResultMatchersTests {

	@Test
	void attributeExists() throws Exception {
		new FlashAttributeResultMatchers().attributeExists("good").match(getStubMvcResult());
	}

	@Test
	void attributeExists_DoesNotExist() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new FlashAttributeResultMatchers().attributeExists("bad").match(getStubMvcResult()));
	}

	@Test
	void attribute() throws Exception {
		new FlashAttributeResultMatchers().attribute("good", "good").match(getStubMvcResult());
	}

	@Test
	void attribute_incorrectValue() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new FlashAttributeResultMatchers().attribute("good", "not good").match(getStubMvcResult()));
	}

	private StubMvcResult getStubMvcResult() {
		FlashMap flashMap = new FlashMap();
		flashMap.put("good", "good");
		return new StubMvcResult(null, null, null, null, null, flashMap, null);
	}

}
