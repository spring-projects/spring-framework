package org.springframework.http.client.reactive;

import io.netty.buffer.AdaptiveByteBufAllocator;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import reactor.netty.Connection;
import reactor.netty.NettyOutbound;
import reactor.netty.http.client.HttpClientResponse;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ReactorClientHttpResponseTest {
	private final HttpClientResponse mockResponse = mock();

	private final DefaultHttpHeaders httpHeaders = mock();

	private final Connection connection = mock();

	private final NettyOutbound outbound = mock();

	private ReactorClientHttpResponse reactorClientHttpResponse;


	@BeforeEach
	void configureMocks() {
		given(mockResponse.responseHeaders()).willReturn(httpHeaders);
		given(connection.outbound()).willReturn(outbound);
		given(outbound.alloc()).willReturn(new AdaptiveByteBufAllocator());
		reactorClientHttpResponse = new ReactorClientHttpResponse(mockResponse, connection);
	}

	@Test
	void defaultCookies() {
		Map<CharSequence, Set<Cookie>> mockCookieMap = new HashMap<>();
		DefaultCookie cookie = new DefaultCookie("foo", "bar");
		cookie.setPartitioned(true);
		mockCookieMap.put("foo", Set.of(cookie));
		given(mockResponse.cookies()).willReturn(mockCookieMap);

		Assertions.assertTrue(reactorClientHttpResponse.getCookies().size() == 1 &&
				reactorClientHttpResponse.getCookies().containsKey("foo"));
		ResponseCookie foo = reactorClientHttpResponse.getCookies().getFirst("foo");
		assertThat(foo.isPartitioned()).isSameAs(cookie.isPartitioned());
	}
}