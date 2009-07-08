package org.springframework.ui.lifecycle;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.alert.Alert;
import org.springframework.ui.alert.Alerts;
import org.springframework.ui.alert.Severity;
import org.springframework.ui.alert.support.DefaultAlertContext;
import org.springframework.ui.binding.Bound;
import org.springframework.ui.binding.Model;
import org.springframework.ui.binding.support.WebBinder;
import org.springframework.ui.format.number.CurrencyFormat;
import org.springframework.ui.validation.ValidationFailure;
import org.springframework.ui.validation.Validator;
import org.springframework.ui.validation.constraint.Impact;
import org.springframework.ui.validation.constraint.Message;
import org.springframework.ui.validation.constraint.ValidationConstraint;

import edu.emory.mathcs.backport.java.util.Collections;

public class BindAndValidateLifecycleTests {

	private BindAndValidateLifecycle lifecycle;

	private TestBean model;
	
	private DefaultAlertContext alertContext;

	@Before
	public void setUp() {
		model = new TestBean();
		alertContext = new DefaultAlertContext();
		WebBinder binder = new WebBinder(model);
		binder.addBinding("string");
		binder.addBinding("integer");
		binder.addBinding("foo");
		Validator validator = new TestBeanValidator();
		lifecycle = new BindAndValidateLifecycle(binder, validator, alertContext);
	}
	
	static class TestBeanValidator implements Validator {
		public List<ValidationFailure> validate(Object model, List<String> properties) {
			TestBean bean = (TestBean) model;
			RequiredConstraint required = new RequiredConstraint();
			boolean valid = required.validate(bean);
			if (!valid) {
				
			}
			return Collections.emptyList();
		}		
	}
	
	@Message({"en=#{label} is required", "es=#{label} es necesario"})
	static class RequiredConstraint implements ValidationConstraint<Object> {
		public boolean validate(Object value) {
			if (value != null) {
				return value instanceof String ? ((String) value).length() > 0 : true;
			} else {
				return false;
			}
		}
	}
	
	@Message("#{label} is a weak password")
	@Impact(Severity.WARNING)
	static class StrongPasswordConstraint implements ValidationConstraint<String> {		
		public boolean validate(String password) {
			if (password.length() > 6) {
				return true;
			} else {
				return false;
			}
		}		
	}
	
	@Message("#{label} could not be confirmed; #{value} must match #{model.confirmPassword}")
	static class ConfirmedPasswordConstraint implements ValidationConstraint<SignupForm> {
		public boolean validate(SignupForm form) {
			if (form.password.equals(form.confirmPassword)) {
				return true;
			} else {
				return false;
			}
		}		
	}
	
	@Message("#{label} must be between #{this.min} and #{this.max}")
	static class RangeConstraint implements ValidationConstraint<Number> {
		private Long min;
		
		private Long max;
		
		public RangeConstraint(Long min, Long max) {
			this.min = min;
			this.max = max;
		}
		
		public boolean validate(Number value) {
			Long longValue = value.longValue();
			if (longValue >= min && longValue <= max) {
				return true;
			} else {
				return false;
			}
		}
	}

	
	static class TestValidationFailure implements ValidationFailure {

		private String property;
		
		private String message;
		
		public TestValidationFailure(String property, String message) {
			this.property = property;
		}

		public String getProperty() {
			return property;
		}

		public Alert getAlert() {
			return Alerts.error(message);
		}

	}

	@Test
	public void testExecuteLifecycleNoErrors() {
		Map<String, Object> userMap = new HashMap<String, Object>();
		userMap.put("string", "test");
		userMap.put("integer", "3");
		userMap.put("foo", "BAR");
		lifecycle.execute(userMap);
		assertEquals(0, alertContext.getAlerts().size());
	}

	@Test
	public void testExecuteLifecycleBindingErrors() {
		Map<String, Object> userMap = new HashMap<String, Object>();
		userMap.put("string", "test");
		userMap.put("integer", "bogus");
		userMap.put("foo", "BAR");
		lifecycle.execute(userMap);
		assertEquals(1, alertContext.getAlerts().size());
		assertEquals(Severity.FATAL, alertContext.getAlerts("integer").get(0).getSeverity());
		assertEquals("Failed to bind to property 'integer'; user value 'bogus' could not be converted to property type [java.lang.Integer]", alertContext.getAlerts("integer").get(0).getMessage());
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
		private List<FooEnum> foos;
		private List<Address> addresses;

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

		@CurrencyFormat
		public BigDecimal getCurrency() {
			return currency;
		}

		public void setCurrency(BigDecimal currency) {
			this.currency = currency;
		}

		public List<FooEnum> getFoos() {
			return foos;
		}

		public void setFoos(List<FooEnum> foos) {
			this.foos = foos;
		}

		public List<Address> getAddresses() {
			return addresses;
		}

		public void setAddresses(List<Address> addresses) {
			this.addresses = addresses;
		}

	}

	public static class Address {
		private String street;
		private String city;
		private String state;
		private String zip;
		private String country;

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}

		public String getZip() {
			return zip;
		}

		public void setZip(String zip) {
			this.zip = zip;
		}

		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}

	}
	
	@Model(value="testBean")
	public class TestAnnotatedBean {

		private String editable;
		
		private String notEditable;
		
		@Bound
		public String getEditable() {
			return editable;
		}
		
		public void setEditable(String editable) {
			this.editable = editable;
		}
		
		public String getNotEditable() {
			return notEditable;
		}
		
		public void setNotEditable(String notEditable) {
			this.notEditable = notEditable;
		}
			
	}
	
	public static class SignupForm {
		private String username;
		private String password;
		private String confirmPassword;
	}
}
