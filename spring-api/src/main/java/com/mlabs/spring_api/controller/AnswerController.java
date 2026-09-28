package com.mlabs.spring_api.controller;

import com.mlabs.spring_api.dto.AnswerRequest;
import com.mlabs.spring_api.dto.AnswerResponse;
import com.mlabs.spring_api.service.AnswerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Employee Policy Triage", description = "Policy questions and reimbursement batch review APIs")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AnswerController {
    private final AnswerService answerService;
    public AnswerController(AnswerService answerService) { this.answerService = answerService; }

    @Operation(summary = "Answer a policy question", description = "Answers using eligible policy evidence for the authenticated caller and requested date.")
    @ApiResponse(responseCode = "200", description = "Business outcome returned")
    @ApiResponse(responseCode = "401", description = "Unknown caller")
    @PostMapping("/answer")
    public ResponseEntity<AnswerResponse> answer(
            @Parameter(description = "Assessment caller identity", example = "atlas-employee-01", required = true)
            @RequestHeader("X-Caller-Id") String callerId,
            @Valid @org.springframework.web.bind.annotation.RequestBody AnswerRequest request) {
        return ResponseEntity.ok(answerService.answer(request, callerId));
    }

    @Operation(summary = "Process reimbursement documents", description = "Processes a synchronous multipart batch of TXT/PDF requests. Every submitted item receives a result and human review remains required.")
    @ApiResponse(responseCode = "200", description = "Batch result returned")
    @ApiResponse(responseCode = "400", description = "Invalid metadata or files")
    @ApiResponse(responseCode = "401", description = "Unknown caller")
    @PostMapping(value = "/batches", consumes = "multipart/form-data")
    public ResponseEntity<Map<String,Object>> batch(
        @RequestHeader("X-Caller-Id") String callerId,
        @RequestPart("metadata") String metadata,
        @RequestPart("files") List<MultipartFile> files) throws IOException {
        return ResponseEntity.ok(answerService.batch(metadata, files, callerId));
    }
}
