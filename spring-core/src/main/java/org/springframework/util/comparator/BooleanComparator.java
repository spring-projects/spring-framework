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

package org.springframework.util.comparator;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A Comparator for Boolean objects that can sort either true or false first.
 *
 * @author Keith Donald
 * @since 1.2.2
 */
@SuppressWarnings("serial")
public final class BooleanComparator implements Comparator<Boolean>, Serializable {

	/**
	 * A shared default instance of this comparator, treating true lower
	 * than false.
	 */
	public static final BooleanComparator TRUE_LOW = new BooleanComparator(true);

	/**
	 * A shared default instance of this comparator, treating true higher
	 * than false.
	 */
	public static final BooleanComparator TRUE_HIGH = new BooleanComparator(false);


	private final boolean trueLow;


	/**
	 * Create a BooleanComparator that sorts boolean values based on
	 * the provided flag.
	 * <p>Alternatively, you can use the default shared instances:
	 * {@code BooleanComparator.TRUE_LOW} and
	 * {@code BooleanComparator.TRUE_HIGH}.
	 * @param trueLow whether to treat true as lower or higher than false
	 * @see #TRUE_LOW
	 * @see #TRUE_HIGH
	 */
	public BooleanComparator(boolean trueLow) {
		this.trueLow = trueLow;
	}


	@Override
	public int compare(Boolean v1, Boolean v2) {
		return (v1 ^ v2) ? ((v1 ^ this.trueLow) ? 1 : -1) : 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof BooleanComparator)) {
			return false;
		}
		return (this.trueLow == ((BooleanComparator) obj).trueLow);
	}

	@Override
	public int hashCode() {
		return (this.trueLow ? -1 : 1) * getClass().hashCode();
	}

	@Override
	public String toString() {
		return "BooleanComparator: " + (this.trueLow ? "true low" : "true high");
	}

}
