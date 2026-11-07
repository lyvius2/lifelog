#!/usr/bin/env bash
# =============================================================================
# 구 DB 가 아직 접속 가능할 때 전량 덤프
# =============================================================================
# 이 저장소의 DDL 로는 테이블 구조와 최소 시드까지만 복구됩니다.
# 게시글 본문·사진 메타데이터·활동 로그·콘텐츠 문서는 코드에 없습니다.
# 구 DB 에 아직 접속이 된다면 다른 무엇보다 이 스크립트를 먼저 돌리십시오.
#
# 사용법:
#   cp db/dump.env.example db/dump.env   # 접속 정보 입력 (gitignore 됨)
#   ./db/00_dump_old.sh
#
# 필요 도구: mysqldump, pg_dump, mongodump
#   macOS: brew install mysql-client libpq mongodb-database-tools
# =============================================================================
set -euo pipefail

cd "$(dirname "$0")/.."
ENV_FILE="db/dump.env"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "오류: $ENV_FILE 이 없습니다. db/dump.env.example 를 복사해 채우십시오." >&2
  exit 1
fi
# shellcheck disable=SC1090
set -a; source "$ENV_FILE"; set +a

OUT="db/dump/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$OUT"
echo "덤프 위치: $OUT"

# -----------------------------------------------------------------------------
# MySQL — 구조 + 데이터
# -----------------------------------------------------------------------------
if [[ -n "${MYSQL_HOST:-}" ]]; then
  echo "==> MySQL ${MYSQL_DATABASE}"
  # --single-transaction : InnoDB 를 락 없이 일관된 시점으로 읽습니다.
  # --set-gtid-purged=OFF: 새 서버에 넣을 때 GTID 충돌을 피합니다.
  # --no-tablespaces    : Aiven 처럼 PROCESS 권한이 없는 환경에서 필요합니다.
  MYSQL_PWD="$MYSQL_PASSWORD" mysqldump \
    --host="$MYSQL_HOST" --port="${MYSQL_PORT:-3306}" --user="$MYSQL_USER" \
    --ssl-mode=REQUIRED \
    --single-transaction --quick --no-tablespaces \
    --set-gtid-purged=OFF \
    --default-character-set=utf8mb4 \
    --routines --triggers --events \
    "$MYSQL_DATABASE" > "$OUT/mysql_full.sql"
  echo "    $(du -h "$OUT/mysql_full.sql" | cut -f1)  mysql_full.sql"

  # 구조만 / 데이터만 따로도 받아 둡니다. 구조는 이 저장소의 DDL 과 비교하는 데 씁니다.
  MYSQL_PWD="$MYSQL_PASSWORD" mysqldump \
    --host="$MYSQL_HOST" --port="${MYSQL_PORT:-3306}" --user="$MYSQL_USER" \
    --ssl-mode=REQUIRED --no-tablespaces --no-data --set-gtid-purged=OFF \
    "$MYSQL_DATABASE" > "$OUT/mysql_schema_only.sql"
  MYSQL_PWD="$MYSQL_PASSWORD" mysqldump \
    --host="$MYSQL_HOST" --port="${MYSQL_PORT:-3306}" --user="$MYSQL_USER" \
    --ssl-mode=REQUIRED --no-tablespaces --no-create-info --set-gtid-purged=OFF \
    --single-transaction --quick --complete-insert \
    "$MYSQL_DATABASE" > "$OUT/mysql_data_only.sql"
  echo "    mysql_schema_only.sql / mysql_data_only.sql"
else
  echo "==> MySQL 건너뜀 (MYSQL_HOST 미설정)"
fi

# -----------------------------------------------------------------------------
# PostgreSQL — 과거 활동 로그 (posts_log) / 보관 목적
# -----------------------------------------------------------------------------
# 현재 애플리케이션은 PostgreSQL 을 쓰지 않습니다(worker 모듈 제거와 함께 폐기).
# 그래도 posts_log 는 구 DB 에만 존재하는 과거 데이터이므로, 접속이 살아 있는 동안
# 받아 둡니다. 새 환경에 복원할 대상은 아니고 보관용입니다.
if [[ -n "${PG_HOST:-}" ]]; then
  echo "==> PostgreSQL ${PG_DATABASE}"
  PGPASSWORD="$PG_PASSWORD" PGSSLMODE=require pg_dump \
    --host="$PG_HOST" --port="${PG_PORT:-5432}" --username="$PG_USER" \
    --no-owner --no-privileges \
    --format=plain \
    "$PG_DATABASE" > "$OUT/postgres_full.sql"
  echo "    $(du -h "$OUT/postgres_full.sql" | cut -f1)  postgres_full.sql"
else
  echo "==> PostgreSQL 건너뜀 (PG_HOST 미설정)"
fi

# -----------------------------------------------------------------------------
# MongoDB — 콘텐츠 문서
# -----------------------------------------------------------------------------
if [[ -n "${MONGO_URI:-}" ]]; then
  echo "==> MongoDB"
  mongodump --uri="$MONGO_URI" --out="$OUT/mongo"
  echo "    $(du -sh "$OUT/mongo" | cut -f1)  mongo/"

  # 사람이 읽을 수 있는 형태로도 한 벌 빼 둡니다.
  if command -v mongoexport >/dev/null 2>&1; then
    mongoexport --uri="$MONGO_URI" \
      --collection=content-documents \
      --out="$OUT/content-documents.ndjson" 2>/dev/null \
      && echo "    content-documents.ndjson"
  fi
else
  echo "==> MongoDB 건너뜀 (MONGO_URI 미설정)"
fi

echo
echo "완료. 받은 덤프를 반드시 다른 저장매체에도 복사해 두십시오."
echo "복원 방법은 db/README.md 의 '구 DB 덤프로 복원' 절을 보십시오."
