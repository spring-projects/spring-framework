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

package org.springframework.web.servlet.view.document;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;

/**
 * Tests for the AbstractExcelView and the AbstractJExcelView classes.
 *
 * @author Alef Arendsen
 * @author Bram Smeets
 */
public class ExcelViewTests extends TestCase {

	private MockServletContext servletCtx;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private StaticWebApplicationContext webAppCtx;

	@Override
	public void setUp() {
		servletCtx = new MockServletContext("org/springframework/web/servlet/view/document");
		request = new MockHttpServletRequest(servletCtx);
		response = new MockHttpServletResponse();
		webAppCtx = new StaticWebApplicationContext();
		webAppCtx.setServletContext(servletCtx);
	}

	public void testExcel() throws Exception {
		AbstractExcelView excelView = new AbstractExcelView() {
			@Override
			protected void buildExcelDocument(Map model, HSSFWorkbook wb,
					HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				HSSFSheet sheet = wb.createSheet();
				wb.setSheetName(0, "Test Sheet");

				// test all possible permutation of row or column not existing
				HSSFCell cell = getCell(sheet, 2, 4);
				cell.setCellValue("Test Value");
				cell = getCell(sheet, 2, 3);
				setText(cell, "Test Value");
				cell = getCell(sheet, 3, 4);
				setText(cell, "Test Value");
				cell = getCell(sheet, 2, 4);
				setText(cell, "Test Value");
			}
		};

		excelView.render(new HashMap(), request, response);

		POIFSFileSystem poiFs = new POIFSFileSystem(new ByteArrayInputStream(response.getContentAsByteArray()));
		HSSFWorkbook wb = new HSSFWorkbook(poiFs);
		assertEquals("Test Sheet", wb.getSheetName(0));
		HSSFSheet sheet = wb.getSheet("Test Sheet");
		HSSFRow row = sheet.getRow(2);
		HSSFCell cell = row.getCell((short) 4);
		assertEquals("Test Value", cell.getStringCellValue());
	}

	public void testExcelWithTemplateNoLoc() throws Exception {
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE,
				newDummyLocaleResolver("nl", "nl"));

		AbstractExcelView excelView = new AbstractExcelView() {
			@Override
			protected void buildExcelDocument(Map model, HSSFWorkbook wb,
					HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				HSSFSheet sheet = wb.getSheet("Sheet1");

				// test all possible permutation of row or column not existing
				HSSFCell cell = getCell(sheet, 2, 4);
				cell.setCellValue("Test Value");
				cell = getCell(sheet, 2, 3);
				setText(cell, "Test Value");
				cell = getCell(sheet, 3, 4);
				setText(cell, "Test Value");
				cell = getCell(sheet, 2, 4);
				setText(cell, "Test Value");
			}
		};

		excelView.setApplicationContext(webAppCtx);
		excelView.setUrl("template");
		excelView.render(new HashMap(), request, response);

		POIFSFileSystem poiFs = new POIFSFileSystem(new ByteArrayInputStream(response.getContentAsByteArray()));
		HSSFWorkbook wb = new HSSFWorkbook(poiFs);
		HSSFSheet sheet = wb.getSheet("Sheet1");
		HSSFRow row = sheet.getRow(0);
		HSSFCell cell = row.getCell((short) 0);
		assertEquals("Test Template", cell.getStringCellValue());
	}

	public void testExcelWithTemplateAndCountryAndLanguage() throws Exception {
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE,
				newDummyLocaleResolver("en", "US"));

		AbstractExcelView excelView = new AbstractExcelView() {
			@Override
			protected void buildExcelDocument(Map model, HSSFWorkbook wb,
					HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				HSSFSheet sheet = wb.getSheet("Sheet1");

				// test all possible permutation of row or column not existing
				HSSFCell cell = getCell(sheet, 2, 4);
				cell.setCellValue("Test Value");
				cell = getCell(sheet, 2, 3);
				setText(cell, "Test Value");
				cell = getCell(sheet, 3, 4);
				setText(cell, "Test Value");
				cell = getCell(sheet, 2, 4);
				setText(cell, "Test Value");
			}
		};

		excelView.setApplicationContext(webAppCtx);
		excelView.setUrl("template");
		excelView.render(new HashMap(), request, response);

		POIFSFileSystem poiFs = new POIFSFileSystem(new ByteArrayInputStream(response.getContentAsByteArray()));
		HSSFWorkbook wb = new HSSFWorkbook(poiFs);
		HSSFSheet sheet = wb.getSheet("Sheet1");
		HSSFRow row = sheet.getRow(0);
		HSSFCell cell = row.getCell((short) 0);
		assertEquals("Test Template American English", cell.getStringCellValue());
	}

	public void testExcelWithTemplateAndLanguage() throws Exception {
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE,
				newDummyLocaleResolver("de", ""));

		AbstractExcelView excelView = new AbstractExcelView() {
			@Override
			protected void buildExcelDocument(Map model, HSSFWorkbook wb,
					HttpServletRequest request, HttpServletResponse response)
					throws Exception {
				HSSFSheet sheet = wb.getSheet("Sheet1");

				// test all possible permutation of row or column not existing
				HSSFCell cell = getCell(sheet, 2, 4);
				cell.setCellValue("Test Value");
				cell = getCell(sheet, 2, 3);
				setText(cell, "Test Value");
				cell = getCell(sheet, 3, 4);
				setText(cell, "Test Value");
				cell = getCell(sheet, 2, 4);
				setText(cell, "Test Value");
			}
		};

		excelView.setApplicationContext(webAppCtx);
		excelView.setUrl("template");
		excelView.render(new HashMap(), request, response);

		POIFSFileSystem poiFs = new POIFSFileSystem(new ByteArrayInputStream(response.getContentAsByteArray()));
		HSSFWorkbook wb = new HSSFWorkbook(poiFs);
		HSSFSheet sheet = wb.getSheet("Sheet1");
		HSSFRow row = sheet.getRow(0);
		HSSFCell cell = row.getCell((short) 0);
		assertEquals("Test Template auf Deutsch", cell.getStringCellValue());
	}

	public void testJExcel() throws Exception {
		AbstractJExcelView excelView = new AbstractJExcelView() {
			@Override
			protected void buildExcelDocument(Map model,
					WritableWorkbook wb,
					HttpServletRequest request,
					HttpServletResponse response)
					throws Exception {
				WritableSheet sheet = wb.createSheet("Test Sheet", 0);

				// test all possible permutation of row or column not existing
				sheet.addCell(new Label(2, 4, "Test Value"));
				sheet.addCell(new Label(2, 3, "Test Value"));
				sheet.addCell(new Label(3, 4, "Test Value"));
				sheet.addCell(new Label(2, 4, "Test Value"));
			}
		};

		excelView.render(new HashMap(), request, response);

		Workbook wb = Workbook.getWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
		assertEquals("Test Sheet", wb.getSheet(0).getName());
		Sheet sheet = wb.getSheet("Test Sheet");
		Cell cell = sheet.getCell(2, 4);
		assertEquals("Test Value", cell.getContents());
	}

	public void testJExcelWithTemplateNoLoc() throws Exception {
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE,
				newDummyLocaleResolver("nl", "nl"));

		AbstractJExcelView excelView = new AbstractJExcelView() {
			@Override
			protected void buildExcelDocument(Map model,
					WritableWorkbook wb,
					HttpServletRequest request,
					HttpServletResponse response)
					throws Exception {
				WritableSheet sheet = wb.getSheet("Sheet1");

				// test all possible permutation of row or column not existing
				sheet.addCell(new Label(2, 4, "Test Value"));
				sheet.addCell(new Label(2, 3, "Test Value"));
				sheet.addCell(new Label(3, 4, "Test Value"));
				sheet.addCell(new Label(2, 4, "Test Value"));
			}
		};

		excelView.setApplicationContext(webAppCtx);
		excelView.setUrl("template");
		excelView.render(new HashMap(), request, response);

		Workbook wb = Workbook.getWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
		Sheet sheet = wb.getSheet("Sheet1");
		Cell cell = sheet.getCell(0, 0);
		assertEquals("Test Template", cell.getContents());
	}

	public void testJExcelWithTemplateAndCountryAndLanguage() throws Exception {
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE,
				newDummyLocaleResolver("en", "US"));

		AbstractJExcelView excelView = new AbstractJExcelView() {
			@Override
			protected void buildExcelDocument(Map model,
					WritableWorkbook wb,
					HttpServletRequest request,
					HttpServletResponse response)
					throws Exception {
				WritableSheet sheet = wb.getSheet("Sheet1");

				// test all possible permutation of row or column not existing
				sheet.addCell(new Label(2, 4, "Test Value"));
				sheet.addCell(new Label(2, 3, "Test Value"));
				sheet.addCell(new Label(3, 4, "Test Value"));
				sheet.addCell(new Label(2, 4, "Test Value"));
			}
		};

		excelView.setApplicationContext(webAppCtx);
		excelView.setUrl("template");
		excelView.render(new HashMap(), request, response);

		Workbook wb = Workbook.getWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
		Sheet sheet = wb.getSheet("Sheet1");
		Cell cell = sheet.getCell(0, 0);
		assertEquals("Test Template American English", cell.getContents());
	}

	public void testJExcelWithTemplateAndLanguage() throws Exception {
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE,
				newDummyLocaleResolver("de", ""));

		AbstractJExcelView excelView = new AbstractJExcelView() {
			@Override
			protected void buildExcelDocument(Map model,
					WritableWorkbook wb,
					HttpServletRequest request,
					HttpServletResponse response)
					throws Exception {
				WritableSheet sheet = wb.getSheet("Sheet1");

				// test all possible permutation of row or column not existing
				sheet.addCell(new Label(2, 4, "Test Value"));
				sheet.addCell(new Label(2, 3, "Test Value"));
				sheet.addCell(new Label(3, 4, "Test Value"));
				sheet.addCell(new Label(2, 4, "Test Value"));
			}
		};

		excelView.setApplicationContext(webAppCtx);
		excelView.setUrl("template");
		excelView.render(new HashMap(), request, response);

		Workbook wb = Workbook.getWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
		Sheet sheet = wb.getSheet("Sheet1");
		Cell cell = sheet.getCell(0, 0);
		assertEquals("Test Template auf Deutsch", cell.getContents());
	}

	private LocaleResolver newDummyLocaleResolver(final String lang, final String country) {
		return new LocaleResolver() {
			@Override
			public Locale resolveLocale(HttpServletRequest request) {
				return new Locale(lang, country);
			}

			@Override
			public void setLocale(HttpServletRequest request,
					HttpServletResponse response, Locale locale) {
				// not supported!

			}
		};
	}

}
