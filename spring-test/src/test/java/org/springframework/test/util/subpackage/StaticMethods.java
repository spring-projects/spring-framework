/*
 * Copyright 2002-2019 the original author or authors.
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
 * Simple class with static methods; intended for use in unit tests.
 *
 * @author Sam Brannen
 * @since 5.2
 */
public class StaticMethods {

	public static String publicMethodValue = "public";

	private static String privateMethodValue = "private";


	public static void publicMethod(String value) {
		publicMethodValue = value;
	}

	public static String publicMethod() {
		return publicMethodValue;
	}

	@SuppressWarnings("unused")
	private static void privateMethod(String value) {
		privateMethodValue = value;
	}

	@SuppressWarnings("unused")
	private static String privateMethod() {
		return privateMethodValue;
	}

	public static void reset() {
		publicMethodValue = "public";
		privateMethodValue = "private";
	}

	public static String getPublicMethodValue() {
		return publicMethodValue;
	}

	public static String getPrivateMethodValue() {
		return privateMethodValue;
	}

}
