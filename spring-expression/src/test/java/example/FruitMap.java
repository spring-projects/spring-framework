/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
