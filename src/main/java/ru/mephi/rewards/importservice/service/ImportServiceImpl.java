package ru.mephi.rewards.importservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.mephi.rewards.importservice.entity.Employee;
import ru.mephi.rewards.importservice.entity.RewardAssignment;
import ru.mephi.rewards.importservice.exception.InvalidFileStructureException;
import ru.mephi.rewards.importservice.exception.UnsupportedFileFormatException;
import ru.mephi.rewards.importservice.parser.AwardRecord;
import ru.mephi.rewards.importservice.parser.AwardRecordParser;
import ru.mephi.rewards.importservice.repository.EmployeeRepository;
import ru.mephi.rewards.importservice.repository.RewardAssignmentRepository;
import ru.mephi.rewards.importservice.service.model.ImportError;
import ru.mephi.rewards.importservice.service.model.ImportReport;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ImportServiceImpl implements ImportService {

    private final List<AwardRecordParser> parsers;
    private final EmployeeRepository employeeRepository;
    private final RewardAssignmentRepository rewardAssignmentRepository;

    private static final int BATCH_SIZE = 100;

    @Override
    public ImportReport importFile(MultipartFile file) {
        validateFile(file);
        AwardRecordParser parser = findParser(file);
        Stats s = new Stats();
        List<ImportError> errors = new ArrayList<>();
        List<RewardAssignment> batch = new ArrayList<>(BATCH_SIZE);

        try (InputStream is = file.getInputStream(); Stream<AwardRecord> records = parser.parse(is)) {
            processStream(records, s, errors, batch);
            flushBatch(batch, s);
        } catch (InvalidFileStructureException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("File processing error", e);
        }
        return new ImportReport(s.processed, s.saved, s.skipped, errors);
    }


    private void processStream(Stream<AwardRecord> records, Stats s, List<ImportError> errors, List<RewardAssignment> batch) {
        Iterator<AwardRecord> it = records.iterator();
        while (it.hasNext()) {
            AwardRecord r = it.next();
            s.processed++;
            if (!validateRequired(r, errors)) { s.skipped++; continue; }
            if (!validateBusiness(r, errors)) { s.skipped++; continue; }
            batch.add(toEntity(r));
            if (batch.size() == BATCH_SIZE) flushBatch(batch, s);
        }
    }

    private boolean validateRequired(AwardRecord r, List<ImportError> errors) {
        if (r.employeeId() == null) { errors.add(err(r, "Invalid employee_id")); return false; }
        if (r.employeeFullName() == null || r.employeeFullName().isBlank()) { errors.add(err(r, "Invalid employee_full_name")); return false; }
        if (r.rewardId() == null) { errors.add(err(r, "Invalid reward_id")); return false; }
        if (r.rewardName() == null || r.rewardName().isBlank()) { errors.add(err(r, "Invalid reward_name")); return false; }
        if (r.awardedAt() == null) { errors.add(err(r, "Invalid award_date")); return false; }
        return true;
    }

    private boolean validateBusiness(AwardRecord r, List<ImportError> errors) {
        if (!employeeRepository.existsById(r.employeeId())) {
            errors.add(err(r, "Employee " + r.employeeId() + " not found")); return false;
        }
        if (rewardAssignmentRepository.existsByEmployeeIdAndRewardIdAndAwardedAt(r.employeeId(), r.rewardId(), r.awardedAt())) {
            errors.add(err(r, "Duplicate (employeeId=" + r.employeeId() + ", rewardId=" + r.rewardId() + ", date=" + r.awardedAt() + ")"));
            return false;
        }
        return true;
    }

    private RewardAssignment toEntity(AwardRecord r) {
        Employee e = new Employee();
        e.setId(r.employeeId());
        return new RewardAssignment(null, e, r.rewardId(), r.rewardName(), r.awardedAt());
    }

    private void flushBatch(List<RewardAssignment> batch, Stats s) {
        if (batch.isEmpty()) return;
        rewardAssignmentRepository.saveAll(batch);
        s.saved += batch.size();
        batch.clear();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new UnsupportedFileFormatException("File is empty");
    }

    private AwardRecordParser findParser(MultipartFile file) {
        String name = file.getOriginalFilename();
        String type = file.getContentType();
        return parsers.stream()
                .filter(p -> p.supports(name, type))
                .findFirst()
                .orElseThrow(() -> new UnsupportedFileFormatException("Unsupported file format"));
    }

    private ImportError err(AwardRecord r, String msg) {
        return new ImportError(r.rowNumber(), msg);
    }

    private static final class Stats { int processed; int saved; int skipped; }
}
