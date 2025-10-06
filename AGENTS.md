# 개요

Wiki Dump, 언론사별 기사, SNS API에서 데이터를 수집하여 실시간 검색어 기능을 제공하는 서비스다.

# 프로젝트 구조

아래와 같은 4개의 멀티 모듈 프로젝트로 구성됩니다.

1. Ingest
    - 각 소스로부터 데이터를 수집한다. (Wiki dump, RSS, Youtube API)
    - 수집한 데이터는 Mongo DB에 저장한다.
    - 저장 후 정제 시스템에서 사용할 Kafka 메시지를 전송한다. (Producer)
2. cleansing
    - Kafka 메시지를 가져온다. (Consumer)
        - 초당 1000건 처리 가능해야한다.
    - 메시지에 담긴 정보를 토대로 Mongo DB에서 수집한 데이터를 조회한다.
    - 데이터를 정제한다.
        - 데이터 넘버링(PK부여)
        - 메타데이터 생성
        - FullText 생성
    - 저장 후 색인 시스템에서 사용할 Kafka 메시지를 전송한다. (Producer)
3. indexing
    - 색인 후 엘라스틱 서치와 레디스로 직접 전송한다.
    - 메시지에 담긴 정보를 토대로 Mongo DB에서 정제된 데이터를 조회한다.
    - Elastic Search 인덱스에 색인을 한다.
4. serving
    - Elastic Search를 활용해 실시간 검색어 서비스 API를 제공한다.

# 핵심 기술 스택

- Java 21
- Spring Boot 3.5.6
- Mongo DB
- Kafka
- Elastic Search
- Redis
- Docker

# 공통 요구 사항

- 모든 시스템은 스프링 부트 프로젝트로 구성
- 프로젝트에 필요로 하는 모든 구성요소는 Docker 이미지화
- Docker Compose로  구성요소들을 묶음
- 수집 데이터
    - 제목
    - 본문
-

# 수집 시스템

## 수집 방식

### RSS 방식

1. RSS 피드 읽어오기
2. 피드에서 기사별 URL 가져오기
    - https://www.yonhapnewstv.co.kr/browse/feed/
    - https://www.yna.co.kr/rss/news.xml
    - https://news-ex.jtbc.co.kr/v1/get/rss/issue
    - https://news.sbs.co.kr/news/newsflashRssFeed.do?plink=RSSREADER
3. 기사별로 아래의 과정 병렬로 처리 (CompletableFuture 사용)
    1. JSoup으로 URL 크롤링
    2. 각 기사별 link 태그에 명시된 주소로 들어가 HTML 크롤링
    3. Doc에서 필요한 데이터 추출
    4. Mongo DB에 저장
    5. Kafka 메시지 전송
4. 1시간 단위로 RSS 데이터를 수집
    - 피드 요청 시 If-Modified-Since/If-None-Match 사용
    - 304일 경우 본문 fetch 생략 / 처리 통계 시 Not Modified 카운트 반영

### Wiki Dump 방식

1. 특정 경로에 대용량 xml 파일 파싱
    - 적은 메모리 환경에서 구동 가능
    - 효율적인 처리 가능
    - 파서는 nio로 직접 구현하거나 woodstox, jaxb 사용
    - Page 단위로 파싱
    - Mongo DB에 저장

### Youtube API

1. 유튜브 데이터 API를 사용
2. 인기 급상승 동영상 목록 조회
3. 영상별로 아래의 과정 병렬로 처리 (CompletableFuture 사용)
    1. 필요한 데이터 추출
    2. Mongo DB에 저장

### 공통 요구사항

- 모든 데이터는 MongoDB에 저장
- 데이터 한 건(기사/아티클/영상 1개)은 한 개의 파일로 저장
- Spring Batch를 사용하여 구현
- 각 배치 작업마다 로그 기록
    - Ex. Total: {totalCount}, Ingest: {processedCount}, Duplicated: {duplicatedCount}, Failed: {failedCount}
    - 중복과 실패 데이터는 로그를 통해 어떤 데이터에서 중복 됐는 지 출력
- 수집된 데이터는 카프카 메시지로 전송
- canonical_url = normalize(host↓, strip(#), strip(utm_*, gclid, fbclid), normalize trailing slash, sort query) - cononical_url 생성 방식

## 카프카 메시지 형식

- Ingest → Cleansing

    ```json
    {
      "dedup_key": "rss#yonhapnewstv#2ab3...f1e",
      "source": "rss",
      "source_id": "nyt-20250917-12345",
      "event_time": "2025-09-17T05:12:30Z",
      "ingest_time": "2025-09-17T05:12:35Z",
      "trace_id": "550e8400-e29b-41d4-a716-446655440000",
      "mongo_ref": { "db":"news", "collection":"raw_docs", "_id":"rss#nyt#2ab3...f1e" },
    }
    ```

- Cleansing → Indexing

    ```json
    
    ```

- Indexing → Serving

    ```json
    
    ```


## 기술 스택

- JSoup : HTML 크롤링

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
            - [RawDoc.java](http://RawDoc.java) - Mongo raw docs
            - [IngestRawDocMessage.java](http://IngestRawDocMessage.java) - Kafka 메시지 DTO
            - [DedupKeyService.java](http://DedupKeyService.java) - sha256(canonical_url)
        - support
            - [CanonicalUrlUtils.java](http://CanonicalUrlUtils.java)
            - RetryBackoffPolicyFactory.java
            - [LoggingListeners.java](http://LoggingListeners.java) - JobExecution/StepExecutionListener
        - resources
            - application.properties
            - feeds.yml - RSS 피드 목록