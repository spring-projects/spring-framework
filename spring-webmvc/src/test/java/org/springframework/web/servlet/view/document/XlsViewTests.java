/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.view.document;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import org.springframework.web.servlet.View;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AbstractXlsView and its subclasses.
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
@SuppressWarnings("removal")
class XlsViewTests {

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private final MockHttpServletResponse response = new MockHttpServletResponse();


	@Test
	void xls() throws Exception {
		View excelView = new AbstractXlsView() {
			@Override
			protected void buildExcelDocument(Map<String, Object> model, Workbook workbook,
					HttpServletRequest request, HttpServletResponse response) {
				Sheet sheet = workbook.createSheet("Test Sheet");
				Row row = sheet.createRow(0);
				Cell cell = row.createCell(0);
				cell.setCellValue("Test Value");
			}
		};

		excelView.render(new HashMap<>(), request, response);

		Workbook wb = new HSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
		assertThat(wb.getSheetName(0)).isEqualTo("Test Sheet");
		Sheet sheet = wb.getSheet("Test Sheet");
		Row row = sheet.getRow(0);
		Cell cell = row.getCell(0);
		assertThat(cell.getStringCellValue()).isEqualTo("Test Value");
	}

	@Test
	void xlsxView() throws Exception {
		View excelView = new AbstractXlsxView() {
			@Override
			protected void buildExcelDocument(Map<String, Object> model, Workbook workbook,
					HttpServletRequest request, HttpServletResponse response) {
				Sheet sheet = workbook.createSheet("Test Sheet");
				Row row = sheet.createRow(0);
				Cell cell = row.createCell(0);
				cell.setCellValue("Test Value");
			}
		};

		excelView.render(new HashMap<>(), request, response);

		Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
		assertThat(wb.getSheetName(0)).isEqualTo("Test Sheet");
		Sheet sheet = wb.getSheet("Test Sheet");
		Row row = sheet.getRow(0);
		Cell cell = row.getCell(0);
		assertThat(cell.getStringCellValue()).isEqualTo("Test Value");
	}

	@Test
	void xlsxStreamingView() throws Exception {
		View excelView = new AbstractXlsxStreamingView() {
			@Override
			protected void buildExcelDocument(Map<String, Object> model, Workbook workbook,
					HttpServletRequest request, HttpServletResponse response) {
				Sheet sheet = workbook.createSheet("Test Sheet");
				Row row = sheet.createRow(0);
				Cell cell = row.createCell(0);
				cell.setCellValue("Test Value");
			}
		};

		excelView.render(new HashMap<>(), request, response);

		Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()));
		assertThat(wb.getSheetName(0)).isEqualTo("Test Sheet");
		Sheet sheet = wb.getSheet("Test Sheet");
		Row row = sheet.getRow(0);
		Cell cell = row.getCell(0);
		assertThat(cell.getStringCellValue()).isEqualTo("Test Value");
	}

}
