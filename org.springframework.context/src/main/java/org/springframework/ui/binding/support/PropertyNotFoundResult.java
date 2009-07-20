package org.springframework.ui.binding.support;

import org.springframework.context.MessageSource;
import org.springframework.core.style.StylerUtils;
import org.springframework.ui.alert.Alert;
import org.springframework.ui.alert.Severity;
import org.springframework.ui.binding.BindingResult;
import org.springframework.ui.message.MessageBuilder;
import org.springframework.ui.message.ResolvableArgument;

class PropertyNotFoundResult implements BindingResult {

	private String property;

	private Object sourceValue;

	private MessageSource messageSource;
	
	public PropertyNotFoundResult(String property, Object sourceValue, MessageSource messageSource) {
		this.property = property;
		this.sourceValue = sourceValue;
		this.messageSource = messageSource;
	}

	public String getProperty() {
		return property;
	}

	public Object getSourceValue() {
		return sourceValue;
	}

	public boolean isFailure() {
		return true;
	}

	public Alert getAlert() {
		return new Alert() {
			public String getCode() {
				return "propertyNotFound";
			}

			public Severity getSeverity() {
				return Severity.WARNING;
			}

			public String getMessage() {
				MessageBuilder builder = new MessageBuilder(messageSource);
				builder.code("bindSuccess");
				builder.arg("label", new ResolvableArgument(property));
				builder.arg("value", sourceValue);
				// TODO lazily create default message
				builder.defaultMessage("Successfully bound user value " + StylerUtils.style(sourceValue)
						+ " to property '" + property + "'");
				return builder.build();
			}
		};
	}

	public String toString() {
		return getAlert().toString();
	}

}