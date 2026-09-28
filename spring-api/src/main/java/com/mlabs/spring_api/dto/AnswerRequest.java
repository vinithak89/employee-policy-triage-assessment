package com.mlabs.spring_api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class AnswerRequest {

    @NotBlank
    private String question;

    @NotNull
    @JsonProperty("as_of")
    @JsonAlias("asOf")
    private LocalDate asOf;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public LocalDate getAsOf() {
        return asOf;
    }

    public void setAsOf(LocalDate asOf) {
        this.asOf = asOf;
    }
}