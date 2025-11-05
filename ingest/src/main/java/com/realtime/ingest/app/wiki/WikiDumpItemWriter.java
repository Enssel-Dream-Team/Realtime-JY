package com.realtime.ingest.app.wiki;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.realtime.ingest.domain.RawDoc;
import com.realtime.ingest.service.RawDocSaveResult;
import com.realtime.ingest.service.RawDocService;

@Component
public class WikiDumpItemWriter implements ItemWriter<RawDoc> {

    private static final Logger log = LoggerFactory.getLogger(WikiDumpItemWriter.class);

    private final RawDocService rawDocService;

    public WikiDumpItemWriter(RawDocService rawDocService) {
        this.rawDocService = rawDocService;
    }

    @Override
    public void write(Chunk<? extends RawDoc> chunk) {
        chunk.forEach(doc -> {
            RawDocSaveResult result = rawDocService.storeRawDoc(doc);
            if (!result.isSaved()) {
                log.debug("Wiki doc skipped {} reason {}", result.getDedupKey(), result.getReason());
            }
        });
    }
}
