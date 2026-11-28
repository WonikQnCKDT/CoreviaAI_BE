# 02. 관리자 — 공통 코드·다국어 라벨 변경 이력 (API)

> 상태: Draft · 담당자: 미정 · 마지막 검토일: 2026-11-28
> 기준: FR-ADM-07(공통 코드 관리), FR-ADM-06(다국어 라벨 관리), 요구사항 명세서 3.3, ERD 02 (`audit_logs`)
> 감사 로그(`audit_logs`) 기반 조회이며 DB 스키마(ERD) 변경은 없다. 단순 조회 흐름이므로 시퀀스 다이어그램은 생략한다.

- 관련: [SCRUM-168](https://jnu-team-aaptc1y6.atlassian.net/browse/SCRUM-168), [SCRUM-169](https://jnu-team-aaptc1y6.atlassian.net/browse/SCRUM-169), [전사관리 요구사항](https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/8126909)
- 공식 API 명세서 템플릿과 [00. 공통 규약 (API)](https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/12877846)을 따른다.

## 요약

| Method | Path | 설명 | 권한 | 상태 |
| --- | --- | --- | --- | --- |
| GET | `/api/admin/code-groups/{groupCode}/codes/{code}/histories` | 공통 코드 변경 이력 조회(커서) | `SYSTEM_ADMIN` | 구현 |
| GET | `/api/admin/i18n/labels/{labelId}/histories` | 다국어 라벨 변경 이력 조회(커서) | `SYSTEM_ADMIN` | 구현 |

## 공통 사항

- 응답 형식(`ApiResponse<T>`), 쿠키 인증, 공통 오류 규격은 `00. 공통 규약 (API)`를 따른다.
- 접근 제어는 `CoreviaSecurityConfig`의 `/api/admin/**` 규칙이 맡는다.
  - 미인증 요청: `401 UNAUTHORIZED`
  - 인증됐지만 `SYSTEM_ADMIN`이 아닌 사용자(`USER`, `EXECUTIVE` 등): `404 NOT_FOUND` (관리자 API 존재 비노출 정책)
- 이력은 `audit_logs` 테이블에서 대상별(`target_type`, `target_id`)로 `id` 내림차순(최신순) 커서 페이지네이션으로 조회한다.
- 응답 항목 스키마(`AdminHistoryResponse`)는 사용자 변경 이력(`GET /api/admin/users/{userId}/histories`의 `UserHistoryResponse`)과 동일하다.
- `details`는 감사 로그에 저장된 JSON 문자열을 객체(`JsonNode`)로 파싱해 내려준다.
- `actorName`은 `actorUserId`를 일괄 조회해 채우며, 행위자가 없거나 삭제된 사용자인 경우 `null`이다.
- 커서(`cursor`)는 불투명 Base64URL(`v1:<id>`) 문자열이다. 이전 응답의 `nextCursor`를 변경 없이 전달한다. 형식이 잘못되면 `400 INVALID_CURSOR`.
- `size`는 기본 `20`, 허용 범위 `1`~`100`. 범위를 벗어나면 `400`.
- 별도 rate limit은 없으며 기존 인증·세션 정책을 따른다. GET 조회이므로 상태를 변경하지 않는다(멱등).

## enum 값 (`action`)

| 대상 | `AuditAction` 값 | 설명 |
| --- | --- | --- |
| 공통 코드(`CODE`) | `CODE_CHANGED` | 코드 정렬 순서 변경 (`PATCH /api/admin/code-groups/{groupCode}/codes/{code}`) |
| 다국어 라벨(`I18N_LABEL`) | `I18N_LABEL_CREATED` | 라벨 신규 등록 (`POST /api/admin/i18n/labels`) |
| 다국어 라벨(`I18N_LABEL`) | `I18N_LABEL_CHANGED` | 라벨 문구 수정 (`PATCH /api/admin/i18n/labels/{labelId}`, 바뀐 언어마다 1건) |

## GET /api/admin/code-groups/{groupCode}/codes/{code}/histories — 공통 코드 변경 이력 조회

- 목적: 특정 공통 코드의 변경 이력(정렬 순서 변경 전·후)을 최신순으로 조회한다.
- 인증·권한: 로그인 필수, `SYSTEM_ADMIN` 전용.

### Request

| 위치 | 이름 | 타입 | 필수 | 제약 |
| --- | --- | --- | --- | --- |
| Path | `groupCode` | string | Y | `^[A-Z][A-Z0-9_]{0,49}$` |
| Path | `code` | string | Y | `^[A-Z][A-Z0-9_]{0,39}$` |
| Query | `cursor` | string | N | 첫 페이지는 생략. 이전 응답의 `nextCursor` 전달, 최대 256자 |
| Query | `size` | integer | N | 기본 `20`, `1`~`100` |

### Response (`200 OK`)

`response`: `CursorPage<AdminHistoryResponse>` (`items`, `nextCursor`, `hasNext`).

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `items[].id` | number | 감사 로그 ID |
| `items[].action` | string | `CODE_CHANGED` |
| `items[].actorUserId` | number 또는 null | 변경 수행자 사용자 ID |
| `items[].actorName` | string 또는 null | 변경 수행자 이름 |
| `items[].details` | object 또는 null | `{"groupCode": "...", "code": "...", "sortOrder": {"before": 1, "after": 3}}` |
| `items[].ipAddress` | string 또는 null | 요청 IP 주소 |
| `items[].occurredAt` | string (ISO-8601 UTC) | 변경 일시 |
| `nextCursor` | string 또는 null | 다음 페이지 커서(`hasNext=false`이면 `null`) |
| `hasNext` | boolean | 다음 페이지 존재 여부 |

```json
{
  "success": true,
  "response": {
    "items": [
      {
        "id": 42,
        "action": "CODE_CHANGED",
        "actorUserId": 1,
        "actorName": "시스템 관리자",
        "details": {
          "groupCode": "RISK_LEVEL",
          "code": "NORMAL",
          "sortOrder": {
            "before": 1,
            "after": 3
          }
        },
        "ipAddress": "127.0.0.1",
        "occurredAt": "2026-11-28T11:20:00Z"
      }
    ],
    "nextCursor": "djE6NDI",
    "hasNext": true
  },
  "error": null
}
```

### 오류 응답

| 상태 코드 | 오류 코드 | 사유 |
| --- | --- | --- |
| `400` | `BAD_REQUEST` | `groupCode`·`code` 패턴 위반, `size` 범위(1~100) 위반 |
| `400` | `INVALID_CURSOR` | 잘못된 커서 형식 |
| `401` | `UNAUTHORIZED` | 미인증 또는 만료 세션 |
| `404` | `NOT_FOUND` | 비관리자(`USER`, `EXECUTIVE` 등) 접근 |
| `404` | `CODE_GROUP_NOT_FOUND` | 존재하지 않는 코드 그룹 |
| `404` | `CODE_NOT_FOUND` | 그룹 내 존재하지 않는 코드 |

## GET /api/admin/i18n/labels/{labelId}/histories — 다국어 라벨 변경 이력 조회

- 목적: 특정 다국어 라벨의 등록 및 언어별 문구 변경 이력을 최신순으로 조회한다.
- 인증·권한: 로그인 필수, `SYSTEM_ADMIN` 전용.

### Request

| 위치 | 이름 | 타입 | 필수 | 제약 |
| --- | --- | --- | --- | --- |
| Path | `labelId` | number | Y | 양수(`>= 1`) |
| Query | `cursor` | string | N | 첫 페이지는 생략. 이전 응답의 `nextCursor` 전달, 최대 256자 |
| Query | `size` | integer | N | 기본 `20`, `1`~`100` |

### Response (`200 OK`)

`response`: `CursorPage<AdminHistoryResponse>` (`items`, `nextCursor`, `hasNext`).

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `items[].id` | number | 감사 로그 ID |
| `items[].action` | string | `I18N_LABEL_CREATED` 또는 `I18N_LABEL_CHANGED` |
| `items[].actorUserId` | number 또는 null | 변경 수행자 사용자 ID |
| `items[].actorName` | string 또는 null | 변경 수행자 이름 |
| `items[].details` | object 또는 null | 등록 시 `{"labelKey","textKo","textEn","textZhTw"}`, 수정 시 `{"labelKey","lang","text":{"before","after"}}` |
| `items[].ipAddress` | string 또는 null | 요청 IP 주소 |
| `items[].occurredAt` | string (ISO-8601 UTC) | 변경 일시 |
| `nextCursor` | string 또는 null | 다음 페이지 커서(`hasNext=false`이면 `null`) |
| `hasNext` | boolean | 다음 페이지 존재 여부 |

```json
{
  "success": true,
  "response": {
    "items": [
      {
        "id": 55,
        "action": "I18N_LABEL_CHANGED",
        "actorUserId": 1,
        "actorName": "시스템 관리자",
        "details": {
          "labelKey": "menu.nav.home",
          "lang": "en",
          "text": {
            "before": "Home",
            "after": "Home Page"
          }
        },
        "ipAddress": "127.0.0.1",
        "occurredAt": "2026-11-28T11:25:00Z"
      },
      {
        "id": 54,
        "action": "I18N_LABEL_CREATED",
        "actorUserId": 1,
        "actorName": "시스템 관리자",
        "details": {
          "labelKey": "menu.nav.home",
          "textKo": "홈",
          "textEn": "Home",
          "textZhTw": null
        },
        "ipAddress": "127.0.0.1",
        "occurredAt": "2026-11-28T11:20:00Z"
      }
    ],
    "nextCursor": null,
    "hasNext": false
  },
  "error": null
}
```

### 오류 응답

| 상태 코드 | 오류 코드 | 사유 |
| --- | --- | --- |
| `400` | `BAD_REQUEST` | `labelId` 양수 위반, `size` 범위(1~100) 위반 |
| `400` | `INVALID_CURSOR` | 잘못된 커서 형식 |
| `401` | `UNAUTHORIZED` | 미인증 또는 만료 세션 |
| `404` | `NOT_FOUND` | 비관리자(`USER`, `EXECUTIVE` 등) 접근 |
| `404` | `LABEL_NOT_FOUND` | 존재하지 않는 라벨 ID |

## 변경 이력

| 날짜 | 내용 | 관련 |
| --- | --- | --- |
| 2026-11-28 | 공통 코드·다국어 라벨 변경 이력 조회 API 명세 작성 | SCRUM-168, SCRUM-169 |
