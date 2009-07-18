/**
 * 
 */
package org.springframework.ui.binding.support;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeConverter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.ui.alert.Alert;
import org.springframework.ui.binding.Binding;
import org.springframework.ui.format.Formatter;
import org.springframework.util.ReflectionUtils;

@SuppressWarnings("unchecked")
public class PropertyBinding implements Binding {

	private String property;

	private Object model;

	private Formatter valueFormatter = DefaultFormatter.INSTANCE;

	private Formatter mapKeyFormatter = DefaultFormatter.INSTANCE;

	private Formatter indexedValueFormatter = DefaultFormatter.INSTANCE;

	private PropertyDescriptor propertyDescriptor;

	private TypeConverter typeConverter;

	private Object sourceValue;
	
	// TODO make a ValueBuffer
	private Object bufferedValue;
	
	public PropertyBinding(String property, Object model, TypeConverter typeConverter) {
		this.propertyDescriptor = findPropertyDescriptor(property, model);
		this.property = property;
		this.model = model;
		this.typeConverter = typeConverter;
	}

	public Object getValue() {
		if (isDirty()) {
			// TODO null check isn't good enough
			if (bufferedValue != null) {
				return formatValue(bufferedValue);
			} else {
				return sourceValue;
			}
		} else {
			return formatValue(getModel().getValue());
		}
	}

	public void applySourceValue(Object sourceValue) {
		if (isReadOnly()) {
			throw new IllegalStateException("Property is read-only");
		}
		this.sourceValue = sourceValue;
		if (sourceValue instanceof String) {
			try { 
				this.bufferedValue = valueFormatter.parse((String) sourceValue, getLocale());
			} catch (ParseException e) {
				
			}
		} else if (sourceValue instanceof String[]) {
			String[] sourceValues = (String[]) sourceValue;
			Class<?> parsedType = getFormattedObjectType(indexedValueFormatter.getClass());
			if (parsedType == null) {
				parsedType = String.class;
			}
			Object parsed = Array.newInstance(parsedType, sourceValues.length);
			boolean parseError = false;
			for (int i = 0; i < sourceValues.length; i++) {
				Object parsedValue;
				try {
					parsedValue = indexedValueFormatter.parse(sourceValues[i], LocaleContextHolder.getLocale());
					Array.set(parsed, i, parsedValue);
				} catch (ParseException e) {
					parseError = true;
				}
			}
			if (!parseError) {
				bufferedValue = parsed;
			}
		}
	}

	public boolean isDirty() {
		return sourceValue != null || bufferedValue != null;
	}

	public boolean isValid() {
		if (!isDirty()) {
			return true;
		} else {
			if (bufferedValue == null) {
				return false;
			} else {
				return true;
			}
		}
	}

	public void commit() {
		if (!isDirty()) {
			throw new IllegalStateException("Binding not dirty; nothing to commit");
		}
		if (!isValid()) {
			throw new IllegalStateException("Binding is invalid; only commit valid bindings");
		}
		try {
			getModel().setValue(bufferedValue);
			this.bufferedValue = null;
		} catch (Exception e) {
			
		}
	}

	public Model getModel() {
		return new Model() {		
			public Object getValue() {
				return ReflectionUtils.invokeMethod(propertyDescriptor.getReadMethod(), model);
			}

			public Class<?> getValueType() {
				return propertyDescriptor.getPropertyType();
			}
			
			public void setValue(Object value) {
				if (isReadOnly()) {
					throw new IllegalStateException("Property is read-only");
				}						
				TypeDescriptor targetType = new TypeDescriptor(new MethodParameter(propertyDescriptor.getWriteMethod(), 0));
				if (value != null && typeConverter.canConvert(value.getClass(), targetType)) {
					value = typeConverter.convert(value, targetType);					
				}
				ReflectionUtils.invokeMethod(propertyDescriptor.getWriteMethod(), model, value);
			}
		};
	}

	public Alert getStatusAlert() {
		return null;
	}

	public Binding getBinding(String nestedProperty) {
		assertScalarProperty();
		if (getValue() == null) {
			createValue();
		}
		return new PropertyBinding(nestedProperty, getValue(), typeConverter);
	}

	public boolean isIndexable() {
		return getModel().getValueType().isArray() || List.class.isAssignableFrom(getModel().getValueType());
	}

	public Binding getIndexedBinding(int index) {
		assertListProperty();
		//return new IndexedBinding(index, (List) getValue(), getCollectionTypeDescriptor(), typeConverter);
		return null;
	}

	public boolean isMap() {
		return Map.class.isAssignableFrom(getModel().getValueType());
	}

	public Binding getKeyedBinding(Object key) {
		assertMapProperty();
		if (key instanceof String) {
			try {
				key = mapKeyFormatter.parse((String) key, getLocale());
			} catch (ParseException e) {
				throw new IllegalArgumentException("Invald key", e);
			}
		}
		//TODO return new KeyedPropertyBinding(key, (Map) getValue(), getMapTypeDescriptor());
		return null;
	}

	public boolean isReadOnly() {
		return propertyDescriptor.getWriteMethod() != null && !markedNotEditable();
	}

	public String formatValue(Object value) {
		Class<?> formattedType = getFormattedObjectType(valueFormatter.getClass());
		value = typeConverter.convert(value, formattedType);
		return valueFormatter.format(value, getLocale());
	}

	// internal helpers

	private PropertyDescriptor findPropertyDescriptor(String property, Object model) {
		PropertyDescriptor[] propDescs = getBeanInfo(model.getClass()).getPropertyDescriptors();
		for (PropertyDescriptor propDesc : propDescs) {
			if (propDesc.getName().equals(property)) {
				return propDesc;
			}
		}
		throw new IllegalArgumentException("No property '" + property + "' found on model ["
				+ model.getClass().getName() + "]");
	}

	private BeanInfo getBeanInfo(Class<?> clazz) {
		try {
			return Introspector.getBeanInfo(clazz);
		} catch (IntrospectionException e) {
			throw new IllegalStateException("Unable to introspect model type " + clazz);
		}
	}

	private Locale getLocale() {
		return LocaleContextHolder.getLocale();
	}

	private void createValue() {
		try {
			Object value = getModel().getValueType().newInstance();
			getModel().setValue(value);
		} catch (InstantiationException e) {
			throw new IllegalStateException("Could not lazily instantiate model of type [" + getModel().getValueType().getName()
					+ "] to access property" + property, e);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Could not lazily instantiate model of type [" + getModel().getValueType().getName()
					+ "] to access property" + property, e);
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

	@SuppressWarnings("unused")
	private CollectionTypeDescriptor getCollectionTypeDescriptor() {
		Class<?> elementType = GenericCollectionTypeResolver.getCollectionReturnType(propertyDescriptor
				.getReadMethod());
		return new CollectionTypeDescriptor(getModel().getValueType(), elementType);
	}

	private void assertScalarProperty() {
		if (isIndexable()) {
			throw new IllegalArgumentException("Is a Collection but should be a scalar");
		}
		if (isMap()) {
			throw new IllegalArgumentException("Is a Map but should be a scalar");
		}
	}

	private void assertListProperty() {
		if (!isIndexable()) {
			throw new IllegalStateException("Not a List property binding");
		}
	}

	private void assertMapProperty() {
		if (!isIndexable()) {
			throw new IllegalStateException("Not a Map property binding");
		}
	}

	private boolean markedNotEditable() {
		return false;
	}

}