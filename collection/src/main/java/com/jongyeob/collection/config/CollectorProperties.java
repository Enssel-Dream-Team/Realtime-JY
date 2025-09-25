package com.jongyeob.collection.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "collector")
public class CollectorProperties {

    private final Topic topic = new Topic();
    private final Mongo mongo = new Mongo();
    private final Rss rss = new Rss();

    public Topic getTopic() {
        return topic;
    }

    public Mongo getMongo() {
        return mongo;
    }

    public Rss getRss() {
        return rss;
    }

    public static class Topic {
        private String rawIngest = "ingest.raw.doc.v1";

        public String getRawIngest() {
            return rawIngest;
        }

        public void setRawIngest(String rawIngest) {
            this.rawIngest = rawIngest;
        }
    }

    public static class Mongo {
        private String database = "news";
        private String collection = "raw_docs";

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getCollection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }
    }

    public static class Rss {

        private Duration pollInterval = Duration.ofMinutes(5);
        private final List<Publisher> publishers = new ArrayList<>();

        public Duration getPollInterval() {
            return pollInterval;
        }

        public void setPollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
        }

        public List<Publisher> getPublishers() {
            return publishers;
        }

        public static class Publisher {
            private String id;
            private String source;
            private String url;

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getSource() {
                return source;
            }

            public void setSource(String source) {
                this.source = source;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }
        }
    }
}
