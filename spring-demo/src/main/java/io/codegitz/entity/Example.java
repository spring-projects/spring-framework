package io.codegitz.entity;

import org.springframework.context.MessageSource;

import java.util.Locale;

/**
 * @author 张观权
 * @date 2020/8/17 15:15
 **/
public class Example {

	private MessageSource messages;

	public void setMessages(MessageSource messages) {
		this.messages = messages;
	}

	public void execute() {
		String message = this.messages.getMessage("argument.required",
				new Object [] {"userDao"}, "Required", Locale.ENGLISH);
		System.out.println(message);
	}
}