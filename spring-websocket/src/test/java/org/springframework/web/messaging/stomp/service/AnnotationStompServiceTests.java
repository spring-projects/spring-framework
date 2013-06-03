/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.messaging.stomp.service;

import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.messaging.MessageMapping;
import org.springframework.web.messaging.MessageType;
import org.springframework.web.method.HandlerMethod;

import reactor.core.Environment;
import reactor.core.Reactor;

import static org.junit.Assert.*;


/**
 * Test fixture for {@link AnnotationStompService}.
 * @author Rossen Stoyanchev
 */
public class AnnotationStompServiceTests {


	private AnnotationStompService service;


	@Before
	public void setup() {
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("controller", TestController.class);

		Reactor reactor = new Reactor(new Environment());
		this.service = new AnnotationStompService(reactor);
		this.service.setApplicationContext(wac);
		this.service.afterPropertiesSet();
	}

	@Test
	public void noMatch() {
		assertNull(this.service.getHandlerMethod("/nomatch", MessageType.CONNECT));
	}

	@Test
	public void match() {
		HandlerMethod handlerMethod = this.service.getHandlerMethod("/foo", MessageType.SUBSCRIBE);

		assertNotNull(handlerMethod);
		assertEquals("handleSubscribe", handlerMethod.getMethod().getName());
		assertEquals("controller", handlerMethod.getBean());
	}


	@Controller
	private static class TestController {

		@MessageMapping(value="/foo", messageType=MessageType.SUBSCRIBE)
		public void handleSubscribe() {
		}

	}

}
