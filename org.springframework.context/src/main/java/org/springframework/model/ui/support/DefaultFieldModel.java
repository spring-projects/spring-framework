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
package org.springframework.model.ui.support;

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
import org.springframework.model.alert.Alert;
import org.springframework.model.alert.Severity;
import org.springframework.model.message.MessageBuilder;
import org.springframework.model.message.ResolvableArgument;
import org.springframework.model.ui.BindingStatus;
import org.springframework.model.ui.FieldModel;
import org.springframework.model.ui.ValidationStatus;
import org.springframework.model.ui.format.Formatter;

/**
 * Default FieldModel implementation suitable for use in most environments.
 * @author Keith Donald
 * @since 3.0
 */
public class DefaultFieldModel implements FieldModel {

	private ValueModel valueModel;

	private FieldModelContext context;

	private ValueBuffer buffer;

	private BindingStatus bindingStatus;

	private Object submittedValue;

	private Exception invalidSubmittedValueCause;

	public DefaultFieldModel(ValueModel valueModel, FieldModelContext context) {
		this.valueModel = valueModel;
		this.context = context;
		buffer = new ValueBuffer(valueModel);
		bindingStatus = BindingStatus.CLEAN;
	}

	// implementing FieldModel

	public String getRenderValue() {
		return format(getValue(), context.getFormatter());
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
		return valueModel.isWriteable() && context.getEditableCondition().isTrue();
	}

	public boolean isEnabled() {
		return context.getEnabledCondition().isTrue();
	}

	public boolean isVisible() {
		return context.getVisibleCondition().isTrue();
	}

	@SuppressWarnings("unchecked")
	public void applySubmittedValue(Object submittedValue) {
		assertEditable();
		assertEnabled();
		if (submittedValue instanceof String) {
			try {
				Object parsed = context.getFormatter().parse((String) submittedValue, getLocale());
				buffer.setValue(coerseToValueType(parsed));
				submittedValue = null;
				bindingStatus = BindingStatus.DIRTY;
			} catch (ParseException e) {
				this.submittedValue = submittedValue;
				invalidSubmittedValueCause = e;
				bindingStatus = BindingStatus.INVALID_SUBMITTED_VALUE;
			} catch (ConversionFailedException e) {
				this.submittedValue = submittedValue;
				invalidSubmittedValueCause = e;
				bindingStatus = BindingStatus.INVALID_SUBMITTED_VALUE;
			}
		} else if (submittedValue instanceof String[]) {
			Object parsed;
			if (isMap()) {
				String[] sourceValues = (String[]) submittedValue;
				Formatter keyFormatter = context.getKeyFormatter();
				Formatter valueFormatter = context.getElementFormatter();
				Map map = new LinkedHashMap(sourceValues.length);
				for (int i = 0; i < sourceValues.length; i++) {
					String entryString = sourceValues[i];
					try {
						String[] keyValue = entryString.split("=");
						Object parsedMapKey = keyFormatter.parse(keyValue[0], getLocale());
						Object parsedMapValue = valueFormatter.parse(keyValue[1], getLocale());
						map.put(parsedMapKey, parsedMapValue);
					} catch (ParseException e) {
						this.submittedValue = submittedValue;
						invalidSubmittedValueCause = e;
						bindingStatus = BindingStatus.INVALID_SUBMITTED_VALUE;
						break;
					}
				}
				parsed = map;
			} else {
				String[] sourceValues = (String[]) submittedValue;
				List list = new ArrayList(sourceValues.length);
				for (int i = 0; i < sourceValues.length; i++) {
					Object parsedValue;
					try {
						parsedValue = context.getElementFormatter().parse(sourceValues[i], getLocale());
						list.add(parsedValue);
					} catch (ParseException e) {
						this.submittedValue = submittedValue;
						invalidSubmittedValueCause = e;
						bindingStatus = BindingStatus.INVALID_SUBMITTED_VALUE;
						break;
					}
				}
				parsed = list;
			}
			if (bindingStatus != BindingStatus.INVALID_SUBMITTED_VALUE) {
				try {
					buffer.setValue(coerseToValueType(parsed));
					submittedValue = null;
					bindingStatus = BindingStatus.DIRTY;
				} catch (ConversionFailedException e) {
					this.submittedValue = submittedValue;
					invalidSubmittedValueCause = e;
					bindingStatus = BindingStatus.INVALID_SUBMITTED_VALUE;
				}
			}
		} else {
			try {
				buffer.setValue(coerseToValueType(submittedValue));
				submittedValue = null;
				bindingStatus = BindingStatus.DIRTY;
			} catch (ConversionFailedException e) {
				this.submittedValue = submittedValue;
				invalidSubmittedValueCause = e;
				bindingStatus = BindingStatus.INVALID_SUBMITTED_VALUE;
			}
		}
	}

	public Object getInvalidSubmittedValue() {
		if (bindingStatus != BindingStatus.INVALID_SUBMITTED_VALUE) {
			throw new IllegalStateException("No invalid submitted value applied to this field");
		}
		return submittedValue;
	}

	public BindingStatus getBindingStatus() {
		return bindingStatus;
	}

	public ValidationStatus getValidationStatus() {
		// TODO implementation
		return ValidationStatus.NOT_VALIDATED;
	}

	public Alert getStatusAlert() {
		if (bindingStatus == BindingStatus.INVALID_SUBMITTED_VALUE) {
			return new AbstractAlert() {
				public String getCode() {
					return "typeMismatch";
				}

				public String getMessage() {
					MessageBuilder builder = new MessageBuilder(context.getMessageSource());
					builder.code(getCode());
					if (invalidSubmittedValueCause instanceof ParseException) {
						ParseException e = (ParseException) invalidSubmittedValueCause;
						builder.arg("label", context.getLabel());
						builder.arg("value", submittedValue);
						builder.arg("errorOffset", e.getErrorOffset());
						builder.defaultMessage("Failed to bind '" + context.getLabel() + "'; the submitted value "
								+ StylerUtils.style(submittedValue) + " has an invalid format and could no be parsed");
					} else {
						ConversionFailedException e = (ConversionFailedException) invalidSubmittedValueCause;
						builder.arg("label", new ResolvableArgument(context.getLabel()));
						builder.arg("value", submittedValue);
						builder.defaultMessage("Failed to bind '" + context.getLabel() + "'; the submitted value "
								+ StylerUtils.style(submittedValue) + " has could not be converted to "
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
		// TODO implementation
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
			throw new IllegalStateException("Field is not dirty; nothing to commit");
		}
	}

	public void revert() {
		if (bindingStatus == BindingStatus.INVALID_SUBMITTED_VALUE) {
			submittedValue = null;
			invalidSubmittedValueCause = null;
			bindingStatus = BindingStatus.CLEAN;
		} else if (bindingStatus == BindingStatus.DIRTY || bindingStatus == BindingStatus.COMMIT_FAILURE) {
			buffer.clear();
			bindingStatus = BindingStatus.CLEAN;
		} else {
			throw new IllegalStateException("Field is clean or committed; nothing to revert");
		}
	}

	public FieldModel getNested(String fieldName) {
		return context.getNested(fieldName);
	}

	public boolean isList() {
		return getValueType().isArray() || List.class.isAssignableFrom(getValueType());
	}

	public FieldModel getListElement(int index) {
		return context.getListElement(index);
	}

	public boolean isMap() {
		return Map.class.isAssignableFrom(getValueType());
	}

	public FieldModel getMapValue(Object key) {
		if (key instanceof String) {
			try {
				key = context.getKeyFormatter().parse((String) key, getLocale());
			} catch (ParseException e) {
				throw new IllegalArgumentException("Unable to parse map key '" + key + "'", e);
			}
		}
		return context.getMapValue(key);
	}

	@SuppressWarnings("unchecked")
	public String formatValue(Object value) {
		Formatter formatter;
		if (Collection.class.isAssignableFrom(getValueType()) || getValueType().isArray() || isMap()) {
			formatter = context.getElementFormatter();
		} else {
			formatter = context.getFormatter();
		}
		return format(value, formatter);
	}

	// internal helpers

	@SuppressWarnings("unchecked")
	private String format(Object value, Formatter formatter) {
		Class<?> formattedType = getFormattedObjectType(formatter.getClass());
		value = context.getTypeConverter().convert(value, formattedType);
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
		TypeConverter converter = context.getTypeConverter();
		if (parsed != null && converter.canConvert(parsed.getClass(), targetType)) {
			return converter.convert(parsed, targetType);
		} else {
			return parsed;
		}
	}

	private void assertEditable() {
		if (!isEditable()) {
			throw new IllegalStateException("Field is not editable");
		}
	}

	private void assertEnabled() {
		if (!isEditable()) {
			throw new IllegalStateException("Field is not enabled");
		}
	}

	static abstract class AbstractAlert implements Alert {
		public String toString() {
			return getCode() + " - " + getMessage();
		}
	}

}
