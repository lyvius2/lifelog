#!/usr/bin/env bash
# lifelog / lifelog-worker 이미지를 빌드해 Docker Hub 에 push 합니다.
#
#   DOCKERHUB_USER=yourname ./docker/publish.sh 0.0.1
#   DOCKERHUB_USER=yourname PLATFORM=linux/amd64,linux/arm64 ./docker/publish.sh 0.0.1
#
# buildx --push 는 레지스트리로 직행하므로 로컬 이미지로는 남지 않습니다.
# 로컬에서 바로 실행해보려면 PLATFORM 을 호스트 아키텍처 하나로 두고 --load 를 쓰세요.
set -euo pipefail

: "${DOCKERHUB_USER:?DOCKERHUB_USER 환경변수가 필요합니다}"
TAG="${1:?사용법: DOCKERHUB_USER=<user> $0 <tag>}"
PLATFORM="${PLATFORM:-linux/amd64}"

cd "$(dirname "$0")/.."

# module:image:port
TARGETS=(
  "app:lifelog:8080"
  "worker:lifelog-worker:8081"
)

docker buildx inspect lifelog >/dev/null 2>&1 || docker buildx create --name lifelog --use
docker buildx use lifelog

for target in "${TARGETS[@]}"; do
  IFS=: read -r module image port <<< "$target"
  echo "==> ${DOCKERHUB_USER}/${image}:${TAG} (module=${module}, platform=${PLATFORM})"
  docker buildx build \
    --platform "${PLATFORM}" \
    --build-arg "MODULE=${module}" \
    --build-arg "APP_PORT=${port}" \
    -t "${DOCKERHUB_USER}/${image}:${TAG}" \
    -t "${DOCKERHUB_USER}/${image}:latest" \
    --push \
    .
done

echo "완료: ${DOCKERHUB_USER}/lifelog:${TAG}, ${DOCKERHUB_USER}/lifelog-worker:${TAG}"
