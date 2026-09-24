---
description: 원격 브랜치의 변경사항을 현재 브랜치로 pull
argument-hint: <브랜치명>
allowed-tools: Bash(git status:*), Bash(git branch:*), Bash(git fetch:*), Bash(git pull:*), Bash(git log:*), Bash(git remote:*), Bash(git stash:*)
---

원격 브랜치 `$1`의 소스를 현재 브랜치로 pull 해주세요.

## 절차

1. `$1`이 비어 있으면 어떤 브랜치를 가져올지 사용자에게 먼저 물어본다.
2. `git status`로 워킹 트리를 확인한다. 커밋되지 않은 변경이 있으면 pull 전에 사용자에게 알리고, 계속할지(stash 또는 커밋) 확인받는다.
3. `git remote`로 원격 이름을 확인하고(보통 `origin`), `git fetch <remote> $1`로 최신 정보를 가져온다.
4. 원격에 `$1` 브랜치가 없으면 중단하고, 비슷한 이름의 브랜치 목록을 보여준다.
5. `git pull <remote> $1`을 실행해 현재 브랜치로 병합한다.
6. 충돌이 나면 임의로 해결하지 말고, 충돌 파일 목록과 해결 방법(수동 수정 후 `git add` → `git commit`, 또는 `git merge --abort`)을 안내한다.

## 보고

성공하면 아래를 요약한다.

- 현재 브랜치 이름과 가져온 원격 브랜치
- 가져온 커밋 목록 (`git log`의 한 줄 요약)
- 변경된 파일 수

실패하면 원인과 해결 방법을 함께 제시한다.
