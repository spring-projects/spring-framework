package org.springframework.ui.binding;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.expression.MapAccessor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.TypeConverter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultTypeConverter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.ui.format.Formatter;

public class Binder<T> {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private T model;

	private Map<String, Binding> bindings;

	private Map<Class<?>, Formatter<?>> typeFormatters = new HashMap<Class<?>, Formatter<?>>();
	
	private Map<Annotation, Formatter<?>> annotationFormatters = new HashMap<Annotation, Formatter<?>>();
	
	private ExpressionParser expressionParser;

	private TypeConverter typeConverter;
	
	private boolean optimisticBinding = true;

	private static Formatter<?> defaultFormatter = new Formatter<?>() {
		
		public Class<?> getFormattedObjectType() {
			return String.class;
		}

		public String format(Object object, Locale locale) {
			if (object == null) {
				return "";
			} else {
				return object.toString();
			}
		}

		public Object parse(String formatted, Locale locale)
				throws ParseException {
			if (formatted == "") {
				return null;
			} else {
				return formatted;
			}
		}		
	};
	
	public Binder(T model) {
		this.model = model;
		bindings = new HashMap<String, Binding>();
		expressionParser = new SpelExpressionParser();
		typeConverter = new DefaultTypeConverter();
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
		if (propertyType == null) {
			propertyType = formatter.getFormattedObjectType();
		}
		typeFormatters.put(propertyType, formatter);
	}

	public void add(Formatter<?> formatter, Annotation propertyAnnotation) {
		annotationFormatters.put(propertyAnnotation, formatter);
	}

	public T getModel() {
		return model;
	}

	public Binding getBinding(String property) {
		Binding binding = bindings.get(property);
		if (binding == null && optimisticBinding) {
			return add(new BindingConfiguration(property, null, false));
		} else {
			return binding;
		}
	}

	public void bind(Map<String, ? extends Object> propertyValues) {
		for (Map.Entry<String, ? extends Object> entry : propertyValues
				.entrySet()) {
			Binding binding = getBinding(entry.getKey());
			Object value = entry.getValue();
			if (value instanceof String[]) {
				binding.setValues((String[])value);
			} else if (value instanceof String) {
				binding.setValue((String)entry.getValue());
			} else {
				throw new IllegalArgumentException("Illegal argument " + value);
			}
		}
	}

	class BindingImpl implements Binding {

		private Expression property;

		private Formatter formatter;

		private boolean required;

		public BindingImpl(BindingConfiguration config)
				throws org.springframework.expression.ParseException {
			property = expressionParser.parseExpression(config.getProperty());
			formatter = config.getFormatter();
			required = config.isRequired();
		}

		public String getFormattedValue() {
			try {
				return format(property.getValue(createEvaluationContext()));
			} catch (ExpressionException e) {
				throw new IllegalArgumentException(e);
			}
		}

		public void setValue(String formatted) {
			Object value = parse(formatted);
			assertRequired(value);
			setValue(value);
		}

		public String format(Object possibleValue) {
			Formatter formatter = getFormatter();
			possibleValue = typeConverter.convert(possibleValue, formatter.getFormattedObjectType());
			return formatter.format(possibleValue, LocaleContextHolder.getLocale());
		}

		public boolean isCollection() {
			TypeDescriptor<?> type = TypeDescriptor.valueOf(getValueType());
			return type.isCollection() || type.isArray();
		}

		public String[] getFormattedValues() {
			Object multiValue;
			try {
				multiValue = property.getValue(createEvaluationContext());
			} catch (EvaluationException e) {
				throw new IllegalStateException(e);
			}
			if (multiValue == null) {
				return EMPTY_STRING_ARRAY;
			}
			TypeDescriptor<?> type = TypeDescriptor.valueOf(multiValue.getClass());
			String[] formattedValues;
			if (type.isCollection()) {
				Collection<?> values = ((Collection<?>)multiValue);
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

		public void setValues(String[] formattedValues) {
			Object values = Array.newInstance(getFormatter().getFormattedObjectType(), formattedValues.length);
			for (int i = 0; i < formattedValues.length; i++) {
				Array.set(values, i, parse(formattedValues[i]));
			}
			setValue(values);			
		}

		public boolean isRequired() {
			return required;
		}

		public Messages getMessages() {
			return null;
		}

		// internal helpers
		
		private void assertRequired(Object value) {
			if (required && value == null) {
				throw new IllegalArgumentException("Value required");
			}
		}
		
		private Object parse(String formatted) {
			try {
				return getFormatter().parse(formatted, LocaleContextHolder.getLocale());
			} catch (ParseException e) {
				throw new IllegalArgumentException("Invalid format " + formatted, e);
			}
		}

		private Formatter getFormatter() {
			if (formatter != null) {
				return formatter;
			} else {
				Class<?> type = getValueType();
				Formatter<?> formatter = typeFormatters.get(type);
				if (formatter != null) {
					return formatter;
				} else {
					Annotation[] annotations = getAnnotations();
					for (Annotation a : annotations) {
						formatter = annotationFormatters.get(a);
						if (formatter != null) {
							return formatter;
						}
					}
					return defaultFormatter;
				}
			}
		}

		private Class<?> getValueType() {
			try {
				// TODO Spring EL currently returns null here when value is null - not correct
				return property.getValueType(createEvaluationContext());
			} catch (EvaluationException e) {
				throw new IllegalStateException(e);
			}
		}

		private Annotation[] getAnnotations() {
			// TODO Spring EL presently gives us no way to get this information
			return new Annotation[0];
		}

		private void copy(Iterable values, String[] formattedValues) {
			int i = 0;
			for (Object value : values) {
				formattedValues[i] = format(value);
				i++;
			}
		}
		
		private void setValue(Object values) {
			try {
				property.setValue(createEvaluationContext(), values);
			} catch (ExpressionException e) {
				throw new IllegalArgumentException(e);
			}
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
