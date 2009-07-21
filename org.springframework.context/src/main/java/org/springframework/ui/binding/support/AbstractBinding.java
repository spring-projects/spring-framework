package org.springframework.ui.binding.support;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.text.ParseException;
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
import org.springframework.ui.binding.support.GenericBinder.BindingContext;
import org.springframework.ui.format.Formatter;
import org.springframework.ui.message.MessageBuilder;
import org.springframework.ui.message.ResolvableArgument;

public abstract class AbstractBinding implements Binding {

	private BindingContext bindingContext;

	private ValueBuffer buffer;

	private BindingStatus status;

	private Object sourceValue;
	
	private Exception invalidSourceValueCause;
	
	public AbstractBinding(BindingContext bindingContext) {
		this.bindingContext = bindingContext;
		buffer = new ValueBuffer(getValueModel());
		status = BindingStatus.CLEAN;		
	}

	// implementing Binding
	
	public String getRenderValue() {
		return format(getValue(), bindingContext.getFormatter());
	}

	public Object getValue() {
		if (status == BindingStatus.DIRTY || status == BindingStatus.COMMIT_FAILURE) {
			return buffer.getValue();
		} else {
			return getValueModel().getValue();
		}
	}

	public Class<?> getValueType() {
		return getValueModel().getValueType();
	}

	public boolean isEditable() {
		return bindingContext.getEditableCondition().isTrue();
	}

	public boolean isEnabled() {
		return bindingContext.getEnabledCondition().isTrue();
	}

	public boolean isVisible() {
		return bindingContext.getVisibleCondition().isTrue();
	}

	public void applySourceValue(Object sourceValue) {
		assertEditable();
		assertEnabled();
		if (sourceValue instanceof String) {
			try {
				Object parsed = bindingContext.getFormatter().parse((String) sourceValue, getLocale());
				buffer.setValue(coerseToValueType(parsed));
				sourceValue = null;
				status = BindingStatus.DIRTY;
			} catch (ParseException e) {
				this.sourceValue = sourceValue;
				invalidSourceValueCause = e;
				status = BindingStatus.INVALID_SOURCE_VALUE;
			} catch (ConversionFailedException e) {
				this.sourceValue = sourceValue;
				invalidSourceValueCause = e;
				status = BindingStatus.INVALID_SOURCE_VALUE;
			}
		} else if (sourceValue instanceof String[]) {
			String[] sourceValues = (String[]) sourceValue;
			Class<?> parsedType = getFormattedObjectType(bindingContext.getElementFormatter().getClass());
			if (parsedType == null) {
				parsedType = String.class;
			}
			Object parsed = Array.newInstance(parsedType, sourceValues.length);
			for (int i = 0; i < sourceValues.length; i++) {
				Object parsedValue;
				try {
					parsedValue = bindingContext.getElementFormatter().parse(sourceValues[i], getLocale());
					Array.set(parsed, i, parsedValue);
				} catch (ParseException e) {
					this.sourceValue = sourceValue;
					invalidSourceValueCause = e;
					status = BindingStatus.INVALID_SOURCE_VALUE;
					break;
				}
			}
			if (status != BindingStatus.INVALID_SOURCE_VALUE) {
				try {
					buffer.setValue(coerseToValueType(parsed));
					sourceValue = null;
					status = BindingStatus.DIRTY;
				} catch (ConversionFailedException e) {
					this.sourceValue = sourceValue;
					invalidSourceValueCause = e;
					status = BindingStatus.INVALID_SOURCE_VALUE;
				}
			}
		} else {
			try {
				buffer.setValue(coerseToValueType(sourceValue));
				sourceValue = null;
				status = BindingStatus.DIRTY;
			} catch (ConversionFailedException e) {
				this.sourceValue = sourceValue;
				invalidSourceValueCause = e;
				status = BindingStatus.INVALID_SOURCE_VALUE;
			}
		}
	}

	public Object getInvalidSourceValue() {
		if (status != BindingStatus.INVALID_SOURCE_VALUE) {
			throw new IllegalStateException("No invalid source value");
		}
		return sourceValue;
	}

	public BindingStatus getStatus() {
		return status;
	}

	public Alert getStatusAlert() {
		if (status == BindingStatus.INVALID_SOURCE_VALUE) {
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
		} else if (status == BindingStatus.COMMIT_FAILURE) {
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
		} else if (status == BindingStatus.COMMITTED) {
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

	public void commit() {
		assertEditable();
		assertEnabled();
		if (status == BindingStatus.DIRTY) {
			buffer.flush();
			if (buffer.flushFailed()) {
				status = BindingStatus.COMMIT_FAILURE;
			} else {
				status = BindingStatus.COMMITTED;
			}
		} else {
			throw new IllegalStateException("Binding is not dirty; nothing to commit");
		}
	}

	public void revert() {
		if (status == BindingStatus.INVALID_SOURCE_VALUE) {
			sourceValue = null;
			invalidSourceValueCause = null;
			status = BindingStatus.CLEAN;
		} else if (status == BindingStatus.DIRTY || status == BindingStatus.COMMIT_FAILURE) {
			buffer.clear();
			status = BindingStatus.CLEAN;
		} else {
			throw new IllegalStateException("Nothing to revert");
		}
	}

	public Binding getBinding(String property) {
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
		return bindingContext.getMapValueBinding(key);
	}

	@SuppressWarnings("unchecked")
	public String formatValue(Object value) {
		Formatter formatter;
		if (isList() || isMap()) {
			formatter = getBindingContext().getElementFormatter();
		} else {
			formatter = getBindingContext().getFormatter();
		}
		return format(value, formatter);
	}
	
	// subclassing hooks

	protected BindingContext getBindingContext() {
		return bindingContext;
	}

	protected abstract ValueModel getValueModel();
	
	protected Locale getLocale() {
		return LocaleContextHolder.getLocale();
	}
	
	protected String format(Object value, Formatter formatter) {
		Class<?> formattedType = getFormattedObjectType(formatter.getClass());
		value = bindingContext.getTypeConverter().convert(value, formattedType);
		return formatter.format(value, getLocale());
	}

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

	private Object coerseToValueType(Object parsed) {
		TypeDescriptor targetType = getValueModel().getValueTypeDescriptor();
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

	// internal helpers
	
	static abstract class AbstractAlert implements Alert {
		public String toString() {
			return getCode() + " - " + getMessage();
		}
	}

	/**
	 * For accessing the raw bound model object.
	 * @author Keith Donald
	 */
	public interface ValueModel {
		
		/**
		 * The model value.
		 */
		Object getValue();
		
		/**
		 * The model value type.
		 */
		Class<?> getValueType();		

		/**
		 * The model value type descriptor.
		 */
		TypeDescriptor<?> getValueTypeDescriptor();		

		/**
		 * Set the model value.
		 */
		void setValue(Object value);
	}

}
