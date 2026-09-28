package com.mlabs.spring_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Evidence {
    @JsonProperty("chunk_id")
    private String chunkId;
    private String quote;
    public Evidence() {}
    public String getChunkId() { return chunkId; }
    public void setChunkId(String chunkId) { this.chunkId = chunkId; }
    public String getQuote() { return quote; }
    public void setQuote(String quote) { this.quote = quote; }
}
