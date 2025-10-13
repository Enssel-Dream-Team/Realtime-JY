# 개요

Wiki Dump, 언론사별 기사, SNS API에서 데이터를 수집하여 실시간 검색어 기능을 제공하는 서비스다.

# 프로젝트 구조

현재 저장소는 초기 단계로 Ingest 모듈만을 포함하며, cleansing/indexing/serving 모듈은 이후 단계에서 확장한다.

1. Ingest
    - Wiki dump, RSS, Youtube API에서 데이터를 수집한다.
    - 수집 데이터는 MongoDB `raw_docs` 컬렉션에 저장한다.
    - 정제 파이프라인으로 전달할 Kafka 메시지를 전송한다. (Producer)
    - Spring Batch로 RSS/Wiki/Youtube 잡을 구성하고 스케줄러가 크론 기반으로 구동한다.
2. (미구현) cleansing
    - 수집 메시지를 소비하여 정제한다.
3. (미구현) indexing
    - 정제 데이터를 검색 시스템으로 색인한다.
4. (미구현) serving
    - 실시간 검색어 API를 제공한다.

# 핵심 기술 스택

- Java 21
- Spring Boot 3.5.6
- Spring Batch
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

# 행동 지침
- 모든 답변은 한국어로
- 모든 질문에 대해 순차적 생각 사용