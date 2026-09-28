package com.mlabs.spring_api.client;

import com.mlabs.spring_api.config.CallerRegistry.Caller;
import com.mlabs.spring_api.dto.AnswerRequest;
import com.mlabs.spring_api.dto.AnswerResponse;
import com.mlabs.spring_api.dto.Evidence;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Component
public class PythonAiClient {

    private final HttpClient httpClient;

    public PythonAiClient() {

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }


    // ============================================================
    // /answer
    // ============================================================

    public AnswerResponse getAnswer(
            AnswerRequest request,
            String tenant,
            String role) {

        String jsonBody = buildAnswerJson(request);
        try {

            HttpRequest httpRequest =
                    HttpRequest.newBuilder()
                            .uri(URI.create(
                                    "http://localhost:8001/answer"))
                            .version(HttpClient.Version.HTTP_1_1)
                            .header(
                                    "Content-Type",
                                    "application/json")
                            .header(
                                    "Accept",
                                    "application/json")
                            .header(
                                    "X-Caller-Tenant",
                                    tenant)
                            .header(
                                    "X-Caller-Role",
                                    role)
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            jsonBody,
                                            StandardCharsets.UTF_8))
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            httpRequest,
                            HttpResponse.BodyHandlers.ofString(
                                    StandardCharsets.UTF_8));

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new IllegalStateException(
                        "Python AI service returned HTTP "
                                + response.statusCode()
                                + ": "
                                + response.body());
            }

            return parseAnswerResponse(
                    response.body());

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Interrupted while calling Python AI service",
                    e);

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to call Python AI service",
                    e);
        }
    }


    // ============================================================
    // Parse /answer response
    // ============================================================

    private AnswerResponse parseAnswerResponse(
            String json) {

        AnswerResponse response =
                new AnswerResponse();

        response.setStatus(
                extractString(json, "status"));

        response.setAnswer(
                extractString(json, "answer"));

        String chunkId =
                extractString(json, "chunk_id");

        String quote =
                extractString(json, "quote");

        List<Evidence> citations =
                new ArrayList<>();

        if (chunkId != null || quote != null) {

            Evidence evidence =
                    new Evidence();

            evidence.setChunkId(chunkId);
            evidence.setQuote(quote);

            citations.add(evidence);
        }

        response.setCitations(citations);

        return response;
    }


    // ============================================================
    // /batches
    // ============================================================

    public Map<String, Object> processBatch(
            String metadata,
            List<MultipartFile> files,
            Caller caller) throws IOException {

        if (files == null || files.isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one file is required");
        }


        // --------------------------------------------------------
        // Create a unique multipart boundary
        // --------------------------------------------------------

        String boundary =
                "----SpringBatchBoundary"
                        + UUID.randomUUID();


        // --------------------------------------------------------
        // Build multipart body manually
        // --------------------------------------------------------

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();


        // --------------------------------------------------------
        // metadata part
        // --------------------------------------------------------

        writePartHeader(
                output,
                boundary,
                "metadata",
                null,
                "text/plain; charset=UTF-8");

        output.write(
                metadata.getBytes(
                        StandardCharsets.UTF_8));

        output.write(
                "\r\n".getBytes(
                        StandardCharsets.UTF_8));


        // --------------------------------------------------------
        // file parts
        // --------------------------------------------------------

        for (MultipartFile file : files) {

            String filename =
                    file.getOriginalFilename();

            if (filename == null
                    || filename.isBlank()) {

                filename = "upload.bin";
            }


            String contentType =
                    file.getContentType();

            if (contentType == null
                    || contentType.isBlank()) {

                contentType =
                        "application/octet-stream";
            }


            writePartHeader(
                    output,
                    boundary,
                    "files",
                    filename,
                    contentType);


            output.write(
                    file.getBytes());


            output.write(
                    "\r\n".getBytes(
                            StandardCharsets.UTF_8));
        }


        // --------------------------------------------------------
        // End multipart body
        // --------------------------------------------------------

        output.write(
                ("--"
                        + boundary
                        + "--\r\n")
                        .getBytes(
                                StandardCharsets.UTF_8));


        byte[] requestBody =
                output.toByteArray();

        // --------------------------------------------------------
        // Create HTTP request
        // --------------------------------------------------------

        HttpRequest httpRequest =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        "http://localhost:8001/batches"))
                        .version(
                                HttpClient.Version.HTTP_1_1)
                        .header(
                                "Content-Type",
                                "multipart/form-data; boundary="
                                        + boundary)
                        .header(
                                "Accept",
                                "application/json")
                        .header(
                                "X-Caller-Tenant",
                                caller.tenant())
                        .header(
                                "X-Caller-Role",
                                caller.role())
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofByteArray(
                                                requestBody))
                        .build();

        // --------------------------------------------------------
        // Call Python
        // --------------------------------------------------------

        try {

            HttpResponse<String> response =
                    httpClient.send(
                            httpRequest,
                            HttpResponse.BodyHandlers
                                    .ofString(
                                            StandardCharsets.UTF_8));

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new IllegalStateException(
                        "Python batch service returned HTTP "
                                + response.statusCode()
                                + ": "
                                + response.body());
            }


            /*
             * IMPORTANT:
             *
             * Do not invent the response structure here.
             * For now, return the Python JSON as a simple Map.
             */
            return parseJsonMap(response.body());

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Interrupted while calling Python batch service",
                    e);
        }
    }


    // ============================================================
    // Multipart part writer
    // ============================================================

    private void writePartHeader(
            ByteArrayOutputStream output,
            String boundary,
            String fieldName,
            String filename,
            String contentType)
            throws IOException {

        StringBuilder header =
                new StringBuilder();


        header.append("--")
                .append(boundary)
                .append("\r\n");


        header.append(
                        "Content-Disposition: form-data; name=\"")
                .append(fieldName)
                .append("\"");


        if (filename != null) {

            header.append(
                            "; filename=\"")
                    .append(
                            escapeMultipartFilename(
                                    filename))
                    .append("\"");
        }


        header.append("\r\n");


        header.append(
                        "Content-Type: ")
                .append(contentType)
                .append("\r\n");


        header.append("\r\n");


        output.write(
                header.toString()
                        .getBytes(
                                StandardCharsets.UTF_8));
    }


    // ============================================================
    // Escape filename for Content-Disposition
    // ============================================================

    private String escapeMultipartFilename(
            String filename) {

        return filename
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }


    // ============================================================
    // JSON builder for /answer
    // ============================================================

    private String buildAnswerJson(
            AnswerRequest request) {

        String question =
                escapeJson(
                        request.getQuestion());

        return "{"
                + "\"question\":\""
                + question
                + "\","
                + "\"as_of\":\""
                + request.getAsOf()
                + "\""
                + "}";
    }


    // ============================================================
    // Simple JSON string extraction
    // ============================================================

    private String extractString(
            String json,
            String field) {

        String marker =
                "\"" + field + "\":\"";

        int start =
                json.indexOf(marker);

        if (start < 0) {
            return null;
        }

        start += marker.length();


        StringBuilder value =
                new StringBuilder();

        boolean escaped = false;


        for (int i = start;
             i < json.length();
             i++) {

            char c =
                    json.charAt(i);


            if (escaped) {

                switch (c) {

                    case '"':
                        value.append('"');
                        break;

                    case '\\':
                        value.append('\\');
                        break;

                    case '/':
                        value.append('/');
                        break;

                    case 'b':
                        value.append('\b');
                        break;

                    case 'f':
                        value.append('\f');
                        break;

                    case 'n':
                        value.append('\n');
                        break;

                    case 'r':
                        value.append('\r');
                        break;

                    case 't':
                        value.append('\t');
                        break;

                    default:
                        value.append(c);
                }

                escaped = false;

                continue;
            }


            if (c == '\\') {

                escaped = true;

                continue;
            }


            if (c == '"') {

                return value.toString();
            }


            value.append(c);
        }


        return null;
    }


    // ============================================================
    // JSON escaping
    // ============================================================

    private String escapeJson(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }


    // ============================================================
    // Temporary batch response parser
    // ============================================================

    private Map<String, Object> parseJsonMap(
            String json) {

        /*
         * We need Jackson for the actual batch response.
         *
         * The Spring Boot project already has Jackson through
         * spring-boot-starter-webmvc.
         *
         * This method is intentionally implemented below using
         * ObjectMapper rather than guessing the response fields.
         */

        try {

            com.fasterxml.jackson.databind.ObjectMapper
                    objectMapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();

            return objectMapper.readValue(
                    json,
                    Map.class);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to parse Python batch response: "
                            + json,
                    e);
        }
    }
}