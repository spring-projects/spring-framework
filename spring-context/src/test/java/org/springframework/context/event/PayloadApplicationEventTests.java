/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.event;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 */
public class PayloadApplicationEventTests {

	@Test
	public void testEventClassWithInterface() {
		ApplicationContext ac = new AnnotationConfigApplicationContext(Listener.class);
		MyEventClass event = new MyEventClass<>(this, "xyz");
		ac.publishEvent(event);
		assertTrue(ac.getBean(Listener.class).events.contains(event));
	}


	public interface Auditable {
	}


	public static class MyEventClass<GT> extends PayloadApplicationEvent<GT> implements Auditable {

		public MyEventClass(Object source, GT payload) {
			super(source, payload);
		}

		public String toString() {
			return "Payload: " + getPayload();
		}
	}


	@Component
	public static class Listener {

		public final List<Auditable> events = new ArrayList<>();

		@EventListener
		public void onEvent(Auditable event) {
			events.add(event);
		}
	}

}
