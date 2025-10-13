package com.realtime.ingest.app.youtube;

import java.util.Iterator;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.stereotype.Component;

@Component
public class YoutubeItemReader implements ItemStreamReader<YoutubeVideo>, ItemStream {

    private final YoutubeApiClient apiClient;
    private Iterator<YoutubeVideo> iterator;

    public YoutubeItemReader(YoutubeApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public YoutubeVideo read() {
        if (iterator == null || !iterator.hasNext()) {
            return null;
        }
        return iterator.next();
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        iterator = apiClient.fetchTrending().iterator();
    }

    @Override
    public void update(ExecutionContext executionContext) {
        // no-op
    }

    @Override
    public void close() {
        iterator = null;
    }
}
