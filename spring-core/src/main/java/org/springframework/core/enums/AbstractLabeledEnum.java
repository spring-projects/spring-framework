/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core.enums;

/**
 * Abstract base superclass for LabeledEnum implementations.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.2.2
 * @deprecated as of Spring 3.0, in favor of Java 5 enums.
 */
@Deprecated
@SuppressWarnings("serial")
public abstract class AbstractLabeledEnum implements LabeledEnum {

	/**
	 * Create a new AbstractLabeledEnum instance.
	 */
	protected AbstractLabeledEnum() {
	}

	@Override
	public Class getType() {
		// Could be coded as getClass().isAnonymousClass() on JDK 1.5
		boolean isAnonymous = (getClass().getDeclaringClass() == null && getClass().getName().indexOf('$') != -1);
		return (isAnonymous ? getClass().getSuperclass() : getClass());
	}

	@Override
	public int compareTo(Object obj) {
		if (!(obj instanceof LabeledEnum)) {
			throw new ClassCastException("You may only compare LabeledEnums");
		}
		LabeledEnum that = (LabeledEnum) obj;
		if (!this.getType().equals(that.getType())) {
			throw new ClassCastException("You may only compare LabeledEnums of the same type");
		}
		return this.getCode().compareTo(that.getCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof LabeledEnum)) {
			return false;
		}
		LabeledEnum other = (LabeledEnum) obj;
		return (this.getType().equals(other.getType()) && this.getCode().equals(other.getCode()));
	}

	@Override
	public int hashCode() {
		return (getType().hashCode() * 29 + getCode().hashCode());
	}

	@Override
	public String toString() {
		return getLabel();
	}

}
