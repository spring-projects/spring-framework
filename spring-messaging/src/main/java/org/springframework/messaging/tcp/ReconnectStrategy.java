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

package org.springframework.messaging.tcp;


/**
 * A contract to determine the frequency of reconnect attempts after connection failure.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface ReconnectStrategy {

	/**
	 * Return the time to the next attempt to reconnect.
	 * @param attemptCount how many reconnect attempts have been made already
	 * @return the amount of time in milliseconds or {@code null} to stop
	 */
	Long getTimeToNextAttempt(int attemptCount);

}
