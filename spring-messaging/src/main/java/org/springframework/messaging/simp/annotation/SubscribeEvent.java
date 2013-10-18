/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.messaging.simp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation for mapping subscription events onto specific handler methods based
 * on the destination for the message (e.g. STOMP SUBSCRIBE message).
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SubscribeEvent {

	/**
	 * Destination-based mapping expressed by this annotation.
	 * <p>
	 * For STOMP over WebSocket messages: this is the destination of the STOMP message
	 * (e.g. "/positions"). Ant-style path patterns (e.g. "/price.stock.*") are supported
	 * and so are path template variables (e.g. "/price.stock.{ticker}"").
	 */
	String[] value() default {};

}
