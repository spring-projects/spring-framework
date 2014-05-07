package org.springframework.web.servlet.view.document;
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
}
