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

package example;

import java.util.HashMap;
import java.util.Map;

/**
 * Type that can be indexed by the {@link Color} enum (i.e., something other
 * than an int, Integer, or String) and whose indexed values are Strings.
 */
public class FruitMap {

	private final Map<Color, String> map = new HashMap<>();

	public FruitMap() {
		this.map.put(Color.RED, "cherry");
		this.map.put(Color.ORANGE, "orange");
		this.map.put(Color.YELLOW, "banana");
		this.map.put(Color.GREEN, "kiwi");
		this.map.put(Color.BLUE, "blueberry");
		// We don't map PURPLE so that we can test for an unsupported color.
	}

	public String getFruit(Color color) {
		if (!this.map.containsKey(color)) {
			throw new IllegalArgumentException("No fruit for color " + color);
		}
		return this.map.get(color);
	}

	public void setFruit(Color color, String fruit) {
		this.map.put(color, fruit);
	}

}
