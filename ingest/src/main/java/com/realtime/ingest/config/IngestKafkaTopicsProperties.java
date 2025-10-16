package com.realtime.ingest.config;

import java.util.Objects;

import com.realtime.ingest.domain.SourceType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ingest.kafka.topics")
public class IngestKafkaTopicsProperties {

    private String rss;
    private String wikidump;
    private String youtube;

    public String getRss() {
        return rss;
    }

    public void setRss(String rss) {
        this.rss = rss;
    }

    public String getWikidump() {
        return wikidump;
    }

    public void setWikidump(String wikidump) {
        this.wikidump = wikidump;
    }

    public String getYoutube() {
        return youtube;
    }

    public void setYoutube(String youtube) {
        this.youtube = youtube;
    }

    public String topicFor(SourceType sourceType) {
        return switch (sourceType) {
            case RSS -> requireTopic(rss, SourceType.RSS);
            case WIKIDUMP -> requireTopic(wikidump, SourceType.WIKIDUMP);
            case YOUTUBE -> requireTopic(youtube, SourceType.YOUTUBE);
        };
    }

    private String requireTopic(String value, SourceType sourceType) {
        return Objects.requireNonNull(value, sourceType + " 토픽이 설정되지 않았습니다.");
    }
}
