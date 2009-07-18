package org.springframework.ui.binding.config;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.util.Assert;

/**
 * Builder for constructing the rules for binding to a model.
 * @author Keith Donald
 * @param <M> the model type
 */
public class BindingRulesBuilder {

	private BindingRules bindingRules;
	
	/**
	 * Creates a new BindingRuleBuilder.
	 * @param modelType the type of model to build binding rules against
	 */
	public BindingRulesBuilder(Class<?> modelType) {
		Assert.notNull(modelType, "The model type is required");
		bindingRules = new ArrayListBindingRules(modelType);
	}

	/**
	 * Creates a rule for binding to the model property.
	 * @param propertyPath the model property path
	 * @return allows additional binding configuration options to be specified fluently
	 * @throws IllegalArgumentException if the property path is invalid given the modelType
	 */
	public BindingRuleConfiguration bind(String propertyPath) {
		boolean collectionBinding = validate(propertyPath);
		ConfigurableBindingRule rule = new ConfigurableBindingRule(propertyPath);
		if (collectionBinding) {
			rule.markCollectionBinding();
		}
		bindingRules.add(rule);
		return rule;
	}
	
	/**
	 * The built list of binding rules.
	 * Call after recording {@link #bind(String)} instructions.
	 */
	public BindingRules getBindingRules() {
		return bindingRules;
	}
	
	private boolean validate(String propertyPath) {
		boolean collectionBinding = false;
		String[] props = propertyPath.split("\\.");
		if (props.length == 0) {
			props = new String[] { propertyPath };
		}
		Class<?> modelType = bindingRules.getModelType();
		for (int i = 0; i < props.length; i ++) {
			String prop = props[i];
			PropertyDescriptor[] propDescs = getBeanInfo(modelType).getPropertyDescriptors();
			boolean found = false;
			for (PropertyDescriptor propDesc : propDescs) {
				if (prop.equals(propDesc.getName())) {
					found = true;
					Class<?> propertyType = propDesc.getPropertyType();
					if (Collection.class.isAssignableFrom(propertyType)) {
						modelType = GenericCollectionTypeResolver.getCollectionReturnType(propDesc.getReadMethod());
						if (i == (props.length - 1)) {
							collectionBinding = true;
						}
					} else if (Map.class.isAssignableFrom(propertyType)) {
						modelType = GenericCollectionTypeResolver.getMapValueReturnType(propDesc.getReadMethod());
						if (i == (props.length - 1)) {
							collectionBinding = true;
						}
					} else {
						modelType = propertyType;
					}
					break;
				}
			}
			if (!found) {
				if (props.length > 1) {
					throw new IllegalArgumentException("No property named '" + prop + "' found on model class [" + modelType.getName() + "] as part of property path '" + propertyPath + "'");
				} else {
					throw new IllegalArgumentException("No property named '" + prop + "' found on model class [" + modelType.getName() + "]");					
				}
			}
		}
		return collectionBinding;
	}
	
	private BeanInfo getBeanInfo(Class<?> clazz) {
		try {
			return Introspector.getBeanInfo(clazz);
		} catch (IntrospectionException e) {
			throw new IllegalStateException("Unable to introspect model type " + clazz);
		}
	}
	
	@SuppressWarnings("serial")
	static class ArrayListBindingRules extends ArrayList<BindingRule> implements BindingRules {

		private Class<?> modelType;
		
		public ArrayListBindingRules(Class<?> modelType) {
			this.modelType = modelType;
		}

		public Class<?> getModelType() {
			return modelType;
		}
		
	}
}
