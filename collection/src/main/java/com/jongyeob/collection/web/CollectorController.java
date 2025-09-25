package com.jongyeob.collection.web;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jongyeob.collection.service.CollectorService;
import com.jongyeob.collection.service.IngestCommand;
import com.jongyeob.collection.service.IngestResult;
import com.jongyeob.collection.web.dto.IngestRequest;
import com.jongyeob.collection.web.dto.IngestResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/collector")
@Validated
public class CollectorController {

    private final CollectorService collectorService;

    public CollectorController(CollectorService collectorService) {
        this.collectorService = collectorService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<IngestResponse> ingest(@Valid @RequestBody IngestRequest request) {
        IngestCommand command = new IngestCommand(
            request.source(),
            request.originUrl(),
            request.eventTime(),
            request.title(),
            request.body()
        );
        IngestResult result = collectorService.ingest(command);
        IngestResponse response = new IngestResponse(
            result.dedupKey(),
            result.canonicalUrl(),
            result.canonicalized()
        );
        return ResponseEntity.ok(response);
    }
}
