/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.expression.spel;

/**
 * This is intentionally a top-level public class.
 */
public class PublicSuperclass {

	public int process(int num) {
		return num + 1;
	}

	public int getNumber() {
		return 1;
	}

	public String getMessage() {
		return "goodbye";
	}

	public String greet(String name) {
		return "Super, " + name;
	}

	public String getIndex(int index) {
		return "value-" + index;
	}

	public String getIndex2(int index) {
		return "value-" + (2 * index);
	}

}
