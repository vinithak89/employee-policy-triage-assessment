package com.mlabs.spring_api.controller;

import com.mlabs.spring_api.dto.AnswerResponse;
import com.mlabs.spring_api.dto.Evidence;
import com.mlabs.spring_api.service.AnswerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnswerController.class)
class AnswerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnswerService answerService;

    @Test
    void answer_shouldReturnSuccessfulPolicyResponse() throws Exception {

        AnswerResponse response = new AnswerResponse();
        response.setStatus("ANSWERED");
        response.setAnswer(
                "The annual certification reimbursement limit for employees is INR 25000."
        );

        Evidence evidence = new Evidence();
        evidence.setChunkId("atlas-cert-current");
        evidence.setQuote(
                "The annual certification reimbursement limit for employees is INR 25000."
        );

        response.setCitations(List.of(evidence));

        when(answerService.answer(any(), eq("atlas-employee-01")))
                .thenReturn(response);

        mockMvc.perform(post("/answer")
                        .header("X-Caller-Id", "atlas-employee-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "What is my annual certification reimbursement limit?",
                                  "as_of": "2026-09-21"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANSWERED"))
                .andExpect(jsonPath("$.answer")
                        .value(
                                "The annual certification reimbursement limit for employees is INR 25000."
                        ))
                .andExpect(jsonPath("$.citations[0].chunk_id")
                        .value("atlas-cert-current"))
                .andExpect(jsonPath("$.citations[0].quote")
                        .value(
                                "The annual certification reimbursement limit for employees is INR 25000."
                        ));

        verify(answerService)
                .answer(any(), eq("atlas-employee-01"));
    }

    @Test
    void answer_shouldReturnInsufficientEvidence() throws Exception {

        AnswerResponse response = new AnswerResponse();
        response.setStatus("INSUFFICIENT_EVIDENCE");
        response.setAnswer(null);
        response.setCitations(List.of());

        when(answerService.answer(any(), eq("atlas-employee-01")))
                .thenReturn(response);

        mockMvc.perform(post("/answer")
                        .header("X-Caller-Id", "atlas-employee-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "What is my wellness reimbursement limit?",
                                  "as_of": "2026-09-21"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("INSUFFICIENT_EVIDENCE"))
                .andExpect(jsonPath("$.answer").doesNotExist())
                .andExpect(jsonPath("$.citations").isArray())
                .andExpect(jsonPath("$.citations").isEmpty());
    }

    @Test
    void answer_shouldRejectMissingCallerHeader() throws Exception {

        mockMvc.perform(post("/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "What is my annual certification reimbursement limit?",
                                  "as_of": "2026-09-21"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void answer_shouldRejectInvalidRequestBody() throws Exception {

        mockMvc.perform(post("/answer")
                        .header("X-Caller-Id", "atlas-employee-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "",
                                  "as_of": "2026-09-21"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void batch_shouldAcceptMetadataAndRepeatedFiles() throws Exception {

        Map<String, Object> batchResponse = Map.of(
                "batch_id", "demo-01",
                "summary", Map.of(
                        "total", 2,
                        "completed", 2,
                        "failed", 0
                ),
                "results", List.of(
                        Map.of(
                                "document_id", "request-03",
                                "processing_status", "COMPLETED",
                                "review_required", true
                        ),
                        Map.of(
                                "document_id", "request-07",
                                "processing_status", "COMPLETED",
                                "review_required", true
                        )
                )
        );

        String metadataJson =
                "{\"batch_id\":\"demo-01\","
                        + "\"as_of\":\"2026-09-21\","
                        + "\"documents\":["
                        + "{\"document_id\":\"request-03\","
                        + "\"filename\":\"request-03.txt\"},"
                        + "{\"document_id\":\"request-07\","
                        + "\"filename\":\"request-07.txt\"}"
                        + "]}";

        when(answerService.batch(
                eq(metadataJson),
                any(),
                eq("atlas-employee-01")))
                .thenReturn(batchResponse);

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata",
                "",
                MediaType.TEXT_PLAIN_VALUE,
                metadataJson.getBytes()
        );

        MockMultipartFile file03 = new MockMultipartFile(
                "files",
                "request-03.txt",
                MediaType.TEXT_PLAIN_VALUE,
                """
                Reference: CERT-303
                Certification reimbursement request.
                """.getBytes()
        );

        MockMultipartFile file07 = new MockMultipartFile(
                "files",
                "request-07.txt",
                MediaType.TEXT_PLAIN_VALUE,
                """
                Reference: TRAIN-707
                I have already booked external training.
                """.getBytes()
        );

        mockMvc.perform(multipart("/batches")
                        .file(metadata)
                        .file(file03)
                        .file(file07)
                        .header("X-Caller-Id", "atlas-employee-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch_id")
                        .value("demo-01"))
                .andExpect(jsonPath("$.summary.total")
                        .value(2))
                .andExpect(jsonPath("$.summary.completed")
                        .value(2))
                .andExpect(jsonPath("$.summary.failed")
                        .value(0))
                .andExpect(jsonPath("$.results[0].document_id")
                        .value("request-03"))
                .andExpect(jsonPath("$.results[0].processing_status")
                        .value("COMPLETED"))
                .andExpect(jsonPath("$.results[1].document_id")
                        .value("request-07"))
                .andExpect(jsonPath("$.results[1].processing_status")
                        .value("COMPLETED"));

        verify(answerService).batch(
                eq(metadataJson),
                any(),
                eq("atlas-employee-01"));
    }

    @Test
    void batch_shouldRejectMissingMetadata() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "request-03.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Reference: CERT-303".getBytes()
        );

        mockMvc.perform(multipart("/batches")
                        .file(file)
                        .header("X-Caller-Id", "atlas-employee-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void batch_shouldRejectMissingCallerHeader() throws Exception {

        String metadataJson =
                "{\"batch_id\":\"demo-01\","
                        + "\"as_of\":\"2026-09-21\","
                        + "\"documents\":[]}";

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata",
                "",
                MediaType.TEXT_PLAIN_VALUE,
                metadataJson.getBytes()
        );

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "request-03.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Reference: CERT-303".getBytes()
        );

        mockMvc.perform(multipart("/batches")
                        .file(metadata)
                        .file(file))
                .andExpect(status().isBadRequest());
    }
}