package com.realtime.ingest.config;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.realtime.ingest.support.YamlPropertySourceFactory;

@Configuration
@ConfigurationProperties(prefix = "wikidump")
@PropertySource(value = "classpath:dumps.yml", factory = YamlPropertySourceFactory.class)
public class WikiDumpProperties {

    private List<Dump> dumps = Collections.emptyList();

    public List<Dump> getDumps() {
        return dumps;
    }

    public void setDumps(List<Dump> dumps) {
        this.dumps = dumps;
    }

    public static class Dump {
        private String id;
        private String url;
        private String localPath;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getLocalPath() {
            return localPath;
        }

        public void setLocalPath(String localPath) {
            this.localPath = localPath;
        }
    }
}
