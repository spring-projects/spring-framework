package org.springframework.ui.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.ui.format.DateFormatter;
import org.springframework.ui.format.number.CurrencyFormatter;

public class BinderTests {

	@Test
	public void bindSingleValuesWithDefaultTypeConverterConversion() {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		Map<String, String> propertyValues = new HashMap<String, String>();
		propertyValues.put("string", "test");
		propertyValues.put("integer", "3");
		propertyValues.put("foo", "BAR");
		binder.bind(propertyValues);
		assertEquals("test", binder.getModel().getString());
		assertEquals(3, binder.getModel().getInteger());
		assertEquals(FooEnum.BAR, binder.getModel().getFoo());
	}

	// TODO should update error context, not throw exception
	@Test(expected=IllegalArgumentException.class)
	public void bindSingleValuesWithDefaultTypeCoversionFailures() {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		Map<String, String> propertyValues = new HashMap<String, String>();
		propertyValues.put("string", "test");
		propertyValues.put("integer", "bogus");
		propertyValues.put("foo", "bogus");
		binder.bind(propertyValues);
	}

	@Test
	public void bindSingleValuePropertyFormatterParsing() throws ParseException {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		binder.add(new BindingConfiguration("date", new DateFormatter(), false));
		Map<String, String> propertyValues = new HashMap<String, String>();
		propertyValues.put("date", "2009-06-01");
		binder.bind(propertyValues);
		assertEquals(new DateFormatter().parse("2009-06-01", Locale.US), binder.getModel().getDate());
	}

	// TODO should update error context, not throw exception
	@Test(expected=IllegalArgumentException.class)	
	public void bindSingleValuePropertyFormatterParseException() {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		binder.add(new BindingConfiguration("date", new DateFormatter(), false));
		Map<String, String> propertyValues = new HashMap<String, String>();
		propertyValues.put("date", "bogus");
		binder.bind(propertyValues);
	}

	@Test
	@Ignore
	public void bindSingleValueTypeFormatterParsing() throws ParseException {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		binder.add(new DateFormatter(), Date.class);
		Map<String, String> propertyValues = new HashMap<String, String>();
		propertyValues.put("date", "2009-06-01");
		// TODO presently fails because Spring EL does not obtain property valueType using property metadata
		// instead it relies on value itself being not null
		// talk to andy about this
		binder.bind(propertyValues);
		assertEquals(new DateFormatter().parse("2009-06-01", Locale.US), binder.getModel().getDate());
	}
	
	@Test
	@Ignore
	public void bindSingleValueAnnotationFormatterParsing() throws ParseException {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		binder.add(new CurrencyFormatter(), Currency.class);
		Map<String, String> propertyValues = new HashMap<String, String>();
		propertyValues.put("currency", "$23.56");
		// TODO presently fails because Spring EL does not obtain property valueType using property metadata
		// instead it relies on value itself being not null
		// talk to andy about this
		binder.bind(propertyValues);
		assertEquals(new BigDecimal("23.56"), binder.getModel().getCurrency());
	}
	
	@Test
	public void getBindingOptimistic() {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		Binding b = binder.getBinding("integer");
		assertFalse(b.isRequired());
		assertFalse(b.isCollection());
		assertEquals("0", b.getFormattedValue());
		b.setValue("5");
		assertEquals("5", b.getFormattedValue());
	}

	@Test
	public void getBindingCustomFormatter() {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		binder.add(new BindingConfiguration("currency", new CurrencyFormatter(), false));
		Binding b = binder.getBinding("currency");
		assertFalse(b.isRequired());
		assertFalse(b.isCollection());
		assertEquals("", b.getFormattedValue());
		b.setValue("$23.56");
		assertEquals("$23.56", b.getFormattedValue());
	}
	
	// TODO  should update error context, not throw exception
	@Test(expected=IllegalArgumentException.class)
	public void getBindingRequired() {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		binder.add(new BindingConfiguration("string", null, true));
		Binding b = binder.getBinding("string");
		assertTrue(b.isRequired());
		assertFalse(b.isCollection());
		assertEquals("", b.getFormattedValue());
		b.setValue("");
	}
	
	@Test
	public void getBindingMultiValued() {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		Binding b = binder.getBinding("foos");
		assertTrue(b.isCollection());
		assertEquals(0, b.getFormattedValues().length);
		b.setValues(new String[] { "BAR", "BAZ", "BOOP" });
		assertEquals(FooEnum.BAR, binder.getModel().getFoos().get(0));
		assertEquals(FooEnum.BAZ, binder.getModel().getFoos().get(1));
		assertEquals(FooEnum.BOOP, binder.getModel().getFoos().get(2));
		String[] values = b.getFormattedValues();
		assertEquals(3, values.length);
		assertEquals("BAR", values[0]);
		assertEquals("BAZ", values[1]);
		assertEquals("BOOP", values[2]);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void getBindingMultiValuedTypeConversionError() {
		Binder<TestBean> binder = new Binder<TestBean>(new TestBean());
		Binding b = binder.getBinding("foos");
		assertTrue(b.isCollection());
		assertEquals(0, b.getFormattedValues().length);
		b.setValues(new String[] { "BAR", "BOGUS", "BOOP" });
	}

	public static enum FooEnum {
		BAR, BAZ, BOOP;
	}

	public static class TestBean {
		private String string;
		private int integer;
		private Date date;
		private FooEnum foo;
		private BigDecimal currency;
		private List<FooEnum> foos = new ArrayList<FooEnum>();
		
		public String getString() {
			return string;
		}

		public void setString(String string) {
			this.string = string;
		}

		public int getInteger() {
			return integer;
		}

		public void setInteger(int integer) {
			this.integer = integer;
		}

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}

		public FooEnum getFoo() {
			return foo;
		}

		public void setFoo(FooEnum foo) {
			this.foo = foo;
		}

		public BigDecimal getCurrency() {
			return currency;
		}

		@Currency
		public void setCurrency(BigDecimal currency) {
			this.currency = currency;
		}

		public List<FooEnum> getFoos() {
			return foos;
		}

		public void setFoos(List<FooEnum> foos) {
			this.foos = foos;
		}
		
	}

	public @interface Currency {

	}
}
