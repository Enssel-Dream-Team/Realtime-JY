package com.realtime.ingest.app.youtube;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.realtime.ingest.domain.RawDoc;
import com.realtime.ingest.domain.SourceType;

@Component
public class YoutubeItemProcessor implements ItemProcessor<YoutubeVideo, RawDoc> {

    @Override
    public RawDoc process(YoutubeVideo item) {
        RawDoc doc = new RawDoc();
        doc.setSource(SourceType.YOUTUBE);
        doc.setSourceId(item.videoId());
        doc.setTitle(item.title());
        doc.setContent(item.description());
        doc.setOriginalUrl("https://www.youtube.com/watch?v=" + item.videoId());
        doc.setEventTime(item.publishedAt());
        doc.getMetadata().put("channelId", item.channelId());
        return doc;
    }
}
