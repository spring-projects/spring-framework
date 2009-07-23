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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.convert.TypeConverter;
import org.springframework.core.convert.support.DefaultTypeConverter;
import org.springframework.ui.binding.Binding;
import org.springframework.ui.binding.BindingFactory;
import org.springframework.ui.binding.binder.Binder;
import org.springframework.ui.binding.binder.BindingResult;
import org.springframework.ui.binding.config.BindingRuleConfiguration;
import org.springframework.ui.binding.config.Condition;
import org.springframework.ui.format.Formatter;
import org.springframework.util.Assert;

/**
 * A generic {@link Binder binder} suitable for use in most environments.
 * @author Keith Donald
 * @since 3.0
 * @see #setMessageSource(MessageSource)
 * @see #setTypeConverter(TypeConverter)
 * @see #bind(Map)
 */
public class GenericBindingFactory implements BindingFactory {

	private Object model;

	private Map<String, GenericBindingRule> bindingRules;

	private FormatterRegistry formatterRegistry;

	private TypeConverter typeConverter;

	private MessageSource messageSource;

	/**
	 * Creates a new binder for the model object.
	 * @param model the model object containing properties this binder will bind to
	 */
	public GenericBindingFactory(Object model) {
		Assert.notNull(model, "The model to bind to is required");
		this.model = model;
		bindingRules = new HashMap<String, GenericBindingRule>();
		formatterRegistry = new GenericFormatterRegistry();
		typeConverter = new DefaultTypeConverter();
	}

	/**
	 * Configures the registry of Formatters to query when no explicit Formatter has been registered for a Binding.
	 * Allows Formatters to be applied by property type and by property annotation.
	 * @param registry the formatter registry
	 */
	public void setFormatterRegistry(FormatterRegistry formatterRegistry) {
		Assert.notNull(formatterRegistry, "The FormatterRegistry is required");
		this.formatterRegistry = formatterRegistry;
	}

	/**
	 * Configure the MessageSource that resolves localized {@link BindingResult} alert messages.
	 * @param messageSource the message source
	 */
	public void setMessageSource(MessageSource messageSource) {
		Assert.notNull(messageSource, "The MessageSource is required");
		this.messageSource = messageSource;
	}

	/**
	 * Configure the TypeConverter that converts values as required by Binding setValue and getValue attempts.
	 * For a setValue attempt, the TypeConverter will be asked to perform a conversion if the value parsed by the Binding's Formatter is not assignable to the target property type.
	 * For a getValue attempt, the TypeConverter will be asked to perform a conversion if the property type does not match the type T required by the Binding's Formatter.
	 * @param typeConverter the type converter used by the binding system, which is based on Spring EL
	 */
	public void setTypeConverter(TypeConverter typeConverter) {
		Assert.notNull(typeConverter, "The TypeConverter is required");
		this.typeConverter = typeConverter;
	}

	/**
	 * Add a new binding rule for the property at the path specified.
	 * @param propertyPath binding rule property path in format prop.nestedProp
	 * @return a builder for the binding rule
	 */
	public BindingRuleConfiguration bindingRule(String propertyPath) {
		PropertyPath path = new PropertyPath(propertyPath);
		GenericBindingRule rule = getBindingRule(path.getFirstElement().getValue());
		for (PropertyPathElement element : path.getNestedElements()) {
			rule = rule.getBindingRule(element.getValue());
		}
		return rule;
	}

	// implementing Binder

	public Object getModel() {
		return model;
	}

	public Binding getBinding(String property) {
		PropertyPath path = new PropertyPath(property);
		Binding binding = getBindingRule(path.getFirstElement().getValue()).getBinding(model);
		for (PropertyPathElement element : path.getNestedElements()) {
			if (element.isIndex()) {
				if (binding.isMap()) {
					binding = binding.getMapValueBinding(element.getValue());
				} else if (binding.isList()) {
					binding = binding.getListElementBinding(element.getIntValue());
				} else {
					throw new IllegalArgumentException("Attempted to index a property that is not a List or Map");
				}
			} else {
				binding = binding.getNestedBinding(element.getValue());
			}
		}
		return binding;
	}

	private GenericBindingRule getBindingRule(String property) {
		GenericBindingRule rule = bindingRules.get(property);
		if (rule == null) {
			rule = new GenericBindingRule(property, model.getClass());
			bindingRules.put(property, rule);
		}
		return rule;
	}

	@SuppressWarnings("unchecked")
	class GenericBindingRule implements BindingRuleConfiguration, BindingContext {

		private Class<?> modelClass;

		private PropertyDescriptor property;

		private Formatter formatter;

		private Formatter keyFormatter;

		private Formatter elementFormatter;

		private Condition editableCondition = Condition.ALWAYS_TRUE;

		private Condition enabledCondition = Condition.ALWAYS_TRUE;

		private Condition visibleCondition = Condition.ALWAYS_TRUE;

		private Map<String, GenericBindingRule> nestedBindingRules;

		private Binding binding;

		private Map<Integer, Binding> listElementBindings;

		private Map<Object, Binding> mapValueBindings;
		
		public GenericBindingRule(String property, Class modelClass) {
			this.modelClass = modelClass;
			this.property = findPropertyDescriptor(property);
		}

		// implementing BindingContext

		public MessageSource getMessageSource() {
			return messageSource;
		}

		public TypeConverter getTypeConverter() {
			return typeConverter;
		}

		public Formatter<?> getFormatter() {
			if (formatter != null) {
				return formatter;
			} else {
				return formatterRegistry.getFormatter(property);
			}
		}

		public Formatter<?> getKeyFormatter() {
			if (keyFormatter != null) {
				return keyFormatter;
			} else {
				return formatterRegistry.getFormatter(getKeyType());
			}
		}

		public Formatter<?> getElementFormatter() {
			if (elementFormatter != null) {
				return formatter;
			} else {
				return formatterRegistry.getFormatter(getElementType());
			}
		}

		public Condition getEnabledCondition() {
			return enabledCondition;
		}

		public Condition getEditableCondition() {
			return editableCondition;
		}

		public Condition getVisibleCondition() {
			return visibleCondition;
		}

		public Binding getNestedBinding(String property) {
			createValueIfNecessary();
			return getBindingRule(property, binding.getValueType()).getBinding(binding.getValue());
		}

		public Binding getListElementBinding(int index) {
			if (listElementBindings == null) {
				listElementBindings = new HashMap<Integer, Binding>();
			}
			growListIfNecessary(index);
			Binding binding = listElementBindings.get(index);
			if (binding == null) {
				BindingContext bindingContext = new ListElementBindingContext(index, this);
				ValueModel valueModel = new ListElementValueModel(index, getElementType(), (List) this.binding.getValue());
				binding = new GenericBinding(valueModel, bindingContext);
				listElementBindings.put(index, binding);
			}
			return binding;
		}

		public Binding getMapValueBinding(Object key) {
			if (mapValueBindings == null) {
				mapValueBindings = new HashMap<Object, Binding>();
			}
			createMapValueIfNecessary();
			Binding binding = mapValueBindings.get(key);
			if (binding == null) {
				BindingContext bindingContext = new MapValueBindingContext(key, this);
				ValueModel valueModel = new MapValueValueModel(key, getElementType(), (Map) this.binding.getValue(), bindingContext);
				binding = new GenericBinding(valueModel, bindingContext);
				mapValueBindings.put(key, binding);
			}
			return binding;
		}

		public String getLabel() {
			return property.getName();
		}

		// implementing BindingRuleConfiguration

		public BindingRuleConfiguration formatWith(Formatter<?> formatter) {
			this.formatter = formatter;
			return this;
		}

		public BindingRuleConfiguration formatElementsWith(Formatter<?> formatter) {
			if (!List.class.isAssignableFrom(modelClass) || modelClass.isArray()) {
				throw new IllegalStateException(
						"Bound property is not a List or an array; cannot set a element formatter");
			}
			elementFormatter = formatter;
			return this;
		}

		public BindingRuleConfiguration formatKeysWith(Formatter<?> formatter) {
			if (!Map.class.isAssignableFrom(modelClass)) {
				throw new IllegalStateException("Bound property is not a Map; cannot set a key formatter");
			}
			keyFormatter = formatter;
			return this;
		}

		public BindingRuleConfiguration editableWhen(Condition condition) {
			editableCondition = condition;
			return this;
		}

		public BindingRuleConfiguration enabledWhen(Condition condition) {
			enabledCondition = condition;
			return this;
		}

		public BindingRuleConfiguration visibleWhen(Condition condition) {
			visibleCondition = condition;
			return this;
		}

		// internal helpers

		private Class<?> getElementType() {
			if (Map.class.isAssignableFrom(property.getPropertyType())) {
				return GenericCollectionTypeResolver.getMapValueReturnType(property.getReadMethod());				
			} else {
				return GenericCollectionTypeResolver.getCollectionReturnType(property.getReadMethod());				
			}
		}

		private Class<?> getKeyType() {
			return GenericCollectionTypeResolver.getMapKeyReturnType(property.getReadMethod());
		}

		GenericBindingRule getBindingRule(String property) {
			return getBindingRule(property, this.property.getPropertyType());
		}

		GenericBindingRule getBindingRule(String property, Class<?> modelClass) {
			if (nestedBindingRules == null) {
				nestedBindingRules = new HashMap<String, GenericBindingRule>();
			}
			GenericBindingRule rule = nestedBindingRules.get(property);
			if (rule == null) {
				rule = new GenericBindingRule(property, modelClass);
				nestedBindingRules.put(property, rule);
			}
			return rule;
		}

		// internal helpers

		Binding getBinding(Object model) {
			if (binding == null) {
				PropertyValueModel valueModel = new PropertyValueModel(property, model);
				binding = new GenericBinding(valueModel, this);
			}
			return binding;
		}

		private PropertyDescriptor findPropertyDescriptor(String property) {
			PropertyDescriptor[] propDescs = getBeanInfo(modelClass).getPropertyDescriptors();
			for (PropertyDescriptor propDesc : propDescs) {
				if (propDesc.getName().equals(property)) {
					return propDesc;
				}
			}
			throw new PropertyNotFoundException(property, modelClass);
		}

		private BeanInfo getBeanInfo(Class<?> clazz) {
			try {
				return Introspector.getBeanInfo(clazz);
			} catch (IntrospectionException e) {
				throw new IllegalStateException("Unable to introspect model type " + clazz);
			}
		}

		private void createValueIfNecessary() {
			Object value = binding.getValue();
			if (value == null) {
				value = newValue(binding.getValueType());
				binding.applySourceValue(value);
				binding.commit();
			}
		}

		private void createMapValueIfNecessary() {
			Object value = binding.getValue();
			if (value == null) {
				value = newMapValue(binding.getValueType());
				binding.applySourceValue(value);
				binding.commit();
			}			
		}
		
		private void growListIfNecessary(int index) {
			List list = (List) binding.getValue();
			if (list == null) {
				list = newListValue(binding.getValueType());
				binding.applySourceValue(list);
				binding.commit();
				list = (List) binding.getValue();
			}
			if (index >= list.size()) {
				for (int i = list.size(); i <= index; i++) {
					list.add(newValue(getElementType()));
				}
			}
		}

		private Map newMapValue(Class<?> type) {
			if (type.isInterface()) {
				return (Map) newValue(LinkedHashMap.class);
			} else {
				return (Map) newValue(type);
			}
		}

		private List newListValue(Class<?> type) {
			if (type.isInterface()) {
				return (List) newValue(ArrayList.class);
			} else {
				return (List) newValue(type);
			}
		}

		private Object newValue(Class<?> type) {
			try {
				return type.newInstance();
			} catch (InstantiationException e) {
				throw new IllegalStateException("Could not instantiate element of type [" + type.getName() + "]", e);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException("Could not instantiate element of type [" + type.getName() + "]", e);
			}
		}

	}

	private static class ListElementBindingContext implements BindingContext {
		
		private int index;

		private GenericBindingRule listBindingContext;
		
		final Map<String, Binding> nestedBindings = new HashMap<String, Binding>();
		
		public ListElementBindingContext(int index, GenericBindingRule listBindingContext) {
			this.index = index;
			this.listBindingContext = listBindingContext;
		}
		
		public MessageSource getMessageSource() {
			return listBindingContext.getMessageSource();
		}

		public TypeConverter getTypeConverter() {
			return listBindingContext.getTypeConverter();
		}

		public Binding getNestedBinding(String property) {
			Object model = ((List<?>) listBindingContext.binding.getValue()).get(index);
			Class<?> elementType = listBindingContext.getElementType();
			if (elementType == null) {
				elementType = model.getClass();
			}
			GenericBindingRule rule = listBindingContext.getBindingRule(property, elementType);
			Binding binding = nestedBindings.get(property);
			if (binding == null) {
				PropertyValueModel valueModel = new PropertyValueModel(rule.property, model);
				binding = new GenericBinding(valueModel, rule);
				nestedBindings.put(property, binding);
			}
			return binding;
		}

		@SuppressWarnings("unchecked")
		public Formatter getFormatter() {
			return listBindingContext.getElementFormatter();
		}

		@SuppressWarnings("unchecked")
		public Formatter getElementFormatter() {
			return null;
		}

		@SuppressWarnings("unchecked")
		public Formatter getKeyFormatter() {
			return null;
		}

		public Condition getEditableCondition() {
			return listBindingContext.getEditableCondition();
		}

		public Condition getEnabledCondition() {
			return listBindingContext.getEnabledCondition();
		}

		public Condition getVisibleCondition() {
			return listBindingContext.getVisibleCondition();
		}

		public Binding getListElementBinding(int index) {
			throw new IllegalArgumentException("Not yet supported");
		}

		public Binding getMapValueBinding(Object key) {
			throw new IllegalArgumentException("Not yet supported");
		}

		public String getLabel() {
			return listBindingContext.getLabel() + "[" + index + "]";
		}

	};
	
	private static class MapValueBindingContext implements BindingContext {
		
		private Object key;

		private GenericBindingRule mapBindingContext;
		
		final Map<String, Binding> nestedBindings = new HashMap<String, Binding>();
		
		public MapValueBindingContext(Object key, GenericBindingRule mapBindingContext) {
			this.key = key;
			this.mapBindingContext = mapBindingContext;
		}
		
		public MessageSource getMessageSource() {
			return mapBindingContext.getMessageSource();
		}

		public TypeConverter getTypeConverter() {
			return mapBindingContext.getTypeConverter();
		}

		@SuppressWarnings("unchecked")
		public Binding getNestedBinding(String property) {
			Object model = ((Map) mapBindingContext.binding.getValue()).get(key);
			Class<?> elementType = mapBindingContext.getElementType();
			if (elementType == null) {
				elementType = model.getClass();
			}
			GenericBindingRule rule = mapBindingContext.getBindingRule(property, elementType);
			Binding binding = nestedBindings.get(property);
			if (binding == null) {
				PropertyValueModel valueModel = new PropertyValueModel(rule.property, model);
				binding = new GenericBinding(valueModel, rule);
				nestedBindings.put(property, binding);
			}
			return binding;
		}

		@SuppressWarnings("unchecked")
		public Formatter getFormatter() {
			return mapBindingContext.getElementFormatter();
		}

		@SuppressWarnings("unchecked")
		public Formatter getElementFormatter() {
			return null;
		}

		@SuppressWarnings("unchecked")
		public Formatter getKeyFormatter() {
			return null;
		}

		public Condition getEditableCondition() {
			return mapBindingContext.getEditableCondition();
		}

		public Condition getEnabledCondition() {
			return mapBindingContext.getEnabledCondition();
		}

		public Condition getVisibleCondition() {
			return mapBindingContext.getVisibleCondition();
		}

		public Binding getListElementBinding(int index) {
			throw new IllegalArgumentException("Not yet supported");
		}

		public Binding getMapValueBinding(Object key) {
			throw new IllegalArgumentException("Not yet supported");
		}

		public String getLabel() {
			return mapBindingContext.getLabel() + "[" + key + "]";
		}

	};
}
