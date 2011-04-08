/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.condition;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Abstract base class for {@link RequestCondition} implementations that wrap other request conditions.
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
abstract class RequestConditionComposite extends AbstractRequestCondition {

	protected final List<RequestCondition> conditions;

	protected RequestConditionComposite(List<RequestCondition> conditions) {
		super(getWeight(conditions));
		this.conditions = Collections.unmodifiableList(conditions);
	}

	private static int getWeight(List<RequestCondition> conditions) {
		int weight = 0;
		for (RequestCondition condition : conditions) {
			if (condition instanceof AbstractRequestCondition) {
				AbstractRequestCondition abstractRequestCondition = (AbstractRequestCondition) condition;
				weight += abstractRequestCondition.getWeight();
			}
		}
		return weight;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o != null && getClass().equals(o.getClass())) {
			RequestConditionComposite other = (RequestConditionComposite) o;
			return this.conditions.equals(other.conditions);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return conditions.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("[");
		String infix = getToStringInfix();
		for (Iterator<RequestCondition> iterator = conditions.iterator(); iterator.hasNext();) {
			RequestCondition condition = iterator.next();
			builder.append(condition.toString());
			if (iterator.hasNext()) {
				builder.append(infix);
			}
		}
		builder.append("]");
		return builder.toString();
	}

	protected abstract String getToStringInfix();
}
