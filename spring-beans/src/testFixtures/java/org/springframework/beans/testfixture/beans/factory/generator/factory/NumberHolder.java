/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.testfixture.beans.factory.generator.factory;

import java.io.Serializable;

/**
 * A sample object with a generic type.
 *
 * @param <T> the number type
 * @author Stephane Nicoll
 */
@SuppressWarnings("serial")
public class NumberHolder<T extends Number> implements Serializable {

	@SuppressWarnings("unused")
	private final T number;

	public NumberHolder(T number) {
		this.number = number;
	}

}
