package com.realtime.ingest.app.wiki;

import java.time.Instant;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.realtime.ingest.domain.RawDoc;
import com.realtime.ingest.domain.SourceType;

@Component
public class WikiDumpItemProcessor implements ItemProcessor<WikiPage, RawDoc> {

    @Override
    public RawDoc process(WikiPage item) {
        RawDoc doc = new RawDoc();
        doc.setSource(SourceType.WIKIDUMP);
        doc.setSourceId(item.dumpId() + "-" + item.pageId());
        doc.setTitle(item.title());
        doc.setContent(item.text());
        doc.setOriginalUrl("https://ko.wikipedia.org/wiki/" + item.title().replace(' ', '_'));
        doc.setEventTime(item.revisionTimestamp() != null ? item.revisionTimestamp() : Instant.now());
        doc.getMetadata().put("dumpId", item.dumpId());
        return doc;
    }
}
