package org.springframework.ui.format.support;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;
import org.springframework.ui.format.support.MethodInvokingFormatter;

public class MethodInvokingFormatterTests {

	private MethodInvokingFormatter formatter = new MethodInvokingFormatter(AccountNumber.class, "getFormatted",
			"valueOf");

	private MethodInvokingFormatter formatter2 = new MethodInvokingFormatter(I8nAccountNumber.class, "getFormatted",
			"valueOf");

	@Test
	public void testFormat() {
		assertEquals("123456789", formatter.format(new AccountNumber(123456789L), null));
	}

	@Test
	public void testParse() {
		assertEquals(new Long(123456789), ((AccountNumber) formatter.parse("123456789", null)).number);
	}

	@Test
	public void testFormatI18n() {
		assertEquals("123456789", formatter2.format(new I8nAccountNumber(123456789L), Locale.GERMAN));
	}

	@Test
	public void testParseI18n() {
		assertEquals(new Long(123456789), ((I8nAccountNumber) formatter2.parse("123456789", Locale.GERMAN)).number);
	}

	public static class AccountNumber {

		private Long number;

		public AccountNumber(Long number) {
			this.number = number;
		}

		public String getFormatted() {
			return number.toString();
		}

		public static AccountNumber valueOf(String str) {
			return new AccountNumber(Long.valueOf(str));
		}

	}

	public static class I8nAccountNumber {

		private Long number;

		public I8nAccountNumber(Long number) {
			this.number = number;
		}

		public String getFormatted(Locale locale) {
			assertEquals(Locale.GERMAN, locale);
			return number.toString();
		}

		public static I8nAccountNumber valueOf(String str, Locale locale) {
			assertEquals(Locale.GERMAN, locale);
			return new I8nAccountNumber(Long.valueOf(str));
		}

	}
}
