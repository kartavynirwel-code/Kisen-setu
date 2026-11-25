package com.kisansetu.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kisansetu.validation.Fields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HeaderMapperTest {

    private List<String> allColumns() {
        return new ArrayList<>(Fields.ALL);
    }

    @Test
    void mapsColumnsToPositions() {
        Map<String, Integer> columns = HeaderMapper.map(allColumns());
        assertThat(columns.get(Fields.KCC_ACCOUNT_NO)).isEqualTo(0);
        assertThat(columns.size()).isEqualTo(Fields.ALL.size());
    }

    @Test
    void matchesHeadersIgnoringCaseSpacesAndHyphens() {
        List<String> headers = allColumns();
        headers.set(0, "  KCC Account No ");
        headers.set(1, "Full-Name");
        headers.set(2, "\uFEFFDate of Birth");

        Map<String, Integer> columns = HeaderMapper.map(headers);

        assertThat(columns.get(Fields.KCC_ACCOUNT_NO)).isEqualTo(0);
        assertThat(columns.get(Fields.FULL_NAME)).isEqualTo(1);
        assertThat(columns.get(Fields.DATE_OF_BIRTH)).isEqualTo(2);
    }

    @Test
    void optionalColumnsMayBeAbsentAndUnknownColumnsAreIgnored() {
        List<String> headers = new ArrayList<>(Fields.REQUIRED);
        headers.add("remarks");
        Map<String, Integer> columns = HeaderMapper.map(headers);
        assertThat(columns.size()).isEqualTo(Fields.REQUIRED.size());
    }

    @Test
    void rejectsMissingRequiredColumnsAndNamesThem() {
        List<String> headers = allColumns();
        headers.remove(Fields.AADHAAR);
        headers.remove(Fields.IFSC);

        assertThatThrownBy(() -> HeaderMapper.map(headers))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("aadhaar")
                .hasMessageContaining("ifsc");
    }

    @Test
    void rejectsDuplicateColumns() {
        List<String> headers = allColumns();
        headers.add("Aadhaar");

        assertThatThrownBy(() -> HeaderMapper.map(headers))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("more than once");
    }
}
