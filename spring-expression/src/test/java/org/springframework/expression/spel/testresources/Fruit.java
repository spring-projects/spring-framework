/**
 * 
 */
package org.springframework.expression.spel.testresources;

import java.awt.Color;

///CLOVER:OFF
public class Fruit {
	public String name; // accessible as property field
	public Color color; // accessible as property through getter/setter
	public String colorName; // accessible as property through getter/setter
	public int stringscount = -1;
	
	public Fruit(String name, Color color, String colorName) {
		this.name = name;
		this.color = color;
		this.colorName = colorName;
	}

	public Color getColor() {
		return color;
	}
	
	public Fruit(String... strings) {
		stringscount = strings.length;
	}
	
	public Fruit(int i, String... strings) {
		stringscount = i + strings.length;
	}
	
	public int stringscount() {
		return stringscount;
	}

	public String toString() {
		return "A" + (colorName != null && colorName.startsWith("o") ? "n " : " ") + colorName + " " + name;
	}
}