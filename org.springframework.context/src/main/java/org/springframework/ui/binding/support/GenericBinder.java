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
import java.util.List;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.convert.TypeConverter;
import org.springframework.core.convert.support.DefaultTypeConverter;
import org.springframework.ui.binding.Binder;
import org.springframework.ui.binding.Binding;
import org.springframework.ui.binding.BindingResult;
import org.springframework.ui.binding.BindingResults;
import org.springframework.ui.binding.MissingSourceValuesException;
import org.springframework.ui.binding.Binding.BindingStatus;
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
public class GenericBinder implements Binder {

	private Object model;

	private Map<String, GenericBindingRule> bindingRules;

	private FormatterRegistry formatterRegistry;

	private TypeConverter typeConverter;

	private MessageSource messageSource;

	private String[] requiredProperties = new String[0];

	/**
	 * Creates a new binder for the model object.
	 * @param model the model object containing properties this binder will bind to
	 */
	public GenericBinder(Object model) {
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
	 * 
	 * @param propertyPath binding rule property path in format prop.nestedProp
	 * @return
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
		System.out.println(path);
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
				binding = binding.getBinding(element.getValue());
			}
		}
		return binding;
	}

	public BindingResults bind(Map<String, ? extends Object> sourceValues) {
		sourceValues = filter(sourceValues);
		checkRequired(sourceValues);
		ArrayListBindingResults results = new ArrayListBindingResults(sourceValues.size());
		for (Map.Entry<String, ? extends Object> sourceValue : sourceValues.entrySet()) {
			try {
				Binding binding = getBinding(sourceValue.getKey());
				results.add(bind(sourceValue, binding));
			} catch (PropertyNotFoundException e) {
				results.add(new PropertyNotFoundResult(sourceValue.getKey(), sourceValue.getValue(), messageSource));
			}
		}
		return results;
	}

	// subclassing hooks

	/**
	 * Hook subclasses may use to filter the source values to bind.
	 * This hook allows the binder to pre-process the source values before binding occurs.
	 * For example, a Binder might insert empty or default values for fields that are not present.
	 * As another example, a Binder might collapse multiple source values into a single source value. 
	 * @param sourceValues the original source values map provided by the caller
	 * @return the filtered source values map that will be used to bind
	 */
	protected Map<String, ? extends Object> filter(Map<String, ? extends Object> sourceValues) {
		return sourceValues;
	}

	// internal helpers

	private void checkRequired(Map<String, ? extends Object> sourceValues) {
		List<String> missingRequired = new ArrayList<String>();
		for (String required : requiredProperties) {
			boolean found = false;
			for (String property : sourceValues.keySet()) {
				if (property.equals(required)) {
					found = true;
				}
			}
			if (!found) {
				missingRequired.add(required);
			}
		}
		if (!missingRequired.isEmpty()) {
			throw new MissingSourceValuesException(missingRequired, sourceValues);
		}
	}

	private GenericBindingRule getBindingRule(String property) {
		GenericBindingRule rule = bindingRules.get(property);
		if (rule == null) {
			rule = new GenericBindingRule(property, model.getClass());
			bindingRules.put(property, rule);
		}
		return rule;
	}

	private BindingResult bind(Map.Entry<String, ? extends Object> sourceValue, Binding binding) {
		String property = sourceValue.getKey();
		Object value = sourceValue.getValue();
		if (!binding.isEditable()) {
			return new PropertyNotEditableResult(property, value, messageSource);
		} else {
			binding.applySourceValue(value);
			if (binding.getStatus() == BindingStatus.DIRTY) {
				binding.commit();
			}
			return new BindingStatusResult(property, value, binding.getStatusAlert());
		}
	}

	@SuppressWarnings("unchecked")
	class GenericBindingRule implements BindingRuleConfiguration, BindingContext {

		private Class<?> modelClass;

		private PropertyDescriptor property;

		private Formatter formatter;

		private Formatter elementFormatter;

		private Formatter keyFormatter;

		private Condition editableCondition = Condition.ALWAYS_TRUE;

		private Condition enabledCondition = Condition.ALWAYS_TRUE;

		private Condition visibleCondition = Condition.ALWAYS_TRUE;

		private Map<String, GenericBindingRule> nestedBindingRules;

		private Binding binding;

		private Map<Integer, Binding> listElementBindings;

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

		public Formatter<?> getElementFormatter() {
			if (elementFormatter != null) {
				return formatter;
			} else {
				return formatterRegistry.getFormatter(getElementType());
			}
		}

		public Formatter<?> getKeyFormatter() {
			if (keyFormatter != null) {
				return keyFormatter;
			} else {
				return formatterRegistry.getFormatter(getKeyType());
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

		public Binding getBinding(String property) {
			createValueIfNecessary();
			return getBindingRule(property, binding.getValueType()).getBinding(binding.getValue());
		}

		public Binding getListElementBinding(final int index) {
			if (listElementBindings == null) {
				listElementBindings = new HashMap<Integer, Binding>();
			}
			growListIfNecessary(index);
			Binding binding = listElementBindings.get(index);
			if (binding == null) {
				BindingContext listContext = new BindingContext() {
					public MessageSource getMessageSource() {
						return GenericBindingRule.this.getMessageSource();
					}

					public TypeConverter getTypeConverter() {
						return GenericBindingRule.this.getTypeConverter();
					}

					public Binding getBinding(String property) {
						Object model = ((List) GenericBindingRule.this.binding.getValue()).get(index);
						return GenericBindingRule.this.getBindingRule(property, getElementType()).getBinding(model);
					}

					public Formatter getFormatter() {
						return GenericBindingRule.this.getElementFormatter();
					}

					public Formatter getElementFormatter() {
						return null;
					}

					public Formatter getKeyFormatter() {
						return null;
					}

					public Condition getEditableCondition() {
						return GenericBindingRule.this.getEditableCondition();
					}

					public Condition getEnabledCondition() {
						return GenericBindingRule.this.getEnabledCondition();
					}

					public Condition getVisibleCondition() {
						return GenericBindingRule.this.getVisibleCondition();
					}

					public Binding getListElementBinding(int index) {
						throw new IllegalArgumentException("Not yet supported");
					}

					public Binding getMapValueBinding(Object key) {
						throw new IllegalArgumentException("Not yet supported");
					}

					public String getLabel() {
						return GenericBindingRule.this.getLabel() + "[" + index + "]";
					}

				};
				binding = new ListElementBinding(index, getElementType(), (List) this.binding.getValue(), listContext);
				listElementBindings.put(index, binding);
			}
			return binding;
		}

		public Binding getMapValueBinding(Object key) {
			return null;
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
			return GenericCollectionTypeResolver.getCollectionReturnType(property.getReadMethod());
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
				binding = new PropertyBinding(property, model, this);
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

	public interface BindingContext {

		MessageSource getMessageSource();

		TypeConverter getTypeConverter();

		Condition getEditableCondition();

		Condition getEnabledCondition();

		Condition getVisibleCondition();

		Binding getBinding(String property);

		Formatter getFormatter();

		Formatter getElementFormatter();

		Formatter getKeyFormatter();

		Binding getListElementBinding(int index);

		Binding getMapValueBinding(Object key);

		String getLabel();

	}

	public void setRequired(String[] propertyPaths) {
		this.requiredProperties = propertyPaths;
	}

}
