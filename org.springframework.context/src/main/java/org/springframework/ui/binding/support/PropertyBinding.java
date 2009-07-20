/**
 * 
 */
package org.springframework.ui.binding.support;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
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
import org.springframework.util.ReflectionUtils;

@SuppressWarnings("unchecked")
public class PropertyBinding implements Binding {

	private PropertyDescriptor property;

	private Object object;

	private BindingContext bindingContext;

	private Object sourceValue;

	private Exception invalidSourceValueCause;

	private ValueBuffer buffer;

	private BindingStatus status;

	private Map<Integer, ListElementBinding> listElementBindings;

	private Class<?> elementType;
	
	public PropertyBinding(PropertyDescriptor property, Object object, BindingContext bindingContext) {
		this.property = property;
		this.object = object;
		this.bindingContext = bindingContext;
		buffer = new ValueBuffer(getModel());
		status = BindingStatus.CLEAN;
		if (isList()) {
			listElementBindings = new HashMap<Integer, ListElementBinding>();
			elementType = GenericCollectionTypeResolver.getCollectionReturnType(property.getReadMethod());
		}
	}

	public String getRenderValue() {
		return format(getValue(), getFormatter());
	}

	public Object getValue() {
		if (status == BindingStatus.DIRTY || status == BindingStatus.COMMIT_FAILURE) {
			return buffer.getValue();
		} else {
			return getModel().getValue();
		}
	}

	public Class<?> getValueType() {
		return getModel().getValueType();
	}

	public boolean isEditable() {
		return isWriteableProperty() && bindingContext.getEditableCondition().isTrue();
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
				Object parsed = getFormatter().parse((String) sourceValue, getLocale());
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
						builder.arg("label", new ResolvableArgument(property.getName()));
						builder.arg("value", sourceValue);
						builder.arg("errorOffset", e.getErrorOffset());
						builder.defaultMessage("Failed to bind to property '" + property + "'; the user value "
								+ StylerUtils.style(sourceValue) + " has an invalid format and could no be parsed");
					} else {
						ConversionFailedException e = (ConversionFailedException) invalidSourceValueCause;
						builder.arg("label", new ResolvableArgument(property.getName()));
						builder.arg("value", sourceValue);
						builder.defaultMessage("Failed to bind to property '" + property + "'; the user value "
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
					return "Internal error occurred; message = [" + buffer.getFlushException().getMessage() + "]";
				}

				public Severity getSeverity() {
					return Severity.FATAL;
				}
			};
		} else if (status == BindingStatus.COMMITTED) {
			return new AbstractAlert() {
				public String getCode() {
					return "bindSucces";
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

	public Model getModel() {
		return new Model() {
			public Object getValue() {
				return ReflectionUtils.invokeMethod(property.getReadMethod(), object);
			}

			public Class<?> getValueType() {
				return property.getPropertyType();
			}
			
			public TypeDescriptor<?> getValueTypeDescriptor() {
				return new TypeDescriptor(new MethodParameter(property.getReadMethod(), -1));
			}

			public void setValue(Object value) {
				ReflectionUtils.invokeMethod(property.getWriteMethod(), object, value);
			}
		};
	}

	public Binding getBinding(String property) {
		assertScalarProperty();
		if (getValue() == null) {
			createValue();
		}
		return bindingContext.getBinding(property, getValue());
	}

	public boolean isList() {
		return List.class.isAssignableFrom(getValueType());
	}

	public Binding getListElementBinding(int index) {
		assertListProperty();
		ListElementBinding binding = listElementBindings.get(index);
		if (binding == null) {
			binding = new ListElementBinding(index);
			listElementBindings.put(index, binding);
		}
		return binding;
	}

	public boolean isMap() {
		return Map.class.isAssignableFrom(getValueType());
	}

	public Binding getMapValueBinding(Object key) {
		assertMapProperty();
		if (key instanceof String) {
			try {
				key = bindingContext.getKeyFormatter().parse((String) key, getLocale());
			} catch (ParseException e) {
				throw new IllegalArgumentException("Invald key", e);
			}
		}
		//TODO return new KeyedPropertyBinding(key, (Map) getValue(), getMapTypeDescriptor());
		return null;
	}

	public String formatValue(Object value) {
		Formatter formatter;
		if (isList() || isMap()) {
			formatter = bindingContext.getElementFormatter();
		} else {
			formatter = bindingContext.getFormatter();
		}
		return format(value, formatter);
	}

	private String format(Object value, Formatter formatter) {
		Class<?> formattedType = getFormattedObjectType(formatter.getClass());
		value = bindingContext.getTypeConverter().convert(value, formattedType);
		return formatter.format(value, getLocale());
	}

	// internal helpers

	protected Formatter getFormatter() {
		return bindingContext.getFormatter();
	}
	
	private Locale getLocale() {
		return LocaleContextHolder.getLocale();
	}

	private void createValue() {
		try {
			Object value = getModel().getValueType().newInstance();
			getModel().setValue(value);
		} catch (InstantiationException e) {
			throw new IllegalStateException("Could not lazily instantiate object of type ["
					+ getModel().getValueType().getName() + "] to access property" + property, e);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Could not lazily instantiate object of type ["
					+ getModel().getValueType().getName() + "] to access property" + property, e);
		}
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
		TypeDescriptor targetType = getModel().getValueTypeDescriptor();
		TypeConverter converter = bindingContext.getTypeConverter();
		if (parsed != null && converter.canConvert(parsed.getClass(), targetType)) {
			return converter.convert(parsed, targetType);
		} else {
			return parsed;
		}
	}

	private void assertScalarProperty() {
		if (isList()) {
			throw new IllegalArgumentException("Is a Collection but should be a scalar");
		}
		if (isMap()) {
			throw new IllegalArgumentException("Is a Map but should be a scalar");
		}
	}

	private void assertListProperty() {
		if (!isList()) {
			throw new IllegalStateException("Not a List property binding");
		}
	}

	private void assertMapProperty() {
		if (!isList()) {
			throw new IllegalStateException("Not a Map property binding");
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

	private boolean isWriteableProperty() {
		return property.getWriteMethod() != null;
	}

	static abstract class AbstractAlert implements Alert {
		public String toString() {
			return getCode() + " - " + getMessage();
		}
	}
	
	class ListElementBinding extends PropertyBinding {

		private int index;

		public ListElementBinding(int index) {
			super(property, object, bindingContext);
			this.index = index;
			growListIfNecessary();
		}

		protected Formatter getFormatter() {
			return bindingContext.getElementFormatter();
		}
			
		public Model getModel() {
			return new Model() {
				public Object getValue() {
					return getList().get(index);
				}

				public Class<?> getValueType() {
					if (elementType != null) {
						return elementType;
					} else {
						return getValue().getClass();
					}
				}
				
				public TypeDescriptor<?> getValueTypeDescriptor() {
					return TypeDescriptor.valueOf(getValueType());
				}

				public void setValue(Object value) {
					getList().set(index, value);
				}
			};
		}
		
		// internal helpers
		
		private void growListIfNecessary() {
			if (index >= getList().size()) {
				for (int i = getList().size(); i <= index; i++) {
					addValue();
				}
			}
		}
		
		private List getList() {
			return (List) PropertyBinding.this.getValue();
		}

		private void addValue() {
			try {
				Object value = getValueType().newInstance();
				getList().add(value);
			} catch (InstantiationException e) {
				throw new IllegalStateException("Could not lazily instantiate model of type ["
						+ getValueType().getName() + "] to grow List", e);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException("Could not lazily instantiate model of type ["
						+ getValueType().getName() + "] to grow List", e);
			}
		}

	}

}