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

package org.springframework.http.client;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;

/**
 * @author Arjen Poutsma
 */
public class Netty4AsyncClientHttpRequestFactoryTests extends AbstractAsyncHttpRequestFactoryTests {

	private static EventLoopGroup eventLoopGroup;


	@BeforeAll
	public static void createEventLoopGroup() {
		eventLoopGroup = new NioEventLoopGroup();
	}

	@AfterAll
	public static void shutdownEventLoopGroup() throws InterruptedException {
		eventLoopGroup.shutdownGracefully().sync();
	}

	@SuppressWarnings("deprecation")
	@Override
	protected AsyncClientHttpRequestFactory createRequestFactory() {
		return new Netty4ClientHttpRequestFactory(eventLoopGroup);
	}

	@Override
	@Test
	public void httpMethods() throws Exception {
		super.httpMethods();
		assertHttpMethod("patch", HttpMethod.PATCH);
	}

}
