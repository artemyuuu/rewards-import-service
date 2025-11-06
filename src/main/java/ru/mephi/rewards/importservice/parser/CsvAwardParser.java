package ru.mephi.rewards.importservice.parser;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import ru.mephi.rewards.importservice.exception.InvalidFileStructureException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class CsvAwardParser implements AwardRecordParser {

    private static final List<String> REQUIRED_HEADER = Arrays.asList(
            "employee_id", "employee_full_name", "reward_id", "reward_name", "awarded_at"
    );

    @Override
    public boolean supports(String filename, String contentType) {
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return name.endsWith(".csv") || type.contains("text/csv") || type.contains("application/csv");
    }

    @Override
    public Stream<AwardRecord> parse(InputStream in) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

            // 1) читаем заголовок (обязателен)
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new InvalidFileStructureException("Empty file");
            }
            List<String> header = Arrays.stream(headerLine.split(",", -1))
                    .map(s -> s.trim().toLowerCase(Locale.ROOT))
                    .toList();
            if (header.size() != 5 || !header.equals(REQUIRED_HEADER)) {
                throw new InvalidFileStructureException("Invalid header");
            }

            // 2) создаём CSVParser для оставшихся строк (разделитель — запятая)
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setDelimiter(',')
                    .setTrim(false)
                    .setIgnoreSurroundingSpaces(false)
                    .build();

            CSVParser parser = new CSVParser(reader, format);

            // 3) превращаем CSVRecord -> AwardRecord; нумерация строк с 2
            final int[] row = {2};
            Stream<AwardRecord> stream = StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(parser.iterator(), 0), false
                    ).map(rec -> toRecord(rec, row[0]++))
                    .onClose(() -> {
                        try {
                            parser.close();
                        } catch (IOException ignore) {
                        }
                        try {
                            reader.close();
                        } catch (IOException ignore) {
                        }
                    });

            return stream;

        } catch (IOException e) {
            throw new InvalidFileStructureException("Failed to read CSV: " + e.getMessage(), e);
        }
    }

    private AwardRecord toRecord(CSVRecord rec, int rowNumber) {
        if (rec.size() != 5) {
            throw new InvalidFileStructureException("Invalid columns count at row " + rowNumber);
        }
        Long employeeId = parseLongSafe(rec.get(0));
        String fullName = rec.get(1);
        Long rewardId = parseLongSafe(rec.get(2));
        String rewardName = rec.get(3);
        OffsetDateTime dt = parseDateSafe(rec.get(4));

        return new AwardRecord(employeeId, fullName, rewardId, rewardName, dt, rowNumber);
    }

    private Long parseLongSafe(String s) {
        if (s == null)
            return null;
        String t = s.trim();
        if (t.isEmpty())
            return null;
        try {
            return Long.parseLong(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private OffsetDateTime parseDateSafe(String s) {
        if (s == null)
            return null;
        String t = s.trim();
        if (t.isEmpty())
            return null;
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
