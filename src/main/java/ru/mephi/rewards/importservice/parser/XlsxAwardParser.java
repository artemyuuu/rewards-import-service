package ru.mephi.rewards.importservice.parser;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import ru.mephi.rewards.importservice.exception.InvalidFileStructureException;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class XlsxAwardParser implements AwardRecordParser {

    @Override
    public boolean supports(String filename, String contentType) {
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return name.endsWith(".xlsx") ||
                type.contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Override
    public Stream<AwardRecord> parse(InputStream in) {
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new InvalidFileStructureException("Empty Excel sheet");
            }

            // читаем первую строку как заголовок
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new InvalidFileStructureException("Missing header");
            }

            List<String> header = readRow(headerRow);
            validateHeader(header);

            // итерация по строкам данных
            Iterator<Row> it = sheet.rowIterator();
            it.next(); // пропускаем header
            final int[] rowNumber = {2};

            Iterator<AwardRecord> recordIterator = new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public AwardRecord next() {
                    Row row = it.next();
                    int current = rowNumber[0]++;

                    List<String> cells = readRow(row);
                    if (cells.size() != 5) {
                        throw new InvalidFileStructureException("Invalid columns count at row " + current);
                    }

                    Long employeeId = parseLongSafe(cells.get(0));
                    String employeeFullName = cells.get(1);
                    Long rewardId = parseLongSafe(cells.get(2));
                    String rewardName = cells.get(3);
                    OffsetDateTime awardedAt = parseDateSafe(cells.get(4));

                    return new AwardRecord(employeeId, employeeFullName, rewardId, rewardName, awardedAt, current);
                }
            };

            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(recordIterator, 0), false);

        } catch (IOException e) {
            throw new InvalidFileStructureException("Failed to read Excel file: " + e.getMessage(), e);
        }
    }


    private List<String> readRow(Row row) {
        List<String> cells = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cells.add(getCellValue(cell));
        }
        return cells;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    // конвертим дату в ISO-8601
                    yield cell.getLocalDateTimeCellValue().toString();
                } else {
                    yield String.valueOf((long) cell.getNumericCellValue());
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private void validateHeader(List<String> header) {
        List<String> expected = List.of(
                "employee_id", "employee_full_name", "reward_id", "reward_name", "awarded_at"
        );
        List<String> normalized = header.stream()
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .toList();

        if (!normalized.equals(expected)) {
            throw new InvalidFileStructureException("Invalid header");
        }
    }

    private Long parseLongSafe(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try {
            return Long.parseLong(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private OffsetDateTime parseDateSafe(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try {
            return OffsetDateTime.parse(t);
        } catch (Exception e) {
            if (t.matches("\\d{4}-\\d{2}-\\d{2}")) {
                try {
                    return OffsetDateTime.parse(t + "T00:00:00Z");
                } catch (Exception ignore) {
                    return null;
                }
            }
            return null;
        }
    }
}
