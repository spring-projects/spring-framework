/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.messaging.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Composite {@link MessageCondition} that delegates to other message conditions.
 *
 * <p>For {@link #combine} and {@link #compareTo} it is expected that the "other"
 * composite contains the same number, type, and order of message conditions.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class CompositeMessageCondition implements MessageCondition<CompositeMessageCondition> {

	private final List<MessageCondition<?>> messageConditions;


	public CompositeMessageCondition(MessageCondition<?>... messageConditions) {
		this(Arrays.asList(messageConditions));
	}

	private CompositeMessageCondition(List<MessageCondition<?>> messageConditions) {
		Assert.notEmpty(messageConditions, "No message conditions");
		this.messageConditions = messageConditions;
	}


	public List<MessageCondition<?>> getMessageConditions() {
		return this.messageConditions;
	}

	@SuppressWarnings("unchecked")
	public <T extends MessageCondition<T>> T getCondition(Class<T> messageConditionType) {
		for (MessageCondition<?> condition : this.messageConditions) {
			if (messageConditionType.isAssignableFrom(condition.getClass())) {
				return (T) condition;
			}
		}
		throw new IllegalStateException("No condition of type: " + messageConditionType);
	}


	@Override
	public CompositeMessageCondition combine(CompositeMessageCondition other) {
		checkCompatible(other);
		List<MessageCondition<?>> result = new ArrayList<>(this.messageConditions.size());
		for (int i = 0; i < this.messageConditions.size(); i++) {
			result.add(combine(getMessageConditions().get(i), other.getMessageConditions().get(i)));
		}
		return new CompositeMessageCondition(result);
	}

	@SuppressWarnings("unchecked")
	private <T extends MessageCondition<T>> T combine(MessageCondition<?> first, MessageCondition<?> second) {
		return ((T) first).combine((T) second);
	}

	@Override
	@Nullable
	public CompositeMessageCondition getMatchingCondition(Message<?> message) {
		List<MessageCondition<?>> result = new ArrayList<>(this.messageConditions.size());
		for (MessageCondition<?> condition : this.messageConditions) {
			MessageCondition<?> matchingCondition = (MessageCondition<?>) condition.getMatchingCondition(message);
			if (matchingCondition == null) {
				return null;
			}
			result.add(matchingCondition);
		}
		return new CompositeMessageCondition(result);
	}

	@Override
	public int compareTo(CompositeMessageCondition other, Message<?> message) {
		checkCompatible(other);
		List<MessageCondition<?>> otherConditions = other.getMessageConditions();
		for (int i = 0; i < this.messageConditions.size(); i++) {
			int result = compare (this.messageConditions.get(i), otherConditions.get(i), message);
			if (result != 0) {
				return result;
			}
		}
		return 0;
	}

	@SuppressWarnings("unchecked")
	private <T extends MessageCondition<T>> int compare(
			MessageCondition<?> first, MessageCondition<?> second, Message<?> message) {

		return ((T) first).compareTo((T) second, message);
	}

	private void checkCompatible(CompositeMessageCondition other) {
		List<MessageCondition<?>> others = other.getMessageConditions();
		for (int i = 0; i < this.messageConditions.size(); i++) {
			if (i < others.size()) {
				if (this.messageConditions.get(i).getClass().equals(others.get(i).getClass())) {
					continue;
				}
			}
			throw new IllegalArgumentException("Mismatched CompositeMessageCondition: " +
					this.messageConditions + " vs " + others);
		}
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof CompositeMessageCondition otherComposite)) {
			return false;
		}
		checkCompatible(otherComposite);
		List<MessageCondition<?>> otherConditions = otherComposite.getMessageConditions();
		for (int i = 0; i < this.messageConditions.size(); i++) {
			if (!this.messageConditions.get(i).equals(otherConditions.get(i))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hashCode = 0;
		for (MessageCondition<?> condition : this.messageConditions) {
			hashCode += condition.hashCode() * 31;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return this.messageConditions.stream().map(Object::toString).collect(Collectors.joining(",", "{", "}"));
	}

}
