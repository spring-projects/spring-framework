/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for StandardReflectionParameterNameDiscoverer
 *
 * @author Rob Winch
 */
class StandardReflectionParameterNameDiscoverTests {

	private ParameterNameDiscoverer parameterNameDiscoverer;

	@BeforeEach
	void setup() {
		parameterNameDiscoverer = new StandardReflectionParameterNameDiscoverer();
	}

	@Test
	void getParameterNamesOnInterface() {
		Method method = ReflectionUtils.findMethod(MessageService.class,"sendMessage", String.class);
		String[] actualParams = parameterNameDiscoverer.getParameterNames(method);
		assertThat(actualParams).isEqualTo(new String[]{"message"});
	}

	public interface MessageService {
		void sendMessage(String message);
	}

}
