/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.messaging.support;

import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.IdGenerator;

/**
 * A {@link org.springframework.messaging.support.MessageHeaderInitializer MessageHeaderInitializer}
 * to customize the strategy for ID and TIMESTAMP message header generation.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class IdTimestampMessageHeaderInitializer implements MessageHeaderInitializer {

	private static final IdGenerator ID_VALUE_NONE_GENERATOR = () -> MessageHeaders.ID_VALUE_NONE;


	@Nullable
	private IdGenerator idGenerator;

	private boolean enableTimestamp;


	/**
	 * Configure the IdGenerator strategy to initialize {@code MessageHeaderAccessor}
	 * instances with.
	 * <p>By default this property is set to {@code null} in which case the default
	 * IdGenerator of {@link org.springframework.messaging.MessageHeaders} is used.
	 * <p>To have no ids generated at all, see {@link #setDisableIdGeneration()}.
	 */
	public void setIdGenerator(@Nullable IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}

	/**
	 * Return the configured {@code IdGenerator}, if any.
	 */
	@Nullable
	public IdGenerator getIdGenerator() {
		return this.idGenerator;
	}

	/**
	 * A shortcut for calling {@link #setIdGenerator} with an id generation strategy
	 * to disable id generation completely.
	 */
	public void setDisableIdGeneration() {
		this.idGenerator = ID_VALUE_NONE_GENERATOR;
	}

	/**
	 * Whether to enable the automatic addition of the
	 * {@link org.springframework.messaging.MessageHeaders#TIMESTAMP} header on
	 * {@code MessageHeaderAccessor} instances being initialized.
	 * <p>By default this property is set to false.
	 */
	public void setEnableTimestamp(boolean enableTimestamp) {
		this.enableTimestamp = enableTimestamp;
	}

	/**
	 * Return whether the timestamp header is enabled or not.
	 */
	public boolean isEnableTimestamp() {
		return this.enableTimestamp;
	}


	@Override
	public void initHeaders(MessageHeaderAccessor headerAccessor) {
		IdGenerator idGenerator = getIdGenerator();
		if (idGenerator != null) {
			headerAccessor.setIdGenerator(idGenerator);
		}
		headerAccessor.setEnableTimestamp(isEnableTimestamp());
	}

}
