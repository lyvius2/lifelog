#!/usr/bin/env bash
# lifelog 이미지를 빌드해 Docker Hub 에 push 합니다.
#
#   DOCKERHUB_USER=yourname ./docker/publish.sh 0.0.1
#   DOCKERHUB_USER=yourname PLATFORM=linux/amd64 ./docker/publish.sh 0.0.1
#
# 기본값은 linux/arm64 (Apple Silicon Mac 에서 실행하는 경우).
# Intel 장비에 배포하면 PLATFORM=linux/amd64, 둘 다 필요하면 쉼표로 나열합니다.
#
# buildx --push 는 레지스트리로 직행하므로 로컬 이미지로는 남지 않습니다.
# 로컬에서 바로 실행해보려면 PLATFORM 을 호스트 아키텍처 하나로 두고 --load 를 쓰세요.
set -euo pipefail

: "${DOCKERHUB_USER:?DOCKERHUB_USER 환경변수가 필요합니다}"
TAG="${1:?사용법: DOCKERHUB_USER=<user> $0 <tag>}"
PLATFORM="${PLATFORM:-linux/arm64}"

cd "$(dirname "$0")/.."

docker buildx inspect lifelog >/dev/null 2>&1 || docker buildx create --name lifelog --use
docker buildx use lifelog

echo "==> ${DOCKERHUB_USER}/lifelog:${TAG} (platform=${PLATFORM})"
docker buildx build \
  --platform "${PLATFORM}" \
  -t "${DOCKERHUB_USER}/lifelog:${TAG}" \
  -t "${DOCKERHUB_USER}/lifelog:latest" \
  --push \
  .

echo "완료: ${DOCKERHUB_USER}/lifelog:${TAG}"
