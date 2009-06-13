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

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.expression.MapAccessor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.TypeConverter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultTypeConverter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParserConfiguration;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.ui.binding.Binder;
import org.springframework.ui.binding.Binding;
import org.springframework.ui.binding.BindingConfiguration;
import org.springframework.ui.binding.BindingResult;
import org.springframework.ui.binding.BindingResults;
import org.springframework.ui.binding.FormatterRegistry;
import org.springframework.ui.binding.UserValue;
import org.springframework.ui.binding.UserValues;
import org.springframework.ui.format.AnnotationFormatterFactory;
import org.springframework.ui.format.Formatter;

/**
 * A generic {@link Binder binder} suitable for use in most binding environments.
 * @author Keith Donald
 * @see #add(BindingConfiguration)
 * @see #bind(UserValues)
 */
@SuppressWarnings("unchecked")
public class GenericBinder implements Binder {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private Object model;

	private Map<String, Binding> bindings;

	private FormatterRegistry formatterRegistry = new GenericFormatterRegistry();

	private ExpressionParser expressionParser;

	private TypeConverter typeConverter;

	private boolean strict = false;

	private static Formatter defaultFormatter = new DefaultFormatter();

	/**
	 * Creates a new binder for the model object.
	 * @param model the model object containing properties this binder will bind to
	 */
	public GenericBinder(Object model) {
		this.model = model;
		bindings = new HashMap<String, Binding>();
		int parserConfig = SpelExpressionParserConfiguration.CreateListsOnAttemptToIndexIntoNull
				| SpelExpressionParserConfiguration.GrowListsOnIndexBeyondSize;
		expressionParser = new SpelExpressionParser(parserConfig);
		typeConverter = new DefaultTypeConverter();
	}

	public Object getModel() {
		return model;
	}

	public void setStrict(boolean strict) {
		this.strict = strict;
	}

	public Binding add(BindingConfiguration binding) {
		Binding newBinding;
		try {
			newBinding = new BindingImpl(binding);
		} catch (org.springframework.expression.ParseException e) {
			throw new IllegalArgumentException(e);
		}
		bindings.put(binding.getProperty(), newBinding);
		return newBinding;
	}

	public void add(Formatter<?> formatter, Class<?> propertyType) {
		formatterRegistry.add(formatter, propertyType);
	}

	public void add(AnnotationFormatterFactory<?, ?> factory) {
		formatterRegistry.add(factory);
	}

	public Binding getBinding(String property) {
		Binding binding = bindings.get(property);
		if (binding == null && !strict) {
			return add(new BindingConfiguration(property, null));
		} else {
			return binding;
		}
	}

	public BindingResults bind(UserValues values) {
		ArrayListBindingResults results = new ArrayListBindingResults(values.size());
		for (UserValue value : values) {
			BindingImpl binding = (BindingImpl) getBinding(value.getProperty());
			results.add(binding.setValue(value.getValue()));
		}
		return results;
	}

	public UserValues createUserValues(Map<String, ? extends Object> userMap) {
		UserValues values = new UserValues(userMap.size());
		for (Map.Entry<String, ? extends Object> entry : userMap.entrySet()) {
			values.add(entry.getKey(), entry.getValue());
		}
		return values;
	}

	// internal helpers

	static class ArrayListBindingResults implements BindingResults {

		private List<BindingResult> results;
		
		public ArrayListBindingResults() {
			results = new ArrayList<BindingResult>();
		}
		
		public ArrayListBindingResults(int size) {
			results = new ArrayList<BindingResult>(size);
		}

		public void add(BindingResult result) {
			results.add(result);
		}

		// implementing Iterable
		
		public Iterator<BindingResult> iterator() {
			return results.iterator();
		}

		// implementing BindingResults
		
		public BindingResults successes() {
			ArrayListBindingResults results = new ArrayListBindingResults();
			for (BindingResult result : this) {
				if (!result.isError()) {
					results.add(result);
				}
			}
			return results;
		}
		
		public BindingResults failures() {
			ArrayListBindingResults results = new ArrayListBindingResults();
			for (BindingResult result : this) {
				if (result.isError()) {
					results.add(result);
				}
			}
			return results;
		}

		public BindingResult get(int index) {
			return results.get(index);
		}

		public List<String> properties() {
			List<String> properties = new ArrayList<String>(results.size());
			for (BindingResult result : this) {
				properties.add(result.getProperty());
			}
			return properties;
		}

		public int size() {
			return results.size();
		}
		
	}
	
	
	class BindingImpl implements Binding {

		private Expression property;

		private Formatter formatter;

		public BindingImpl(BindingConfiguration config) throws org.springframework.expression.ParseException {
			property = expressionParser.parseExpression(config.getProperty());
			formatter = config.getFormatter();
		}

		// implementing Binding

		public String getValue() {
			Object value;
			try {
				value = property.getValue(createEvaluationContext());
			} catch (ExpressionException e) {
				throw new IllegalStateException("Failed to get property expression value - this should not happen", e);
			}
			return format(value);
		}

		public BindingResult setValue(Object value) {
			if (value instanceof String) {
				return setStringValue((String) value);
			} else if (value instanceof String[]) {
				return setStringValues((String[]) value);
			} else {
				return setObjectValue(value);
			}
		}

		public String format(Object selectableValue) {
			Formatter formatter;
			try {
				formatter = getFormatter();
			} catch (EvaluationException e) {
				throw new IllegalStateException(
						"Failed to get property expression value type - this should not happen", e);
			}
			Class<?> formattedType = getFormattedObjectType(formatter);
			selectableValue = typeConverter.convert(selectableValue, formattedType);
			return formatter.format(selectableValue, LocaleContextHolder.getLocale());
		}

		public boolean isCollection() {
			Class type = getType();
			TypeDescriptor<?> typeDesc = TypeDescriptor.valueOf(type);
			return typeDesc.isCollection() || typeDesc.isArray();
		}

		public String[] getCollectionValues() {
			Object multiValue;
			try {
				multiValue = property.getValue(createEvaluationContext());
			} catch (EvaluationException e) {
				throw new IllegalStateException("Failed to get property expression value - this should not happen", e);
			}
			if (multiValue == null) {
				return EMPTY_STRING_ARRAY;
			}
			TypeDescriptor<?> type = TypeDescriptor.valueOf(multiValue.getClass());
			String[] formattedValues;
			if (type.isCollection()) {
				Collection<?> values = ((Collection<?>) multiValue);
				formattedValues = (String[]) Array.newInstance(String.class, values.size());
				copy(values, formattedValues);
			} else if (type.isArray()) {
				formattedValues = (String[]) Array.newInstance(String.class, Array.getLength(multiValue));
				copy((Iterable<?>) multiValue, formattedValues);
			} else {
				throw new IllegalStateException();
			}
			return formattedValues;
		}

		// public impl only

		public Class<?> getType() {
			Class<?> type;
			try {
				type = property.getValueType(createEvaluationContext());
			} catch (EvaluationException e) {
				throw new IllegalArgumentException(
						"Failed to get property expression value type - this should not happen", e);
			}
			return type;
		}

		// internal helpers

		private BindingResult setStringValue(String formatted) {
			Formatter formatter;
			try {
				formatter = getFormatter();
			} catch (EvaluationException e) {
				// could occur the property was not found or is not readable
				// TODO probably should not handle all EL failures, only type conversion & property not found?
				return new ExpressionEvaluationErrorResult(property.getExpressionString(), formatted, e);
			}
			Object parsed;
			try {
				parsed = formatter.parse(formatted, LocaleContextHolder.getLocale());
			} catch (ParseException e) {
				return new InvalidFormatResult(property.getExpressionString(), formatted, e);
			}
			return setValue(parsed, formatted);
		}

		private BindingResult setStringValues(String[] formatted) {
			Formatter formatter;
			try {
				formatter = getFormatter();
			} catch (EvaluationException e) {
				// could occur the property was not found or is not readable
				// TODO probably should not handle all EL failures, only type conversion & property not found?
				return new ExpressionEvaluationErrorResult(property.getExpressionString(), formatted, e);
			}
			Class parsedType = getFormattedObjectType(formatter);
			if (parsedType == null) {
				parsedType = String.class;
			}
			Object parsed = Array.newInstance(parsedType, formatted.length);
			for (int i = 0; i < formatted.length; i++) {
				Object parsedValue;
				try {
					parsedValue = formatter.parse(formatted[i], LocaleContextHolder.getLocale());
				} catch (ParseException e) {
					return new InvalidFormatResult(property.getExpressionString(), formatted, e);
				}
				Array.set(parsed, i, parsedValue);
			}
			return setValue(parsed, formatted);
		}

		private BindingResult setObjectValue(Object value) {
			return setValue(value, value);
		}

		private Formatter getFormatter() throws EvaluationException {
			if (formatter != null) {
				return formatter;
			} else {
				Formatter<?> formatter = formatterRegistry.getFormatter(property
						.getValueTypeDescriptor(createEvaluationContext()));
				return formatter != null ? formatter : defaultFormatter;
			}
		}

		private void copy(Iterable<?> values, String[] formattedValues) {
			int i = 0;
			for (Object value : values) {
				formattedValues[i] = format(value);
				i++;
			}
		}

		private BindingResult setValue(Object parsedValue, Object userValue) {
			try {
				property.setValue(createEvaluationContext(), parsedValue);
				return new SuccessResult(property.getExpressionString(), userValue);
			} catch (EvaluationException e) {
				return new ExpressionEvaluationErrorResult(property.getExpressionString(), userValue, e);
			}
		}

		private EvaluationContext createEvaluationContext() {
			StandardEvaluationContext context = new StandardEvaluationContext();
			context.setRootObject(model);
			context.addPropertyAccessor(new MapAccessor());
			context.setTypeConverter(new StandardTypeConverter(typeConverter));
			return context;
		}

	}

	private Class getFormattedObjectType(Formatter formatter) {
		// TODO consider caching this info
		Class classToIntrospect = formatter.getClass();
		while (classToIntrospect != null) {
			Type[] genericInterfaces = classToIntrospect.getGenericInterfaces();
			for (Type genericInterface : genericInterfaces) {
				if (genericInterface instanceof ParameterizedType) {
					ParameterizedType pInterface = (ParameterizedType) genericInterface;
					if (Formatter.class.isAssignableFrom((Class) pInterface.getRawType())) {
						return getParameterClass(pInterface.getActualTypeArguments()[0], formatter.getClass());
					}
				}
			}
			classToIntrospect = classToIntrospect.getSuperclass();
		}
		return null;
	}

	private Class getParameterClass(Type parameterType, Class converterClass) {
		if (parameterType instanceof TypeVariable) {
			parameterType = GenericTypeResolver.resolveTypeVariable((TypeVariable) parameterType, converterClass);
		}
		if (parameterType instanceof Class) {
			return (Class) parameterType;
		}
		throw new IllegalArgumentException("Unable to obtain the java.lang.Class for parameterType [" + parameterType
				+ "] on Formatter [" + converterClass.getName() + "]");
	}

	static class DefaultFormatter implements Formatter {
		public String format(Object object, Locale locale) {
			if (object == null) {
				return "";
			} else {
				return object.toString();
			}
		}

		public Object parse(String formatted, Locale locale) throws ParseException {
			if (formatted == "") {
				return null;
			} else {
				return formatted;
			}
		}
	}

	static class InvalidFormatResult implements BindingResult {

		private String property;

		private Object formatted;

		private ParseException e;

		public InvalidFormatResult(String property, Object formatted, ParseException e) {
			this.property = property;
			this.formatted = formatted;
			this.e = e;
		}

		public String getProperty() {
			return property;
		}

		public boolean isError() {
			return true;
		}

		public String getErrorCode() {
			return "invalidFormat";
		}

		public Throwable getErrorCause() {
			return e;
		}

		public Object getUserValue() {
			return formatted;
		}
	}

	static class ExpressionEvaluationErrorResult implements BindingResult {

		private String property;

		private Object formatted;

		private EvaluationException e;

		public ExpressionEvaluationErrorResult(String property, Object formatted, EvaluationException e) {
			this.property = property;
			this.formatted = formatted;
			this.e = e;
		}

		public String getProperty() {
			return property;
		}

		public boolean isError() {
			return true;
		}

		public String getErrorCode() {
		    SpelMessage spelCode = ((SpelEvaluationException) e).getMessageCode();
		    if (spelCode==SpelMessage.EXCEPTION_DURING_PROPERTY_WRITE) {
		    	return "typeConversionFailure";                                 
		    } else if (spelCode==SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE) {
		    	return "propertyNotFound";                              
		    } else {
		    	// TODO return more specific code based on underlying EvaluationException error code
				return "couldNotSetValue";		    	 
		    }
		}

		public Throwable getErrorCause() {
			return e;
		}

		public Object getUserValue() {
			return formatted;
		}
	}

	static class SuccessResult implements BindingResult {

		private String property;

		private Object formatted;

		public SuccessResult(String property, Object formatted) {
			this.property = property;
			this.formatted = formatted;
		}

		public String getProperty() {
			return property;
		}

		public boolean isError() {
			return false;
		}

		public String getErrorCode() {
			return null;
		}

		public Throwable getErrorCause() {
			return null;
		}

		public Object getUserValue() {
			return formatted;
		}
	}
}
