package com.kisansetu.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kisansetu.validation.ClaimRow;
import com.kisansetu.validation.Fields;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ClaimFileReaderTest {

    private final ClaimFileReader reader = new ClaimFileReader();
    private final String header = String.join(",", Fields.ALL);

    private static ByteArrayInputStream bytes(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void readsCsvWithSheetRowNumbersAndSkipsBlankRows() throws IOException {
        String csv = header + "\n"
                + "KCC1,Ramesh Patel" + ",".repeat(Fields.ALL.size() - 2) + "\n"
                + ",".repeat(Fields.ALL.size() - 1) + "\n"
                + "KCC2,Sunita Devi" + ",".repeat(Fields.ALL.size() - 2) + "\n";

        List<ClaimRow> rows = reader.read("claims.csv", bytes(csv));

        assertThat(rows.size()).isEqualTo(2);
        assertThat(rows.get(0).rowNo()).isEqualTo(2);
        assertThat(rows.get(0).get(Fields.FULL_NAME)).isEqualTo("Ramesh Patel");
        assertThat(rows.get(1).rowNo()).isEqualTo(4);
    }

    @Test
    void readsCsvWithByteOrderMarkAndQuotedCommas() throws IOException {
        String csv = "\uFEFF" + header + "\n"
                + "KCC1,\"Patel, Ramesh\"" + ",".repeat(Fields.ALL.size() - 2) + "\n";

        List<ClaimRow> rows = reader.read("claims.CSV", bytes(csv));

        assertThat(rows.get(0).get(Fields.KCC_ACCOUNT_NO)).isEqualTo("KCC1");
        assertThat(rows.get(0).get(Fields.FULL_NAME)).isEqualTo("Patel, Ramesh");
    }

    @Test
    void readsXlsxNumbersAsPlainDigitsAndDatesAsIso() throws IOException {
        byte[] file;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("claims");
            Row head = sheet.createRow(0);
            for (int i = 0; i < Fields.ALL.size(); i++) {
                head.createCell(i).setCellValue(Fields.ALL.get(i));
            }
            Row data = sheet.createRow(1);
            data.createCell(Fields.ALL.indexOf(Fields.AADHAAR)).setCellValue(234123412346.0);
            data.createCell(Fields.ALL.indexOf(Fields.LOAN_AMOUNT)).setCellValue(150000.0);
            data.createCell(Fields.ALL.indexOf(Fields.AREA_HECTARES)).setCellValue(1.25);

            CreationHelper helper = workbook.getCreationHelper();
            DataFormat dataFormat = helper.createDataFormat();
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(dataFormat.getFormat("dd/mm/yyyy"));
            Cell dob = data.createCell(Fields.ALL.indexOf(Fields.DATE_OF_BIRTH));
            dob.setCellValue(java.sql.Date.valueOf(LocalDate.of(1985, 4, 12)));
            dob.setCellStyle(dateStyle);

            workbook.write(out);
            file = out.toByteArray();
        }

        List<ClaimRow> rows = reader.read("claims.xlsx", new ByteArrayInputStream(file));

        assertThat(rows.size()).isEqualTo(1);
        assertThat(rows.get(0).rowNo()).isEqualTo(2);
        assertThat(rows.get(0).get(Fields.AADHAAR)).isEqualTo("234123412346");
        assertThat(rows.get(0).get(Fields.LOAN_AMOUNT)).isEqualTo("150000");
        assertThat(rows.get(0).get(Fields.AREA_HECTARES)).isEqualTo("1.25");
        assertThat(rows.get(0).get(Fields.DATE_OF_BIRTH)).isEqualTo("1985-04-12");
    }

    @Test
    void rejectsUnsupportedFileType() {
        assertThatThrownBy(() -> reader.read("claims.pdf", bytes("x")))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("Unsupported");
    }

    @Test
    void rejectsCorruptXlsx() {
        assertThatThrownBy(() -> reader.read("claims.xlsx", bytes("this is not a spreadsheet")))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("could not be read");
    }

    @Test
    void rejectsEmptyCsv() {
        assertThatThrownBy(() -> reader.read("claims.csv", bytes("")))
                .isInstanceOf(InvalidUploadException.class);
    }

    @Test
    void rejectsCsvWithMissingColumns() {
        assertThatThrownBy(() -> reader.read("claims.csv", bytes("kcc_account_no,full_name\nKCC1,Ramesh\n")))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("Missing required column");
    }
}
