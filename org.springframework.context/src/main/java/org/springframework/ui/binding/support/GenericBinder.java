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
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.convert.TypeConverter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultTypeConverter;
import org.springframework.ui.binding.Binder;
import org.springframework.ui.binding.Binding;
import org.springframework.ui.binding.BindingResult;
import org.springframework.ui.binding.BindingResults;
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
		for (PropertyPathElement element : path.getNestedElements()) {
			if (element.isIndex()) {
				if (binding.isMap()) {
					binding = binding.getMapValueBinding(element.getValue());
				} else if (binding.isList()) {
					binding = binding.getListElementBinding(element.getIntValue());
				} else {
					throw new IllegalArgumentException("Attempted to index a property that is not a Collection or Map");
				}
			} else {
				binding = binding.getBinding(element.getValue());
			}
		}
		return binding;
	}

	public BindingResults bind(Map<String, ? extends Object> sourceValues) {
		sourceValues = filter(sourceValues);
		ArrayListBindingResults results = new ArrayListBindingResults(sourceValues.size());
		for (Map.Entry<String, ? extends Object> sourceValue : sourceValues.entrySet()) {
			Binding binding = getBinding(sourceValue.getKey());
			results.add(bind(sourceValue, binding));
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
		
		private Condition visibleCondition  = Condition.ALWAYS_TRUE;
		
		private Map<String, GenericBindingRule> nestedBindingRules;
		
		private Binding binding;
		
		public GenericBindingRule(String property, Class modelClass) {
			this.modelClass = modelClass;
			this.property = findPropertyDescriptor(property);
		}
		
		// implementing BindingContext

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
		
		public Binding getBinding(String property, Object model) {
			return getBindingRule(property).getBinding(model);
		}

		// implementing BindingRuleConfiguration

		public BindingRuleConfiguration formatWith(Formatter<?> formatter) {
			this.formatter = formatter;
			return this;
		}

		public BindingRuleConfiguration formatElementsWith(Formatter<?> formatter) {
			elementFormatter = formatter;
			return this;
		}

		public BindingRuleConfiguration formatKeysWith(Formatter<?> formatter) {
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
			GenericBindingRule rule = nestedBindingRules.get(property);
			if (rule == null) {
				rule = new GenericBindingRule(property, this.property.getPropertyType());
				nestedBindingRules.put(property, rule);
			}
			return rule;
		}
		
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
			throw new IllegalArgumentException("No property '" + property + "' found on model ["
					+ modelClass.getName() + "]");
		}

		private BeanInfo getBeanInfo(Class<?> clazz) {
			try {
				return Introspector.getBeanInfo(clazz);
			} catch (IntrospectionException e) {
				throw new IllegalStateException("Unable to introspect model type " + clazz);
			}
		}
	}
	
	public interface BindingContext {
		
		TypeConverter getTypeConverter();

		Condition getEditableCondition();

		Condition getEnabledCondition();

		Condition getVisibleCondition();

		Binding getBinding(String property, Object model);

		@SuppressWarnings("unchecked")
		Formatter getFormatter();

		@SuppressWarnings("unchecked")
		Formatter getElementFormatter();

		@SuppressWarnings("unchecked")
		Formatter getKeyFormatter();

	}

}
