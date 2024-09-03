/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.messaging.simp;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.CompositeMessageCondition;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.MessageCondition;

/**
 * {@link MessageCondition} for SImple Messaging Protocols. Encapsulates the following
 * request mapping conditions:
 * <ol>
 * <li>{@link SimpMessageTypeMessageCondition}
 * <li>{@link DestinationPatternsMessageCondition}
 * </ol>
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpMessageMappingInfo implements MessageCondition<SimpMessageMappingInfo> {

	private final CompositeMessageCondition delegate;


	public SimpMessageMappingInfo(SimpMessageTypeMessageCondition messageTypeMessageCondition,
			DestinationPatternsMessageCondition destinationConditions) {

		this.delegate = new CompositeMessageCondition(messageTypeMessageCondition, destinationConditions);
	}

	private SimpMessageMappingInfo(CompositeMessageCondition delegate) {
		this.delegate = delegate;
	}


	public SimpMessageTypeMessageCondition getMessageTypeMessageCondition() {
		return this.delegate.getCondition(SimpMessageTypeMessageCondition.class);
	}

	public DestinationPatternsMessageCondition getDestinationConditions() {
		return this.delegate.getCondition(DestinationPatternsMessageCondition.class);
	}


	@Override
	public SimpMessageMappingInfo combine(SimpMessageMappingInfo other) {
		return new SimpMessageMappingInfo(this.delegate.combine(other.delegate));
	}

	@Override
	@Nullable
	public SimpMessageMappingInfo getMatchingCondition(Message<?> message) {
		CompositeMessageCondition condition = this.delegate.getMatchingCondition(message);
		return condition != null ? new SimpMessageMappingInfo(condition) : null;
	}

	@Override
	public int compareTo(SimpMessageMappingInfo other, Message<?> message) {
		return this.delegate.compareTo(other.delegate, message);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof SimpMessageMappingInfo that && this.delegate.equals(that.delegate)));
	}

	@Override
	public int hashCode() {
		return this.delegate.hashCode();
	}

	@Override
	public String toString() {
		return this.delegate.toString();
	}

}
