# 수집 시스템

## 수집 방식

### RSS 방식

1. `feeds.yml`에 정의된 RSS 목록을 순회한다.
2. HTTP 요청 시 `If-Modified-Since/If-None-Match` 헤더를 적용해 변경 여부를 확인한다.
3. 새로운 항목은 JSoup으로 본문 HTML을 크롤링하여 텍스트를 추출한다.
4. MongoDB에 저장하고 Kafka로 메시지를 전송한다.
5. Spring Batch 잡은 30분 주기로 실행된다.

### Wiki Dump 방식

1. 특정 경로에 대용량 xml 파일 파싱
    - 적은 메모리 환경에서 구동 가능
    - 효율적인 처리 가능
    - 파서는 nio로 직접 구현하거나 woodstox, jaxb 사용
    - Page 단위로 파싱
    - Mongo DB에 저장
2. 온라인 다운로드에서 dump 파일을 다운받는 방식
    - dumps.yml에 주소를 설정하고 `data/` 경로에 파일을 둔다

### Youtube API

1. 유튜브 데이터 API를 사용
2. 인기 급상승 동영상 목록 조회
    3. 영상별로 메타데이터를 추출하고 MongoDB에 저장

### 공통 요구사항

- 모든 데이터는 MongoDB에 저장
- Spring Batch를 사용하여 구현
- 각 배치 작업마다 로그 기록
    - Ex. Total: {totalCount}, Ingest: {processedCount}, Duplicated: {duplicatedCount}, Failed: {failedCount}
    - 중복과 실패 데이터는 로그를 통해 어떤 데이터에서 중복 됐는 지 출력
- 수집된 데이터는 카프카 메시지로 전송
- canonical_url = normalize(host↓, strip(#), strip(utm_*, gclid, fbclid), normalize trailing slash, sort query)

## 카프카 메시지 형식

- Ingest → Cleansing
- group: 'ingest-cleanse-group'
- topic: 'ingest.cleansing.raw_docs'
- message:
    ```json
    {
      "dedup_key": "rss#yonhapnewstv#2ab3...f1e",
      "source": "rss",
      "source_id": "nyt-20250917-12345",
      "event_time": "2025-09-17T05:12:30Z",
      "ingest_time": "2025-09-17T05:12:35Z",
      "trace_id": "550e8400-e29b-41d4-a716-446655440000",
      "mongo_ref": { "db":"news", "collection":"raw_docs", "_id":"rss#nyt#2ab3...f1e" }
    }
    ```

## 패키지 구조

- collection
    - main
        - config
            - BatchConfig.java
            - KafkaProducerConfig.java
            - MongoConfig.java
            - JobScheduleConfig.java
        - app
            - news
                - RssIngestJobConfig.java
                - RssItemReader.java
                - RssItemProcessor.java
                - RssItemWriter.java
            - wiki
                - WikiDumpJobConfig.java
                - WikiDumpItemReader.java
                - WikiDumpItemProcessor.java
                - WikiDumpItemWriter.java
            - youtube
                - YoutubeIngestJobConfig.java
                - YoutubeItemReader.java
                - YoutubeItemProcessor.java
                - YoutubeItemWriter.java
        - domain
            - RawDoc.java - Mongo raw docs
            - IngestRawDocMessage.java - Kafka 메시지 DTO
            - SourceType.java
        - support
            - CanonicalUrlUtil.java
            - DedupKeyService.java
            - JobLoggingListener.java - JobExecutionListener
        - resources
            - application.yml
            - feeds.yml - RSS 피드 목록
            - dumps.yml - Wiki 덤프 목록
