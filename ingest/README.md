# 수집 시스템 (ingest)

Wiki Dump · RSS · Youtube 데이터를 정기적으로 수집하여 MongoDB `raw_docs` 컬렉션에 저장하고, Cleansing 모듈이 사용할 Kafka 토픽(`ingest.cleansing.raw_docs`)으로 이벤트를 발행하는 Spring Batch 기반 모듈입니다.

## 배치 구성
- **rssIngestJob** : `feeds.yml`에 정의된 RSS 피드를 순회하여 ROME 으로 메타데이터를 파싱하고, JSoup 으로 본문을 추출해 저장합니다.
- **wikiDumpJob** : `dumps.yml`에 등록된 덤프 파일을 Woodstox 기반 스트리밍 파서로 페이지 단위 파싱 후 저장합니다.
- **youtubeIngestJob** : Youtube Data API v3 `videos` 엔드포인트에서 인기 급상승 영상을 가져와 메타데이터를 저장합니다.
- 각 Step 은 `RawDocService` 로 저장을 위임하고 `JobLoggingListener` 가 실행 요약을 남깁니다.

## 저장 및 메시지 흐름
1. `DedupKeyService` 가 canonical URL(`CanonicalUrlUtil`)을 기반으로 `source#sourceId#sha256(canonical)` 키를 생성
2. MongoDB `raw_docs` 컬렉션에 `@Id = dedup_key` 로 저장 (중복일 경우 저장 및 전송 스킵)
3. Kafka 메시지 (`IngestRawDocMessage`) 발행

```json
{
  "dedup_key": "rss#yonhapnewstv#5d1e...",
  "source": "rss",
  "source_id": "item-guid",
  "event_time": "2024-05-01T00:00:00Z",
  "ingest_time": "2024-05-01T00:05:00Z",
  "trace_id": "550e8400-e29b-41d4-a716-446655440000",
  "mongo_ref": { "db":"realtime", "collection":"raw_docs", "_id":"rss#yonhapnewstv#5d1e..." }
}
```

## 주요 설정
- `application.yml`
  - `spring.data.mongodb.uri` / `spring.data.mongodb.database`
  - `spring.kafka.bootstrap-servers`
  - `ingest.schedule.rss-cron`, `ingest.schedule.wiki-cron`, `ingest.schedule.youtube-initial-delay`, `ingest.schedule.youtube-fixed-delay`
  - `youtube.api-key`, `youtube.channel-ids`, `youtube.max-results`
- `feeds.yml` : RSS 피드 정의 (id/name/url)
- `dumps.yml` : Wiki 덤프 파일 정의 (id/url/localPath)

## 실행 방법
```bash
./gradlew :ingest:bootRun
```

환경 변수로 MongoDB, Kafka, Youtube API Key 등을 주입할 수 있습니다. (예: `MONGODB_URI`, `KAFKA_BOOTSTRAP_SERVERS`, `YOUTUBE_API_KEY`)

## 테스트
```bash
./gradlew :ingest:test
```

단위 테스트는 `DedupKeyService` 와 `RawDocService` 의 핵심 로직(카노니컬 URL 생성, 중복 처리, Kafka 전송)을 검증합니다.
