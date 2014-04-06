/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.support;

import org.springframework.util.IdGenerator;

/**
 * A support class for factories creating pre-configured instances of type
 * {@link org.springframework.messaging.support.MessageHeaderAccessor}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class MessageHeaderAccessorFactorySupport {

	private IdGenerator idGenerator;

	private Boolean enableTimestamp;


	protected MessageHeaderAccessorFactorySupport() {
	}

	/**
	 *
	 * @param idGenerator
	 */
	public void setIdGenerator(IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}

	public IdGenerator getIdGenerator() {
		return this.idGenerator;
	}

	public void setEnableTimestamp(boolean enableTimestamp) {
		this.enableTimestamp = enableTimestamp;
	}

	public boolean isEnableTimestamp() {
		return this.enableTimestamp;
	}

	protected void updateMessageHeaderAccessor(MessageHeaderAccessor headerAccessor) {
		if (this.idGenerator != null) {
			headerAccessor.setIdGenerator(this.idGenerator);
		}
		if (this.enableTimestamp != null) {
			headerAccessor.setEnableTimestamp(this.enableTimestamp);
		}
	}

}
