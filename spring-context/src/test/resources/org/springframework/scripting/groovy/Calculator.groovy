package org.springframework.scripting.groovy;

import org.springframework.scripting.Calculator

class GroovyCalculator implements Calculator {

	@Override
	int add(int x, int y) {
		return x + y;
	}

}
