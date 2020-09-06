/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.scheduling.support;

import java.util.BitSet;

import org.assertj.core.api.AbstractAssert;

/**
 * @author Arjen Poutsma
 */
public class BitSetAssert extends AbstractAssert<BitSetAssert, BitSet> {

	private BitSetAssert(BitSet bitSet) {
		super(bitSet, BitSetAssert.class);
	}

	public static BitSetAssert assertThat(BitSet actual) {
		return new BitSetAssert(actual);
	}

	public BitSetAssert hasSet(int... indices) {
		isNotNull();

		for (int index : indices) {
			if (!this.actual.get(index)) {
				failWithMessage("Invalid disabled bit at @%d", index);
			}
		}
		return this;
	}

	public BitSetAssert hasSetRange(int min, int max) {
		isNotNull();

		for (int i = min; i < max; i++) {
			if (!this.actual.get(i)) {
				failWithMessage("Invalid disabled bit at @%d", i);
			}
		}
		return this;
	}

	public BitSetAssert hasUnset(int... indices) {
		isNotNull();

		for (int index : indices) {
			if (this.actual.get(index)) {
				failWithMessage("Invalid enabled bit at @%d", index);
			}
		}
		return this;
	}

	public BitSetAssert hasUnsetRange(int min, int max) {
		isNotNull();

		for (int i = min; i < max; i++) {
			if (this.actual.get(i)) {
				failWithMessage("Invalid enabled bit at @%d", i);
			}
		}
		return this;
	}

}

