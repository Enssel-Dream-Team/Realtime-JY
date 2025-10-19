package com.realtime.ingest.app.rss;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.realtime.ingest.domain.RawDoc;
import com.realtime.ingest.service.RawDocSaveResult;
import com.realtime.ingest.service.RawDocService;

@Component
public class RssItemWriter implements ItemWriter<RawDoc> {

    private static final Logger log = LoggerFactory.getLogger(RssItemWriter.class);

    private final RawDocService rawDocService;

    public RssItemWriter(RawDocService rawDocService) {
        this.rawDocService = rawDocService;
    }

    @Override
    public void write(Chunk<? extends RawDoc> chunk) {
        List<? extends RawDoc> items = chunk.getItems();
        items.forEach(rawDoc -> {
            RawDocSaveResult result = rawDocService.storeRawDoc(rawDoc);
            if (!result.isSaved()) {
                log.debug("Skipped {} reason {}", result.getDedupKey(), result.getReason());
            }
        });
    }
}
