# IT:SSUM Contributing Guide

## 1. 브랜치 전략

IT:SSUM은 간소화된 Git Flow를 사용합니다.

- `main`: 운영 배포 브랜치입니다. `main`에 반영되면 GitHub Actions가 EC2에 자동으로 배포합니다. 직접 푸시하지 않습니다.
- `develop`: 기능 통합 브랜치입니다. 모든 작업 PR은 여기로 올리며, `develop` 반영만으로는 배포되지 않습니다.
- `feature/*`, `fix/*`, `refactor/*` 등: 이슈 단위 작업 브랜치입니다.
- 기본 흐름은 `작업 브랜치` → PR → `develop` → 배포 시 PR → `main`입니다.

## 2. 브랜치 네이밍

브랜치는 이슈 번호를 포함해서 생성합니다.

- 기능 개발: `feature/{이슈번호}-{작업}`
- 버그 수정: `fix/{이슈번호}-{작업}`
- 리팩토링: `refactor/{이슈번호}-{작업}`
- 문서: `docs/{이슈번호}-{작업}`
- 설정: `chore/{이슈번호}-{작업}`

예시:

```text
feature/12-donation-item-create
feature/44-needed-item-close
fix/31-match-cancel-quantity
refactor/40-recommendation-scorer
docs/50-readme
```

작업을 시작할 때는 이슈를 먼저 만들고, 최신 `develop` 기준으로 브랜치를 만듭니다.

```bash
git switch develop
git pull origin develop
git switch -c feature/{이슈번호}-{작업}
```

## 3. 커밋 메시지

커밋 메시지는 아래 형식을 사용합니다.

```text
타입: 작업 내용 (#이슈번호)
```

| 타입 | 언제 쓰는지 |
| --- | --- |
| `feat` | 새로운 기능 |
| `fix` | 버그 수정 |
| `refactor` | 동작은 그대로 두고 코드 구조만 변경 |
| `docs` | README, ERD, API 명세 등 문서 |
| `test` | 테스트 코드 추가·수정 |
| `chore` | 빌드 설정, 의존성, 설정 파일 |
| `style` | 포맷팅, import 정리처럼 로직과 상관없는 변경 |

- 제목은 한글로, 50자 이내로 씁니다. 끝에 마침표는 찍지 않습니다.
- 왜 바꿨는지 설명이 필요하면 한 줄 띄우고 본문에 적습니다.
- 한 커밋에는 한 가지 작업만 담습니다.

예시:

```text
feat: 기부 물품 등록 API 구현 (#12)
feat: 필요 물품 모집 종료 API 추가 (#44)
fix: 매칭 취소 시 예약 수량이 복원되지 않던 문제 수정 (#31)
refactor: 추천 점수 계산을 RecommendationScorer로 분리 (#40)
test: 거래중 합의 수량 변경 검증 보강 (#35)
docs: 필요 물품 모집 종료 README 반영 (#44)
```

## 4. PR 규칙

- PR은 이슈 단위로 생성하고, 대상 브랜치는 `develop`입니다.
- 제목은 커밋 메시지 형식(`타입: 작업 내용 (#이슈번호)`)을 따릅니다.
- 본문은 [`.github/PULL_REQUEST_TEMPLATE.md`](.github/PULL_REQUEST_TEMPLATE.md) 양식에 맞춰 작업 내용, 주요 변경 사항, 확인 사항, 테스트 결과를 적고 `Closes #이슈번호`로 이슈를 연결합니다.
- 리뷰어 1명이 승인하면 **Squash and merge**로 합치고, 머지한 브랜치는 삭제합니다.
- 배포할 때만 `develop`에서 `main`으로 PR을 생성합니다.
- 충돌 해결은 PR 작성자가 합니다.
- PR을 열면 CI가 MySQL 8.4 컨테이너에서 빌드·테스트를 실행합니다. 통과한 뒤 머지합니다.

PR을 올리기 전에는 다음 항목을 확인합니다.

- 변경 범위가 한 이슈 안에서 설명 가능한 크기인지 확인합니다.
- API 경로, 요청, 응답, 에러 코드 변경 여부를 PR에 적습니다.
- 엔티티(DB 스키마), 환경변수, 외부 API 연동 변경 여부를 PR에 적습니다.
- 테스트 방법과 결과를 PR에 적습니다.
- 불필요한 로그, 주석, 민감 정보가 포함되지 않았는지 확인합니다.

## 5. Issue 규칙

- Issue는 기능, 버그, 리팩토링, 문서, 설정 작업 단위로 생성합니다.
- [`.github/ISSUE_TEMPLATE/ISSUE_TEMPLATE.md`](.github/ISSUE_TEMPLATE/ISSUE_TEMPLATE.md) 양식으로 어떤 작업인지, 작업 상세 내용, 참고 자료를 적습니다.
- 브랜치 이름과 커밋·PR 제목에 이슈 번호를 넣어 Issue, Branch, PR을 연결합니다.

## 6. 패키지 구조

도메인별로 패키지를 나누고, 각 도메인 안에 `controller / dto / entity / repository / service`를 둡니다. 여러 도메인이 같이 쓰는 설정과 외부 연동은 `global`에 모읍니다.

- `domain/{도메인}/dto/request`, `domain/{도메인}/dto/response`에 요청·응답 DTO를 둡니다.
- `global/common`: `BaseTimeEntity`, `ApiResponse`
- `global/config`: JPA Auditing, Security, Swagger 설정
- `global/exception`: `ErrorCode`, `SuccessCode`, `BusinessException`, `GlobalExceptionHandler`
- `global/infra`: S3, 국세청 사업자 API, 카카오 로컬, 메일, FCM 연동

## 7. Java 코드 컨벤션

**공통**

- 클래스는 PascalCase, 변수와 메서드는 camelCase, 상수는 UPPER_SNAKE_CASE를 사용합니다.
- 의존성 주입은 생성자 주입(`@RequiredArgsConstructor`)을 사용합니다.
- 비즈니스 로직은 Controller가 아니라 Service에 둡니다. Repository는 데이터 접근 책임만 갖습니다.
- 불필요한 전체 포맷 변경과 기능 변경을 한 PR에 섞지 않습니다.
- 로그에는 비밀번호, 토큰, 인증 코드, 개인 식별 정보를 남기지 않습니다.

**DTO**

- DTO는 전부 `record`로 만듭니다.
- 이름은 `{동작}{대상}Request`, `{대상}{용도}Response` 형식입니다. 예: `CreateDonationItemRequest`, `DonationItemDetailResponse`
- 요청 값 검증은 record 필드에 `@NotBlank`, `@Positive` 같은 Bean Validation을 붙여서 합니다.
- 응답 DTO는 `from(Entity)` 정적 메서드로 만들고, 컨트롤러에서 엔티티를 그대로 반환하지 않습니다.

**Entity**

- setter를 만들지 않고, 기본 생성자는 `protected`로 둡니다.
- 생성은 `@Builder`나 정적 팩토리 메서드로, 값 변경은 의미 있는 이름의 메서드로 합니다. 예: `match.accept()`, `neededItem.close()`
- 연관관계는 전부 `FetchType.LAZY`로 걸고, 필요한 경우에만 양방향으로 만듭니다.
- enum은 `@Enumerated(EnumType.STRING)`으로 저장합니다.
- 삭제는 `deleted_at`을 채우는 soft delete를 사용합니다.

## 8. API 응답 형식

모든 응답은 `ApiResponse`로 감싸서 내려줍니다. 성공·실패 모두 `success`, `code`, `message`가 같은 위치에 있습니다.

```json
{
  "success": true,
  "code": "MATCH_201",
  "message": "기부 제안·요청을 생성했습니다.",
  "data": {}
}
```

실패 응답은 `data` 없이 내려가고, 입력값 검증 실패는 `fieldErrors`가 추가됩니다.

```json
{
  "success": false,
  "code": "COMMON_400",
  "message": "입력값이 올바르지 않습니다.",
  "fieldErrors": [
    { "field": "quantity", "reason": "수량은 1 이상으로 입력해 주세요." }
  ]
}
```

- 성공 코드는 `SuccessCode`에 추가하고 `ApiResponse.ok(SuccessCode.XXX, data)`로 반환합니다. HTTP 상태(예: 생성 201)는 `SuccessCode`를 따라 자동으로 정해집니다.
- 코드는 `도메인_HTTP상태` 형식이고, 같은 상태가 여러 개면 `도메인_HTTP상태_번호`를 붙입니다. 예: `COMMON_200`, `MATCH_200_3`, `AUTH_401`

## 9. 예외 처리

- 실패 코드는 `ErrorCode`에 추가하고, 서비스 로직에서 `BusinessException`으로 던집니다.
- 컨트롤러에서 예외 응답을 직접 만들지 않고 `GlobalExceptionHandler` 한 곳에서 처리합니다.
- 인증, 권한, 유효성 검증, 외부 API 오류는 구분 가능한 에러 코드로 관리합니다.

## 10. DB 스키마

Flyway 없이 `spring.jpa.hibernate.ddl-auto: update`로 관리하며, 실제 스키마의 기준은 엔티티입니다.

- 엔티티를 고치면 `docs/erd/itssum_erd_v3.sql`, ERD 이미지(`docs/images/erd/erd.png`), ERDCloud도 같이 고칩니다.
- `update`는 컬럼 추가만 반영하고 타입 변경이나 컬럼 삭제는 반영하지 않습니다. 이런 변경이 있으면 로컬 DB를 지우고 다시 띄우고, 운영 반영 방법을 PR에 적습니다.
- 기준 데이터(지역, 카테고리)는 `src/main/resources/data.sql`의 `INSERT IGNORE`로 넣습니다.

## 11. 설정 및 보안

- 비밀번호, 토큰, API 키, SSH 키, FCM 서비스 계정 JSON 등 실제 인증 정보는 저장소에 작성하지 않습니다.
- 로컬은 환경변수로, 운영은 GitHub Secrets로 주입합니다. CD가 배포할 때마다 EC2의 `.env`를 새로 만듭니다.
- 환경변수를 새로 추가하면 GitHub Secrets 등록, `cd.yml`의 `.env` 블록, README의 로컬 실행 방법을 같이 수정합니다.
- 운영 데이터 삭제, Docker volume 삭제, Git reset 등 복구가 어려운 작업은 사전 합의 후 진행합니다.

## 12. 테스트 및 검증

작업 성격에 맞는 검증을 수행하고 PR에 결과를 남깁니다.

- 단위 테스트: Service, 점수 계산처럼 순수 로직이 있는 코드
- 통합 테스트: Repository, Security, Controller처럼 Spring Context가 필요한 코드
- 직접 실행 확인: 애플리케이션 기동, 주요 API 호출, Swagger UI 확인
- 로컬에서 `./gradlew test`가 통과한 뒤 PR을 올립니다.

## 13. 문서 업데이트 기준

아래 변경이 있으면 관련 문서를 함께 수정합니다.

- API가 추가, 수정, 삭제되면 노션 API 명세와 Swagger 설명(`@Operation`, `@Schema`)을 확인합니다.
- 환경변수가 추가되면 README와 GitHub Secrets 목록을 확인합니다.
- 엔티티가 바뀌면 ERD SQL과 이미지를 수정하고, PR에 변경 의도를 적습니다.
- 패키지 구조나 협업 규칙이 바뀌면 README와 CONTRIBUTING을 확인합니다.
