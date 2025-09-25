# Repository Guidelines

## 프로젝트 구조 및 모듈
루트 프로젝트(realtime) 안에는 총 4개의 시스템(모듈)로 구성되어 있다.
- 수집(Collector) 시스템: RSS와 Wiki Dump, Youtube API를 사용하여 데이터를 공통 형식으로 수집하여, raw 데이터는 MongoDB에 저장하고, Kafka 메시로 메타 데이터와 데이터 파일 URL로 전달
- 정제(Cleaning) 시스템: Kafka의 Consumer로써 메시지를 소비하며,Raw 데이터를 정제하고, 카프카 메시지에 정제한 메시지를 전송
- 색인(Indexing) 시스템: 정제 시스템에서 전송한 이벤트를 소비하며, 색인 작업을 진행하고, 그 결과를 Elastic Search와 Redis로 전송
- 서빙(Serving) 시스템: Elastic Search와 Redis를 사용하여 실시간 검색에 서비스를 제공
해당 프로젝트는 원천 데이테를 수집하고, 가공하여 사용자에게 실시간 인기 검색어 서비스를 제공하는 것을 목표로 한다.

## 수집(Collector) 시스템
- 

## 사용 기술
- 스프링 부트(3.5.5)
- Kafka
- Elastic Search
- Redis

## Build, Test, and Development Commands
Run `./gradlew clean build` from the repo root to compile every module with Java 21 and produce test reports. Use `./gradlew :collection:bootRun` for local development; it honours the UTF-8 JVM flag defined in the module build script. Execute `./gradlew :collection:test` to run JUnit 5 suites, and `./gradlew :collection:asciidoctor` when you need updated API snippets after tests pass.

## Coding Style & Naming Conventions
Follow Spring Boot defaults with four-space indentation and UTF-8 source files. Keep package names under `com.jongyeob.<module>` and prefer descriptive nouns like `...Adapter` for external connectors (MinIO, Kafka, RSS). Reuse Lombok annotations already enabled via `compileOnly` and `annotationProcessor` dependencies, but keep constructors explicit where builder readability matters. Configuration properties belong in `@ConfigurationProperties` classes grouped under `config/`.

## Testing Guidelines
Write unit tests beside implementation classes using the `*Test` suffix. Use Mockito or Spring test slices (`@DataMongoTest`, `@SpringBootTest`) for integration behaviors. Ensure asynchronous flows publish deterministic Kafka events before asserting storage writes. Check coverage of crawl de-duplication logic and parsers; add regression tests when touching these areas. Always run `./gradlew :collection:test` before pushing.

## Commit & Pull Request Guidelines
Follow the conventional commit style evident in history (`feat:`, `fix:`, `chore:`). Each commit should encapsulate a single concern and include updated tests when behavior changes. Pull requests must state the problem, the approach, and any follow-up tasks; link issues and document manual test evidence (commands, REST calls, screenshots for UI clients).
