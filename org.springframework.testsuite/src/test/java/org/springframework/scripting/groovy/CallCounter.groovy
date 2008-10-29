package org.springframework.scripting.groovy;

import org.springframework.scripting.CallCounter;

class GroovyCallCounter implements CallCounter {

	int count = -100;

	void init() {
		count = 0;
	}

	void before() {
	  count++;
	}

	int getCalls() {
	  return count;
	}

	void destroy() {
		count = -200;
	}
}
