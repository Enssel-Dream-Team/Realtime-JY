package com.realtime.ingest.app.youtube;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.realtime.ingest.domain.RawDoc;
import com.realtime.ingest.service.RawDocSaveResult;
import com.realtime.ingest.service.RawDocService;

@Component
public class YoutubeItemWriter implements ItemWriter<RawDoc> {

    private static final Logger log = LoggerFactory.getLogger(YoutubeItemWriter.class);

    private final RawDocService rawDocService;

    public YoutubeItemWriter(RawDocService rawDocService) {
        this.rawDocService = rawDocService;
    }

    @Override
    public void write(Chunk<? extends RawDoc> chunk) {
        chunk.forEach(doc -> {
            RawDocSaveResult result = rawDocService.storeRawDoc(doc);
            if (!result.isSaved()) {
                log.debug("YouTube doc skipped {} reason {}", result.getDedupKey(), result.getReason());
            }
        });
    }
}
