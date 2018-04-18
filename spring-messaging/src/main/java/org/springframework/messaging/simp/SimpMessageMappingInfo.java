/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.messaging.simp;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.MessageCondition;

/**
 * Encapsulates the following request mapping conditions:
 * <ol>
 * <li>{@link SimpMessageTypeMessageCondition}
 * <li>{@link DestinationPatternsMessageCondition}
 * </ol>
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpMessageMappingInfo implements MessageCondition<SimpMessageMappingInfo> {

	private final SimpMessageTypeMessageCondition messageTypeMessageCondition;

	private final DestinationPatternsMessageCondition destinationConditions;


	public SimpMessageMappingInfo(SimpMessageTypeMessageCondition messageTypeMessageCondition,
			DestinationPatternsMessageCondition destinationConditions) {

		this.messageTypeMessageCondition = messageTypeMessageCondition;
		this.destinationConditions = destinationConditions;
	}


	public SimpMessageTypeMessageCondition getMessageTypeMessageCondition() {
		return this.messageTypeMessageCondition;
	}

	public DestinationPatternsMessageCondition getDestinationConditions() {
		return this.destinationConditions;
	}


	@Override
	public SimpMessageMappingInfo combine(SimpMessageMappingInfo other) {
		SimpMessageTypeMessageCondition typeCond =
				this.getMessageTypeMessageCondition().combine(other.getMessageTypeMessageCondition());
		DestinationPatternsMessageCondition destCond =
				this.destinationConditions.combine(other.getDestinationConditions());
		return new SimpMessageMappingInfo(typeCond, destCond);
	}

	@Override
	@Nullable
	public SimpMessageMappingInfo getMatchingCondition(Message<?> message) {
		SimpMessageTypeMessageCondition typeCond = this.messageTypeMessageCondition.getMatchingCondition(message);
		if (typeCond == null) {
			return null;
		}
		DestinationPatternsMessageCondition destCond = this.destinationConditions.getMatchingCondition(message);
		if (destCond == null) {
			return null;
		}
		return new SimpMessageMappingInfo(typeCond, destCond);
	}

	@Override
	public int compareTo(SimpMessageMappingInfo other, Message<?> message) {
		int result = this.messageTypeMessageCondition.compareTo(other.messageTypeMessageCondition, message);
		if (result != 0) {
			return result;
		}
		result = this.destinationConditions.compareTo(other.destinationConditions, message);
		if (result != 0) {
			return result;
		}
		return 0;
	}


	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && obj instanceof SimpMessageMappingInfo) {
			SimpMessageMappingInfo other = (SimpMessageMappingInfo) obj;
			return (this.destinationConditions.equals(other.destinationConditions) &&
					this.messageTypeMessageCondition.equals(other.messageTypeMessageCondition));
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (this.destinationConditions.hashCode() * 31 + this.messageTypeMessageCondition.hashCode());
	}

	@Override
	public String toString() {
		return "{" + this.destinationConditions + ",messageType=" + this.messageTypeMessageCondition + '}';
	}

}
