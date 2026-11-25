package com.kisansetu.upload;

import com.kisansetu.domain.ValidationError;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/** Builds the error report that bank staff download: one line per problem, with a suggested fix. */
@Component
public class ErrorReportWriter {

    private static final String[] HEADERS = {"Row", "Field", "Error code", "Problem", "Suggested fix"};
    private static final int[] WIDTHS = {8, 20, 30, 70, 55};

    public byte[] write(List<ValidationError> errors) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Errors");

            Font bold = workbook.createFont();
            bold.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(bold);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (ValidationError error : errors) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(error.getRowNo());
                row.createCell(1).setCellValue(error.getFieldName());
                row.createCell(2).setCellValue(error.getErrorCode());
                row.createCell(3).setCellValue(error.getMessage());
                row.createCell(4).setCellValue(error.getSuggestion() == null ? "" : error.getSuggestion());
            }

            for (int i = 0; i < WIDTHS.length; i++) {
                sheet.setColumnWidth(i, WIDTHS[i] * 256);
            }
            sheet.createFreezePane(0, 1);

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
