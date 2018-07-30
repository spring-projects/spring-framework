package org.springframework.web.reactive.function.client;

import org.junit.Test;

public class ResponseBodyLimitFilterTests {

	@Test(expected = IllegalArgumentException.class)
	public void negativeLimit() {
		new ResponseBodyLimitFilterFunction(-1);
	}
}
