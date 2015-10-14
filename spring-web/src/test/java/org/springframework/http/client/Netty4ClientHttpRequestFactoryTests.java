/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.http.HttpMethod;

/**
 * @author Arjen Poutsma
 */
public class Netty4ClientHttpRequestFactoryTests extends AbstractHttpRequestFactoryTestCase {

	private static EventLoopGroup eventLoopGroup;


	@BeforeClass
	public static void createEventLoopGroup() {
		eventLoopGroup = new NioEventLoopGroup();
	}

	@AfterClass
	public static void shutdownEventLoopGroup() throws InterruptedException {
		eventLoopGroup.shutdownGracefully().sync();
	}

	@Override
	protected ClientHttpRequestFactory createRequestFactory() {
		return new Netty4ClientHttpRequestFactory(eventLoopGroup);
	}

	@Override
	@Test
	public void httpMethods() throws Exception {
		super.httpMethods();
		assertHttpMethod("patch", HttpMethod.PATCH);
	}

}
