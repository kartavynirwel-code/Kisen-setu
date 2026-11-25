package com.kisansetu.upload;

import com.kisansetu.validation.ClaimRow;
import com.kisansetu.validation.Fields;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

/**
 * Reads an uploaded .xlsx (first sheet) or .csv (UTF-8) into rows. The first row must be the header.
 * Row numbers are the row numbers a person sees in the sheet, so row 1 is the header and data starts at row 2.
 * Nothing is validated or fixed here; values are passed on exactly as text.
 */
@Component
public class ClaimFileReader {

    static final int MAX_ROWS = 5000;

    public List<ClaimRow> read(String fileName, InputStream in) throws IOException {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".csv")) {
                return readCsv(in);
            }
            if (name.endsWith(".xlsx")) {
                return readXlsx(in);
            }
        } catch (InvalidUploadException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new InvalidUploadException("The file could not be read. Check that it is a valid .xlsx or .csv file");
        }
        throw new InvalidUploadException("Unsupported file type. Upload a .xlsx or .csv file");
    }

    private List<ClaimRow> readCsv(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        reader.mark(1);
        if (reader.read() != 0xFEFF) { // skip the BOM that Excel's "CSV UTF-8" adds
            reader.reset();
        }
        CSVFormat format = CSVFormat.DEFAULT.builder().setIgnoreEmptyLines(true).setTrim(true).build();
        try (CSVParser parser = format.parse(reader)) {
            Iterator<CSVRecord> records = parser.iterator();
            if (!records.hasNext()) {
                throw new InvalidUploadException("The file is empty");
            }
            Map<String, Integer> columns = HeaderMapper.map(records.next().toList());

            List<ClaimRow> rows = new ArrayList<>();
            while (records.hasNext()) {
                CSVRecord record = records.next();
                Map<String, String> values = new HashMap<>();
                columns.forEach((field, index) -> values.put(field, index < record.size() ? record.get(index) : ""));
                addIfNotBlank(rows, new ClaimRow((int) record.getRecordNumber(), values));
            }
            return rows;
        }
    }

    private List<ClaimRow> readXlsx(InputStream in) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            int headerIndex = sheet.getFirstRowNum();
            Row headerRow = sheet.getRow(headerIndex);
            if (headerRow == null) {
                throw new InvalidUploadException("The first sheet is empty");
            }
            List<String> headers = new ArrayList<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                headers.add(cellText(headerRow.getCell(c)));
            }
            Map<String, Integer> columns = HeaderMapper.map(headers);

            List<ClaimRow> rows = new ArrayList<>();
            for (int r = headerIndex + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new HashMap<>();
                columns.forEach((field, index) -> values.put(field, cellText(row.getCell(index))));
                addIfNotBlank(rows, new ClaimRow(r + 1, values));
            }
            return rows;
        }
    }

    private static void addIfNotBlank(List<ClaimRow> rows, ClaimRow row) {
        boolean blank = Fields.ALL.stream().allMatch(f -> !row.has(f));
        if (blank) {
            return;
        }
        if (rows.size() >= MAX_ROWS) {
            throw new InvalidUploadException("The file has more than " + MAX_ROWS + " rows. Split it into smaller files");
        }
        rows.add(row);
    }

    /** Text of a cell as a person would read it: numbers without exponent, date cells as yyyy-MM-dd. */
    private static String cellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        return switch (type) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : CellFormat.numberToText(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
