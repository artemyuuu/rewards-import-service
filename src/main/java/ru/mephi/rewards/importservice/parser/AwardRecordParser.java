package ru.mephi.rewards.importservice.parser;

import java.io.InputStream;
import java.util.stream.Stream;

public interface AwardRecordParser {
    boolean supports(String filename, String contentType);

    Stream<AwardRecord> parse(InputStream in);
}
