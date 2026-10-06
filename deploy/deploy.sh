#!/usr/bin/env bash
# GitHub Actions 가 SSH 로 실행하는 배포 스크립트 (서버: /srv/devgear/fashion-archive)
#   bash /srv/devgear/fashion-archive/deploy/deploy.sh
#
# 전체를 main 함수로 감싼다 — git pull 이 이 파일 자체를 바꿔도 bash 는 이미 읽은 함수를 끝까지 실행한다.
set -euo pipefail

main() {
    # 같은 서버의 다른 앱(ledger·fashion-archive·workout) 배포와 빌드가 겹치지 않게 한 번에 하나씩 실행한다
    exec 9>/tmp/devgear-deploy.lock
    flock -w 900 9 || { echo "다른 앱 배포가 15분 안에 끝나지 않아 중단합니다" >&2; return 1; }

    cd /srv/devgear/fashion-archive
    git pull --ff-only
    # Dockerfile 이 target/fashion-archive.war 를 복사하므로 이미지 빌드 전에 war 를 만든다
    mvn -q package
    docker compose up -d --build

    # 빌드할수록 쌓이는 이름 없는 이미지와 7일 지난 빌드 캐시를 지운다. 실패해도 배포는 끝났으므로 알리기만 한다
    docker image prune -f > /dev/null || echo "이미지 정리 실패 (배포는 완료)" >&2
    docker builder prune -f --filter until=168h > /dev/null || echo "빌드 캐시 정리 실패 (배포는 완료)" >&2
    echo "배포 완료: $(git log --oneline -1)"
}

main "$@"
exit
