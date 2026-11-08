#!/usr/bin/env bash
# lifelog 이미지를 빌드해 Docker Hub 에 push 합니다.
#
#   JVM 이미지 (루트 Dockerfile):
#     DOCKERHUB_USER=yourname ./docker/publish.sh 0.0.1
#     DOCKERHUB_USER=yourname PLATFORM=linux/amd64 ./docker/publish.sh 0.0.1
#
#   네이티브 이미지 (Dockerfile.native):
#     DOCKERHUB_USER=yourname NATIVE=true ./docker/publish.sh 0.0.1
#     -> <user>/lifelog:0.0.1-native, <user>/lifelog:native
#
# 기본값은 linux/arm64 (Apple Silicon Mac 에서 실행하는 경우).
# Intel 장비에 배포하면 PLATFORM=linux/amd64, 둘 다 필요하면 쉼표로 나열합니다.
#
# 네이티브는 PLATFORM 을 하나만 지정하고, 그 아키텍처의 빌더에서 돌리십시오.
# native-image 컴파일을 QEMU 에뮬레이션으로 돌리면 수십 분에서 수 시간이 걸리거나
# 빌더가 메모리를 소진하고 죽습니다. 두 아키텍처가 필요하면 각 장비에서 따로 빌드해
# push 하고 docker manifest 로 묶으십시오.
#
# buildx --push 는 레지스트리로 직행하므로 로컬 이미지로는 남지 않습니다.
# 로컬에서 바로 실행해보려면 PLATFORM 을 호스트 아키텍처 하나로 두고 --load 를 쓰세요.
set -euo pipefail

: "${DOCKERHUB_USER:?DOCKERHUB_USER 환경변수가 필요합니다}"
TAG="${1:?사용법: DOCKERHUB_USER=<user> $0 <tag>}"
PLATFORM="${PLATFORM:-linux/arm64}"
NATIVE="${NATIVE:-false}"

cd "$(dirname "$0")/.."

if [ "${NATIVE}" = "true" ]; then
    DOCKERFILE="Dockerfile.native"
    VERSION_TAG="${TAG}-native"
    MOVING_TAG="native"
    if [[ "${PLATFORM}" == *,* ]]; then
        echo "오류: 네이티브 이미지는 PLATFORM 을 하나만 지정하십시오 (현재: ${PLATFORM})" >&2
        exit 1
    fi
else
    DOCKERFILE="Dockerfile"
    VERSION_TAG="${TAG}"
    MOVING_TAG="latest"
fi

docker buildx inspect lifelog >/dev/null 2>&1 || docker buildx create --name lifelog --use
docker buildx use lifelog

echo "==> ${DOCKERHUB_USER}/lifelog:${VERSION_TAG} (${DOCKERFILE}, platform=${PLATFORM})"
docker buildx build \
  --platform "${PLATFORM}" \
  -f "${DOCKERFILE}" \
  -t "${DOCKERHUB_USER}/lifelog:${VERSION_TAG}" \
  -t "${DOCKERHUB_USER}/lifelog:${MOVING_TAG}" \
  --push \
  .

echo "완료: ${DOCKERHUB_USER}/lifelog:${VERSION_TAG}"
