package com.realtime.ingest.config;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "youtube")
public class YoutubeProperties {

    private String apiKey;
    private String regionCode = "KR";
    private Integer maxResults = 25;
    private List<String> channelIds = Collections.emptyList();

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public Integer getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }

    public List<String> getChannelIds() {
        return channelIds;
    }

    public void setChannelIds(List<String> channelIds) {
        this.channelIds = channelIds;
    }
}
