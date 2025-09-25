# 단순화된 수집(Collector) 시스템 설계

## ✅ 원칙(심플 버전)
- **중복 키 1가지 규칙만**: `dedup_key = sha256(canonical_url)`
  - GUID 신뢰/ETag/Redis/근사중복 등 전부 배제
  - URL 캐논컬이 실패하면 **원본 URL 그대로** 해시
- **멱등 보장 1곳만**: **Mongo upsert(_id=dedup_key)**
  - 재수집이 와도 덮어쓰기 → 중복 자동 흡수
- **메시지 경량화**: Kafka에는 **본문 없이** `mongo_ref`와 메타만
- **순서 고정**: **Mongo 저장 성공 → Kafka 발행**

---

## 🚦 데이터 흐름
```
RSS item → canonical_url → dedup_key=sha256(canonical_url)
 → Mongo raw_docs.save(_id=dedup_key)   // upsert 멱등
 → Kafka produce ingest.raw.doc.v1 (key=dedup_key, value={mongo_ref, origin_url, ...})
```

---

## 🧱 스키마 (최소 필드만)

### Mongo `raw_docs`
```json
{
  "_id": "rss#<sha256(canonical_url)>",
  "source": "rss",
  "canonical_url": "https://news.example.com/a/123",
  "event_time": "2025-09-17T05:12:30Z",
  "ingest_time": "2025-09-17T05:12:35Z",
  "title": "기사 제목",
  "body": "원문 전체 본문(<= ~128KB)"
}
```

### Kafka `ingest.raw.doc.v1`
**key**: `rss#<sha256(canonical_url)>`
```json
{
  "dedup_key": "rss#<sha256(canonical_url)>",
  "source": "rss",
  "event_time": "2025-09-17T05:12:30Z",
  "ingest_time": "2025-09-17T05:12:35Z",
  "title": "기사 제목",
  "origin_url": "https://news.example.com/a/123",
  "mongo_ref": { "db":"news","collection":"raw_docs","_id":"rss#<sha256...>" }
}
```

---

## 🧪 URL 캐논컬(필수만)
- 소문자 호스트, `#fragment` 제거, 트래킹 파라미터(utm_*, gclid 등) 제거, trailing slash 정규화  
- 실패해도 **그냥 입력 URL 사용**

---

## 📈 장단점
- **장점**: 구현·운영 단순, 디버깅 쉬움, 멱등은 Mongo 하나로 충분  
- **단점**:  
  - 캐논컬이 허술한 매체는 같은 기사 다른 URL로 **중복** 생성 가능  
  - 네트워크 최적화 없음 → **트래픽 증가**  
  - 본문이 바뀌어도 URL이 같으면 **덮어쓰기**
