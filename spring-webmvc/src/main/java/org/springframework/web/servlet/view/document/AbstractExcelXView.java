package org.springframework.web.servlet.view.document;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 * Convenient superclass for Excel (Open XML Format) document views.
 * Compatible with Apache POI 3.5 and higher, as of Spring 4.0.
 *
 * <p>For working with the workbook in the subclass, see
 * <a href="http://jakarta.apache.org/poi/index.html">Jakarta's POI site</a>
 *
 * <p>As an example, you can try this snippet:
 *
 * <pre class="code">
 * protected void buildExcelDocument(
 *     Map&lt;String, Object&gt; model, XSSFWorkbook workbook,
 *     HttpServletRequest request, HttpServletResponse response) {
 *
 *   // Go to the first sheet.
 *   // getSheetAt: only if workbook is created from an existing document
 * 	 // XSSFSheet sheet = workbook.getSheetAt(0);
 * 	 XSSFSheet sheet = workbook.createSheet("Spring");
 * 	 sheet.setDefaultColumnWidth(12);
 *
 *   // Write a text at A1.
 *   XSSFCell cell = getCell(sheet, 0, 0);
 *   setText(cell, "Spring POI test");
 *
 *   // Write the current date at A2.
 *   XSSFCellStyle dateStyle = workbook.createCellStyle();
 *   dateStyle.setDataFormat(XSSFDataFormat.getBuiltinFormat("m/d/yy"));
 *   cell = getCell(sheet, 1, 0);
 *   cell.setCellValue(new Date());
 *   cell.setCellStyle(dateStyle);
 *
 *   // Write a number at A3
 *   getCell(sheet, 2, 0).setCellValue(458);
 *
 *   // Write a range of numbers.
 *   XSSFRow sheetRow = sheet.createRow(3);
 *   for (short i = 0; i < 10; i++) {
 *     sheetRow.createCell(i).setCellValue(i * 10);
 *   }
 * }</pre>
 *
 * This class is similar to the AbstractPdfView class in usage style.
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @see AbstractPdfView
 */
public abstract class AbstractExcelXView extends AbstractPoiExcelView<XSSFWorkbook> {

    /**
     * The content type for an Excel response
     */
    private static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * The extension to look for existing templates
     */
    private static final String EXTENSION = ".xlsx";

    /**
     * Default Constructor.
     * Sets the content type of the view to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
     * and extension to ".xlsx".
     */
    public AbstractExcelXView() {
        setContentType(CONTENT_TYPE);
        setExtension(EXTENSION);
    }

    @Override
    protected XSSFWorkbook createWorkbook() {
        return new XSSFWorkbook();
    }

    @Override
    protected XSSFWorkbook createWorkbookFromTemplate(Resource resource) throws IOException {
        return new XSSFWorkbook(resource.getInputStream());
    }


    /**
     * Convenient method to obtain the cell in the given sheet, row and column.
     * <p>Creates the row and the cell if they still doesn't already exist.
     * Thus, the column can be passed as an int, the method making the needed downcasts.
     * @param sheet a sheet object. The first sheet is usually obtained by workbook.getSheetAt(0)
     * @param row thr row number
     * @param col the column number
     * @return the XSSFCell
     */
    protected XSSFCell getCell(XSSFSheet sheet, int row, int col) {
        XSSFRow sheetRow = sheet.getRow(row);
        if (sheetRow == null) {
            sheetRow = sheet.createRow(row);
        }
        XSSFCell cell = sheetRow.getCell(col);
        if (cell == null) {
            cell = sheetRow.createCell(col);
        }
        return cell;
    }

    /**
     * Convenient method to set a String as text content in a cell.
     * @param cell the cell in which the text must be put
     * @param text the text to put in the cell
     */
    protected void setText(XSSFCell cell, String text) {
        cell.setCellType(XSSFCell.CELL_TYPE_STRING);
        cell.setCellValue(text);
    }
}
