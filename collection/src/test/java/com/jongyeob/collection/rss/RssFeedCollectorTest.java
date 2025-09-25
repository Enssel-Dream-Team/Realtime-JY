package com.jongyeob.collection.rss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.jongyeob.collection.canonical.CanonicalUrlNormalizer;
import com.jongyeob.collection.config.CollectorProperties;
import com.jongyeob.collection.document.RawDocumentRepository;
import com.jongyeob.collection.service.CollectorService;
import com.jongyeob.collection.service.DedupKeyGenerator;
import com.jongyeob.collection.service.IngestCommand;

class RssFeedCollectorTest {

    @Test
    void ingestsNewEntriesAndSkipsAlreadyProcessedOnes() {
        CollectorProperties properties = new CollectorProperties();
        CollectorProperties.Rss.Publisher publisher = new CollectorProperties.Rss.Publisher();
        publisher.setId("khan");
        publisher.setSource("khan");
        publisher.setUrl("https://example.com/khan.xml");
        properties.getRss().getPublishers().add(publisher);

        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo(publisher.getUrl()))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(sampleFeed(), MediaType.APPLICATION_XML));

        CollectorService collectorService = mock(CollectorService.class);
        RawDocumentRepository repository = mock(RawDocumentRepository.class);
        when(repository.existsById(any())).thenReturn(false, true);

        RssFeedCollector collector = new RssFeedCollector(
            properties,
            restTemplate,
            collectorService,
            repository,
            new CanonicalUrlNormalizer(),
            new DedupKeyGenerator()
        );

        collector.pollFeeds();

        ArgumentCaptor<IngestCommand> commandCaptor = ArgumentCaptor.forClass(IngestCommand.class);
        verify(collectorService, times(1)).ingest(commandCaptor.capture());

        IngestCommand command = commandCaptor.getValue();
        assertThat(command.source()).isEqualTo("khan");
        assertThat(command.originUrl()).isEqualTo("https://example.com/articles/1");
        assertThat(command.title()).isEqualTo("Article 1");
        assertThat(command.body()).contains("Body 1");

        server.verify();
    }

    private byte[] sampleFeed() {
        String rss = """
            <?xml version=\"1.0\" encoding=\"UTF-8\"?>
            <rss version=\"2.0\">
              <channel>
                <title>Sample Feed</title>
                <item>
                  <title>Article 1</title>
                  <link>https://example.com/articles/1</link>
                  <pubDate>Tue, 10 Feb 2025 12:34:56 GMT</pubDate>
                  <description><![CDATA[Body 1]]></description>
                </item>
                <item>
                  <title>Article 2</title>
                  <link>https://example.com/articles/2</link>
                  <description><![CDATA[Body 2]]></description>
                </item>
              </channel>
            </rss>
            """;
        return rss.getBytes(StandardCharsets.UTF_8);
    }
}
