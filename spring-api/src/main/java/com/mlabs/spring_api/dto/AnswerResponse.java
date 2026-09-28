package com.mlabs.spring_api.dto;

import java.util.List;

public class AnswerResponse {
    private String status;
    private String answer;
    private List<Evidence> citations;
    public AnswerResponse() {}
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public List<Evidence> getCitations() { return citations; }
    public void setCitations(List<Evidence> citations) { this.citations = citations; }
}
