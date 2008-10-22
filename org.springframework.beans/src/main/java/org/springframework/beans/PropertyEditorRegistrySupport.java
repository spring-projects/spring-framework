/*
 * Copyright 2002-2008 the original author or authors.
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
import java.util.regex.Pattern;

import org.springframework.beans.propertyeditors.ByteArrayPropertyEditor;
import org.springframework.beans.propertyeditors.CharArrayPropertyEditor;
import org.springframework.beans.propertyeditors.CharacterEditor;
import org.springframework.beans.propertyeditors.CharsetEditor;
import org.springframework.beans.propertyeditors.ClassArrayEditor;
import org.springframework.beans.propertyeditors.ClassEditor;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.beans.propertyeditors.CustomMapEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.beans.propertyeditors.FileEditor;
import org.springframework.beans.propertyeditors.InputStreamEditor;
import org.springframework.beans.propertyeditors.LocaleEditor;
import org.springframework.beans.propertyeditors.PatternEditor;
import org.springframework.beans.propertyeditors.PropertiesEditor;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.beans.propertyeditors.URIEditor;
import org.springframework.beans.propertyeditors.URLEditor;
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
 * @since 1.2.6
 * @see java.beans.PropertyEditorManager
 * @see java.beans.PropertyEditorSupport#setAsText
 * @see java.beans.PropertyEditorSupport#setValue
 */
public class PropertyEditorRegistrySupport implements PropertyEditorRegistry {

	private boolean defaultEditorsActive = false;

	private boolean configValueEditorsActive = false;

	private boolean propertySpecificEditorsRegistered = false;

	private Map defaultEditors;

	private Map customEditors;

	private Set sharedEditors;

	private Map customEditorCache;


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
	 * Retrieve the default editor for the given property type, if any.
	 * <p>Lazily registers the default editors, if they are active.
	 * @param requiredType type of the property
	 * @return the default editor, or <code>null</code> if none found
	 * @see #registerDefaultEditors
	 */
	public PropertyEditor getDefaultEditor(Class requiredType) {
		if (!this.defaultEditorsActive) {
			return null;
		}
		if (this.defaultEditors == null) {
			doRegisterDefaultEditors();
		}
		return (PropertyEditor) this.defaultEditors.get(requiredType);
	}

	/**
	 * Actually register the default editors for this registry instance.
	 * @see org.springframework.beans.propertyeditors.ByteArrayPropertyEditor
	 * @see org.springframework.beans.propertyeditors.ClassEditor
	 * @see org.springframework.beans.propertyeditors.CharacterEditor
	 * @see org.springframework.beans.propertyeditors.CustomBooleanEditor
	 * @see org.springframework.beans.propertyeditors.CustomNumberEditor
	 * @see org.springframework.beans.propertyeditors.CustomCollectionEditor
	 * @see org.springframework.beans.propertyeditors.CustomMapEditor
	 * @see org.springframework.beans.propertyeditors.FileEditor
	 * @see org.springframework.beans.propertyeditors.InputStreamEditor
	 * @see org.springframework.jndi.JndiTemplateEditor
	 * @see org.springframework.beans.propertyeditors.LocaleEditor
	 * @see org.springframework.beans.propertyeditors.PropertiesEditor
	 * @see org.springframework.beans.PropertyValuesEditor
	 * @see org.springframework.core.io.support.ResourceArrayPropertyEditor
	 * @see org.springframework.core.io.ResourceEditor
	 * @see org.springframework.transaction.interceptor.TransactionAttributeEditor
	 * @see org.springframework.transaction.interceptor.TransactionAttributeSourceEditor
	 * @see org.springframework.beans.propertyeditors.URLEditor
	 */
	private void doRegisterDefaultEditors() {
		this.defaultEditors = new HashMap(64);

		// Simple editors, without parameterization capabilities.
		// The JDK does not contain a default editor for any of these target types.
		this.defaultEditors.put(Charset.class, new CharsetEditor());
		this.defaultEditors.put(Class.class, new ClassEditor());
		this.defaultEditors.put(Class[].class, new ClassArrayEditor());
		this.defaultEditors.put(File.class, new FileEditor());
		this.defaultEditors.put(InputStream.class, new InputStreamEditor());
		this.defaultEditors.put(Locale.class, new LocaleEditor());
		this.defaultEditors.put(Pattern.class, new PatternEditor());
		this.defaultEditors.put(Properties.class, new PropertiesEditor());
		this.defaultEditors.put(Resource[].class, new ResourceArrayPropertyEditor());
		this.defaultEditors.put(URI.class, new URIEditor());
		this.defaultEditors.put(URL.class, new URLEditor());

		// Default instances of collection editors.
		// Can be overridden by registering custom instances of those as custom editors.
		this.defaultEditors.put(Collection.class, new CustomCollectionEditor(Collection.class));
		this.defaultEditors.put(Set.class, new CustomCollectionEditor(Set.class));
		this.defaultEditors.put(SortedSet.class, new CustomCollectionEditor(SortedSet.class));
		this.defaultEditors.put(List.class, new CustomCollectionEditor(List.class));
		this.defaultEditors.put(SortedMap.class, new CustomMapEditor(SortedMap.class));

		// Default editors for primitive arrays.
		this.defaultEditors.put(byte[].class, new ByteArrayPropertyEditor());
		this.defaultEditors.put(char[].class, new CharArrayPropertyEditor());

		// The JDK does not contain a default editor for char!
		this.defaultEditors.put(char.class, new CharacterEditor(false));
		this.defaultEditors.put(Character.class, new CharacterEditor(true));

		// Spring's CustomBooleanEditor accepts more flag values than the JDK's default editor.
		this.defaultEditors.put(boolean.class, new CustomBooleanEditor(false));
		this.defaultEditors.put(Boolean.class, new CustomBooleanEditor(true));

		// The JDK does not contain default editors for number wrapper types!
		// Override JDK primitive number editors with our own CustomNumberEditor.
		this.defaultEditors.put(byte.class, new CustomNumberEditor(Byte.class, false));
		this.defaultEditors.put(Byte.class, new CustomNumberEditor(Byte.class, true));
		this.defaultEditors.put(short.class, new CustomNumberEditor(Short.class, false));
		this.defaultEditors.put(Short.class, new CustomNumberEditor(Short.class, true));
		this.defaultEditors.put(int.class, new CustomNumberEditor(Integer.class, false));
		this.defaultEditors.put(Integer.class, new CustomNumberEditor(Integer.class, true));
		this.defaultEditors.put(long.class, new CustomNumberEditor(Long.class, false));
		this.defaultEditors.put(Long.class, new CustomNumberEditor(Long.class, true));
		this.defaultEditors.put(float.class, new CustomNumberEditor(Float.class, false));
		this.defaultEditors.put(Float.class, new CustomNumberEditor(Float.class, true));
		this.defaultEditors.put(double.class, new CustomNumberEditor(Double.class, false));
		this.defaultEditors.put(Double.class, new CustomNumberEditor(Double.class, true));
		this.defaultEditors.put(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, true));
		this.defaultEditors.put(BigInteger.class, new CustomNumberEditor(BigInteger.class, true));

		// Only register config value editors if explicitly requested.
		if (this.configValueEditorsActive) {
			StringArrayPropertyEditor sae = new StringArrayPropertyEditor();
			this.defaultEditors.put(String[].class, sae);
			this.defaultEditors.put(short[].class, sae);
			this.defaultEditors.put(int[].class, sae);
			this.defaultEditors.put(long[].class, sae);
		}
	}

	/**
	 * Copy the default editors registered in this instance to the given target registry.
	 * @param target the target registry to copy to
	 */
	protected void copyDefaultEditorsTo(PropertyEditorRegistrySupport target) {
		target.defaultEditors = this.defaultEditors;
		target.defaultEditorsActive = this.defaultEditorsActive;
		target.configValueEditorsActive = this.configValueEditorsActive;
	}


	//---------------------------------------------------------------------
	// Management of custom editors
	//---------------------------------------------------------------------

	public void registerCustomEditor(Class requiredType, PropertyEditor propertyEditor) {
		registerCustomEditor(requiredType, null, propertyEditor);
	}

	public void registerCustomEditor(Class requiredType, String propertyPath, PropertyEditor propertyEditor) {
		if (requiredType == null && propertyPath == null) {
			throw new IllegalArgumentException("Either requiredType or propertyPath is required");
		}
		if (this.customEditors == null) {
			this.customEditors = new LinkedHashMap(16);
		}
		if (propertyPath != null) {
			this.customEditors.put(propertyPath, new CustomEditorHolder(propertyEditor, requiredType));
			this.propertySpecificEditorsRegistered = true;
		}
		else {
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
	 */
	public void registerSharedEditor(Class requiredType, PropertyEditor propertyEditor) {
		registerCustomEditor(requiredType, null, propertyEditor);
		if (this.sharedEditors == null) {
			this.sharedEditors = new HashSet();
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

	public PropertyEditor findCustomEditor(Class requiredType, String propertyPath) {
		if (this.customEditors == null) {
			return null;
		}
		Class requiredTypeToUse = requiredType;
		if (propertyPath != null) {
			if (this.propertySpecificEditorsRegistered) {
				// Check property-specific editor first.
				PropertyEditor editor = getCustomEditor(propertyPath, requiredType);
				if (editor == null) {
					List strippedPaths = new LinkedList();
					addStrippedPropertyPaths(strippedPaths, "", propertyPath);
					for (Iterator it = strippedPaths.iterator(); it.hasNext() && editor == null;) {
						String strippedPath = (String) it.next();
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
	 * (can be <code>null</code> if not known)
	 * @param propertyPath the property path (typically of the array/collection;
	 * can be <code>null</code> if not known)
	 * @return whether a matching custom editor has been found
	 */
	public boolean hasCustomEditorForElement(Class elementType, String propertyPath) {
		if (this.customEditors == null) {
			return false;
		}
		if (propertyPath != null && this.propertySpecificEditorsRegistered) {
			for (Iterator it = this.customEditors.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				if (entry.getKey() instanceof String) {
					String regPath = (String) entry.getKey();
					if (PropertyAccessorUtils.matchesProperty(regPath, propertyPath)) {
						CustomEditorHolder editorHolder = (CustomEditorHolder) entry.getValue();
						if (editorHolder.getPropertyEditor(elementType) != null) {
							return true;
						}
					}
				}
			}
		}
		// No property-specific editor -> check type-specific editor.
		return (elementType != null && this.customEditors.containsKey(elementType));
	}

	/**
	 * Determine the property type for the given property path.
	 * <p>Called by {@link #findCustomEditor} if no required type has been specified,
	 * to be able to find a type-specific editor even if just given a property path.
	 * <p>The default implementation always returns <code>null</code>.
	 * BeanWrapperImpl overrides this with the standard <code>getPropertyType</code>
	 * method as defined by the BeanWrapper interface.
	 * @param propertyPath the property path to determine the type for
	 * @return the type of the property, or <code>null</code> if not determinable
	 * @see BeanWrapper#getPropertyType(String)
	 */
	protected Class getPropertyType(String propertyPath) {
		return null;
	}

	/**
	 * Get custom editor that has been registered for the given property.
	 * @param propertyName the property path to look for
	 * @param requiredType the type to look for
	 * @return the custom editor, or <code>null</code> if none specific for this property
	 */
	private PropertyEditor getCustomEditor(String propertyName, Class requiredType) {
		CustomEditorHolder holder = (CustomEditorHolder) this.customEditors.get(propertyName);
		return (holder != null ? holder.getPropertyEditor(requiredType) : null);
	}

	/**
	 * Get custom editor for the given type. If no direct match found,
	 * try custom editor for superclass (which will in any case be able
	 * to render a value as String via <code>getAsText</code>).
	 * @param requiredType the type to look for
	 * @return the custom editor, or <code>null</code> if none found for this type
	 * @see java.beans.PropertyEditor#getAsText()
	 */
	private PropertyEditor getCustomEditor(Class requiredType) {
		if (requiredType == null) {
			return null;
		}
		// Check directly registered editor for type.
		PropertyEditor editor = (PropertyEditor) this.customEditors.get(requiredType);
		if (editor == null) {
			// Check cached editor for type, registered for superclass or interface.
			if (this.customEditorCache != null) {
				editor = (PropertyEditor) this.customEditorCache.get(requiredType);
			}
			if (editor == null) {
				// Find editor for superclass or interface.
				for (Iterator it = this.customEditors.keySet().iterator(); it.hasNext() && editor == null;) {
					Object key = it.next();
					if (key instanceof Class && ((Class) key).isAssignableFrom(requiredType)) {
						editor = (PropertyEditor) this.customEditors.get(key);
						// Cache editor for search type, to avoid the overhead
						// of repeated assignable-from checks.
						if (this.customEditorCache == null) {
							this.customEditorCache = new HashMap();
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
	 * @return the property type, or <code>null</code> if not determinable
	 */
	protected Class guessPropertyTypeFromEditors(String propertyName) {
		if (this.customEditors != null) {
			CustomEditorHolder editorHolder = (CustomEditorHolder) this.customEditors.get(propertyName);
			if (editorHolder == null) {
				List strippedPaths = new LinkedList();
				addStrippedPropertyPaths(strippedPaths, "", propertyName);
				for (Iterator it = strippedPaths.iterator(); it.hasNext() && editorHolder == null;) {
					String strippedName = (String) it.next();
					editorHolder = (CustomEditorHolder) this.customEditors.get(strippedName);
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
			for (Iterator it = this.customEditors.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				if (entry.getKey() instanceof Class) {
					Class requiredType = (Class) entry.getKey();
					PropertyEditor editor = (PropertyEditor) entry.getValue();
					target.registerCustomEditor(requiredType, editor);
				}
				else if (entry.getKey() instanceof String) {
					String editorPath = (String) entry.getKey();
					CustomEditorHolder editorHolder = (CustomEditorHolder) entry.getValue();
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
	}


	/**
	 * Add property paths with all variations of stripped keys and/or indexes.
	 * Invokes itself recursively with nested paths.
	 * @param strippedPaths the result list to add to
	 * @param nestedPath the current nested path
	 * @param propertyPath the property path to check for keys/indexes to strip
	 */
	private void addStrippedPropertyPaths(List strippedPaths, String nestedPath, String propertyPath) {
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

		private final Class registeredType;

		private CustomEditorHolder(PropertyEditor propertyEditor, Class registeredType) {
			this.propertyEditor = propertyEditor;
			this.registeredType = registeredType;
		}

		private PropertyEditor getPropertyEditor() {
			return this.propertyEditor;
		}

		private Class getRegisteredType() {
			return this.registeredType;
		}

		private PropertyEditor getPropertyEditor(Class requiredType) {
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

}
