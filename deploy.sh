#!/bin/bash
set -e

IMAGE_NAME="ddingsh9/user-activites-server"
TAG="${1:-latest}"

echo "=== Activity Server Multi-Arch Build & Deploy ==="
echo "Image: ${IMAGE_NAME}:${TAG}"
echo ""

# 1. Gradle 빌드
echo "[1/4] Building JAR..."
./gradlew clean bootJar -x test

# 2. Docker buildx 빌더 확인/생성
echo "[2/4] Setting up Docker buildx..."
if ! docker buildx inspect multiarch-builder > /dev/null 2>&1; then
    docker buildx create --name multiarch-builder --use
else
    docker buildx use multiarch-builder
fi
docker buildx inspect --bootstrap

# 3. Multi-arch 빌드 및 푸시
echo "[3/4] Building and pushing multi-arch image..."
docker buildx build \
    --platform linux/amd64,linux/arm64 \
    -t ${IMAGE_NAME}:${TAG} \
    --push \
    .

# 4. 완료
echo "[4/4] Done!"
echo ""
echo "Pushed: ${IMAGE_NAME}:${TAG}"
echo "Platforms: linux/amd64, linux/arm64"
