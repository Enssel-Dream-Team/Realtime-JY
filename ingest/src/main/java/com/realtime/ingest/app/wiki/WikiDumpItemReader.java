package com.realtime.ingest.app.wiki;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Iterator;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.stereotype.Component;

import com.realtime.ingest.config.WikiDumpProperties;

@Component
public class WikiDumpItemReader implements ItemStreamReader<WikiPage>, ItemStream {

    private static final Logger log = LoggerFactory.getLogger(WikiDumpItemReader.class);

    private final WikiDumpProperties properties;
    private Iterator<WikiDumpProperties.Dump> dumpIterator;
    private WikiDumpProperties.Dump currentDump;
    private XMLStreamReader xmlReader;
    private InputStream currentStream;

    public WikiDumpItemReader(WikiDumpProperties properties) {
        this.properties = properties;
    }

    @Override
    public WikiPage read() throws Exception, NonTransientResourceException {
        while (true) {
            if (xmlReader == null) {
                if (!openNextDump()) {
                    return null;
                }
            }
            WikiPage page = nextPage();
            if (page != null) {
                return page;
            }
            closeCurrentDump();
        }
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        this.dumpIterator = properties.getDumps().iterator();
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        // no state to persist for now
    }

    @Override
    public void close() throws ItemStreamException {
        closeCurrentDump();
    }

    private boolean openNextDump() {
        closeCurrentDump();
        if (dumpIterator == null || !dumpIterator.hasNext()) {
            return false;
        }
        currentDump = dumpIterator.next();
        Path path = Path.of(currentDump.getLocalPath());
        if (!Files.exists(path)) {
            log.warn("Wiki dump file {} not found", path);
            return false;
        }
        try {
            currentStream = decompress(Files.newInputStream(path));
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.IS_COALESCING, true);
            xmlReader = factory.createXMLStreamReader(currentStream);
            log.info("Opened wiki dump {} from {}", currentDump.getId(), path);
            return true;
        } catch (IOException | XMLStreamException | CompressorException e) {
            log.error("Failed to open wiki dump {}: {}", currentDump.getId(), e.getMessage(), e);
            closeCurrentDump();
            return false;
        }
    }

    private InputStream decompress(InputStream inputStream) throws CompressorException {
        BufferedInputStream buffered = new BufferedInputStream(inputStream);
        buffered.mark(Integer.MAX_VALUE);
        try {
            return new CompressorStreamFactory().createCompressorInputStream(buffered);
        } catch (CompressorException e) {
            try {
                buffered.reset();
            } catch (IOException ioException) {
                throw new CompressorException("Failed to reset input stream", ioException);
            }
            return buffered;
        }
    }

    private WikiPage nextPage() throws XMLStreamException {
        if (xmlReader == null) {
            return null;
        }
        String pageId = null;
        String title = null;
        String text = null;
        Instant timestamp = null;

        while (xmlReader.hasNext()) {
            int event = xmlReader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = xmlReader.getLocalName();
                switch (localName) {
                    case "id" -> {
                        if (pageId == null) {
                            pageId = xmlReader.getElementText();
                        }
                    }
                    case "title" -> title = xmlReader.getElementText();
                    case "timestamp" -> timestamp = Instant.parse(xmlReader.getElementText());
                    case "text" -> text = xmlReader.getElementText();
                    default -> {
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "page".equals(xmlReader.getLocalName())) {
                if (title != null && text != null) {
                    return new WikiPage(
                        currentDump.getId(),
                        pageId != null ? pageId : title,
                        title,
                        text,
                        timestamp
                    );
                }
                pageId = null;
                title = null;
                text = null;
                timestamp = null;
            }
        }
        return null;
    }

    private void closeCurrentDump() throws ItemStreamException {
        try {
            if (xmlReader != null) {
                xmlReader.close();
            }
            if (currentStream != null) {
                currentStream.close();
            }
        } catch (Exception e) {
            throw new ItemStreamException("Failed to close dump stream", e);
        } finally {
            xmlReader = null;
            currentStream = null;
            currentDump = null;
        }
    }
}
