package com.mlabs.spring_api.service;

import com.mlabs.spring_api.client.PythonAiClient;
import com.mlabs.spring_api.config.CallerRegistry;
import com.mlabs.spring_api.dto.AnswerRequest;
import com.mlabs.spring_api.dto.AnswerResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class AnswerService {

    private final PythonAiClient pythonAiClient;
    private final CallerRegistry callerRegistry;

    public AnswerService(
            PythonAiClient pythonAiClient,
            CallerRegistry callerRegistry) {
        this.pythonAiClient = pythonAiClient;
        this.callerRegistry = callerRegistry;
    }

    public AnswerResponse answer(AnswerRequest request, String callerId) {
        CallerRegistry.Caller caller =
                callerRegistry.find(callerId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Unknown caller"));

        return pythonAiClient.getAnswer(
                request,
                caller.tenant(),
                caller.role());
    }

    public Map<String, Object> batch(
            String metadata,
            List<MultipartFile> files,
            String callerId) throws IOException {

        CallerRegistry.Caller caller = findCaller(callerId);

        return pythonAiClient.processBatch(
                metadata,
                files,
                caller);
    }

    private CallerRegistry.Caller findCaller(String callerId) {
        return callerRegistry.find(callerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Unknown caller"));
    }
}
