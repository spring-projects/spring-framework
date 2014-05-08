package org.springframework.web.servlet.view.document;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.Resource;

import java.io.IOException;

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
