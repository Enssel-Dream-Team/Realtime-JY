package com.realtime.ingest.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.realtime.ingest.domain.SourceType;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "ingest")
public class IngestProperties {

    private final Rss rss = new Rss();
    private final WikiDump wikidump = new WikiDump();
    private final Youtube youtube = new Youtube();
    private final Kafka kafka = new Kafka();

    public Rss getRss() {
        return rss;
    }

    public WikiDump getWikidump() {
        return wikidump;
    }

    public Youtube getYoutube() {
        return youtube;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public static class Rss {
        private List<Feed> feeds = Collections.emptyList();

        public List<Feed> getFeeds() {
            return feeds;
        }

        public void setFeeds(List<Feed> feeds) {
            this.feeds = feeds;
        }

        @Getter
        @Setter
        public static class Feed {
            private String id;
            private String name;
            private String url;

        }
    }

    public static class WikiDump {
        private List<Dump> dumps = Collections.emptyList();

        public List<Dump> getDumps() {
            return dumps;
        }

        public void setDumps(List<Dump> dumps) {
            this.dumps = dumps;
        }

        @Getter
        @Setter
        public static class Dump {
            private String id;
            private String url;
            private String localPath;

        }
    }

    @Getter
    @Setter
    public static class Youtube {
        private String apiKey;
        private String regionCode = "KR";
        private Integer maxResults = 100;
        private List<String> channelIds = Collections.emptyList();

    }

    public static class Kafka {
        private final Topics topics = new Topics();

        public Topics getTopics() {
            return topics;
        }

        @Getter
        @Setter
        public static class Topics {
            private String rss;
            private String wikiDump;
            private String youtube;

            public String topicFor(SourceType sourceType) {
                return switch (sourceType) {
                    case RSS -> requireTopic(rss, SourceType.RSS);
                    case WIKIDUMP -> requireTopic(wikiDump, SourceType.WIKIDUMP);
                    case YOUTUBE -> requireTopic(youtube, SourceType.YOUTUBE);
                };
            }

            private String requireTopic(String value, SourceType sourceType) {
                return Objects.requireNonNull(value, sourceType + " 토픽이 설정되지 않았습니다.");
            }
        }
    }
}
