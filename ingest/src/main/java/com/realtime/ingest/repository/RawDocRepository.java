package com.realtime.ingest.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.realtime.ingest.domain.RawDoc;

public interface RawDocRepository extends MongoRepository<RawDoc, String> {

    Optional<RawDoc> findByCanonicalUrl(String canonicalUrl);
}
