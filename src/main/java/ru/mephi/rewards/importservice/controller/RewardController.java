package ru.mephi.rewards.importservice.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.mephi.rewards.importservice.service.ImportService;
import ru.mephi.rewards.importservice.service.model.ImportReport;

@RestController
@RequestMapping("/api/v1/rewards")
public class RewardController {
    private final ImportService importService;

    public RewardController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/upload")
    public ImportReport upload(@RequestParam("file") MultipartFile file) {
        return importService.importFile(file);
    }

}
