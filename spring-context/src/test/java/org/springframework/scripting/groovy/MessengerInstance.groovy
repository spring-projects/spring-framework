package org.springframework.scripting.groovy;

import org.springframework.scripting.Messenger

class GroovyMessenger implements Messenger {

	GroovyMessenger() {
		println "GroovyMessenger"
	}

	def String message;
}

return new GroovyMessenger();
