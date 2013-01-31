/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans;

import java.beans.PropertyEditor;
import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

import org.xml.sax.InputSource;

import org.springframework.beans.propertyeditors.ByteArrayPropertyEditor;
import org.springframework.beans.propertyeditors.CharArrayPropertyEditor;
import org.springframework.beans.propertyeditors.CharacterEditor;
import org.springframework.beans.propertyeditors.CharsetEditor;
import org.springframework.beans.propertyeditors.ClassArrayEditor;
import org.springframework.beans.propertyeditors.ClassEditor;
import org.springframework.beans.propertyeditors.CurrencyEditor;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.beans.propertyeditors.CustomMapEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.beans.propertyeditors.FileEditor;
import org.springframework.beans.propertyeditors.InputSourceEditor;
import org.springframework.beans.propertyeditors.InputStreamEditor;
import org.springframework.beans.propertyeditors.LocaleEditor;
import org.springframework.beans.propertyeditors.PatternEditor;
import org.springframework.beans.propertyeditors.PropertiesEditor;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.beans.propertyeditors.TimeZoneEditor;
import org.springframework.beans.propertyeditors.URIEditor;
import org.springframework.beans.propertyeditors.URLEditor;
import org.springframework.beans.propertyeditors.UUIDEditor;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceArrayPropertyEditor;
import org.springframework.util.ClassUtils;

/**
 * Base implementation of the {@link PropertyEditorRegistry} interface.
 * Provides management of default editors and custom editors.
 * Mainly serves as base class for {@link BeanWrapperImpl}.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Phillip Webb
 * @since 1.2.6
 * @see java.beans.PropertyEditorManager
 * @see java.beans.PropertyEditorSupport#setAsText
 * @see java.beans.PropertyEditorSupport#setValue
 */
public class PropertyEditorRegistrySupport implements PropertyEditorRegistry {

	private static final PropertyEditorFactories PROPERTY_EDITOR_FACTORIES = new PropertyEditorFactories();

	private ConversionService conversionService;

	private boolean defaultEditorsActive = false;

	private boolean configValueEditorsActive = false;

	private Map<Class<?>, PropertyEditor> defaultEditors;

	private Map<Class<?>, PropertyEditor> overriddenDefaultEditors;

	private Map<Class<?>, PropertyEditor> customEditors;

	private Map<String, CustomEditorHolder> customEditorsForPath;

	private Set<PropertyEditor> sharedEditors;

	private Map<Class<?>, PropertyEditor> customEditorCache;


	/**
	 * Specify a Spring 3.0 ConversionService to use for converting
	 * property values, as an alternative to JavaBeans PropertyEditors.
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Return the associated ConversionService, if any.
	 */
	public ConversionService getConversionService() {
		return this.conversionService;
	}


	//---------------------------------------------------------------------
	// Management of default editors
	//---------------------------------------------------------------------

	/**
	 * Activate the default editors for this registry instance,
	 * allowing for lazily registering default editors when needed.
	 */
	protected void registerDefaultEditors() {
		this.defaultEditorsActive = true;
	}

	/**
	 * Activate config value editors which are only intended for configuration purposes,
	 * such as {@link org.springframework.beans.propertyeditors.StringArrayPropertyEditor}.
	 * <p>Those editors are not registered by default simply because they are in
	 * general inappropriate for data binding purposes. Of course, you may register
	 * them individually in any case, through {@link #registerCustomEditor}.
	 */
	public void useConfigValueEditors() {
		this.configValueEditorsActive = true;
	}

	/**
	 * Override the default editor for the specified type with the given property editor.
	 * <p>Note that this is different from registering a custom editor in that the editor
	 * semantically still is a default editor. A ConversionService will override such a
	 * default editor, whereas custom editors usually override the ConversionService.
	 * @param requiredType the type of the property
	 * @param propertyEditor the editor to register
	 * @see #registerCustomEditor(Class, PropertyEditor)
	 */
	public void overrideDefaultEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
		if (this.overriddenDefaultEditors == null) {
			this.overriddenDefaultEditors = new HashMap<Class<?>, PropertyEditor>();
		}
		this.overriddenDefaultEditors.put(requiredType, propertyEditor);
	}

	/**
	 * Retrieve the default editor for the given property type, if any.
	 * <p>Lazily registers the default editors, if they are active.
	 * @param requiredType type of the property
	 * @return the default editor, or {@code null} if none found
	 * @see #registerDefaultEditors
	 */
	public PropertyEditor getDefaultEditor(Class<?> requiredType) {
		if (!this.defaultEditorsActive) {
			return null;
		}
		if (this.overriddenDefaultEditors != null) {
			PropertyEditor editor = this.overriddenDefaultEditors.get(requiredType);
			if (editor != null) {
				return editor;
			}
		}
		return getOrCreateDefaultEditor(requiredType);
	}

	private PropertyEditor getOrCreateDefaultEditor(Class<?> requiredType) {
		if(this.defaultEditors == null) {
			this.defaultEditors = new HashMap<Class<?>, PropertyEditor>();
		}
		PropertyEditor propertyEditor = this.defaultEditors.get(requiredType);
		if(propertyEditor == null) {
			PropertyEditorFactory factory = PROPERTY_EDITOR_FACTORIES.get(requiredType, this.configValueEditorsActive);
			if(factory != null) {
				propertyEditor = factory.create();
				defaultEditors.put(requiredType, propertyEditor);
			}
		}
		return propertyEditor;
	}

	/**
	 * Copy the default editors registered in this instance to the given target registry.
	 * @param target the target registry to copy to
	 */
	protected void copyDefaultEditorsTo(PropertyEditorRegistrySupport target) {
		target.defaultEditorsActive = this.defaultEditorsActive;
		target.configValueEditorsActive = this.configValueEditorsActive;
		target.defaultEditors = this.defaultEditors;
		target.overriddenDefaultEditors = this.overriddenDefaultEditors;
	}


	//---------------------------------------------------------------------
	// Management of custom editors
	//---------------------------------------------------------------------

	public void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
		registerCustomEditor(requiredType, null, propertyEditor);
	}

	public void registerCustomEditor(Class<?> requiredType, String propertyPath, PropertyEditor propertyEditor) {
		if (requiredType == null && propertyPath == null) {
			throw new IllegalArgumentException("Either requiredType or propertyPath is required");
		}
		if (propertyPath != null) {
			if (this.customEditorsForPath == null) {
				this.customEditorsForPath = new LinkedHashMap<String, CustomEditorHolder>(16);
			}
			this.customEditorsForPath.put(propertyPath, new CustomEditorHolder(propertyEditor, requiredType));
		}
		else {
			if (this.customEditors == null) {
				this.customEditors = new LinkedHashMap<Class<?>, PropertyEditor>(16);
			}
			this.customEditors.put(requiredType, propertyEditor);
			this.customEditorCache = null;
		}
	}

	/**
	 * Register the given custom property editor for all properties
	 * of the given type, indicating that the given instance is a
	 * shared editor that might be used concurrently.
	 * @param requiredType the type of the property
	 * @param propertyEditor the shared editor to register
	 * @deprecated as of Spring 3.0, in favor of PropertyEditorRegistrars or ConversionService usage
	 */
	@Deprecated
	public void registerSharedEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
		registerCustomEditor(requiredType, null, propertyEditor);
		if (this.sharedEditors == null) {
			this.sharedEditors = new HashSet<PropertyEditor>();
		}
		this.sharedEditors.add(propertyEditor);
	}

	/**
	 * Check whether the given editor instance is a shared editor, that is,
	 * whether the given editor instance might be used concurrently.
	 * @param propertyEditor the editor instance to check
	 * @return whether the editor is a shared instance
	 */
	public boolean isSharedEditor(PropertyEditor propertyEditor) {
		return (this.sharedEditors != null && this.sharedEditors.contains(propertyEditor));
	}

	public PropertyEditor findCustomEditor(Class<?> requiredType, String propertyPath) {
		Class<?> requiredTypeToUse = requiredType;
		if (propertyPath != null) {
			if (this.customEditorsForPath != null) {
				// Check property-specific editor first.
				PropertyEditor editor = getCustomEditor(propertyPath, requiredType);
				if (editor == null) {
					List<String> strippedPaths = new LinkedList<String>();
					addStrippedPropertyPaths(strippedPaths, "", propertyPath);
					for (Iterator<String> it = strippedPaths.iterator(); it.hasNext() && editor == null;) {
						String strippedPath = it.next();
						editor = getCustomEditor(strippedPath, requiredType);
					}
				}
				if (editor != null) {
					return editor;
				}
			}
			if (requiredType == null) {
				requiredTypeToUse = getPropertyType(propertyPath);
			}
		}
		// No property-specific editor -> check type-specific editor.
		return getCustomEditor(requiredTypeToUse);
	}

	/**
	 * Determine whether this registry contains a custom editor
	 * for the specified array/collection element.
	 * @param elementType the target type of the element
	 * (can be {@code null} if not known)
	 * @param propertyPath the property path (typically of the array/collection;
	 * can be {@code null} if not known)
	 * @return whether a matching custom editor has been found
	 */
	public boolean hasCustomEditorForElement(Class<?> elementType, String propertyPath) {
		if (propertyPath != null && this.customEditorsForPath != null) {
			for (Map.Entry<String, CustomEditorHolder> entry : this.customEditorsForPath.entrySet()) {
				if (PropertyAccessorUtils.matchesProperty(entry.getKey(), propertyPath)) {
					if (entry.getValue().getPropertyEditor(elementType) != null) {
						return true;
					}
				}
			}
		}
		// No property-specific editor -> check type-specific editor.
		return (elementType != null && this.customEditors != null && this.customEditors.containsKey(elementType));
	}

	/**
	 * Determine the property type for the given property path.
	 * <p>Called by {@link #findCustomEditor} if no required type has been specified,
	 * to be able to find a type-specific editor even if just given a property path.
	 * <p>The default implementation always returns {@code null}.
	 * BeanWrapperImpl overrides this with the standard {@code getPropertyType}
	 * method as defined by the BeanWrapper interface.
	 * @param propertyPath the property path to determine the type for
	 * @return the type of the property, or {@code null} if not determinable
	 * @see BeanWrapper#getPropertyType(String)
	 */
	protected Class<?> getPropertyType(String propertyPath) {
		return null;
	}

	/**
	 * Get custom editor that has been registered for the given property.
	 * @param propertyName the property path to look for
	 * @param requiredType the type to look for
	 * @return the custom editor, or {@code null} if none specific for this property
	 */
	private PropertyEditor getCustomEditor(String propertyName, Class<?> requiredType) {
		CustomEditorHolder holder = this.customEditorsForPath.get(propertyName);
		return (holder != null ? holder.getPropertyEditor(requiredType) : null);
	}

	/**
	 * Get custom editor for the given type. If no direct match found,
	 * try custom editor for superclass (which will in any case be able
	 * to render a value as String via {@code getAsText}).
	 * @param requiredType the type to look for
	 * @return the custom editor, or {@code null} if none found for this type
	 * @see java.beans.PropertyEditor#getAsText()
	 */
	private PropertyEditor getCustomEditor(Class<?> requiredType) {
		if (requiredType == null || this.customEditors == null) {
			return null;
		}
		// Check directly registered editor for type.
		PropertyEditor editor = this.customEditors.get(requiredType);
		if (editor == null) {
			// Check cached editor for type, registered for superclass or interface.
			if (this.customEditorCache != null) {
				editor = this.customEditorCache.get(requiredType);
			}
			if (editor == null) {
				// Find editor for superclass or interface.
				for (Iterator<Class<?>> it = this.customEditors.keySet().iterator(); it.hasNext() && editor == null;) {
					Class<?> key = it.next();
					if (key.isAssignableFrom(requiredType)) {
						editor = this.customEditors.get(key);
						// Cache editor for search type, to avoid the overhead
						// of repeated assignable-from checks.
						if (this.customEditorCache == null) {
							this.customEditorCache = new HashMap<Class<?>, PropertyEditor>();
						}
						this.customEditorCache.put(requiredType, editor);
					}
				}
			}
		}
		return editor;
	}

	/**
	 * Guess the property type of the specified property from the registered
	 * custom editors (provided that they were registered for a specific type).
	 * @param propertyName the name of the property
	 * @return the property type, or {@code null} if not determinable
	 */
	protected Class<?> guessPropertyTypeFromEditors(String propertyName) {
		if (this.customEditorsForPath != null) {
			CustomEditorHolder editorHolder = this.customEditorsForPath.get(propertyName);
			if (editorHolder == null) {
				List<String> strippedPaths = new LinkedList<String>();
				addStrippedPropertyPaths(strippedPaths, "", propertyName);
				for (Iterator<String> it = strippedPaths.iterator(); it.hasNext() && editorHolder == null;) {
					String strippedName = it.next();
					editorHolder = this.customEditorsForPath.get(strippedName);
				}
			}
			if (editorHolder != null) {
				return editorHolder.getRegisteredType();
			}
		}
		return null;
	}

	/**
	 * Copy the custom editors registered in this instance to the given target registry.
	 * @param target the target registry to copy to
	 * @param nestedProperty the nested property path of the target registry, if any.
	 * If this is non-null, only editors registered for a path below this nested property
	 * will be copied. If this is null, all editors will be copied.
	 */
	protected void copyCustomEditorsTo(PropertyEditorRegistry target, String nestedProperty) {
		String actualPropertyName =
				(nestedProperty != null ? PropertyAccessorUtils.getPropertyName(nestedProperty) : null);
		if (this.customEditors != null) {
			for (Map.Entry<Class<?>, PropertyEditor> entry : this.customEditors.entrySet()) {
				target.registerCustomEditor(entry.getKey(), entry.getValue());
			}
		}
		if (this.customEditorsForPath != null) {
			for (Map.Entry<String, CustomEditorHolder> entry : this.customEditorsForPath.entrySet()) {
				String editorPath = entry.getKey();
				CustomEditorHolder editorHolder = entry.getValue();
				if (nestedProperty != null) {
					int pos = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(editorPath);
					if (pos != -1) {
						String editorNestedProperty = editorPath.substring(0, pos);
						String editorNestedPath = editorPath.substring(pos + 1);
						if (editorNestedProperty.equals(nestedProperty) || editorNestedProperty.equals(actualPropertyName)) {
							target.registerCustomEditor(
									editorHolder.getRegisteredType(), editorNestedPath, editorHolder.getPropertyEditor());
						}
					}
				}
				else {
					target.registerCustomEditor(
							editorHolder.getRegisteredType(), editorPath, editorHolder.getPropertyEditor());
				}
			}
		}
	}


	/**
	 * Add property paths with all variations of stripped keys and/or indexes.
	 * Invokes itself recursively with nested paths.
	 * @param strippedPaths the result list to add to
	 * @param nestedPath the current nested path
	 * @param propertyPath the property path to check for keys/indexes to strip
	 */
	private void addStrippedPropertyPaths(List<String> strippedPaths, String nestedPath, String propertyPath) {
		int startIndex = propertyPath.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR);
		if (startIndex != -1) {
			int endIndex = propertyPath.indexOf(PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR);
			if (endIndex != -1) {
				String prefix = propertyPath.substring(0, startIndex);
				String key = propertyPath.substring(startIndex, endIndex + 1);
				String suffix = propertyPath.substring(endIndex + 1, propertyPath.length());
				// Strip the first key.
				strippedPaths.add(nestedPath + prefix + suffix);
				// Search for further keys to strip, with the first key stripped.
				addStrippedPropertyPaths(strippedPaths, nestedPath + prefix, suffix);
				// Search for further keys to strip, with the first key not stripped.
				addStrippedPropertyPaths(strippedPaths, nestedPath + prefix + key, suffix);
			}
		}
	}


	/**
	 * Holder for a registered custom editor with property name.
	 * Keeps the PropertyEditor itself plus the type it was registered for.
	 */
	private static class CustomEditorHolder {

		private final PropertyEditor propertyEditor;

		private final Class<?> registeredType;

		private CustomEditorHolder(PropertyEditor propertyEditor, Class<?> registeredType) {
			this.propertyEditor = propertyEditor;
			this.registeredType = registeredType;
		}

		private PropertyEditor getPropertyEditor() {
			return this.propertyEditor;
		}

		private Class<?> getRegisteredType() {
			return this.registeredType;
		}

		private PropertyEditor getPropertyEditor(Class<?> requiredType) {
			// Special case: If no required type specified, which usually only happens for
			// Collection elements, or required type is not assignable to registered type,
			// which usually only happens for generic properties of type Object -
			// then return PropertyEditor if not registered for Collection or array type.
			// (If not registered for Collection or array, it is assumed to be intended
			// for elements.)
			if (this.registeredType == null ||
					(requiredType != null &&
					(ClassUtils.isAssignable(this.registeredType, requiredType) ||
					ClassUtils.isAssignable(requiredType, this.registeredType))) ||
					(requiredType == null &&
					(!Collection.class.isAssignableFrom(this.registeredType) && !this.registeredType.isArray()))) {
				return this.propertyEditor;
			}
			else {
				return null;
			}
		}
	}

	private enum PropertyEditorFactoryType {DEFAULT, CONFIG_VALUE};

	private static interface PropertyEditorFactory {
		PropertyEditor create();
	}

	private static class PropertyEditorFactories {

		private static final Map<PropertyEditorFactoryType, Map<Class<?>, PropertyEditorFactory>> FACTORIES;
		static {
			Map<PropertyEditorFactoryType, Map<Class<?>, PropertyEditorFactory>> factories = new HashMap<PropertyEditorRegistrySupport.PropertyEditorFactoryType, Map<Class<?>, PropertyEditorFactory>>();
			factories.put(PropertyEditorFactoryType.DEFAULT,
				getDefaultPropertyEditorFactories());
			factories.put(PropertyEditorFactoryType.CONFIG_VALUE,
				getConfigValuePropertyEditorFactories());
			FACTORIES = Collections.unmodifiableMap(factories);
		}

		private static Map<Class<?>, PropertyEditorFactory> getDefaultPropertyEditorFactories() {
			Map<Class<?>, PropertyEditorFactory> factories = new HashMap<Class<?>, PropertyEditorFactory>();
			addSimpleEditors(factories);
			addCollectionEditors(factories);
			addPrimitiveArrays(factories);
			addCharacter(factories);
			addCustomBoolean(factories);
			addNumberWrappers(factories);
			return Collections.unmodifiableMap(factories);
		}

		/**
		 * Add Simple editors, without parameterization capabilities. The JDK does not
		 * contain a default editor for any of these target types.
		 * @param factories
		 */
		private static void addSimpleEditors(
			Map<Class<?>, PropertyEditorFactory> factories) {
			factories.put(Charset.class, new PropertyEditorFactory() {

				public PropertyEditor create() {
					return new CharsetEditor();
				}
			});
			factories.put(Class.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new ClassEditor();
				}
			});
			factories.put(Class[].class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new ClassArrayEditor();
				}
			});
			factories.put(Currency.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CurrencyEditor();
				}
			});
			factories.put(File.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new FileEditor();
				}
			});
			factories.put(InputStream.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new InputStreamEditor();
				}
			});
			factories.put(InputSource.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new InputSourceEditor();
				}
			});
			factories.put(Locale.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new LocaleEditor();
				}
			});
			factories.put(Pattern.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new PatternEditor();
				}
			});
			factories.put(Properties.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new PropertiesEditor();
				}
			});
			factories.put(Resource[].class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new ResourceArrayPropertyEditor();
				}
			});
			factories.put(TimeZone.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new TimeZoneEditor();
				}
			});
			factories.put(URI.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new URIEditor();
				}
			});
			factories.put(URL.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new URLEditor();
				}
			});
			factories.put(UUID.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new UUIDEditor();
				}
			});
		}

		/**
		 * Default instances of collection editors. Can be overridden by registering
		 * custom instances of those as custom editors.
		 * @param factories
		 */
		private static void addCollectionEditors(
			Map<Class<?>, PropertyEditorFactory> factories) {
			factories.put(Collection.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomCollectionEditor(Collection.class);
				}
			});
			factories.put(Set.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomCollectionEditor(Set.class);
				}
			});
			factories.put(SortedSet.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomCollectionEditor(SortedSet.class);
				}
			});
			factories.put(List.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomCollectionEditor(List.class);
				}
			});
			factories.put(SortedMap.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomMapEditor(SortedMap.class);
				}
			});
		}

		/**
		 * Default editors for primitive arrays.
		 * @param factories
		 */
		private static void addPrimitiveArrays(Map<Class<?>, PropertyEditorFactory> factories) {
			factories.put(byte[].class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new ByteArrayPropertyEditor();
				}
			});
			factories.put(char[].class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CharArrayPropertyEditor();
				}
			});
		}

		/**
		 * The JDK does not contain a default editor for char.
		 * @param factories
		 */
		private static void addCharacter(Map<Class<?>, PropertyEditorFactory> factories) {
			factories.put(char.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CharacterEditor(false);
				}
			});
			factories.put(Character.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CharacterEditor(true);
				}
			});
		}

		/**
		 * Spring's CustomBooleanEditor accepts more flag values than the JDK's default editor.
		 * @param factories
		 */
		private static void addCustomBoolean(Map<Class<?>, PropertyEditorFactory> factories) {
			factories.put(boolean.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomBooleanEditor(false);
				}
			});
			factories.put(Boolean.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomBooleanEditor(true);
				}
			});
		}

		/**
		 * The JDK does not contain default editors for number wrapper types. Override JDK
		 * primitive number editors with our own CustomNumberEditor.
		 * @param factories
		 */
		private static void addNumberWrappers(Map<Class<?>, PropertyEditorFactory> factories) {
			factories.put(byte.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomNumberEditor(Byte.class, false);
				}
			});
			factories.put(Byte.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomNumberEditor(Byte.class, true);
				}
			});
			factories.put(short.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomNumberEditor(Short.class, false);
				}
			});
			factories.put(Short.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomNumberEditor(Short.class, true);
				}
			});
			factories.put(int.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomNumberEditor(Integer.class, false);
				}
			});
			factories.put(Integer.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomNumberEditor(Integer.class, true);
				}
			});
			factories.put(long.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomNumberEditor(Long.class, false);
				}
			});
			factories.put(Long.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomNumberEditor(Long.class, true);
				}
			});
			factories.put(float.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomNumberEditor(Float.class, false);
				}
			});
			factories.put(Float.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomNumberEditor(Float.class, true);
				}
			});
			factories.put(double.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomNumberEditor(Double.class, false);
				}
			});
			factories.put(Double.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomNumberEditor(Double.class, true);
				}
			});
			factories.put(BigDecimal.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomNumberEditor(BigDecimal.class, true);
				}
			});
			factories.put(BigInteger.class, new PropertyEditorFactory() {
				public PropertyEditor create() {
					return new CustomNumberEditor(BigInteger.class, true);
				}
			});
		}

		private static Map<Class<?>, PropertyEditorFactory> getConfigValuePropertyEditorFactories() {
			Map<Class<?>, PropertyEditorFactory> factories = new HashMap<Class<?>, PropertyEditorFactory>();
			PropertyEditorFactory stringArrayProperyEditorFactory = new PropertyEditorFactory() {

				public PropertyEditor create() {
					return new StringArrayPropertyEditor();
				}
			};
			factories.put(String[].class, stringArrayProperyEditorFactory);
			factories.put(short[].class, stringArrayProperyEditorFactory);
			factories.put(int[].class, stringArrayProperyEditorFactory);
			factories.put(long[].class, stringArrayProperyEditorFactory);
			return Collections.unmodifiableMap(factories);
		}

		public PropertyEditorFactory get(Class<?> requiredType, boolean configValueEditorsActive) {
			PropertyEditorFactory factory = get(PropertyEditorFactoryType.DEFAULT, requiredType);
			if(factory == null && configValueEditorsActive) {
				factory = get(PropertyEditorFactoryType.CONFIG_VALUE, requiredType);
			}
			return factory;
		}

		private PropertyEditorFactory get(PropertyEditorFactoryType factoryType, Class<?> requiredType) {
			Map<Class<?>, PropertyEditorFactory> factories = FACTORIES.get(factoryType);
			if(factories == null) {
				return null;
			}
			return factories.get(requiredType);
		}
	}
}
