/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.messaging.simp.user;

/**
 * Represents a subscription within a user session.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface SimpSubscription {

	/**
	 * Return the id associated of the subscription, never {@code null}.
	 */
	String getId();

	/**
	 * Return the session of the subscription, never {@code null}.
	 */
	SimpSession getSession();

	/**
	 * Return the subscription's destination, never {@code null}.
	 */
	String getDestination();

}
