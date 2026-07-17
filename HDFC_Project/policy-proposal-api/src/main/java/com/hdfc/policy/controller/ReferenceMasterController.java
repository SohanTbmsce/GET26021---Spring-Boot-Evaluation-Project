package com.hdfc.policy.controller;

import com.hdfc.policy.service.ReferenceMasterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reference-master")
public class ReferenceMasterController {

    private final ReferenceMasterService referenceMasterService;

    public ReferenceMasterController(ReferenceMasterService referenceMasterService) {
        this.referenceMasterService = referenceMasterService;
    }

    @GetMapping("/{category}")
    public ResponseEntity<List<String>> getReferenceData(@PathVariable String category) {
        return ResponseEntity.ok(referenceMasterService.getReferenceData(category));
    }
}
