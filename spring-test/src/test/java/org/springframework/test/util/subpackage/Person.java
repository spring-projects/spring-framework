/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.util.subpackage;

/**
 * Interface representing a <em>person</em> entity; intended for use in unit tests.
 *
 * <p>The introduction of an interface is necessary in order to test support for
 * JDK dynamic proxies.
 *
 * @author Sam Brannen
 * @since 4.3
 */
public interface Person {

	long getId();

	String getName();

	int getAge();

	String getEyeColor();

	boolean likesPets();

	Number getFavoriteNumber();

}
