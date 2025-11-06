package ru.mephi.rewards.importservice.service;

import org.springframework.web.multipart.MultipartFile;
import ru.mephi.rewards.importservice.service.model.ImportReport;

public interface ImportService {
    ImportReport importFile(MultipartFile file);
}
