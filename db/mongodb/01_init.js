// =============================================================================
// lifelog / MongoDB 초기화 스크립트
// =============================================================================
// 대상 : content-service 의 content-documents 컬렉션 (MongoDB Atlas)
// 실행 : mongosh "mongodb+srv://<user>:<pw>@<cluster>/lifelog" --file 01_init.js
// 이후 : 02 단계로 content-documents.ndjson 을 mongoimport 합니다 (db/README.md 참고)
//
// 컬렉션은 Spring Data 가 쓰기 시점에 자동 생성하므로 필수는 아니지만,
// @Indexed 인덱스는 auto-index-creation 이 꺼져 있으면 만들어지지 않습니다.
// 여기서 명시적으로 만들어 둡니다.
// =============================================================================

const COLLECTION = "content-documents";

if (!db.getCollectionNames().includes(COLLECTION)) {
    db.createCollection(COLLECTION);
    print(`컬렉션 생성: ${COLLECTION}`);
} else {
    print(`컬렉션 이미 존재: ${COLLECTION}`);
}

// ContentDocuments.contentType 의 @Indexed 에 대응
db.getCollection(COLLECTION).createIndex({ contentType: 1 }, { name: "contentType" });
print("인덱스 생성: contentType");

// 한 contentType 에 문서가 둘 이상이면 findByContentType 이 어느 것을 집을지
// 보장되지 않습니다. 유일 인덱스로 막아 두는 편이 안전합니다.
// 복구 직후 중복이 있으면 생성이 실패하므로, 중복을 먼저 정리한 뒤 실행하십시오.
// db.getCollection(COLLECTION).createIndex({ contentType: 1 }, { name: "uk_contentType", unique: true });

print("\n현재 보유 문서:");
db.getCollection(COLLECTION)
    .find({}, { contentType: 1 })
    .forEach(d => print(`  ${d.contentType}  (_id=${d._id})`));

// 애플리케이션이 화면에서 찾는 contentType 은 다음 5개입니다.
//   INTRO          /          (index.html)
//   PROFILE        /profile
//   CAR            /my-car
//   ARCHITECTURE   /architecture
//   AI_OPS_DESIGN  /ai-ops/design
// 문서가 없으면 ContentService 가 빈 Map 을 돌려주므로 페이지는 떠도 내용이 비어 있습니다.
// 비어 있는 타입은 로그인 후 /content/editor 에서 직접 입력하십시오.
