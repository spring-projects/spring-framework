package org.springframework.ui.message;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.support.AbstractMessageSource;
import org.springframework.util.Assert;

class MockMessageSource extends AbstractMessageSource {

	/** Map from 'code + locale' keys to message Strings */
	private final Map<String, String> messages = new HashMap<String, String>();
	
	@Override
	protected MessageFormat resolveCode(String code, Locale locale) {
		throw new IllegalStateException("Should not be called");
	}

	@Override
	protected String resolveCodeWithoutArguments(String code, Locale locale) {
		return this.messages.get(code + "_" + locale.toString());
	}

	/**
	 * Associate the given message with the given code.
	 * @param code the lookup code
   * @param locale the locale that the message should be found within
	 * @param msg the message associated with this lookup code
	 */
	public void addMessage(String code, Locale locale, String msg) {
		Assert.notNull(code, "Code must not be null");
		Assert.notNull(locale, "Locale must not be null");
		Assert.notNull(msg, "Message must not be null");
		this.messages.put(code + "_" + locale.toString(), msg);
		if (logger.isDebugEnabled()) {
			logger.debug("Added message [" + msg + "] for code [" + code + "] and Locale [" + locale + "]");
		}
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.messages;
	}

}

