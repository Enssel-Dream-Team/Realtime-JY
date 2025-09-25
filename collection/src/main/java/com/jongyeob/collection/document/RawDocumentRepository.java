package com.jongyeob.collection.document;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface RawDocumentRepository extends MongoRepository<RawDocument, String> {
}
