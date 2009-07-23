/*
 * Copyright 2004-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.ui.binding.support;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeConverter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.style.StylerUtils;
import org.springframework.ui.alert.Alert;
import org.springframework.ui.alert.Severity;
import org.springframework.ui.binding.Binding;
import org.springframework.ui.binding.BindingStatus;
import org.springframework.ui.binding.ValidationStatus;
import org.springframework.ui.format.Formatter;
import org.springframework.ui.message.MessageBuilder;
import org.springframework.ui.message.ResolvableArgument;

public class GenericBinding implements Binding {

	private ValueModel valueModel;

	private BindingContext bindingContext;

	private ValueBuffer buffer;

	private BindingStatus bindingStatus;

	private Object sourceValue;

	private Exception invalidSourceValueCause;

	public GenericBinding(ValueModel valueModel, BindingContext bindingContext) {
		this.valueModel = valueModel;
		this.bindingContext = bindingContext;
		buffer = new ValueBuffer(valueModel);
		bindingStatus = BindingStatus.CLEAN;
	}

	// implementing Binding

	public String getRenderValue() {
		return format(getValue(), bindingContext.getFormatter());
	}

	public Object getValue() {
		if (bindingStatus == BindingStatus.DIRTY || bindingStatus == BindingStatus.COMMIT_FAILURE) {
			return buffer.getValue();
		} else {
			return valueModel.getValue();
		}
	}

	public Class<?> getValueType() {
		return valueModel.getValueType();
	}

	public boolean isEditable() {
		return valueModel.isWriteable() && bindingContext.getEditableCondition().isTrue();
	}

	public boolean isEnabled() {
		return bindingContext.getEnabledCondition().isTrue();
	}

	public boolean isVisible() {
		return bindingContext.getVisibleCondition().isTrue();
	}

	@SuppressWarnings("unchecked")
	public void applySourceValue(Object sourceValue) {
		assertEditable();
		assertEnabled();
		if (sourceValue instanceof String) {
			try {
				Object parsed = bindingContext.getFormatter().parse((String) sourceValue, getLocale());
				buffer.setValue(coerseToValueType(parsed));
				sourceValue = null;
				bindingStatus = BindingStatus.DIRTY;
			} catch (ParseException e) {
				this.sourceValue = sourceValue;
				invalidSourceValueCause = e;
				bindingStatus = BindingStatus.INVALID_SOURCE_VALUE;
			} catch (ConversionFailedException e) {
				this.sourceValue = sourceValue;
				invalidSourceValueCause = e;
				bindingStatus = BindingStatus.INVALID_SOURCE_VALUE;
			}
		} else if (sourceValue instanceof String[]) {
			Object parsed;
			if (isMap()) {
				String[] sourceValues = (String[]) sourceValue;
				Formatter keyFormatter = bindingContext.getKeyFormatter();
				Formatter valueFormatter = bindingContext.getElementFormatter();
				Map map = new LinkedHashMap(sourceValues.length);
				for (int i = 0; i < sourceValues.length; i++) {
					String entryString = sourceValues[i];
					try {
						String[] keyValue = entryString.split("=");
						Object parsedMapKey = keyFormatter.parse(keyValue[0], getLocale());
						Object parsedMapValue = valueFormatter.parse(keyValue[1], getLocale());
						map.put(parsedMapKey, parsedMapValue);
					} catch (ParseException e) {
						this.sourceValue = sourceValue;
						invalidSourceValueCause = e;
						bindingStatus = BindingStatus.INVALID_SOURCE_VALUE;
						break;
					}
				}
				parsed = map;
			} else {
				String[] sourceValues = (String[]) sourceValue;
				List list = new ArrayList(sourceValues.length);
				for (int i = 0; i < sourceValues.length; i++) {
					Object parsedValue;
					try {
						parsedValue = bindingContext.getElementFormatter().parse(sourceValues[i], getLocale());
						list.add(parsedValue);
					} catch (ParseException e) {
						this.sourceValue = sourceValue;
						invalidSourceValueCause = e;
						bindingStatus = BindingStatus.INVALID_SOURCE_VALUE;
						break;
					}
				}
				parsed = list;
			}
			if (bindingStatus != BindingStatus.INVALID_SOURCE_VALUE) {
				try {
					buffer.setValue(coerseToValueType(parsed));
					sourceValue = null;
					bindingStatus = BindingStatus.DIRTY;
				} catch (ConversionFailedException e) {
					this.sourceValue = sourceValue;
					invalidSourceValueCause = e;
					bindingStatus = BindingStatus.INVALID_SOURCE_VALUE;
				}
			}
		} else {
			try {
				buffer.setValue(coerseToValueType(sourceValue));
				sourceValue = null;
				bindingStatus = BindingStatus.DIRTY;
			} catch (ConversionFailedException e) {
				this.sourceValue = sourceValue;
				invalidSourceValueCause = e;
				bindingStatus = BindingStatus.INVALID_SOURCE_VALUE;
			}
		}
	}

	public Object getInvalidSourceValue() {
		if (bindingStatus != BindingStatus.INVALID_SOURCE_VALUE) {
			throw new IllegalStateException("No invalid source value");
		}
		return sourceValue;
	}

	public BindingStatus getBindingStatus() {
		return bindingStatus;
	}

	public ValidationStatus getValidationStatus() {
		return ValidationStatus.NOT_VALIDATED;
	}

	public Alert getStatusAlert() {
		if (bindingStatus == BindingStatus.INVALID_SOURCE_VALUE) {
			return new AbstractAlert() {
				public String getCode() {
					return "typeMismatch";
				}

				public String getMessage() {
					MessageBuilder builder = new MessageBuilder(bindingContext.getMessageSource());
					builder.code(getCode());
					if (invalidSourceValueCause instanceof ParseException) {
						ParseException e = (ParseException) invalidSourceValueCause;
						builder.arg("label", bindingContext.getLabel());
						builder.arg("value", sourceValue);
						builder.arg("errorOffset", e.getErrorOffset());
						builder.defaultMessage("Failed to bind '" + bindingContext.getLabel() + "'; the source value "
								+ StylerUtils.style(sourceValue) + " has an invalid format and could no be parsed");
					} else {
						ConversionFailedException e = (ConversionFailedException) invalidSourceValueCause;
						builder.arg("label", new ResolvableArgument(bindingContext.getLabel()));
						builder.arg("value", sourceValue);
						builder.defaultMessage("Failed to bind '" + bindingContext.getLabel() + "'; the source value "
								+ StylerUtils.style(sourceValue) + " has could not be converted to "
								+ e.getTargetType().getName());

					}
					return builder.build();
				}

				public Severity getSeverity() {
					return Severity.ERROR;
				}
			};
		} else if (bindingStatus == BindingStatus.COMMIT_FAILURE) {
			return new AbstractAlert() {
				public String getCode() {
					return "internalError";
				}

				public String getMessage() {
					buffer.getFlushException().printStackTrace();
					return "Internal error occurred; message = [" + buffer.getFlushException().getMessage() + "]";
				}

				public Severity getSeverity() {
					return Severity.FATAL;
				}
			};
		} else if (bindingStatus == BindingStatus.COMMITTED) {
			return new AbstractAlert() {
				public String getCode() {
					return "bindSuccess";
				}

				public String getMessage() {
					return "Binding successful";
				}

				public Severity getSeverity() {
					return Severity.INFO;
				}
			};
		} else {
			return null;
		}
	}

	public void validate() {
	}

	public void commit() {
		assertEditable();
		assertEnabled();
		if (bindingStatus == BindingStatus.DIRTY) {
			buffer.flush();
			if (buffer.flushFailed()) {
				bindingStatus = BindingStatus.COMMIT_FAILURE;
			} else {
				bindingStatus = BindingStatus.COMMITTED;
			}
		} else {
			throw new IllegalStateException("Binding is not dirty; nothing to commit");
		}
	}

	public void revert() {
		if (bindingStatus == BindingStatus.INVALID_SOURCE_VALUE) {
			sourceValue = null;
			invalidSourceValueCause = null;
			bindingStatus = BindingStatus.CLEAN;
		} else if (bindingStatus == BindingStatus.DIRTY || bindingStatus == BindingStatus.COMMIT_FAILURE) {
			buffer.clear();
			bindingStatus = BindingStatus.CLEAN;
		} else {
			throw new IllegalStateException("Nothing to revert");
		}
	}

	public Binding getNestedBinding(String property) {
		return bindingContext.getBinding(property);
	}

	public boolean isList() {
		return List.class.isAssignableFrom(getValueType());
	}

	public Binding getListElementBinding(int index) {
		return bindingContext.getListElementBinding(index);
	}

	public boolean isMap() {
		return Map.class.isAssignableFrom(getValueType());
	}

	public Binding getMapValueBinding(Object key) {
		if (key instanceof String) {
			try {
				key = bindingContext.getKeyFormatter().parse((String) key, getLocale());
			} catch (ParseException e) {
				throw new IllegalArgumentException("Unable to parse map key '" + key + "'", e);
			}
		}
		return bindingContext.getMapValueBinding(key);
	}

	@SuppressWarnings("unchecked")
	public String formatValue(Object value) {
		Formatter formatter;
		if (Collection.class.isAssignableFrom(getValueType()) || getValueType().isArray() || isMap()) {
			formatter = bindingContext.getElementFormatter();
		} else {
			formatter = bindingContext.getFormatter();
		}
		return format(value, formatter);
	}

	// internal helpers

	@SuppressWarnings("unchecked")
	private String format(Object value, Formatter formatter) {
		Class<?> formattedType = getFormattedObjectType(formatter.getClass());
		value = bindingContext.getTypeConverter().convert(value, formattedType);
		return formatter.format(value, getLocale());
	}

	private Locale getLocale() {
		return LocaleContextHolder.getLocale();
	}

	@SuppressWarnings("unchecked")
	private Class getFormattedObjectType(Class formatterClass) {
		Class classToIntrospect = formatterClass;
		while (classToIntrospect != null) {
			Type[] ifcs = classToIntrospect.getGenericInterfaces();
			for (Type ifc : ifcs) {
				if (ifc instanceof ParameterizedType) {
					ParameterizedType paramIfc = (ParameterizedType) ifc;
					Type rawType = paramIfc.getRawType();
					if (Formatter.class.equals(rawType)) {
						Type arg = paramIfc.getActualTypeArguments()[0];
						if (arg instanceof TypeVariable) {
							arg = GenericTypeResolver.resolveTypeVariable((TypeVariable) arg, formatterClass);
						}
						if (arg instanceof Class) {
							return (Class) arg;
						}
					} else if (Formatter.class.isAssignableFrom((Class) rawType)) {
						return getFormattedObjectType((Class) rawType);
					}
				} else if (Formatter.class.isAssignableFrom((Class) ifc)) {
					return getFormattedObjectType((Class) ifc);
				}
			}
			classToIntrospect = classToIntrospect.getSuperclass();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private Object coerseToValueType(Object parsed) {
		TypeDescriptor targetType = valueModel.getValueTypeDescriptor();
		TypeConverter converter = bindingContext.getTypeConverter();
		if (parsed != null && converter.canConvert(parsed.getClass(), targetType)) {
			return converter.convert(parsed, targetType);
		} else {
			return parsed;
		}
	}

	private void assertEditable() {
		if (!isEditable()) {
			throw new IllegalStateException("Binding is not editable");
		}
	}

	private void assertEnabled() {
		if (!isEditable()) {
			throw new IllegalStateException("Binding is not enabled");
		}
	}

	static abstract class AbstractAlert implements Alert {
		public String toString() {
			return getCode() + " - " + getMessage();
		}
	}

}
