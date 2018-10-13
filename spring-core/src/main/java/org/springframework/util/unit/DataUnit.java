/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.util.unit;

import java.util.Objects;

/**
 * A standard set of data size units.
 *
 * @author Stephane Nicoll
 * @since 5.1
 */
public enum DataUnit {

	/**
	 * Bytes.
	 */
	BYTES("B", DataSize.ofBytes(1)),

	/**
	 * Kilobytes.
	 */
	KILOBYTES("KB", DataSize.ofKilobytes(1)),

	/**
	 * Megabytes.
	 */
	MEGABYTES("MB", DataSize.ofMegabytes(1)),

	/**
	 * Gigabytes.
	 */
	GIGABYTES("GB", DataSize.ofGigabytes(1)),

	/**
	 * Terabytes.
	 */
	TERABYTES("TB", DataSize.ofTerabytes(1));


	private final String suffix;

	private final DataSize size;


	DataUnit(String suffix, DataSize size) {
		this.suffix = suffix;
		this.size = size;
	}

	DataSize size() {
		return this.size;
	}

	/**
	 * Return the {@link DataUnit} matching the specified {@code suffix}.
	 * @param suffix one of the standard suffix
	 * @return the {@link DataUnit} matching the specified {@code suffix}
	 * @throws IllegalArgumentException if the suffix does not match any
	 * of this enum's constants
	 */
	public static DataUnit fromSuffix(String suffix) {
		for (DataUnit candidate : values()) {
			if (Objects.equals(candidate.suffix, suffix)) {
				return candidate;
			}
		}
		throw new IllegalArgumentException("Unknown unit '" + suffix + "'");
	}

}
