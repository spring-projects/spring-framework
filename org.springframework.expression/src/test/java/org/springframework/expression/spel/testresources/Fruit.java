/**
 * 
 */
package org.springframework.expression.spel.testresources;

import java.awt.Color;

public class Fruit {
	public String name; // accessible as property field
	public Color color; // accessible as property through getter/setter
	public String colorName; // accessible as property through getter/setter

	public Fruit(String name, Color color, String colorName) {
		this.name = name;
		this.color = color;
		this.colorName = colorName;
	}

	public Color getColor() {
		return color;
	}

	public String toString() {
		return "A" + (colorName.startsWith("o") ? "n " : " ") + colorName + " " + name;
	}
}