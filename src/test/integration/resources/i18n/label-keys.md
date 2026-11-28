# FE 다국어 라벨 키 목록

> 자동 생성 문서입니다. 직접 고치지 말고 FE에서 `UPDATE_LABEL_DOC=1 npm test`로 다시 만드세요.
> 원본: `src/**/i18n/` 의 `defineLabels`·`defineLabelMap` 정의 (Confluence 02-4. 다국어 라벨, FR-ADM-06)

## 사용 방법 (BE 담당자)

- 아래 **등록 필요** 표의 키를 `i18n_labels`에 시드하거나 관리 화면(시스템 관리 › 다국어 라벨)에서 등록합니다.
- `text_ko`는 표의 한국어 기본 문구를 그대로 쓰면 화면이 지금과 같게 보입니다. `text_en`·`text_zh_tw`는 번역이 준비되면 채웁니다(비우면 한국어로 표시).
- 등록하지 않은 키도 FE는 한국어 기본 문구로 표시하므로 화면이 깨지지 않습니다.
- `{name}`처럼 중괄호로 감싼 부분은 FE가 값을 넣는 자리입니다. 번역문에도 같은 이름으로 남겨 주세요.

## 등록 필요 (1495개)

| 라벨 키 | 한국어 기본 문구 | 구분 |
| --- | --- | --- |
| `admin.activateConfirmBody` | 활성화하면 다시 로그인할 수 있습니다. | admin |
| `admin.activateConfirmTitle` | 계정 활성화 | admin |
| `admin.activateDepartmentLead` | {name} 부서를 활성화합니다. | admin |
| `admin.activateDepartmentTitle` | 부서 활성화 | admin |
| `admin.activeMembersNote` | 활성 인원은 그 부서에 직접 소속된 사람만 셉니다. | admin |
| `admin.actorUnknown` | 알 수 없는 사용자 | admin |
| `admin.addChildDepartment` | 하위 부서 추가 | admin |
| `admin.addDepartment` | 부서 추가 | admin |
| `admin.addUser` | 사용자 추가 | admin |
| `admin.allDepartments` | 부서 전체 | admin |
| `admin.allRoles` | 시스템 역할 전체 | admin |
| `admin.allStatuses` | 상태 전체 | admin |
| `admin.apiHeaderNote` | 시스템 관리자 전용 · 사용자·부서·공통 코드·다국어 라벨 변경은 서버에 바로 반영되고 감사 로그에 기록됩니다 | admin |
| `admin.booleanFalse` | 없음 | admin |
| `admin.booleanTrue` | 있음 | admin |
| `admin.cancel` | 취소 | admin |
| `admin.close` | 닫기 | admin |
| `admin.code.codeListAria` | 코드 목록 | admin.code |
| `admin.code.colActive` | 사용 여부 | admin.code |
| `admin.code.colCodeValue` | 코드 값 | admin.code |
| `admin.code.colLabelKey` | 라벨 키 | admin.code |
| `admin.code.colSortOrder` | 정렬 순서 | admin.code |
| `admin.code.colUpdatedAt` | 수정일 | admin.code |
| `admin.code.description` | 상태값을 코드로 관리합니다. 관리자는 그룹 안 표시 순서만 바꿀 수 있습니다. | admin.code |
| `admin.code.displayText` | 표시 문구 | admin.code |
| `admin.code.emptyCodes` | 이 그룹에는 코드가 없습니다 | admin.code |
| `admin.code.emptyGroups` | 등록된 코드 그룹이 없습니다 | admin.code |
| `admin.code.emptyGroupsHint` | 코드 그룹은 서버 마이그레이션으로만 추가됩니다. | admin.code |
| `admin.code.fixedNote` | 코드 값·라벨 키·사용 여부는 바꿀 수 없습니다(서버 마이그레이션으로만 변경). 표시 텍스트는 다국어 라벨에서 라벨 키로 관리합니다. 정렬 순서를 바꾸면 감사 로그에 기록됩니다. | admin.code |
| `admin.code.groupCodeListAria` | {groupCode} 코드 목록 | admin.code |
| `admin.code.groupsLabel` | 코드 그룹 | admin.code |
| `admin.code.historyButtonAria` | {code} 변경 이력 | admin.code |
| `admin.code.historyDescription` | 정렬 순서 변경 이력을 최신순으로 봅니다. | admin.code |
| `admin.code.inUse` | 사용 | admin.code |
| `admin.code.lastWriteWins` | 다른 관리자가 같은 코드의 순서를 동시에 바꾸면 나중에 저장한 값이 남습니다. | admin.code |
| `admin.code.loadCodesFailed` | 코드를 불러오지 못했습니다 | admin.code |
| `admin.code.loadGroupsFailed` | 코드 그룹을 불러오지 못했습니다 | admin.code |
| `admin.code.notInUse` | 사용 안 함 | admin.code |
| `admin.code.selectGroup` | 왼쪽에서 코드 그룹을 고르세요 | admin.code |
| `admin.code.sortOrderInputAria` | {code} 정렬 순서 | admin.code |
| `admin.code.sortOrderRange` | 정렬 순서는 0 이상 9999 이하의 정수로 입력해 주세요. | admin.code |
| `admin.code.sortOrderRequired` | 정렬 순서를 입력해 주세요. | admin.code |
| `admin.code.title` | 공통 코드 | admin.code |
| `admin.colActiveMembers` | 활성 인원 | admin |
| `admin.colDepartment` | 부서 | admin |
| `admin.colJobTitle` | 직무 | admin |
| `admin.colLastLogin` | 마지막 로그인 | admin |
| `admin.colManage` | 관리 | admin |
| `admin.colStatus` | 상태 | admin |
| `admin.colSystemRole` | 시스템 역할 | admin |
| `admin.colUser` | 사용자 | admin |
| `admin.createdThenReset` | 계정을 만들었습니다. 비밀번호가 없어 아직 로그인할 수 없으니 이어서 임시 비밀번호를 발급해 주세요. | admin |
| `admin.deactivateConfirmBody` | 비활성화하면 대상 사용자의 모든 세션이 즉시 종료되고 이후 로그인할 수 없습니다. | admin |
| `admin.deactivateConfirmTitle` | 계정 비활성화 | admin |
| `admin.deactivateDepartmentBody` | 비활성 부서에는 사용자를 새로 배치하거나 하위 부서를 만들 수 없습니다. 활성 사용자나 활성 하위 부서가 있으면 비활성화할 수 없습니다. | admin |
| `admin.deactivateDepartmentLead` | {name} 부서를 비활성화합니다. | admin |
| `admin.deactivateDepartmentTitle` | 부서 비활성화 | admin |
| `admin.demoHeaderNote` | 시스템 관리자 전용 · 변경 사항은 데모 저장소에만 반영됩니다 | admin |
| `admin.departmentCreateDescription` | 같은 상위 부서 안에서는 이름이 겹칠 수 없습니다. | admin |
| `admin.departmentEditDescription` | 이름을 바꾸거나 다른 상위 부서 밑으로 옮깁니다. | admin |
| `admin.departmentNameRequired` | 부서 이름을 입력해 주세요. | admin |
| `admin.departmentNameTooLong` | 부서 이름은 50자 이하로 입력해 주세요. | admin |
| `admin.departmentRequired` | 시스템 관리자가 아니면 부서를 선택해야 합니다. | admin |
| `admin.departmentsDescription` | 부서를 트리로 관리합니다. 부서는 삭제하지 않고 비활성화합니다. | admin |
| `admin.departmentsListAria` | 부서 목록 | admin |
| `admin.departmentsTitle` | 부서 | admin |
| `admin.deptFilterExcludesChildren` | 부서 필터는 하위 부서를 포함하지 않습니다. | admin |
| `admin.depthSubDepartment` | {depth}단계 하위 부서 | admin |
| `admin.editDepartment` | 부서 수정 | admin |
| `admin.editUser` | 사용자 정보 수정 | admin |
| `admin.emptyDepartments` | 등록된 부서가 없습니다 | admin |
| `admin.emptyDepartmentsHint` | 첫 부서를 추가해 조직을 구성해 주세요. | admin |
| `admin.emptyHistory` | 변경 이력이 없습니다 | admin |
| `admin.emptyUsers` | 조건에 맞는 사용자가 없습니다 | admin |
| `admin.emptyUsersHint` | 검색어나 필터를 바꿔 다시 시도해 주세요. | admin |
| `admin.error.concurrentModification` | 다른 관리자가 먼저 변경했습니다. 최신 내용을 불러왔으니 확인 후 다시 시도해 주세요. | admin.error |
| `admin.error.sessionRevokeFailed` | 변경은 저장됐지만 대상 사용자의 세션을 끊지 못했습니다. 다시 시도하면 세션 종료를 다시 요청합니다. | admin.error |
| `admin.errorLog.anonymous` | 로그인 전 | admin.errorLog |
| `admin.errorLog.apiPath` | API 경로 | admin.errorLog |
| `admin.errorLog.apiPathHint` | 앞부분 일치, 대소문자 구분 | admin.errorLog |
| `admin.errorLog.apiPathTooLong` | API 경로는 300자 이하로 입력해 주세요. | admin.errorLog |
| `admin.errorLog.description` | 서버에서 처리하지 못한 예외를 최신순으로 봅니다. 기록은 고칠 수 없고 1년 보관합니다. | admin.errorLog |
| `admin.errorLog.detailTitle` | 에러 로그 상세 | admin.errorLog |
| `admin.errorLog.empty` | 조건에 맞는 에러 로그가 없습니다 | admin.errorLog |
| `admin.errorLog.emptyHint` | 기간이나 조건을 바꿔 다시 조회해 보세요. | admin.errorLog |
| `admin.errorLog.errorType` | 예외 유형 | admin.errorLog |
| `admin.errorLog.errorTypeHint` | 클래스 전체 이름과 정확히 일치 | admin.errorLog |
| `admin.errorLog.errorTypeTooLong` | 예외 유형은 200자 이하로 입력해 주세요. | admin.errorLog |
| `admin.errorLog.filterTitle` | 조회 조건 | admin.errorLog |
| `admin.errorLog.fromDate` | 시작일 | admin.errorLog |
| `admin.errorLog.invalidDate` | 날짜를 YYYY-MM-DD 형식으로 입력해 주세요. | admin.errorLog |
| `admin.errorLog.invalidPeriod` | 시작일은 종료일보다 늦을 수 없습니다. | admin.errorLog |
| `admin.errorLog.loadFailed` | 에러 로그를 불러오지 못했습니다 | admin.errorLog |
| `admin.errorLog.maskedNote` | 메시지와 스택 트레이스의 비밀번호·토큰·이메일·전화번호 같은 값은 서버가 저장 전에 \*\*\*\* 로 가렸습니다. 요청 본문·헤더는 저장하지 않습니다. | admin.errorLog |
| `admin.errorLog.message` | 메시지 | admin.errorLog |
| `admin.errorLog.noMessage` | 메시지 없음 | admin.errorLog |
| `admin.errorLog.noStackTrace` | 스택 트레이스가 없습니다 | admin.errorLog |
| `admin.errorLog.notFound` | 에러 로그를 찾을 수 없습니다. 목록을 새로 불러와 주세요. | admin.errorLog |
| `admin.errorLog.occurredAt` | 발생 시각 | admin.errorLog |
| `admin.errorLog.openDetail` | 상세 보기 | admin.errorLog |
| `admin.errorLog.periodNote` | 기간은 이 브라우저의 시간대 기준으로, 종료일 하루를 포함해 조회합니다. | admin.errorLog |
| `admin.errorLog.request` | 요청 | admin.errorLog |
| `admin.errorLog.search` | 조회 | admin.errorLog |
| `admin.errorLog.stackTrace` | 스택 트레이스 | admin.errorLog |
| `admin.errorLog.title` | 에러 로그 | admin.errorLog |
| `admin.errorLog.toDate` | 종료일 | admin.errorLog |
| `admin.errorLog.user` | 사용자 | admin.errorLog |
| `admin.errorLog.userIdFallback` | 사용자 #{id} | admin.errorLog |
| `admin.fieldDepartmentName` | 부서 이름 | admin |
| `admin.fieldDepartmentOptional` | 부서 (선택) | admin |
| `admin.fieldDepartmentSelect` | 부서 선택 | admin |
| `admin.fieldJobTitleOptional` | 직무 (선택) | admin |
| `admin.fieldLoginId` | 아이디 | admin |
| `admin.fieldName` | 이름 | admin |
| `admin.fieldParentDepartment` | 상위 부서 | admin |
| `admin.history` | 변경 이력 | admin |
| `admin.history.action.CODE_CHANGED` | 정렬 순서 변경 | admin.history.action |
| `admin.history.action.I18N_LABEL_CHANGED` | 문구 수정 | admin.history.action |
| `admin.history.action.I18N_LABEL_CREATED` | 라벨 등록 | admin.history.action |
| `admin.history.action.PASSWORD_RESET` | 비밀번호 초기화 | admin.history.action |
| `admin.history.action.PROJECT_CREATION_GRANTED` | 프로젝트 생성 권한 부여 (폐지된 기능) | admin.history.action |
| `admin.history.action.PROJECT_CREATION_REVOKED` | 프로젝트 생성 권한 회수 (폐지된 기능) | admin.history.action |
| `admin.history.action.USER_ACTIVATED` | 활성화 | admin.history.action |
| `admin.history.action.USER_CREATED` | 계정 생성 | admin.history.action |
| `admin.history.action.USER_DEACTIVATED` | 비활성화 | admin.history.action |
| `admin.history.action.USER_PROFILE_CHANGED` | 정보 수정 | admin.history.action |
| `admin.history.action.USER_ROLE_CHANGED` | 시스템 역할 변경 | admin.history.action |
| `admin.history.field.canCreateProject` | 프로젝트 생성 권한 | admin.history.field |
| `admin.history.field.departmentId` | 부서 | admin.history.field |
| `admin.history.field.departmentName` | 부서 | admin.history.field |
| `admin.history.field.jobTitle` | 직무 | admin.history.field |
| `admin.history.field.loginId` | 아이디 | admin.history.field |
| `admin.history.field.name` | 이름 | admin.history.field |
| `admin.history.field.status` | 상태 | admin.history.field |
| `admin.history.field.systemRole` | 시스템 역할 | admin.history.field |
| `admin.historyArrowAria` | 에서 | admin |
| `admin.historyDescription` | 최신 변경이 위에 옵니다. 비밀번호 초기화는 기록만 남고 내용은 저장하지 않습니다. | admin |
| `admin.inactiveSuffix` |  (비활성) | admin |
| `admin.jobTitleTooLong` | 직무는 50자 이하로 입력해 주세요. | admin |
| `admin.label.add` | 라벨 등록 | admin.label |
| `admin.label.applyNow` | 지금 화면에 적용 | admin.label |
| `admin.label.concurrentModification` | 다른 관리자가 먼저 수정했습니다. 최신 문구를 불러왔으니 확인 후 다시 저장해 주세요. | admin.label |
| `admin.label.created` | {key} 라벨을 등록했습니다. | admin.label |
| `admin.label.description` | 화면 문구를 언어별로 관리합니다. 라벨 키는 바꾸거나 지울 수 없고, 문구만 고칩니다. | admin.label |
| `admin.label.edit` | 문구 수정 | admin.label |
| `admin.label.empty` | 조건에 맞는 라벨이 없습니다 | admin.label |
| `admin.label.emptyHint` | 검색어나 누락 필터를 바꿔 보세요. | admin.label |
| `admin.label.emptyMeansMissing` | 영어·번체 중국어를 비우면 번역 누락으로 저장되고 화면에는 한국어로 표시됩니다. | admin.label |
| `admin.label.historyButtonAria` | {key} 변경 이력 | admin.label |
| `admin.label.historyDescription` | 라벨 등록 및 언어별 문구 수정 이력을 최신순으로 봅니다. | admin.label |
| `admin.label.key` | 라벨 키 | admin.label |
| `admin.label.keyFixed` | 라벨 키는 바꿀 수 없습니다. | admin.label |
| `admin.label.keyFormat` | 라벨 키는 영문자로 시작하고 점(.)으로 구분한 영문·숫자·밑줄·하이픈 100자 이하로 입력해 주세요. | admin.label |
| `admin.label.keyRequired` | 라벨 키를 입력해 주세요. | admin.label |
| `admin.label.loadFailed` | 라벨을 불러오지 못했습니다 | admin.label |
| `admin.label.missing` | 번역 누락 | admin.label |
| `admin.label.missingAll` | 번역 누락 필터 없음 | admin.label |
| `admin.label.missingEn` | 영어 번역 누락 | admin.label |
| `admin.label.missingFilter` | 번역 누락 필터 | admin.label |
| `admin.label.missingZhTw` | 번체 중국어 번역 누락 | admin.label |
| `admin.label.saved` | {key} 라벨을 저장했습니다. 다른 사용자 화면에는 다음에 문구를 불러올 때 반영됩니다. | admin.label |
| `admin.label.searchPlaceholder` | 라벨 키 또는 한국어 문구 검색 | admin.label |
| `admin.label.textControlCharacter` | 문구에는 줄바꿈·탭 같은 제어 문자를 넣을 수 없습니다. | admin.label |
| `admin.label.textEn` | 영어 | admin.label |
| `admin.label.textKo` | 한국어 | admin.label |
| `admin.label.textKoRequired` | 한국어 문구를 입력해 주세요. | admin.label |
| `admin.label.textOnlyNote` | 문구는 HTML로 해석하지 않고 글자 그대로 표시합니다. 변경 이력은 감사 로그에 남습니다. | admin.label |
| `admin.label.textTooLong` | 문구는 500자 이하로 입력해 주세요. | admin.label |
| `admin.label.textZhTw` | 번체 중국어 | admin.label |
| `admin.label.title` | 다국어 라벨 | admin.label |
| `admin.label.updatedAt` | 수정일 | admin.label |
| `admin.loadDepartmentsFailed` | 부서 목록을 불러오지 못했습니다 | admin |
| `admin.loadHistoryFailed` | 변경 이력을 불러오지 못했습니다 | admin |
| `admin.loading` | 불러오는 중입니다 | admin |
| `admin.loadingMore` | 더 불러오는 중 | admin |
| `admin.loadMore` | 더 보기 | admin |
| `admin.loadUsersFailed` | 사용자 목록을 불러오지 못했습니다 | admin |
| `admin.loginIdFixedNote` | 아이디 {loginId}는 바꿀 수 없습니다. | admin |
| `admin.loginIdFormat` | 아이디는 50자 이하의 영문·숫자·마침표(.)·밑줄(_)·하이픈(-)만 쓸 수 있습니다. | admin |
| `admin.loginIdRequired` | 아이디를 입력해 주세요. | admin |
| `admin.memberCountUnit` | {count}명 | admin |
| `admin.nameRequired` | 이름을 입력해 주세요. | admin |
| `admin.nameTooLong` | 이름은 50자 이하로 입력해 주세요. | admin |
| `admin.neverLoggedIn` | 로그인 기록 없음 | admin |
| `admin.noAccessDescription` | 시스템 관리는 시스템 관리자 역할에게만 표시되는 메뉴입니다. 권한이 필요하면 관리자에게 요청해 주세요. 시스템 관리자 역할은 프로젝트 데이터 접근 권한을 자동으로 부여하지 않습니다. | admin |
| `admin.noAccessTitle` | 시스템 관리 권한이 없습니다 | admin |
| `admin.none` | 없음 | admin |
| `admin.notice.codeSortOrderChanged` | {code} 코드의 정렬 순서를 {sortOrder}(으)로 바꿨습니다. | admin.notice |
| `admin.notice.departmentCreated` | {name} 부서를 추가했습니다. | admin.notice |
| `admin.notice.departmentSaved` | {name} 부서를 저장했습니다. | admin.notice |
| `admin.notice.departmentStatusChanged` | {name} 부서를 {status} 상태로 바꿨습니다. | admin.notice |
| `admin.notice.userRoleChanged` | {name} 사용자의 역할을 {role}(으)로 바꿨습니다. 대상 사용자는 다시 로그인해야 합니다. | admin.notice |
| `admin.notice.userSaved` | {name} 사용자 정보를 저장했습니다. | admin.notice |
| `admin.notice.userStatusChanged` | {name} 계정을 {status} 상태로 바꿨습니다. | admin.notice |
| `admin.ownAccountLocked` | 본인 계정의 역할과 상태는 바꿀 수 없습니다. | admin |
| `admin.parentDepartmentCreateHint` | 활성 부서만 상위 부서로 고를 수 있습니다. | admin |
| `admin.parentDepartmentEditHint` | 활성 부서만 상위 부서로 고를 수 있습니다. 자기 자신과 하위 부서 밑으로는 옮길 수 없습니다. | admin |
| `admin.pmReassign.clearSelection` | 선택 해제 | admin.pmReassign |
| `admin.pmReassign.code` | 프로젝트 코드 | admin.pmReassign |
| `admin.pmReassign.codeFormat` | 프로젝트 코드는 영문·숫자로 시작하는 영문·숫자·하이픈(-) 2~20자입니다. | admin.pmReassign |
| `admin.pmReassign.codePlaceholder` | 예) PRT-2026-001 | admin.pmReassign |
| `admin.pmReassign.codeRequired` | 프로젝트 코드를 입력해 주세요. | admin.pmReassign |
| `admin.pmReassign.confirmBody` | {code} 프로젝트의 PM으로 {name}을(를) 지정합니다. | admin.pmReassign |
| `admin.pmReassign.confirmNote` | 활성 PM이 남아 있는 프로젝트이거나 코드가 없으면 서버가 거부합니다. 남아 있던 비활성 PM은 멤버에서 제외됩니다. | admin.pmReassign |
| `admin.pmReassign.confirmTitle` | PM을 재지정하시겠습니까? | admin.pmReassign |
| `admin.pmReassign.description` | 활성 PM이 한 명도 남지 않은 프로젝트(퇴사·비활성화 등)에만 새 PM을 지정합니다. 남아 있던 비활성 PM은 멤버에서 제외되고, 처리 내용은 프로젝트 멤버 이력과 감사 로그에 남습니다. | admin.pmReassign |
| `admin.pmReassign.done` | {code} 프로젝트의 PM을 재지정했습니다. 현재 PM: {names} | admin.pmReassign |
| `admin.pmReassign.notAllowed` | 재지정할 수 없습니다. 활성 PM이 남아 있는 프로젝트이거나 없는 프로젝트 코드입니다. 코드를 확인하고, 활성 PM이 있으면 그 PM이 직접 PM을 추가·해제하도록 안내해 주세요. | admin.pmReassign |
| `admin.pmReassign.privacyNote` | 시스템 관리자는 프로젝트 데이터를 볼 수 없어 프로젝트 코드로만 지정합니다. 활성 PM이 있는 프로젝트는 PM이 직접 PM을 추가·해제합니다. | admin.pmReassign |
| `admin.pmReassign.selected` | 선택됨: {name} | admin.pmReassign |
| `admin.pmReassign.submit` | PM 재지정 | admin.pmReassign |
| `admin.pmReassign.title` | PM 재지정 | admin.pmReassign |
| `admin.pmReassign.user` | 새 PM | admin.pmReassign |
| `admin.pmReassign.userEmpty` | 조건에 맞는 활성 일반 사용자가 없습니다 | admin.pmReassign |
| `admin.pmReassign.userFailed` | 사용자 목록을 불러오지 못했습니다 | admin.pmReassign |
| `admin.pmReassign.userLoading` | 사용자를 찾는 중입니다 | admin.pmReassign |
| `admin.pmReassign.userRequired` | 새 PM으로 지정할 사용자를 선택해 주세요. | admin.pmReassign |
| `admin.pmReassign.userSearch` | 새 PM 이름 또는 아이디 검색 | admin.pmReassign |
| `admin.pmReassign.userSearchHint` | 활성 일반 사용자만 PM으로 지정할 수 있습니다. | admin.pmReassign |
| `admin.resetConfirmLead` | {name} 사용자의 비밀번호를 초기화합니다. 기존 세션은 모두 종료됩니다. | admin |
| `admin.resetCopied` | 클립보드에 복사했습니다. | admin |
| `admin.resetCopyFailed` | 복사하지 못했습니다. 직접 선택해 복사해 주세요. | admin |
| `admin.resetDescription` | 임시 비밀번호를 발급하고 계정 잠금을 해제합니다. 대상 사용자는 다음 로그인 후 비밀번호를 변경해야 합니다. | admin |
| `admin.resetFilters` | 필터 초기화 | admin |
| `admin.resetIssuedLead` | {name} 사용자의 임시 비밀번호입니다. 이 창을 닫으면 다시 볼 수 없으니 지금 대상 사용자에게 안전하게 전달해 주세요. | admin |
| `admin.resetServerUserIdHint` | 사용자 목록 API가 연결되기 전까지는 인증 서버의 사용자 ID를 직접 입력합니다. 이 브라우저에서 서버로 로그인한 적이 있는 사용자는 자동으로 채워집니다. | admin |
| `admin.resetServerUserIdLabel` | 서버 사용자 ID | admin |
| `admin.resetTitle` | 비밀번호 초기화 | admin |
| `admin.retry` | 다시 시도 | admin |
| `admin.roleChangeConfirmBody` | 역할을 바꾸면 대상 사용자의 모든 세션이 즉시 종료되어 다시 로그인해야 합니다. | admin |
| `admin.roleChangeConfirmLead` | {name}님의 시스템 역할을 {from}에서 {to}(으)로 바꿉니다. | admin |
| `admin.roleChangeConfirmTitle` | 시스템 역할 변경 | admin |
| `admin.save` | 저장 | admin |
| `admin.saving` | 저장 중 | admin |
| `admin.searchPlaceholder` | 아이디 또는 이름 검색 | admin |
| `admin.self` | 나 | admin |
| `admin.serverAppliedNote` | 변경 사항은 서버에 바로 반영되고 감사 로그에 기록됩니다. | admin |
| `admin.showInactive` | 비활성 부서 보기 | admin |
| `admin.statusChangeConfirmLead` | {name} 계정을 {status} 상태로 바꿉니다. | admin |
| `admin.topLevel` | 최상위 | admin |
| `admin.topLevelDepartment` | 최상위 부서 | admin |
| `admin.userCreateDescription` | 계정만 만들어집니다. 만든 뒤 임시 비밀번호를 발급해야 로그인할 수 있습니다. | admin |
| `admin.userEditDescription` | 시스템 역할과 상태는 목록에서 따로 바꿉니다. | admin |
| `admin.userRoleAria` | {name} 시스템 역할 | admin |
| `admin.usersDescription` | 계정·부서·직무·시스템 역할을 관리합니다. 사용자는 삭제하지 않고 비활성화합니다. | admin |
| `admin.usersListAria` | 사용자 목록 | admin |
| `admin.usersTitle` | 사용자 | admin |
| `auth.accountDisabled` | 비활성화된 계정입니다. 시스템 관리자에게 문의해 주세요. | auth |
| `auth.accountLocked` | 비밀번호를 5회 연속 잘못 입력해 계정이 잠겼습니다. 시스템 관리자에게 비밀번호 초기화를 요청해 주세요. | auth |
| `auth.accountLockedShort` | 계정이 잠겨 있습니다. | auth |
| `auth.accountNotFound` | 계정 정보를 찾을 수 없습니다. 시스템 관리자에게 문의해 주세요. | auth |
| `auth.changeModalDescription` | 비밀번호는 {policy}여야 합니다. | auth |
| `auth.changeModalFooterNote` | 비밀번호 규칙: {policy}. 대문자는 필수가 아닙니다. 변경하면 모든 기기에서 로그아웃되며 새 비밀번호로 다시 로그인해야 합니다. | auth |
| `auth.changeModalTitle` | 비밀번호 변경 | auth |
| `auth.checkingSession` | 로그인 상태를 확인하는 중입니다 | auth |
| `auth.confirmPasswordLabel` | 새 비밀번호 확인 | auth |
| `auth.confirmPasswordMismatch` | 새 비밀번호와 일치하지 않습니다. | auth |
| `auth.confirmPasswordPlaceholder` | 새 비밀번호 다시 입력 | auth |
| `auth.confirmPasswordRequired` | 새 비밀번호 확인을 입력해 주세요. | auth |
| `auth.currentPasswordLabel` | 현재 비밀번호 | auth |
| `auth.currentPasswordMismatch` | 현재 비밀번호가 일치하지 않습니다. | auth |
| `auth.currentPasswordPlaceholder` | 현재(또는 임시) 비밀번호 입력 | auth |
| `auth.currentPasswordRequired` | 현재 비밀번호를 입력해 주세요. | auth |
| `auth.forcedChangeDescription` | 임시 비밀번호로 로그인했습니다. 비밀번호를 변경해야 다른 기능을 사용할 수 있습니다. | auth |
| `auth.forcedChangeTitle` | 비밀번호 변경 필요 | auth |
| `auth.goToLogin` | 로그인 화면으로 | auth |
| `auth.hideFieldPassword` | {label} 숨기기 | auth |
| `auth.invalidCredentials` | 아이디 또는 비밀번호가 올바르지 않습니다. | auth |
| `auth.lockoutHint` | 5회 연속 실패하면 계정이 잠깁니다. | auth |
| `auth.loginFooterPolicy` | 비밀번호 규칙: {policy} · 회원가입·소셜 로그인은 제공하지 않습니다. | auth |
| `auth.loginIdEmailLabel` | 아이디 (이메일) | auth |
| `auth.loginIdLabel` | 아이디 | auth |
| `auth.loginIdPlaceholder` | 아이디 입력 | auth |
| `auth.loginIdRequired` | 아이디를 입력해 주세요. | auth |
| `auth.loginIdTooLong` | 아이디는 50자 이하로 입력해 주세요. | auth |
| `auth.loginSubtitle` | 사내 계정으로 로그인해 주세요. | auth |
| `auth.loginTitle` | 로그인 | auth |
| `auth.network` | 서버에 연결하지 못했습니다. 네트워크 상태를 확인한 뒤 다시 시도해 주세요. | auth |
| `auth.newPasswordInvalid` | 새 비밀번호가 비밀번호 규칙에 맞지 않습니다. | auth |
| `auth.newPasswordLabel` | 새 비밀번호 | auth |
| `auth.newPasswordPlaceholder` | 새 비밀번호 입력 | auth |
| `auth.newPasswordRequired` | 새 비밀번호를 입력해 주세요. | auth |
| `auth.newPasswordRulesUnmet` | 비밀번호 규칙을 모두 충족해야 합니다. | auth |
| `auth.passwordChangedNote` | 보안을 위해 모든 기기에서 로그아웃되었습니다. 새 비밀번호로 다시 로그인해 주세요. | auth |
| `auth.passwordChangedTitle` | 비밀번호가 변경되었습니다. | auth |
| `auth.passwordLabel` | 비밀번호 | auth |
| `auth.passwordPlaceholder` | 비밀번호 입력 | auth |
| `auth.passwordPolicy` | 영문·숫자·특수문자를 각 1자 이상 포함한 8~64자 (공백·한글 불가) | auth |
| `auth.passwordPolicyUnmet` | {missing} 조건을 충족해야 합니다. | auth |
| `auth.passwordRequired` | 비밀번호를 입력해 주세요. | auth |
| `auth.passwordRuleCharset` | 공백·한글 없음 | auth |
| `auth.passwordRuleDigit` | 숫자 포함 | auth |
| `auth.passwordRuleLength` | {min}~{max}자 | auth |
| `auth.passwordRuleLetter` | 영문 포함 | auth |
| `auth.passwordRulesAria` | 비밀번호 규칙 | auth |
| `auth.passwordRuleSpecial` | 특수문자 포함 | auth |
| `auth.passwordTooLong` | 비밀번호는 128자 이하로 입력해 주세요. | auth |
| `auth.resetDemoOnly` | 데모 모드에서는 실제 비밀번호를 변경하지 않습니다. | auth |
| `auth.resetForbidden` | 시스템 관리자만 비밀번호를 초기화할 수 있습니다. | auth |
| `auth.resetUserIdInvalid` | 서버 사용자 ID는 1 이상의 숫자로 입력해 주세요. | auth |
| `auth.resetUserNotFound` | 대상 사용자를 찾을 수 없습니다. 서버 사용자 ID를 확인해 주세요. | auth |
| `auth.retry` | 다시 시도 | auth |
| `auth.ruleMet` | 충족 | auth |
| `auth.ruleUnmet` | 미충족 | auth |
| `auth.sameAsCurrent` | 현재 비밀번호와 다른 비밀번호를 입력해 주세요. | auth |
| `auth.serviceUnavailable` | 로그인 서버에 일시적인 문제가 있습니다. 잠시 후 다시 시도해 주세요. | auth |
| `auth.sessionExpiredNote` | 일정 시간 동안 활동이 없어 세션이 종료되었습니다. 다시 로그인하면 이전에 보던 화면으로 이동합니다. | auth |
| `auth.sessionExpiredTitle` | 로그인이 만료되었습니다. 다시 로그인해주세요. | auth |
| `auth.showFieldPassword` | {label} 표시 | auth |
| `auth.timeout` | 서버 응답이 늦어 요청을 중단했습니다. 다시 시도해 주세요. | auth |
| `auth.unknown` | 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요. | auth |
| `auth.unreachableBody` | 로그인 상태를 확인할 수 없습니다. 네트워크와 서버 상태를 확인한 뒤 다시 시도해 주세요. | auth |
| `auth.unreachableTitle` | 서버에 연결하지 못했습니다 | auth |
| `board.column.add` | 추가 | board.column |
| `board.column.added` | 컬럼을 추가했습니다. | board.column |
| `board.column.addTitle` | 컬럼 추가 | board.column |
| `board.column.cannotDelete` | 카드가 있는 컬럼이나 그 상태의 마지막 컬럼은 삭제할 수 없습니다. | board.column |
| `board.column.close` | 닫기 | board.column |
| `board.column.confirmDelete` | '{name}' 컬럼을 삭제할까요? | board.column |
| `board.column.delete` | 삭제 | board.column |
| `board.column.deleted` | 컬럼을 삭제했습니다. | board.column |
| `board.column.description` | 컬럼 이름·WIP 한도·순서를 바꾸고 컬럼을 추가하거나 삭제합니다. 상태마다 컬럼이 하나 이상 있어야 합니다. | board.column |
| `board.column.limitExceeded` | 컬럼은 프로젝트당 최대 20개입니다. | board.column |
| `board.column.moveLeft` | 앞으로 | board.column |
| `board.column.moveRight` | 뒤로 | board.column |
| `board.column.name` | 이름 | board.column |
| `board.column.nameControl` | 컬럼 이름에는 줄바꿈·탭 같은 제어 문자를 쓸 수 없습니다. | board.column |
| `board.column.namePlaceholder` | 비우면 기본 이름 | board.column |
| `board.column.nameTooLong` | 컬럼 이름은 50자 이하로 입력해 주세요. | board.column |
| `board.column.reordered` | 컬럼 순서를 바꿨습니다. | board.column |
| `board.column.save` | 저장 | board.column |
| `board.column.saved` | 컬럼을 저장했습니다. | board.column |
| `board.column.status` | 상태 | board.column |
| `board.column.title` | 컬럼 관리 | board.column |
| `board.column.wipInvalid` | WIP 한도는 1~999 사이의 정수로 입력하거나 비워 주세요. | board.column |
| `board.column.wipLimit` | WIP 한도 | board.column |
| `board.column.wipPlaceholder` | 없음 | board.column |
| `board.server.assigneeAll` | 담당자 전체 | board.server |
| `board.server.assigneeFilter` | 담당자 | board.server |
| `board.server.cardCount` | {count}개 카드 | board.server |
| `board.server.concurrent` | 다른 사용자가 먼저 바꿨습니다. 최신 보드를 다시 불러왔으니 확인한 뒤 다시 시도해 주세요. | board.server |
| `board.server.dragHint` | 카드를 드래그하거나 '이동'에서 컬럼을 고르면 업무 상태가 바뀝니다. | board.server |
| `board.server.emptyColumn` | 카드가 없습니다. | board.server |
| `board.server.emptyFilteredDescription` | 단계·담당자 필터를 변경하거나 초기화해 주세요. | board.server |
| `board.server.emptyFilteredTitle` | 필터 조건에 해당하는 카드가 없습니다. | board.server |
| `board.server.forbidden` | 이 작업을 할 권한이 없습니다. 권한이 바뀌었을 수 있어 보드를 새로 불러왔습니다. | board.server |
| `board.server.invalid` | 입력값을 확인해 주세요. | board.server |
| `board.server.loadFailed` | 칸반 보드를 불러오지 못했습니다. | board.server |
| `board.server.loading` | 칸반 보드를 불러오는 중입니다 | board.server |
| `board.server.manageColumns` | 컬럼 관리 | board.server |
| `board.server.memberNote` | 본인이 담당한 카드만 옮길 수 있습니다. | board.server |
| `board.server.moved` | '{name}'을(를) '{column}'(으)로 옮겼습니다. | board.server |
| `board.server.moveTo` | 이동 | board.server |
| `board.server.moveToLabel` | '{name}' 카드를 옮길 컬럼 | board.server |
| `board.server.network` | 서버에 연결하지 못했습니다. 네트워크를 확인한 뒤 다시 시도해 주세요. | board.server |
| `board.server.notFound` | 카드나 컬럼을 찾을 수 없습니다. 보드를 새로 불러왔습니다. | board.server |
| `board.server.openDetail` | 상세 보기 | board.server |
| `board.server.phaseAll` | 단계 전체 | board.server |
| `board.server.phaseFilter` | 단계 | board.server |
| `board.server.resetFilters` | 필터 초기화 | board.server |
| `board.server.retry` | 다시 시도 | board.server |
| `board.server.serverError` | 서버 오류로 처리하지 못했습니다. 잠시 후 다시 시도해 주세요. | board.server |
| `board.server.subtaskOf` | {name} 하위 | board.server |
| `board.server.timeout` | 응답이 늦어 요청을 멈췄습니다. 잠시 후 다시 시도해 주세요. | board.server |
| `board.server.unassigned` | 미배정 | board.server |
| `board.server.unavailable` | 이 프로젝트에서는 칸반 보드를 사용할 수 없습니다. 기능 구성에서 칸반을 끈 프로젝트이거나 볼 수 없는 프로젝트입니다. | board.server |
| `board.server.unknown` | 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요. | board.server |
| `board.server.viewerNote` | 현재 역할로는 칸반 보드를 조회만 할 수 있습니다. | board.server |
| `board.server.wipExceeded` | WIP 한도 초과 | board.server |
| `board.server.wipLimit` | WIP {count} | board.server |
| `board.server.wipWarning` | '{column}' 컬럼이 WIP 한도를 넘었습니다. 이동은 반영되었습니다. | board.server |
| `button.acknowledged` | 확인했습니다 | button |
| `button.activate` | 활성화 | button |
| `button.add` | 추가 | button |
| `button.addComment` | 코멘트 등록 | button |
| `button.addItem` | 항목 추가 | button |
| `button.addPersonalSchedule` | 개인 일정 추가 | button |
| `button.addSubtask` | 하위작업 추가 | button |
| `button.addTask` | 업무 추가 | button |
| `button.addTerm` | 용어 등록 | button |
| `button.addTodo` | 할 일 추가 | button |
| `button.addUser` | 사용자 추가 | button |
| `button.addWork` | 작업 추가 | button |
| `button.advanceStage` | 다음 단계로 전환 | button |
| `button.aiOrganize` | AI 정리 | button |
| `button.aiSummary` | AI 요약 | button |
| `button.allowMove` | 이동 허용 | button |
| `button.applyAll` | 전체 적용 | button |
| `button.applyDraft` | 초안 반영 | button |
| `button.approve` | 승인 | button |
| `button.approverLaterHint` | 생성 후 멤버·설정에서 단계별로 지정할 수 있습니다. | button |
| `button.approveSelectedAddWbs` | 선택 항목 승인 · WBS 등록 | button |
| `button.approveSelectedCreateTasks` | 선택 항목 승인 · 업무 생성 | button |
| `button.approveWithEdit` | 수정 후 승인 | button |
| `button.calendarHelp` | 캘린더 사용 방법 | button |
| `button.cancel` | 취소 | button |
| `button.cancelInvite` | 초대 해제 | button |
| `button.categoryNotice` | 공지 | button |
| `button.categoryShare` | 자료 공유 | button |
| `button.change` | 변경 | button |
| `button.changePassword` | 비밀번호 변경 | button |
| `button.changing` | 변경 중 | button |
| `button.chatHistory` | 대화 기록 | button |
| `button.clearAll` | 전체 해제 | button |
| `button.clearDraft` | 초안 비우기 | button |
| `button.clearFilter` | 필터 해제 | button |
| `button.close` | 닫기 | button |
| `button.closeChatHistory` | 대화 기록 닫기 | button |
| `button.closeMenu` | 메뉴 닫기 | button |
| `button.closeNotice` | 안내 닫기 | button |
| `button.closeNotifications` | 알림 닫기 | button |
| `button.closeSummary` | 요약 닫기 | button |
| `button.closeTaskDetail` | 업무 상세 닫기 | button |
| `button.closeUserMenu` | 사용자 메뉴 닫기 | button |
| `button.collapse` | 접기 | button |
| `button.collapseAll` | 전체 접기 | button |
| `button.collapseChatHistory` | 대화 기록 접기 | button |
| `button.collapseMenu` | 메뉴 접기 | button |
| `button.collapseStage` | 단계 접기 | button |
| `button.collapseSubtasks` | 하위작업 접기 | button |
| `button.completeLinkedTask` | 연결 업무 완료 처리 | button |
| `button.copied` | 복사됨 | button |
| `button.copiedDone` | 복사되었습니다 | button |
| `button.copy` | 복사 | button |
| `button.copyProjectCode` | 식별 코드 복사 | button |
| `button.createIssue` | 이슈 등록 | button |
| `button.createProject` | 프로젝트 생성 | button |
| `button.deactivate` | 비활성화 | button |
| `button.delegatePm` | PM 위임 | button |
| `button.delete` | 삭제 | button |
| `button.deleteSchedule` | 일정 삭제 | button |
| `button.deleteTask` | 업무 삭제 | button |
| `button.draft` | 작성중 | button |
| `button.edit` | 수정 | button |
| `button.executeDelegation` | 위임 실행 | button |
| `button.expand` | 펼치기 | button |
| `button.expandAll` | 전체 펼치기 | button |
| `button.expandChatHistory` | 대화 기록 펼치기 | button |
| `button.expandMenu` | 메뉴 펼치기 | button |
| `button.filterAll` | 전체 | button |
| `button.generateDraft` | 초안 생성 | button |
| `button.generateWeeklyReportDraft` | AI 주간 리포트 초안 생성 | button |
| `button.goBack` | 뒤로 가기 | button |
| `button.help` | 도움말 | button |
| `button.hide` | 숨기기 | button |
| `button.hidePassword` | 비밀번호 숨기기 | button |
| `button.hintActivateParentFirst` | 상위 부서를 먼저 활성화해야 합니다. | button |
| `button.hintAddScheduleAtTime` | 클릭하면 이 시간에 일정을 추가합니다 | button |
| `button.hintAddScheduleOnDate` | 클릭하면 이 날짜로 일정을 추가합니다 | button |
| `button.hintAllowToolbarMove` | 툴바 이동을 허용합니다 | button |
| `button.hintCalendarMonth` | 날짜 칸을 클릭하면 그 날짜로 일정이 열립니다. 날짜 숫자를 누르면 일간 보기로 이동합니다. | button |
| `button.hintCalendarTime` | 빈 시간대를 클릭하면 그 날짜·시간으로 일정이 열립니다. 등록된 일정을 누르면 수정할 수 있습니다. | button |
| `button.hintChildDepartmentBlocked` | 비활성 부서에는 하위 부서를 만들 수 없습니다. | button |
| `button.hintClickToChangeStatus` | 클릭하면 상태가 변경됩니다 | button |
| `button.hintCreateProjectUserOnly` | 프로젝트는 일반 사용자만 만들 수 있습니다 | button |
| `button.hintGoBack` | 이전 화면으로 | button |
| `button.hintPinToolbar` | 툴바를 상단에 다시 고정합니다 | button |
| `button.hintResetToolbar` | 툴바 위치를 기본 위치로 되돌립니다 | button |
| `button.hintSetWipLimit` | 컬럼별 WIP 한도 설정 | button |
| `button.hintWipPmOnly` | PM만 WIP 한도를 설정할 수 있습니다 | button |
| `button.history` | 이력 | button |
| `button.indent` | 하위작업으로 들여쓰기 | button |
| `button.inUse` | 사용 중 | button |
| `button.invite` | 초대 | button |
| `button.keepWriting` | 계속 작성 | button |
| `button.later` | 나중에 | button |
| `button.leave` | 나가기 | button |
| `button.logout` | 로그아웃 | button |
| `button.markAllRead` | 모두 읽음 | button |
| `button.markDone` | 완료 처리 | button |
| `button.meetingList` | 회의 목록 | button |
| `button.moveDown` | 아래로 이동 | button |
| `button.moveUp` | 위로 이동 | button |
| `button.newChat` | 새 대화 | button |
| `button.nextPeriod` | 다음 기간 | button |
| `button.nextStep` | 다음 단계 | button |
| `button.no` | 아니요 | button |
| `button.noApprover` | 승인자 미지정 | button |
| `button.noCreatePermission` | 생성 권한 없음 | button |
| `button.notifications` | 알림 | button |
| `button.openChatHistory` | 대화 기록 열기 | button |
| `button.openDayView` | 이 날짜의 일간 보기 | button |
| `button.openIssues` | 이슈·결함 화면 | button |
| `button.openMenu` | 메뉴 열기 | button |
| `button.openSourceJournal` | 원본 일지로 이동 | button |
| `button.outdent` | 상위 레벨로 내어쓰기 | button |
| `button.periodDay` | 일 | button |
| `button.periodWeek` | 주 | button |
| `button.pinTop` | 상단 고정 | button |
| `button.previous` | 이전 | button |
| `button.previousPeriod` | 이전 기간 | button |
| `button.register` | 등록 | button |
| `button.reject` | 거절 | button |
| `button.release` | 해제 | button |
| `button.reopen` | 재오픈 | button |
| `button.required` | 필수 | button |
| `button.reset` | 초기화 | button |
| `button.resetDemoData` | 데모 데이터 초기화 | button |
| `button.resetFilters` | 필터 초기화 | button |
| `button.resetPassword` | 비밀번호 초기화 | button |
| `button.resetPosition` | 위치 초기화 | button |
| `button.resetting` | 초기화 중 | button |
| `button.retry` | 다시 시도 | button |
| `button.retryShort` | 재시도 | button |
| `button.returnForRevision` | 반려 | button |
| `button.revert` | 되돌리기 | button |
| `button.review` | 검수 | button |
| `button.save` | 저장 | button |
| `button.saveChanges` | 수정 저장 | button |
| `button.saveJournal` | 업무일지 저장 | button |
| `button.saveReview` | 검수 저장 | button |
| `button.saveSchedule` | 일정 저장 | button |
| `button.saveSettings` | 설정 저장 | button |
| `button.saveToKb` | KB로 저장 | button |
| `button.selectAll` | 전체 선택 | button |
| `button.sendQuestion` | 질문 전송 | button |
| `button.setPredecessors` | 선행작업 설정 | button |
| `button.setWipLimit` | WIP 한도 설정 | button |
| `button.show` | 표시 | button |
| `button.showPassword` | 비밀번호 표시 | button |
| `button.signIn` | 로그인 | button |
| `button.signingIn` | 로그인 중 | button |
| `button.signingOut` | 로그아웃 중 | button |
| `button.submit` | 제출 | button |
| `button.submitNewVersion` | 새 버전 제출 | button |
| `button.switch` | 전환 | button |
| `button.switchDemoAccount` | 데모 계정 전환 | button |
| `button.taskDetail` | 업무 상세 | button |
| `button.thisDay` | 이번 날 | button |
| `button.thisWeek` | 이번 주 | button |
| `button.today` | 오늘 | button |
| `button.undoDone` | 완료 취소 | button |
| `button.unlockDemoAccount` | 데모 계정 잠금 해제 | button |
| `button.unmarkApproval` | 승인 표시 해제 | button |
| `button.unpin` | 고정 해제 | button |
| `button.viewNotifications` | 알림 보기 | button |
| `button.work` | 작업 | button |
| `button.writeDoc` | 문서 작성 | button |
| `button.writeJournal` | 업무일지 작성 | button |
| `button.writePost` | 글쓰기 | button |
| `button.yes` | 네 | button |
| `common.ui.brandLogo` | CoreviaAI 로고 | common.ui |
| `common.ui.comingSoonBadge` | 준비 중 | common.ui |
| `common.ui.comingSoonNotice` | 이 화면은 아직 구현되지 않았습니다. 실제 동작 없이 완료 메시지를 표시하지 않으며, 다음 단계에서 데이터와 함께 제공될 예정입니다. | common.ui |
| `common.ui.countUnit` | 건 | common.ui |
| `common.ui.demoPermissionNote` | 데모 UI 권한 시뮬레이션입니다. 실제 서버 접근 제어가 아닙니다. | common.ui |
| `common.ui.demoPrototypeFooter` | CoreviaAI 데모 프로토타입 | common.ui |
| `common.ui.goToMyWork` | 내 업무로 이동 | common.ui |
| `common.ui.goToProjects` | 프로젝트 목록 | common.ui |
| `common.ui.goToProjectsBack` | 프로젝트 목록으로 | common.ui |
| `common.ui.infoTipLabel` | 설명 보기 | common.ui |
| `common.ui.itemUnit` | 개 | common.ui |
| `common.ui.noAccessDescription` | 현재 계정에는 이 화면을 볼 수 있는 권한이 없습니다. 필요한 경우 프로젝트 PM 또는 시스템 관리자에게 권한을 요청해 주세요. | common.ui |
| `common.ui.noAccessTitle` | 이 화면에 접근할 권한이 없습니다 | common.ui |
| `common.ui.notFoundPathPrefix` | 요청한 경로 | common.ui |
| `common.ui.notFoundPathSuffix` | 가 존재하지 않습니다. | common.ui |
| `common.ui.notFoundTitle` | 페이지를 찾을 수 없습니다 | common.ui |
| `common.ui.personUnit` | 명 | common.ui |
| `common.ui.requiredRolePrefix` | 필요한 권한 | common.ui |
| `common.ui.riskScore` | 리스크 점수: {score}점 | common.ui |
| `common.ui.riskScoreUnknown` | 리스크 점수 정보 없음 | common.ui |
| `common.ui.scoreUnit` | {score}점 | common.ui |
| `common.ui.unassigned` | 미지정 | common.ui |
| `i18n.language.changeFailed` | 언어를 바꾸지 못했습니다. 잠시 후 다시 시도해 주세요. | i18n.language |
| `i18n.language.changing` | 언어를 바꾸는 중 | i18n.language |
| `i18n.language.label` | 표시 언어 | i18n.language |
| `i18n.language.savedButNotApplied` | 언어 설정은 저장했지만 문구를 불러오지 못했습니다. 새로고침하면 적용됩니다. | i18n.language |
| `layout.topbar.currentStage` | 현재 단계 {stage} | layout.topbar |
| `layout.topbar.demoSessionNote` | 데모 세션입니다. 실제 인증·세션 관리는 연결되어 있지 않습니다. | layout.topbar |
| `layout.topbar.progress` | 진척률 | layout.topbar |
| `layout.topbar.unsavedBody` | 저장하지 않은 입력이 있을 때만 이 확인을 표시합니다. 입력을 마무리하려면 계속 작성하기를 선택해 주세요. | layout.topbar |
| `layout.topbar.unsavedDescription` | 이 화면에서 나가면 작성 중인 내용은 저장되지 않습니다. | layout.topbar |
| `layout.topbar.unsavedTitle` | 저장하지 않은 내용이 있습니다 | layout.topbar |
| `meeting.form.actionAssignee` | 담당자 | meeting.form |
| `meeting.form.actionContent` | 할 일 | meeting.form |
| `meeting.form.actionDue` | 기한 | meeting.form |
| `meeting.form.actionItems` | 액션아이템 | meeting.form |
| `meeting.form.actionRequired` | 액션아이템의 할 일을 입력하거나 그 줄을 삭제해 주세요. | meeting.form |
| `meeting.form.actionTooLong` | 액션아이템은 하나에 1,000자 이하로 입력해 주세요. | meeting.form |
| `meeting.form.addAction` | 액션아이템 추가 | meeting.form |
| `meeting.form.cancel` | 취소 | meeting.form |
| `meeting.form.content` | 내용 (선택) | meeting.form |
| `meeting.form.contentTooLong` | 내용은 50,000자 이하로 입력해 주세요. | meeting.form |
| `meeting.form.createTitle` | 회의록 작성 | meeting.form |
| `meeting.form.date` | 회의 날짜 | meeting.form |
| `meeting.form.dateRequired` | 회의 날짜와 시각을 입력해 주세요. | meeting.form |
| `meeting.form.decisions` | 결정사항 (한 줄에 하나) | meeting.form |
| `meeting.form.decisionTooLong` | 결정사항은 하나에 1,000자 이하로 입력해 주세요. | meeting.form |
| `meeting.form.duplicateAttendee` | 같은 참석자를 두 번 넣을 수 없습니다. | meeting.form |
| `meeting.form.editTitle` | 회의록 수정 | meeting.form |
| `meeting.form.externals` | 외부 참석자 (선택, 한 줄에 한 명) | meeting.form |
| `meeting.form.externalTooLong` | 외부 참석자 이름은 50자 이하로 입력해 주세요. | meeting.form |
| `meeting.form.members` | 참석자 (프로젝트 멤버) | meeting.form |
| `meeting.form.removeAction` | 삭제 | meeting.form |
| `meeting.form.save` | 저장 | meeting.form |
| `meeting.form.saving` | 저장 중 | meeting.form |
| `meeting.form.time` | 시각 | meeting.form |
| `meeting.form.title` | 제목 | meeting.form |
| `meeting.form.titleRequired` | 제목을 입력해 주세요. | meeting.form |
| `meeting.form.titleTooLong` | 제목은 200자 이하로 입력해 주세요. | meeting.form |
| `meeting.form.tooMany` | 참석자·결정사항·액션아이템은 각각 100개까지 넣을 수 있습니다. | meeting.form |
| `meeting.server.actionItems` | 액션아이템 | meeting.server |
| `meeting.server.add` | 회의록 작성 | meeting.server |
| `meeting.server.aiSource` | AI | meeting.server |
| `meeting.server.attendees` | 참석자 | meeting.server |
| `meeting.server.author` | 작성자 | meeting.server |
| `meeting.server.clearSearch` | 검색 지우기 | meeting.server |
| `meeting.server.concurrent` | 다른 사용자가 먼저 수정했습니다. 최신 내용을 다시 불러왔으니 확인한 뒤 다시 수정해 주세요. | meeting.server |
| `meeting.server.confirmDelete` | '{name}' 회의록을 삭제할까요? 참석자·결정사항·액션아이템도 함께 삭제됩니다. | meeting.server |
| `meeting.server.content` | 내용 | meeting.server |
| `meeting.server.converted` | 업무 전환 | meeting.server |
| `meeting.server.createConflict` | 같은 등록 요청이 이미 처리되었거나 충돌했습니다. 목록을 새로 불러왔으니 확인한 뒤 다시 작성해 주세요. | meeting.server |
| `meeting.server.created` | '{name}' 회의록을 저장했습니다. | meeting.server |
| `meeting.server.decisions` | 결정사항 | meeting.server |
| `meeting.server.delete` | 삭제 | meeting.server |
| `meeting.server.deleted` | '{name}' 회의록을 삭제했습니다. | meeting.server |
| `meeting.server.description` | 회의 일시·참석자·내용·결정사항·액션아이템을 기록합니다. | meeting.server |
| `meeting.server.detailFailed` | 회의록 내용을 불러오지 못했습니다. | meeting.server |
| `meeting.server.dismissed` | 제외 | meeting.server |
| `meeting.server.due` | 기한 {date} | meeting.server |
| `meeting.server.edit` | 수정 | meeting.server |
| `meeting.server.editorOnly` | 수정·삭제는 작성자 또는 PM만 할 수 있습니다. | meeting.server |
| `meeting.server.empty` | 회의록이 없습니다. | meeting.server |
| `meeting.server.emptySearch` | 검색 결과가 없습니다. | meeting.server |
| `meeting.server.external` | 외부 | meeting.server |
| `meeting.server.forbidden` | 이 회의록을 작성·수정할 권한이 없습니다. 새로 불러왔습니다. | meeting.server |
| `meeting.server.heldAt` | 회의 일시 | meeting.server |
| `meeting.server.invalid` | 입력값을 확인해 주세요. | meeting.server |
| `meeting.server.loadFailed` | 회의록을 불러오지 못했습니다. | meeting.server |
| `meeting.server.loading` | 회의록을 불러오는 중입니다 | meeting.server |
| `meeting.server.memberOnly` | 회의록 작성은 프로젝트 멤버만 할 수 있습니다. | meeting.server |
| `meeting.server.more` | 더 보기 | meeting.server |
| `meeting.server.network` | 서버에 연결하지 못했습니다. 네트워크를 확인한 뒤 다시 시도해 주세요. | meeting.server |
| `meeting.server.none` | 없음 | meeting.server |
| `meeting.server.notFound` | 회의록을 찾을 수 없습니다. 삭제되었을 수 있어 목록을 새로 불러왔습니다. | meeting.server |
| `meeting.server.protected` | AI 정리 항목이나 업무로 전환된 액션아이템이 있어 이 화면에서 수정·삭제할 수 없습니다. | meeting.server |
| `meeting.server.retry` | 다시 시도 | meeting.server |
| `meeting.server.scopeNote` | 산출물·게시판·파일은 아직 서버 API가 없어 연결하지 않았습니다. | meeting.server |
| `meeting.server.search` | 검색 | meeting.server |
| `meeting.server.searchPlaceholder` | 제목·내용·결정사항·액션아이템 검색 | meeting.server |
| `meeting.server.select` | 회의록을 선택하세요. | meeting.server |
| `meeting.server.serverError` | 서버 오류로 처리하지 못했습니다. 잠시 후 다시 시도해 주세요. | meeting.server |
| `meeting.server.timeout` | 응답이 늦어 요청을 멈췄습니다. 같은 내용으로 다시 저장하면 중복 없이 처리됩니다. | meeting.server |
| `meeting.server.title` | 회의록 | meeting.server |
| `meeting.server.unassigned` | 미배정 | meeting.server |
| `meeting.server.unavailable` | 이 프로젝트에서는 회의록을 사용할 수 없습니다. 기능 구성에서 회의록을 끈 프로젝트이거나 볼 수 없는 프로젝트입니다. | meeting.server |
| `meeting.server.unknown` | 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요. | meeting.server |
| `meeting.server.updated` | '{name}' 회의록을 수정했습니다. | meeting.server |
| `menu.admin` | 시스템 관리 | menu |
| `menu.admin.adminOnly` | 시스템 관리자 | menu.admin |
| `menu.admin.audit` | 보안 · 접속 로그 | menu.admin |
| `menu.admin.codes` | 공통 코드 | menu.admin |
| `menu.admin.departments` | 부서 | menu.admin |
| `menu.admin.errorLogs` | 에러 로그 | menu.admin |
| `menu.admin.hintAudit` | 읽기 전용 감사 로그 | menu.admin |
| `menu.admin.hintCodes` | 정렬 순서 | menu.admin |
| `menu.admin.hintDemoData` | 데모 데이터 | menu.admin |
| `menu.admin.hintDepartments` | 조직 트리 | menu.admin |
| `menu.admin.hintErrorLogs` | 미처리 예외 조회 | menu.admin |
| `menu.admin.hintLabels` | 문구·번역 | menu.admin |
| `menu.admin.hintPmReassign` | 활성 PM 없는 프로젝트 | menu.admin |
| `menu.admin.hintRisk` | 임계값·가중치·이력 | menu.admin |
| `menu.admin.hintUsers` | 계정·부서·역할 | menu.admin |
| `menu.admin.labels` | 다국어 라벨 | menu.admin |
| `menu.admin.nav` | 시스템 관리 | menu.admin |
| `menu.admin.pmReassign` | PM 재지정 | menu.admin |
| `menu.admin.risk` | AI · 리스크 설정 | menu.admin |
| `menu.admin.users` | 사용자 | menu.admin |
| `menu.admin.usersAndOrg` | 사용자 · 조직 | menu.admin |
| `menu.demoEnvironment` | 데모 환경 | menu |
| `menu.demoSettings` | 데모 설정 | menu |
| `menu.hintResizeMenu` | 드래그해서 메뉴 폭 조절 | menu |
| `menu.home` | CoreviaAI 홈 | menu |
| `menu.knowledge` | 지식 허브 | menu |
| `menu.knowledge.docs` | 지식 문서 | menu.knowledge |
| `menu.knowledge.glossary` | 용어집 | menu.knowledge |
| `menu.knowledge.qa` | AI 질문 | menu.knowledge |
| `menu.knowledge.views` | 지식 허브 보기 | menu.knowledge |
| `menu.mainNav` | 주요 메뉴 | menu |
| `menu.missing.all` | 전체 | menu.missing |
| `menu.missing.meeting` | 회의 액션 미등록 | menu.missing |
| `menu.missing.overdue` | 마감 경과 | menu.missing |
| `menu.missing.stale` | 진척 기록 없음 | menu.missing |
| `menu.missing.unassigned` | 담당자·기한 미지정 | menu.missing |
| `menu.myWork` | 내 업무 | menu |
| `menu.mywork.approval` | 승인 요청 | menu.mywork |
| `menu.mywork.calendar` | 캘린더 | menu.mywork |
| `menu.mywork.journal` | 업무일지 | menu.mywork |
| `menu.mywork.todo` | TODO | menu.mywork |
| `menu.mywork.views` | 내 업무 보기 | menu.mywork |
| `menu.portfolio` | 전체 현황 | menu |
| `menu.project.board` | 게시판 | menu.project |
| `menu.project.collab` | 협업 문서 | menu.project |
| `menu.project.deliverables` | 산출물·검수 | menu.project |
| `menu.project.gantt` | 간트 | menu.project |
| `menu.project.issues` | 이슈·결함 | menu.project |
| `menu.project.kanban` | 칸반 | menu.project |
| `menu.project.meeting` | 회의록 | menu.project |
| `menu.project.missing` | 누락 의심 업무 | menu.project |
| `menu.project.nav` | 프로젝트 메뉴 | menu.project |
| `menu.project.overview` | 개요 | menu.project |
| `menu.project.report` | 주간 리포트 | menu.project |
| `menu.project.risk` | AI 리스크 | menu.project |
| `menu.project.settings` | 멤버·설정 | menu.project |
| `menu.project.wbs` | WBS | menu.project |
| `menu.project.wbsDraft` | AI WBS 초안 | menu.project |
| `menu.project.workPlan` | 업무 계획 | menu.project |
| `menu.project.workPlanViews` | 업무 보기 전환 | menu.project |
| `menu.projectCreate` | 프로젝트 생성 | menu |
| `menu.projects` | 프로젝트 | menu |
| `menu.resizeMenu` | 메뉴 폭 조절 | menu |
| `menu.view.day` | 일 | menu.view |
| `menu.view.month` | 월 | menu.view |
| `menu.view.week` | 주 | menu.view |
| `mywork.schedule.add` | 일정 추가 | mywork.schedule |
| `mywork.schedule.addTitle` | 개인 일정 추가 | mywork.schedule |
| `mywork.schedule.allDay` | 종일 | mywork.schedule |
| `mywork.schedule.allDayBadge` | 종일 | mywork.schedule |
| `mywork.schedule.cancel` | 취소 | mywork.schedule |
| `mywork.schedule.concurrent` | 다른 요청과 겹쳐 처리하지 못했습니다. 새로 불러왔으니 다시 시도해 주세요. | mywork.schedule |
| `mywork.schedule.created` | '{name}' 일정을 추가했습니다. | mywork.schedule |
| `mywork.schedule.dateRequired` | 시작일과 종료일을 입력해 주세요. | mywork.schedule |
| `mywork.schedule.dayEmpty` | 이 날짜에 일정이나 담당 업무가 없습니다. | mywork.schedule |
| `mywork.schedule.dayTitle` | {date} 일정 | mywork.schedule |
| `mywork.schedule.edit` | 수정 | mywork.schedule |
| `mywork.schedule.editTitle` | 개인 일정 수정 | mywork.schedule |
| `mywork.schedule.endDate` | 종료일 | mywork.schedule |
| `mywork.schedule.endTime` | 종료 시각 | mywork.schedule |
| `mywork.schedule.invalid` | 입력값을 확인해 주세요. | mywork.schedule |
| `mywork.schedule.loadFailed` | 일정을 불러오지 못했습니다. | mywork.schedule |
| `mywork.schedule.loading` | 일정을 불러오는 중입니다 | mywork.schedule |
| `mywork.schedule.memo` | 메모 (선택) | mywork.schedule |
| `mywork.schedule.memoTooLong` | 메모는 2000자 이하로 입력해 주세요. | mywork.schedule |
| `mywork.schedule.monthTitle` | {year}년 {month}월 | mywork.schedule |
| `mywork.schedule.more` | +{count} | mywork.schedule |
| `mywork.schedule.network` | 서버에 연결하지 못했습니다. 네트워크를 확인한 뒤 다시 시도해 주세요. | mywork.schedule |
| `mywork.schedule.nextMonth` | 다음 달 | mywork.schedule |
| `mywork.schedule.noDelete` | 개인 일정 삭제는 아직 지원하지 않습니다(정책 보류). | mywork.schedule |
| `mywork.schedule.notFound` | 일정을 찾을 수 없습니다. 목록을 새로 불러왔습니다. | mywork.schedule |
| `mywork.schedule.openProject` | 프로젝트에서 보기 | mywork.schedule |
| `mywork.schedule.periodInvalid` | 종료는 시작보다 뒤여야 합니다. | mywork.schedule |
| `mywork.schedule.personal` | 개인 일정 | mywork.schedule |
| `mywork.schedule.prevMonth` | 이전 달 | mywork.schedule |
| `mywork.schedule.retry` | 다시 시도 | mywork.schedule |
| `mywork.schedule.save` | 저장 | mywork.schedule |
| `mywork.schedule.saving` | 저장 중 | mywork.schedule |
| `mywork.schedule.selectDay` | {date} 선택 | mywork.schedule |
| `mywork.schedule.serverError` | 서버 오류로 처리하지 못했습니다. 잠시 후 다시 시도해 주세요. | mywork.schedule |
| `mywork.schedule.startDate` | 시작일 | mywork.schedule |
| `mywork.schedule.startTime` | 시작 시각 | mywork.schedule |
| `mywork.schedule.task` | 담당 업무 | mywork.schedule |
| `mywork.schedule.thisMonth` | 이번 달 | mywork.schedule |
| `mywork.schedule.timeout` | 응답이 늦어 요청을 멈췄습니다. 잠시 후 다시 시도해 주세요. | mywork.schedule |
| `mywork.schedule.timeRequired` | 시작 시각과 종료 시각을 입력해 주세요. | mywork.schedule |
| `mywork.schedule.titleControl` | 제목에는 줄바꿈·탭 같은 제어 문자를 쓸 수 없습니다. | mywork.schedule |
| `mywork.schedule.titleLabel` | 제목 | mywork.schedule |
| `mywork.schedule.titleRequired` | 제목을 입력해 주세요. | mywork.schedule |
| `mywork.schedule.titleTooLong` | 제목은 200자 이하로 입력해 주세요. | mywork.schedule |
| `mywork.schedule.truncated` | 항목이 많아 일부만 표시합니다. | mywork.schedule |
| `mywork.schedule.unknown` | 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요. | mywork.schedule |
| `mywork.schedule.updated` | '{name}' 일정을 수정했습니다. | mywork.schedule |
| `mywork.todo.add` | 할 일 추가 | mywork.todo |
| `mywork.todo.addTitle` | 개인 할 일 추가 | mywork.todo |
| `mywork.todo.approvalPendingDesc` | 승인 요청 목록은 후속 서버 연동 후 표시됩니다. | mywork.todo |
| `mywork.todo.approvalPendingTitle` | 승인 요청 연동 준비 중 | mywork.todo |
| `mywork.todo.cancel` | 취소 | mywork.todo |
| `mywork.todo.completed` | '{name}'을(를) 완료했습니다. | mywork.todo |
| `mywork.todo.concurrent` | 다른 요청과 겹쳐 처리하지 못했습니다. 목록을 새로 불러왔으니 다시 시도해 주세요. | mywork.todo |
| `mywork.todo.confirmDelete` | '{name}'을(를) 삭제할까요? | mywork.todo |
| `mywork.todo.createConflict` | 같은 등록 요청이 이미 처리되었거나 충돌했습니다. 목록을 새로 불러왔으니 확인한 뒤 다시 추가해 주세요. | mywork.todo |
| `mywork.todo.created` | '{name}'을(를) 추가했습니다. | mywork.todo |
| `mywork.todo.delete` | 삭제 | mywork.todo |
| `mywork.todo.deleted` | '{name}'을(를) 삭제했습니다. | mywork.todo |
| `mywork.todo.dueLabel` | 마감일 (선택) | mywork.todo |
| `mywork.todo.dueToday` | 오늘 마감 | mywork.todo |
| `mywork.todo.edit` | 수정 | mywork.todo |
| `mywork.todo.editTitle` | 할 일 수정 | mywork.todo |
| `mywork.todo.empty` | 표시할 항목이 없습니다. | mywork.todo |
| `mywork.todo.emptyHint` | 개인 할 일을 추가하거나, 프로젝트에서 업무를 배정받으면 여기에 표시됩니다. | mywork.todo |
| `mywork.todo.invalid` | 입력값을 확인해 주세요. | mywork.todo |
| `mywork.todo.kpiDone` | 완료한 할 일 | mywork.todo |
| `mywork.todo.kpiDoneHint` | 완료 처리한 개인 할 일입니다. 완료된 배정 업무는 목록에 나오지 않습니다. | mywork.todo |
| `mywork.todo.kpiLate` | 지연 | mywork.todo |
| `mywork.todo.kpiLateHint` | 마감일이 지난 미완료 항목입니다. 선택하면 해당 항목만 표시합니다. | mywork.todo |
| `mywork.todo.kpiSoon` | 마감 임박 | mywork.todo |
| `mywork.todo.kpiSoonHint` | 1~3일 안에 마감되는 항목입니다. 선택하면 해당 항목만 표시합니다. | mywork.todo |
| `mywork.todo.kpiToday` | 오늘 할 일 | mywork.todo |
| `mywork.todo.kpiTodayHint` | 오늘 마감인 할 일과 배정 업무입니다. 선택하면 해당 항목만 표시합니다. | mywork.todo |
| `mywork.todo.loadFailed` | 할 일을 불러오지 못했습니다. | mywork.todo |
| `mywork.todo.loading` | 할 일을 불러오는 중입니다 | mywork.todo |
| `mywork.todo.markDone` | 완료로 표시 | mywork.todo |
| `mywork.todo.markOpen` | 완료 해제 | mywork.todo |
| `mywork.todo.network` | 서버에 연결하지 못했습니다. 네트워크를 확인한 뒤 다시 시도해 주세요. | mywork.todo |
| `mywork.todo.noDue` | 마감 없음 | mywork.todo |
| `mywork.todo.notFound` | 할 일을 찾을 수 없습니다. 이미 삭제되었을 수 있어 목록을 새로 불러왔습니다. | mywork.todo |
| `mywork.todo.openProject` | 프로젝트에서 보기 | mywork.todo |
| `mywork.todo.overdue` | 지연 | mywork.todo |
| `mywork.todo.refreshNote` | 1분마다 자동으로 새로 고칩니다. | mywork.todo |
| `mywork.todo.reopened` | '{name}'의 완료를 해제했습니다. | mywork.todo |
| `mywork.todo.retry` | 다시 시도 | mywork.todo |
| `mywork.todo.save` | 저장 | mywork.todo |
| `mywork.todo.saving` | 저장 중 | mywork.todo |
| `mywork.todo.serverError` | 서버 오류로 처리하지 못했습니다. 잠시 후 다시 시도해 주세요. | mywork.todo |
| `mywork.todo.sourcePersonal` | 개인 | mywork.todo |
| `mywork.todo.sourceTask` | 배정 업무 | mywork.todo |
| `mywork.todo.taskNote` | 배정 업무의 상태는 프로젝트의 칸반에서 바꿉니다. | mywork.todo |
| `mywork.todo.timeout` | 응답이 늦어 요청을 멈췄습니다. 같은 내용으로 다시 저장하면 중복 없이 처리됩니다. | mywork.todo |
| `mywork.todo.titleControl` | 할 일에는 줄바꿈·탭 같은 제어 문자를 쓸 수 없습니다. | mywork.todo |
| `mywork.todo.titleLabel` | 할 일 | mywork.todo |
| `mywork.todo.titlePlaceholder` | 예) 주간 보고서 작성 | mywork.todo |
| `mywork.todo.titleRequired` | 할 일을 입력해 주세요. | mywork.todo |
| `mywork.todo.titleTooLong` | 할 일은 200자 이하로 입력해 주세요. | mywork.todo |
| `mywork.todo.truncated` | 항목이 많아 앞의 일부만 표시합니다. | mywork.todo |
| `mywork.todo.unknown` | 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요. | mywork.todo |
| `mywork.todo.updated` | '{name}'을(를) 수정했습니다. | mywork.todo |
| `mywork.worklog.completed` | 완료 | mywork.worklog |
| `mywork.worklog.completedLocked` | 이미 완료된 업무라 완료를 해제할 수 없습니다. | mywork.worklog |
| `mywork.worklog.completedNeeds100` | 완료하려면 진척률이 100이어야 합니다. | mywork.worklog |
| `mywork.worklog.concurrent` | 업무가 그 사이 바뀌었습니다. 최신 값을 다시 불러왔으니 확인한 뒤 다시 저장해 주세요. | mywork.worklog |
| `mywork.worklog.confirmLeave` | 저장하지 않은 노트가 있습니다. 저장하지 않고 다른 날짜로 이동할까요? | mywork.worklog |
| `mywork.worklog.contentTooLong` | 내용은 10,000자 이하로 입력해 주세요. | mywork.worklog |
| `mywork.worklog.counter` | {count} / 10,000자 | mywork.worklog |
| `mywork.worklog.date` | 날짜 | mywork.worklog |
| `mywork.worklog.loadFailed` | 업무일지를 불러오지 못했습니다. | mywork.worklog |
| `mywork.worklog.loading` | 업무일지를 불러오는 중입니다 | mywork.worklog |
| `mywork.worklog.minutesInvalid` | 소요 시간은 0~1440분 사이의 정수로 입력해 주세요. | mywork.worklog |
| `mywork.worklog.nextDay` | 다음 날 | mywork.worklog |
| `mywork.worklog.notAssignee` | 본인이 담당한 업무만 기록할 수 있습니다. 담당이 바뀌었을 수 있어 새로 불러왔습니다. | mywork.worklog |
| `mywork.worklog.noteHint` | Markdown으로 기록할 수 있습니다. 본인만 볼 수 있습니다. | mywork.worklog |
| `mywork.worklog.notePlaceholder` | 오늘 한 일, 진행 상황, 문제 해결 과정을 기록하세요. | mywork.worklog |
| `mywork.worklog.noteSaved` | {date} 업무 노트를 저장했습니다. | mywork.worklog |
| `mywork.worklog.noteTitle` | {date} 업무 노트 | mywork.worklog |
| `mywork.worklog.openProject` | 프로젝트에서 보기 | mywork.worklog |
| `mywork.worklog.prevDay` | 전날 | mywork.worklog |
| `mywork.worklog.progress` | 진척률(%) | mywork.worklog |
| `mywork.worklog.progressInvalid` | 진척률은 0~100 사이의 정수로 입력해 주세요. | mywork.worklog |
| `mywork.worklog.recentEmpty` | 최근 30일 동안 기록한 날이 없습니다. | mywork.worklog |
| `mywork.worklog.recentTitle` | 최근 기록한 날 | mywork.worklog |
| `mywork.worklog.retry` | 다시 시도 | mywork.worklog |
| `mywork.worklog.saveNote` | 노트 저장 | mywork.worklog |
| `mywork.worklog.saveTask` | 기록 저장 | mywork.worklog |
| `mywork.worklog.saving` | 저장 중 | mywork.worklog |
| `mywork.worklog.spentMinutes` | 소요 시간(분) | mywork.worklog |
| `mywork.worklog.taskContent` | 업무 메모 (선택) | mywork.worklog |
| `mywork.worklog.taskNotFound` | 업무를 찾을 수 없습니다. 삭제되었거나 기능이 꺼졌을 수 있어 새로 불러왔습니다. | mywork.worklog |
| `mywork.worklog.taskSaved` | '{name}' 기록을 저장했습니다. | mywork.worklog |
| `mywork.worklog.tasksEmpty` | 이날 기간에 걸친 담당 업무가 없습니다. | mywork.worklog |
| `mywork.worklog.tasksHint` | 진척률·소요 시간을 기록하면 업무와 상위 작업 진척률에 바로 반영됩니다. 연결 기록은 그 프로젝트 PM도 볼 수 있습니다. | mywork.worklog |
| `mywork.worklog.tasksTitle` | 이날의 담당 업무 | mywork.worklog |
| `mywork.worklog.today` | 오늘 | mywork.worklog |
| `mywork.worklog.totalMinutes` | 누적 {minutes}분 | mywork.worklog |
| `mywork.worklog.unsaved` | 저장하지 않은 변경이 있습니다. | mywork.worklog |
| `notification.assigned` | '{taskName}' 업무가 배정되었습니다. | notification |
| `notification.demoFooter` | 데모 알림 · 예약 실행·실시간 발송 미연결 | notification |
| `notification.dueSoon` | '{taskName}' 업무가 내일({endDate}) 마감입니다. | notification |
| `notification.empty` | 알림이 없습니다. | notification |
| `notification.emptyHint` | 업무 배정·검수 요청이 생기면 이곳에 표시됩니다. | notification |
| `notification.evidence` | 근거 · {text} | notification |
| `notification.loadFailed` | 알림을 불러오지 못했습니다. | notification |
| `notification.loading` | 알림을 불러오는 중입니다. | notification |
| `notification.meetingSavedToKb` | 회의록을 지식 허브에 저장했습니다. 원본 링크가 유지됩니다. | notification |
| `notification.offline` | 네트워크 연결이 끊겼습니다. 일시적인 통신 오류는 로그아웃 사유가 아니며, 연결이 복구되면 자동으로 다시 확인합니다. | notification |
| `notification.server.fallbackApprovalRequested` | 승인 요청이 있습니다. | notification.server |
| `notification.server.fallbackAssigned` | 업무가 배정되었습니다. | notification.server |
| `notification.server.fallbackDueSoon` | 내일 마감인 업무가 있습니다. | notification.server |
| `notification.server.fallbackMentioned` | 회원님을 멘션했습니다. | notification.server |
| `notification.server.fallbackMissingTask` | 누락이 의심되는 업무가 있습니다. | notification.server |
| `notification.server.fallbackProjectInactive` | 프로젝트가 비활성 상태입니다. | notification.server |
| `notification.server.fallbackRiskUp` | 프로젝트 위험 등급이 올라갔습니다. | notification.server |
| `notification.server.fallbackStatusChanged` | 업무 상태가 바뀌었습니다. | notification.server |
| `notification.server.live` | 실시간 연결됨 | notification.server |
| `notification.server.more` | 더 보기 | notification.server |
| `notification.server.noMarkAll` | 모두 읽음은 서버 API가 없어 항목을 눌러 하나씩 읽음 처리합니다. | notification.server |
| `notification.server.polling` | 1분마다 새로 확인합니다 | notification.server |
| `notification.server.readFailed` | 읽음 처리를 하지 못했습니다. 잠시 후 다시 시도해 주세요. | notification.server |
| `notification.server.unavailable` | 알림 서버가 아직 준비되지 않았습니다. 서버에 알림 기능이 반영된 뒤 화면을 새로 고치면 표시됩니다. | notification.server |
| `notification.statusChanged` | '{taskName}' 업무 상태가 '{status}'(으)로 바뀌었습니다. | notification |
| `notification.title` | 알림 | notification |
| `notification.type.assign` | 배정 | notification.type |
| `notification.type.deliverable` | 검수 | notification.type |
| `notification.type.due` | 마감 | notification.type |
| `notification.type.mention` | 멘션 | notification.type |
| `notification.type.missing` | 누락 | notification.type |
| `notification.type.risk` | 리스크 | notification.type |
| `notification.type.status` | 상태 | notification.type |
| `notification.typeSuffix` | {type} 알림 | notification |
| `notification.unreadCount` | 읽지 않음 {count}건 | notification |
| `notification.unreadNotice` | 확인하지 않은 알림 {count}건이 있습니다 | notification |
| `portfolio.activeFocusChip` | 현재 지표 · {label} | portfolio |
| `portfolio.allGrades` | 등급 전체 | portfolio |
| `portfolio.allStages` | 단계 전체 | portfolio |
| `portfolio.clickMetricHint` | 지표를 누르면 아래 프로젝트 목록에 해당 조건이 적용됩니다. | portfolio |
| `portfolio.colDeadline` | 마감 | portfolio |
| `portfolio.colStageShort` | 단계 | portfolio |
| `portfolio.deptItemCount` | {projects}개 · 미완료 {openTasks}건 | portfolio |
| `portfolio.deptProgressTitle` | 부서별 진척률 | portfolio |
| `portfolio.deptScopeExec` | 소속 범위 부서 단위 집계 | portfolio |
| `portfolio.deptScopePm` | 프로젝트 주관 부서 단위 집계 | portfolio |
| `portfolio.emptyDeptData` | 부서 데이터가 없습니다. | portfolio |
| `portfolio.emptyProjectsDescription` | 조회 범위에 프로젝트가 없습니다. | portfolio |
| `portfolio.emptyProjectsTitle` | 표시할 프로젝트가 없습니다 | portfolio |
| `portfolio.endDateSuffix` | {date} 종료 | portfolio |
| `portfolio.ended` | 종료 | portfolio |
| `portfolio.execPrivacyNote` | 경영진에게는 개인 업무량·개인 지연 이력·개인 순위를 표시하지 않습니다. | portfolio |
| `portfolio.filteredCount` | {total}건 중 {shown}건 표시 | portfolio |
| `portfolio.footerRowHint` | 행을 클릭하면 해당 프로젝트 상세로 이동합니다. | portfolio |
| `portfolio.hintAll` | 조회 범위 기준 | portfolio |
| `portfolio.hintDone` | 이행까지 종료 | portfolio |
| `portfolio.hintLate` | 완료 전 · 마감일 경과 | portfolio |
| `portfolio.hintOngoing` | 완료 전 · 마감일 이내 | portfolio |
| `portfolio.lateBadge` | 지연 | portfolio |
| `portfolio.metricsAria` | 프로젝트 요약 지표 | portfolio |
| `portfolio.noAccessDescription` | 현재 계정에는 전체 현황 열람 권한이 없습니다. 프로젝트 PM 또는 시스템 관리자에게 권한을 요청해 주세요. | portfolio |
| `portfolio.noAccessTitle` | 전체 현황은 PM과 경영진만 볼 수 있습니다 | portfolio |
| `portfolio.noIndividualMetric` | 개인별 지표 없음 | portfolio |
| `portfolio.noMatchDescription` | 상태·단계·부서·PM·등급 필터를 변경해 다시 확인해 주세요. | portfolio |
| `portfolio.noMatchTitle` | 조건에 맞는 프로젝트가 없습니다 | portfolio |
| `portfolio.overdueDays` | {days}일 경과 | portfolio |
| `portfolio.readOnly` | 읽기 전용 | portfolio |
| `portfolio.requiredRole` | 프로젝트 PM 또는 경영진 | portfolio |
| `portfolio.ruleBasedDemoScore` | 규칙 기반 데모 점수 | portfolio |
| `portfolio.scopeExec` | 경영진 · 소속 부서 범위 | portfolio |
| `portfolio.scopePm` | PM · 참여 프로젝트 | portfolio |
| `portfolio.summaryDescription` | 여러 프로젝트의 진척률·마감일·지연·리스크를 한 화면에서 비교합니다. 개인별 성과 비교는 제공하지 않습니다. | portfolio |
| `portfolio.topRiskDescription` | 리스크 점수 기준 내림차순입니다. | portfolio |
| `portfolio.topRiskTitle` | 위험 프로젝트 상위 5개 | portfolio |
| `portfolio.viewSameInProjects` | 프로젝트 목록에서 같은 조건으로 보기 | portfolio |
| `project.create.actorInUse` | 멤버 역할로 쓰는 액터는 해제할 수 없습니다. 그 멤버의 역할을 바꾸거나 제외해 주세요. | project.create |
| `project.create.actorsHint` | PM은 필수 액터라 해제할 수 없습니다. | project.create |
| `project.create.actorsRequired` | PM 액터는 반드시 포함되어야 합니다. | project.create |
| `project.create.actorsSection` | 액터 | project.create |
| `project.create.approverHint` | 초대한 멤버 중 PM이 아닌 한 명을 고르거나 지정하지 않을 수 있습니다. | project.create |
| `project.create.approverLabel` | 게이트 승인자 | project.create |
| `project.create.approverNoMembers` | 멤버를 초대하면 그중에서 승인자를 고를 수 있습니다. | project.create |
| `project.create.approverNone` | 지정 안 함 | project.create |
| `project.create.approverNoPhases` | 단계가 없는 운영 방식이라 게이트 승인자를 지정하지 않습니다. | project.create |
| `project.create.approverNotMember` | 게이트 승인자는 초대한 멤버 중에서만 지정할 수 있습니다. | project.create |
| `project.create.approverSection` | 게이트 승인자 (선택) | project.create |
| `project.create.basicSection` | 기본 정보 | project.create |
| `project.create.blockedTitle` | 다음 필수 항목을 선택할 수 없어 만들 수 없습니다 | project.create |
| `project.create.cancel` | 취소 | project.create |
| `project.create.checkCode` | 중복 확인 | project.create |
| `project.create.checkingCode` | 확인 중 | project.create |
| `project.create.code` | 프로젝트 코드 | project.create |
| `project.create.codeAvailable` | {code}는 사용할 수 있습니다. 최종 판정은 만들 때 한 번 더 합니다. | project.create |
| `project.create.codeFormat` | 프로젝트 코드는 영문·숫자로 시작하는 영문·숫자·하이픈(-) 2~20자로 입력해 주세요. | project.create |
| `project.create.codeHint` | 영문·숫자로 시작하는 영문·숫자·하이픈 2~20자. 대문자로 저장됩니다. | project.create |
| `project.create.codeRequired` | 프로젝트 코드를 입력해 주세요. | project.create |
| `project.create.codeTaken` | {code}는 이미 사용 중입니다. | project.create |
| `project.create.codeTakenGeneric` | 이미 사용 중인 프로젝트 코드입니다. | project.create |
| `project.create.created` | {name} 프로젝트를 만들었습니다. | project.create |
| `project.create.departmentsEmpty` | 고를 수 있는 활성 부서가 없습니다. | project.create |
| `project.create.departmentsFailed` | 부서 목록을 불러오지 못했습니다. 주관 부서 없이 만들거나 다시 시도해 주세요. | project.create |
| `project.create.departmentsFilter` | 부서 이름으로 거르기 | project.create |
| `project.create.departmentsHint` | 여러 부서를 고를 수 있고(최대 20개), 비워 두고 만든 뒤 지정할 수도 있습니다. | project.create |
| `project.create.departmentsLimit` | 주관 부서는 최대 20개까지 지정할 수 있습니다. | project.create |
| `project.create.departmentsLoading` | 부서 목록을 불러오는 중입니다 | project.create |
| `project.create.departmentsNoMatch` | 이름이 맞는 부서가 없습니다. | project.create |
| `project.create.departmentsSection` | 주관 부서 (선택) | project.create |
| `project.create.departmentsSelected` | {count}개 선택 (최대 20개) | project.create |
| `project.create.description` | 입력한 내용으로 서버에 프로젝트를 만듭니다. 만든 사람이 첫 PM이 됩니다. | project.create |
| `project.create.endDate` | 종료일 (선택) | project.create |
| `project.create.featureNeedsPhases` | 단계 필요 | project.create |
| `project.create.featuresHint` | 템플릿 기본 기능이 체크됩니다. 만들기 전에 하나씩 바꿀 수 있고, 산출물·게이트처럼 단계가 필요한 기능은 단계가 있는 운영 방식(폭포수)에서만 고를 수 있습니다. | project.create |
| `project.create.featuresSection` | 기능 구성 | project.create |
| `project.create.featureUnavailable` | 준비 중 | project.create |
| `project.create.forbidden` | 이 계정은 프로젝트를 만들 수 없습니다. 활성 일반 사용자만 만들 수 있습니다. | project.create |
| `project.create.loadingOptions` | 생성 선택지를 불러오는 중입니다 | project.create |
| `project.create.memberRole` | {name} 역할 | project.create |
| `project.create.memberRoleInvalid` | 지정하지 않은 액터를 역할로 가진 멤버가 있습니다. 역할을 다시 골라 주세요. | project.create |
| `project.create.membersEmpty` | 아직 초대한 멤버가 없습니다. | project.create |
| `project.create.membersHint` | 이름으로 찾아 추가합니다. 역할은 위에서 지정한 액터 중 PM을 뺀 것에서 고릅니다. 비워 두고 만든 뒤 초대할 수도 있습니다. | project.create |
| `project.create.membersLimit` | 생성 시 초대할 수 있는 멤버는 최대 100명입니다. | project.create |
| `project.create.membersNeedActor` | PM 외 액터를 하나 이상 지정해야 멤버를 초대할 수 있습니다. | project.create |
| `project.create.membersSearch` | 초대할 사용자 찾기 | project.create |
| `project.create.membersSection` | 멤버 초대 (선택) | project.create |
| `project.create.methodology` | 운영 방식 | project.create |
| `project.create.name` | 프로젝트명 | project.create |
| `project.create.namePlaceholder` | 예) 사내 업무포털 고도화 | project.create |
| `project.create.nameRequired` | 프로젝트명을 입력해 주세요. | project.create |
| `project.create.nameTooLong` | 프로젝트명은 100자 이하로 입력해 주세요. | project.create |
| `project.create.nextStep` | 다음 | project.create |
| `project.create.noAccessDescription` | 프로젝트는 활성 상태의 일반 사용자라면 누구나 만들 수 있습니다. 경영진은 프로젝트를 조회만 하고, 시스템 관리자는 프로젝트 데이터에 접근하지 않습니다. | project.create |
| `project.create.noAccessTitle` | 프로젝트를 만들 수 없는 계정입니다 | project.create |
| `project.create.noDepartmentType` | 부서 유형 — SW 부서 유형이 활성 상태가 아닙니다. | project.create |
| `project.create.noMethodology` | 운영 방식 — 고를 수 있는(활성) 운영 방식이 없습니다. | project.create |
| `project.create.noPmActor` | 액터 — 필수 PM 액터를 고를 수 없습니다. | project.create |
| `project.create.noTemplate` | 템플릿 — 서버가 고를 수 있는 템플릿을 주지 않았습니다. | project.create |
| `project.create.openCreated` | 프로젝트 열기 | project.create |
| `project.create.optionsFailed` | 생성 선택지(템플릿·운영 방식·액터·기능)를 불러오지 못했습니다. 필수 항목이라 이 정보 없이는 만들 수 없습니다. | project.create |
| `project.create.periodHint` | 기간은 비워 두고 만든 뒤 입력할 수 있습니다. | project.create |
| `project.create.periodInvalid` | 시작일은 종료일보다 늦을 수 없습니다. | project.create |
| `project.create.pmSection` | PM | project.create |
| `project.create.pmSelf` | {name} (나) — 만든 사람이 첫 PM이 됩니다. | project.create |
| `project.create.prevStep` | 이전 | project.create |
| `project.create.removeDepartment` | {name} 주관 부서 해제 | project.create |
| `project.create.removeMember` | {name} 멤버 제외 | project.create |
| `project.create.startDate` | 시작일 (선택) | project.create |
| `project.create.stepActors` | 액터 | project.create |
| `project.create.stepApprover` | 게이트 승인자 | project.create |
| `project.create.stepBasic` | 기본 정보 | project.create |
| `project.create.stepFeatures` | 기능 구성 | project.create |
| `project.create.stepMembers` | 멤버 | project.create |
| `project.create.stepsNavAria` | 프로젝트 생성 단계 | project.create |
| `project.create.stepStructure` | 템플릿·운영 방식 | project.create |
| `project.create.structureSection` | 템플릿·운영 방식 | project.create |
| `project.create.submit` | 프로젝트 만들기 | project.create |
| `project.create.submitting` | 만드는 중 | project.create |
| `project.create.template` | 템플릿 | project.create |
| `project.create.title` | 새 프로젝트 | project.create |
| `project.create.unsavedMessage` | 저장하지 않은 프로젝트 정보가 있습니다. | project.create |
| `project.create.visibility` | 공개 범위 | project.create |
| `project.feature.board` | 게시판 | project.feature |
| `project.feature.deliverable` | 산출물·게이트 | project.feature |
| `project.feature.file` | 파일 | project.feature |
| `project.feature.git` | Git 연동 | project.feature |
| `project.feature.issue` | 이슈 | project.feature |
| `project.feature.kanban` | 칸반 | project.feature |
| `project.feature.meeting` | 회의록 | project.feature |
| `project.feature.wbs` | WBS·간트 | project.feature |
| `project.list.allDepartments` | 부서 전체 | project.list |
| `project.list.allPms` | PM 전체 | project.list |
| `project.list.allRisks` | 리스크 전체 | project.list |
| `project.list.allStatuses` | 상태 전체 | project.list |
| `project.list.colDepartment` | 주관 부서 | project.list |
| `project.list.colMethodology` | 운영 방식 | project.list |
| `project.list.colNameCode` | 프로젝트명 · 코드 | project.list |
| `project.list.colPeriod` | 기간 | project.list |
| `project.list.colPm` | PM | project.list |
| `project.list.colProgress` | 진척률 | project.list |
| `project.list.colStage` | 현재 단계 | project.list |
| `project.list.colStatus` | 상태 | project.list |
| `project.list.emptyAdminDescription` | 시스템 관리자 역할로는 프로젝트 데이터에 접근할 수 없습니다. 프로젝트 멤버로 참여한 계정으로 확인해 주세요. | project.list |
| `project.list.emptyTitle` | 볼 수 있는 프로젝트가 없습니다 | project.list |
| `project.list.emptyUserDescription` | 참여 중이거나 공개 범위에 포함된 프로젝트가 여기에 표시됩니다. | project.list |
| `project.list.focusAppliedSuffix` | 지표가 적용되었습니다. | project.list |
| `project.list.footerCount` | 총 {total}건 중 {shown}건 표시 | project.list |
| `project.list.footerRowHint` | 행을 클릭하면 프로젝트 개요로 이동합니다 | project.list |
| `project.list.hintAll` | 진행·완료 포함 | project.list |
| `project.list.hintDone` | 이행까지 종료된 프로젝트 | project.list |
| `project.list.hintLate` | 완료 전 · 마감일 경과 | project.list |
| `project.list.hintOngoing` | 완료 전 · 마감일 이내 | project.list |
| `project.list.noMatchDescription` | 검색어나 필터 조건을 변경해 다시 시도해 주세요. | project.list |
| `project.list.noMatchTitle` | 검색 결과가 없습니다 | project.list |
| `project.list.progressFormulaNote` | 진척률은 WBS 최하위 업무의 예상 작업량 가중 평균입니다 | project.list |
| `project.list.searchPlaceholder` | 프로젝트명, 코드, 부서로 검색 | project.list |
| `project.list.stageUnknownHint` | 상세 화면에서 단계를 불러오면 표시됩니다 | project.list |
| `project.list.visibleCountApi` | 볼 수 있는 프로젝트 {count}건 | project.list |
| `project.list.visibleCountDemo` | 가상 프로젝트 {count}건 | project.list |
| `project.load.loadDetailFailed` | 프로젝트를 불러오지 못했습니다 | project.load |
| `project.load.loading` | 프로젝트를 불러오는 중입니다 | project.load |
| `project.load.loadListFailed` | 프로젝트 목록을 불러오지 못했습니다 | project.load |
| `project.load.notFoundDescription` | 삭제되었거나 공개 범위에서 제외된 프로젝트입니다. 프로젝트 목록에서 다시 확인해 주세요. | project.load |
| `project.load.notFoundTitle` | 프로젝트를 찾을 수 없거나 접근 권한이 없습니다 | project.load |
| `project.load.retry` | 다시 시도 | project.load |
| `project.load.staleDetailBanner` | 최신 프로젝트 정보를 불러오지 못해 마지막으로 받은 내용을 보여주고 있습니다. {message} | project.load |
| `project.member.actor.customerLiaison` | 고객 담당자 | project.member.actor |
| `project.member.actor.deptStaff` | 부서 담당자 | project.member.actor |
| `project.member.alreadyMember` | 이미 프로젝트 멤버인 사용자가 있어 아무도 초대하지 않았습니다. 멤버 목록을 다시 불러왔으니 확인 후 다시 시도해 주세요. | project.member |
| `project.member.approver` | 승인자 | project.member |
| `project.member.approverHint` | 게이트 승인자는 PM이 될 수 없고 멤버에서 제외할 수 없습니다. 승인자를 먼저 변경해 주세요. | project.member |
| `project.member.assignedTasks` | 담당 업무 {count}건 | project.member |
| `project.member.cancel` | 취소 | project.member |
| `project.member.change.invited` | 초대 | project.member.change |
| `project.member.change.pmAdded` | PM 추가 | project.member.change |
| `project.member.change.pmDelegated` | PM 위임(이전 기록) | project.member.change |
| `project.member.change.pmReassigned` | PM 재지정(관리자) | project.member.change |
| `project.member.change.pmReleased` | PM 해제 | project.member.change |
| `project.member.change.removed` | 제외 | project.member.change |
| `project.member.change.roleChanged` | 역할 변경 | project.member.change |
| `project.member.confirmChange` | 변경 | project.member |
| `project.member.confirmPmAddTitle` | PM으로 추가하시겠습니까? | project.member |
| `project.member.confirmPmReleaseTitle` | PM을 해제하시겠습니까? | project.member |
| `project.member.confirmRemove` | 제외 | project.member |
| `project.member.confirmRemoveBody` | {name}을(를) 프로젝트 멤버에서 제외합니다. 제외한 기록은 변경 이력에 남습니다. | project.member |
| `project.member.confirmRemoveTitle` | 멤버에서 제외하시겠습니까? | project.member |
| `project.member.confirmRoleBody` | {name}의 역할을 {before}에서 {after}(으)로 바꿉니다. | project.member |
| `project.member.confirmRoleTitle` | 역할을 변경하시겠습니까? | project.member |
| `project.member.empty` | 아직 멤버가 없습니다 | project.member |
| `project.member.hasTasks` | 담당 중인 업무가 있어 제외할 수 없습니다. 아래 업무를 다른 멤버에게 먼저 재배정해 주세요. | project.member |
| `project.member.historyBy` | 처리: {name} | project.member |
| `project.member.historyDescription` | 초대·역할 변경·PM 추가·해제·재지정·제외를 최신순으로 보여 줍니다. | project.member |
| `project.member.historyEmpty` | 아직 멤버 변경 이력이 없습니다 | project.member |
| `project.member.historyFailed` | 변경 이력을 불러오지 못했습니다 | project.member |
| `project.member.historyLoading` | 변경 이력을 불러오는 중입니다 | project.member |
| `project.member.historyTitle` | 멤버·역할 변경 이력 | project.member |
| `project.member.invited` | {count}명을 초대했습니다. | project.member |
| `project.member.inviteEmpty` | 아직 고른 사용자가 없습니다. | project.member |
| `project.member.inviteHint` | 이름으로 찾아 추가하고 각자 역할을 고른 뒤 한 번에 초대합니다. 역할은 프로젝트에 지정한 액터 중 PM을 뺀 것에서 고릅니다. PM은 초대한 뒤 역할 변경으로 추가합니다. | project.member |
| `project.member.inviteLimit` | 한 번에 최대 {max}명까지 초대할 수 있습니다. | project.member |
| `project.member.inviteNeedActor` | PM 외 액터가 지정되어 있지 않아 초대할 수 없습니다. | project.member |
| `project.member.inviteRejected` | 초대할 수 없는 사용자이거나 고를 수 없는 역할이 있어 아무도 초대하지 않았습니다. | project.member |
| `project.member.inviteRemove` | {name}을(를) 초대 목록에서 빼기 | project.member |
| `project.member.inviteRole` | {name} 초대 역할 | project.member |
| `project.member.inviteSearch` | 초대할 사용자 찾기 | project.member |
| `project.member.inviteSubmit` | {count}명 초대 | project.member |
| `project.member.inviting` | 초대 중 | project.member |
| `project.member.lastPm` | 프로젝트에는 PM이 1명 이상 있어야 해서 마지막 PM은 해제하거나 제외할 수 없습니다. 다른 멤버를 먼저 PM으로 추가해 주세요. | project.member |
| `project.member.lastPmHint` | 마지막 PM이라 역할을 바꾸거나 제외할 수 없습니다. 다른 멤버를 먼저 PM으로 추가해 주세요. | project.member |
| `project.member.loadFailed` | 멤버 목록을 불러오지 못했습니다 | project.member |
| `project.member.loading` | 멤버를 불러오는 중입니다 | project.member |
| `project.member.loadingMore` | 더 불러오는 중 | project.member |
| `project.member.loadMore` | 더 보기 | project.member |
| `project.member.me` | 나 | project.member |
| `project.member.memberGone` | 이미 제외되었거나 없는 멤버입니다. 목록을 다시 불러왔습니다. | project.member |
| `project.member.noActor` | 없음 | project.member |
| `project.member.pmAddNote` | PM이 되면 멤버·단계·업무를 편집할 수 있습니다. 게이트 승인자로 지정된 멤버는 PM이 될 수 없습니다. | project.member |
| `project.member.pmCount` | PM {count}명 | project.member |
| `project.member.pmGuide` | PM 추가·해제는 아래 멤버 목록에서 역할을 PM으로 바꾸거나 PM에서 다른 역할로 바꿔 합니다. PM은 1명 이상 남아야 합니다. 활성 PM이 모두 없어진 프로젝트는 시스템 관리자가 PM을 재지정합니다. | project.member |
| `project.member.pmGuideReadOnly` | PM 추가·해제는 현재 PM만 할 수 있습니다. 활성 PM이 모두 없어진 프로젝트는 시스템 관리자가 PM을 재지정합니다. | project.member |
| `project.member.pmOnly` | PM 권한이 없어 처리하지 못했습니다. 다른 PM이 먼저 역할을 바꿨을 수 있어 목록을 다시 불러왔습니다. | project.member |
| `project.member.pmReleaseSelfNote` | 본인의 PM을 해제하면 이 프로젝트의 PM 권한이 바로 사라집니다. | project.member |
| `project.member.pmTargetRejected` | 이 멤버는 PM으로 지정할 수 없습니다. 게이트 승인자이거나 비활성 계정인지 확인해 주세요. | project.member |
| `project.member.readOnly` | 읽기 전용 · 역할 변경과 제외는 PM만 할 수 있습니다 | project.member |
| `project.member.remove` | 제외 | project.member |
| `project.member.removeAria` | {name}을(를) 멤버에서 제외 | project.member |
| `project.member.removeConflict` | 마지막 PM이거나 게이트 승인자라 제외할 수 없습니다. 다른 PM을 먼저 추가하거나 승인자를 먼저 변경해 주세요. | project.member |
| `project.member.removed` | {name}을(를) 멤버에서 제외했습니다. | project.member |
| `project.member.retry` | 다시 시도 | project.member |
| `project.member.roleChanged` | {name}의 역할을 {after}(으)로 바꿨습니다. | project.member |
| `project.member.roleRejected` | 선택한 역할로 바꿀 수 없습니다. 프로젝트에 지정한 액터인지 확인해 주세요. | project.member |
| `project.member.roleSelect` | {name}의 역할 | project.member |
| `project.member.submitting` | 처리 중 | project.member |
| `project.member.title` | 현재 멤버 ({count}명) | project.member |
| `project.member.unknownName` | (이름 없음) | project.member |
| `project.settings.approverBadge` | 승인자 | project.settings |
| `project.settings.approverCandidatesHint` | 승인자 후보는 이 중 PM을 제외한 멤버입니다. | project.settings |
| `project.settings.approverNoAutoAssign` | · 승인자로 지정해도 업무 담당자로 자동 배정되지 않습니다. | project.settings |
| `project.settings.approverNotConnected` | 게이트 승인자 변경은 서버 API가 아직 없어 이 화면에서 바꿀 수 없습니다(FR-PRJ-05). | project.settings |
| `project.settings.approverRoleDesc` | · 게이트 승인자: 산출물 검수와 단계 전환을 판정하는 사람 (승인 요청에 표시) | project.settings |
| `project.settings.assigneeRoleDesc` | · 업무 담당자: 업무를 수행하는 사람 (내 업무·업무 목록에 표시) | project.settings |
| `project.settings.assigneeVsApproverTitle` | 업무 담당자와 승인자는 다릅니다 | project.settings |
| `project.settings.badgeCurrentPm` | 현재 PM | project.settings |
| `project.settings.badgeNoPermission` | 권한 없음 | project.settings |
| `project.settings.currentMembersTitle` | 현재 멤버 ({count}명) | project.settings |
| `project.settings.delegateBlockedNote` | 위임 대상이 게이트 승인자로 지정되어 있으면 위임이 차단됩니다. 이 경우 승인자 지정을 먼저 해제한 후 다시 시도해 주세요. 승인자 지정은 자동으로 해제되지 않습니다. | project.settings |
| `project.settings.delegateModalDescription` | 위임이 완료되면 프로젝트의 PM 표시와 PM 권한이 함께 바뀝니다. | project.settings |
| `project.settings.delegateModalTitle` | PM을 위임하시겠습니까? | project.settings |
| `project.settings.description` | 게이트 승인자는 프로젝트 멤버 중에서 PM이 직접 지정합니다. 단계마다 서로 다른 승인자를 둘 수 있습니다. | project.settings |
| `project.settings.gateCandidatesCount` | 5단계 · 승인자 {count}명 후보 | project.settings |
| `project.settings.gateSectionTitle` | 게이트별 승인자 · 승인 상태 | project.settings |
| `project.settings.inviteTitle` | 멤버 초대 | project.settings |
| `project.settings.newPm` | 새 PM | project.settings |
| `project.settings.noPmCandidates` | 위임할 수 있는 다른 활성 멤버가 없습니다. PM이 아닌 프로젝트 멤버만 위임 대상이 될 수 있습니다. | project.settings |
| `project.settings.notFoundDescription` | 삭제되었거나 접근 권한이 없는 프로젝트입니다. 프로젝트 목록에서 다시 확인해 주세요. | project.settings |
| `project.settings.notFoundTitle` | 프로젝트를 찾을 수 없습니다 | project.settings |
| `project.settings.pmCannotBeApprover` | 현재 PM은 게이트 승인자로 지정할 수 없습니다. 아래 PM 위임 절차를 사용해 주세요. | project.settings |
| `project.settings.pmCardTitle` | 프로젝트 PM | project.settings |
| `project.settings.pmChangePermissionNote` | PM 변경은 현재 PM 또는 시스템 관리자만 수행할 수 있습니다. | project.settings |
| `project.settings.pmChangeTitle` | PM 변경·위임 | project.settings |
| `project.settings.pmDelegateGuide` | 위임 전에 기존 PM과 새 PM을 확인합니다. 일반 멤버는 PM을 변경할 수 없습니다. | project.settings |
| `project.settings.pmOnlyInvite` | 멤버 초대는 프로젝트 PM만 할 수 있습니다. | project.settings |
| `project.settings.previousPm` | 기존 PM | project.settings |
| `project.settings.readOnlyPmOnly` | 읽기 전용 · 승인자 변경은 PM만 가능 | project.settings |
| `project.settings.selectNewPm` | 새 PM 선택 | project.settings |
| `project.settings.selectNewPmRequired` | 새 PM으로 위임할 멤버를 선택해 주세요. | project.settings |
| `project.settings.serverMembers` | 서버 멤버 목록 기준 | project.settings |
| `project.settings.title` | 멤버·설정 | project.settings |
| `project.template.empty` | 빈 템플릿 | project.template |
| `project.template.itProject` | IT 프로젝트 템플릿 | project.template |
| `project.userSearch.empty` | '{keyword}'(으)로 초대할 수 있는 사용자를 찾지 못했습니다. | project.userSearch |
| `project.userSearch.failed` | 사용자를 찾지 못했습니다. | project.userSearch |
| `project.userSearch.limitNote` | 최대 20명까지 보여 줍니다. 이름을 더 입력해 좁혀 주세요. | project.userSearch |
| `project.userSearch.loading` | 찾는 중입니다 | project.userSearch |
| `project.userSearch.noDepartment` | 부서 없음 | project.userSearch |
| `project.userSearch.placeholder` | 이름으로 찾기 (2자 이상) | project.userSearch |
| `project.userSearch.resultCount` | {count}명을 찾았습니다. | project.userSearch |
| `project.userSearch.tooShort` | 이름을 2자 이상 입력하면 찾습니다. | project.userSearch |
| `status.activityIssue.open` | 접수 | status.activityIssue |
| `status.activityIssue.progress` | 처리중 | status.activityIssue |
| `status.activityIssue.resolved` | 해결 | status.activityIssue |
| `status.auditType.ai` | AI 요청 | status.auditType |
| `status.auditType.approval` | 승인 | status.auditType |
| `status.auditType.download` | 다운로드 | status.auditType |
| `status.auditType.login_fail` | 로그인 실패 | status.auditType |
| `status.auditType.login_success` | 로그인 성공 | status.auditType |
| `status.auditType.permission` | 권한·설정 변경 | status.auditType |
| `status.deliverable.approved` | 승인 | status.deliverable |
| `status.deliverable.pending` | 미제출 | status.deliverable |
| `status.deliverable.rejected` | 반려 | status.deliverable |
| `status.deliverable.submitted` | 검수 대기 | status.deliverable |
| `status.health.done` | 완료 | 상태 · 리스크 수준 |
| `status.issue.closed` | 종결 | status.issue |
| `status.issue.confirmed` | 확인 | status.issue |
| `status.issue.fixing` | 조치 | status.issue |
| `status.issue.registered` | 등록 | status.issue |
| `status.issue.verifying` | 검증 | status.issue |
| `status.issueKind.defect` | 결함 | status.issueKind |
| `status.issueKind.issue` | 일반 이슈 | status.issueKind |
| `status.issueKind.request` | 요청 | status.issueKind |
| `status.issueSeverity.critical` | 치명 | status.issueSeverity |
| `status.issueSeverity.high` | 높음 | status.issueSeverity |
| `status.issueSeverity.low` | 낮음 | status.issueSeverity |
| `status.issueSeverity.medium` | 보통 | status.issueSeverity |
| `status.methodology.agile` | 애자일 | status.methodology |
| `status.methodology.none` | 단계 없음 | status.methodology |
| `status.methodology.waterfall` | 폭포수 | status.methodology |
| `status.priority.high` | 높음 | status.priority |
| `status.priority.low` | 낮음 | status.priority |
| `status.priority.normal` | 보통 | status.priority |
| `status.projectFocus.all` | 전체 프로젝트 | status.projectFocus |
| `status.projectFocus.done` | 완료 | status.projectFocus |
| `status.projectFocus.late` | 지연 | status.projectFocus |
| `status.projectFocus.ongoing` | 진행 중 | status.projectFocus |
| `status.projectRole.biz` | 현업 담당자 | status.projectRole |
| `status.projectRole.dev` | 개발자 | status.projectRole |
| `status.projectRole.pm` | PM | status.projectRole |
| `status.projectRole.qa` | QA | status.projectRole |
| `status.projectSort.endDate` | 종료일 임박 순 | status.projectSort |
| `status.projectSort.name` | 프로젝트명 순 | status.projectSort |
| `status.projectSort.progressAsc` | 진척률 낮은 순 | status.projectSort |
| `status.projectSort.progressDesc` | 진척률 높은 순 | status.projectSort |
| `status.projectSort.risk` | 리스크 높은 순 | status.projectSort |
| `status.record.active` | 활성 | status.record |
| `status.record.inactive` | 비활성 | status.record |
| `status.stage.analysis` | 분석 | status.stage |
| `status.stage.deploy` | 이행 | status.stage |
| `status.stage.design` | 설계 | status.stage |
| `status.stage.dev` | 개발 | status.stage |
| `status.stage.test` | 테스트 | status.stage |
| `status.systemRole.admin` | 시스템 관리자 | status.systemRole |
| `status.systemRole.exec` | 경영진 | status.systemRole |
| `status.systemRole.user` | 일반 사용자 | status.systemRole |
| `status.taskFilter.all` | 전체 | status.taskFilter |
| `status.taskFilter.done` | 완료 | status.taskFilter |
| `status.taskFilter.late` | 지연 | status.taskFilter |
| `status.taskFilter.open` | 진행 중 | status.taskFilter |
| `status.taskFilter.soon` | 마감 임박 | status.taskFilter |
| `status.taskFilter.today` | 오늘 | status.taskFilter |
| `status.taskFilterHint.all` | 완료 포함 전체 업무 | status.taskFilterHint |
| `status.taskFilterHint.done` | 완료 처리한 업무 | status.taskFilterHint |
| `status.taskFilterHint.late` | 마감일이 지난 미완료 업무 | status.taskFilterHint |
| `status.taskFilterHint.open` | 완료되지 않은 업무 | status.taskFilterHint |
| `status.taskFilterHint.soon` | 3일 이내 마감 예정 | status.taskFilterHint |
| `status.taskFilterHint.today` | 오늘 마감인 미완료 업무 | status.taskFilterHint |
| `status.visibility.company` | 전사 공개 | status.visibility |
| `status.visibility.department` | 부서 공개 | status.visibility |
| `status.visibility.members` | 멤버 전용 | status.visibility |
| `task.server.activityAssigned` | 담당자 배정 | task.server |
| `task.server.activityBoardColumnChanged` | 컬럼 이동 | task.server |
| `task.server.activityCreated` | 등록 | task.server |
| `task.server.activityDateChanged` | 기간 변경 | task.server |
| `task.server.activityDeleted` | 삭제 | task.server |
| `task.server.activityDetailsChanged` | 내용 변경 | task.server |
| `task.server.activityMoved` | 위치 이동 | task.server |
| `task.server.activityOther` | 변경 | task.server |
| `task.server.activityProgressChanged` | 진척률 변경 | task.server |
| `task.server.activityStatusChanged` | 상태 변경 | task.server |
| `task.server.activityUnassigned` | 담당자 해제 | task.server |
| `task.server.assignee` | 담당자 | task.server |
| `task.server.attributes` | 업무 속성 | task.server |
| `task.server.close` | 닫기 | task.server |
| `task.server.completionCriteria` | 완료 조건 (선택) | task.server |
| `task.server.concurrent` | 다른 사용자가 먼저 수정했습니다. 최신 내용을 다시 불러왔으니 확인한 뒤 다시 시도해 주세요. | task.server |
| `task.server.createdBy` | 등록 | task.server |
| `task.server.criteriaTooLong` | 완료 조건은 2000자 이하로 입력해 주세요. | task.server |
| `task.server.derived` | 하위작업 기간으로 산출 | task.server |
| `task.server.description` | 설명 | task.server |
| `task.server.effort` | 예상 공수 | task.server |
| `task.server.fieldBoardColumnId` | 보드 컬럼 | task.server |
| `task.server.fieldCompletionCriteria` | 완료 조건 | task.server |
| `task.server.fieldDueDate` | 마감일 | task.server |
| `task.server.fieldEndDate` | 종료일 | task.server |
| `task.server.fieldName` | 작업명 | task.server |
| `task.server.fieldSortOrder` | 정렬 순서 | task.server |
| `task.server.fieldStartDate` | 시작일 | task.server |
| `task.server.forbidden` | 이 업무를 수정할 권한이 없습니다. 업무를 새로 불러왔습니다. | task.server |
| `task.server.histories` | 변경 이력 | task.server |
| `task.server.historiesEmpty` | 변경 이력이 없습니다. | task.server |
| `task.server.historiesFailed` | 변경 이력을 불러오지 못했습니다. | task.server |
| `task.server.loadFailed` | 업무를 불러오지 못했습니다. | task.server |
| `task.server.loading` | 업무를 불러오는 중입니다 | task.server |
| `task.server.more` | 더 보기 | task.server |
| `task.server.none` | 없음 | task.server |
| `task.server.notFound` | 업무를 찾을 수 없습니다. 삭제되었을 수 있습니다. | task.server |
| `task.server.parent` | 상위 작업 | task.server |
| `task.server.period` | 기간 | task.server |
| `task.server.priority` | 우선순위 | task.server |
| `task.server.priorityHigh` | 높음 | task.server |
| `task.server.priorityLow` | 낮음 | task.server |
| `task.server.priorityMedium` | 보통 | task.server |
| `task.server.progress` | 진척률 | task.server |
| `task.server.readOnly` | 현재 역할로는 업무 속성을 조회만 할 수 있습니다. | task.server |
| `task.server.retry` | 다시 시도 | task.server |
| `task.server.save` | 저장 | task.server |
| `task.server.saved` | 업무 속성을 저장했습니다. | task.server |
| `task.server.saving` | 저장 중 | task.server |
| `task.server.source` | 출처 | task.server |
| `task.server.sourceAi` | AI 초안 | task.server |
| `task.server.sourceManual` | 직접 등록 | task.server |
| `task.server.status` | 상태 | task.server |
| `task.server.statusNote` | 상태는 칸반 보드에서 카드를 옮겨 바꿉니다. | task.server |
| `task.server.system` | 시스템 | task.server |
| `task.server.taskType` | 유형 | task.server |
| `task.server.title` | 업무 상세 | task.server |
| `task.server.typeDefect` | 결함 | task.server |
| `task.server.typeTask` | 작업 | task.server |
| `wbs.draft.aiBadge` | AI 초안 | wbs.draft |
| `wbs.draft.aiFailed` | AI 초안을 만들지 못했습니다. 잠시 후 다시 시도해 주세요. | wbs.draft |
| `wbs.draft.aiTimeout` | AI 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요. | wbs.draft |
| `wbs.draft.aiUnavailable` | AI 기능을 지금 사용할 수 없습니다(서버 준비 중이거나 일시 장애). 잠시 후 다시 시도해 주세요. | wbs.draft |
| `wbs.draft.alreadyApplied` | 이미 반영한 초안입니다. WBS 화면에서 결과를 확인해 주세요. | wbs.draft |
| `wbs.draft.applied` | 작업 {tasks}건·마일스톤 {milestones}건을 WBS에 반영했습니다. | wbs.draft |
| `wbs.draft.apply` | 선택 항목 반영 | wbs.draft |
| `wbs.draft.applying` | 반영 중 | wbs.draft |
| `wbs.draft.assumptions` | 가정 | wbs.draft |
| `wbs.draft.atRisk` | 위험 있음 | wbs.draft |
| `wbs.draft.constraints` | 제약조건 (선택, 한 줄에 하나) | wbs.draft |
| `wbs.draft.constraintsInvalid` | 제약조건은 최대 20개, 각 200자 이하로 입력해 주세요. | wbs.draft |
| `wbs.draft.description` | 프로젝트 목적이나 요구사항을 입력하면 AI가 WBS 초안을 만듭니다. 초안은 WBS에 바로 들어가지 않고, PM이 고른 항목만 반영됩니다. | wbs.draft |
| `wbs.draft.expired` | 초안이 만료되었거나 사용할 수 없습니다. 초안을 다시 만들어 주세요. | wbs.draft |
| `wbs.draft.expiresAt` | {time}까지 반영할 수 있습니다. | wbs.draft |
| `wbs.draft.feasibility` | 실현 가능성 | wbs.draft |
| `wbs.draft.feasible` | 실현 가능 | wbs.draft |
| `wbs.draft.generate` | 초안 만들기 | wbs.draft |
| `wbs.draft.generating` | AI가 초안을 만드는 중입니다 | wbs.draft |
| `wbs.draft.goWbs` | WBS로 이동 | wbs.draft |
| `wbs.draft.infeasible` | 실현 어려움 | wbs.draft |
| `wbs.draft.inputRequired` | 프로젝트 목적이나 요구사항 중 하나 이상 입력해 주세요. | wbs.draft |
| `wbs.draft.instruction` | 추가 지시 (선택) | wbs.draft |
| `wbs.draft.instructionTooLong` | 추가 지시는 1000자 이하로 입력해 주세요. | wbs.draft |
| `wbs.draft.insufficientInfo` | 정보 부족 | wbs.draft |
| `wbs.draft.milestoneDateRequired` | 선택한 마일스톤 '{name}'의 목표일을 입력해 주세요. | wbs.draft |
| `wbs.draft.milestonesSection` | 마일스톤 ({count}) | wbs.draft |
| `wbs.draft.nothingSelected` | 반영할 항목을 하나 이상 선택해 주세요. | wbs.draft |
| `wbs.draft.pmOnly` | AI 초안 생성과 반영은 프로젝트 PM만 할 수 있습니다. | wbs.draft |
| `wbs.draft.purpose` | 프로젝트 목적 | wbs.draft |
| `wbs.draft.purposeTooLong` | 프로젝트 목적은 2000자 이하로 입력해 주세요. | wbs.draft |
| `wbs.draft.questions` | 확인할 질문 | wbs.draft |
| `wbs.draft.reason` | 근거 | wbs.draft |
| `wbs.draft.requirements` | 요구사항 (선택) | wbs.draft |
| `wbs.draft.requirementsTooLong` | 요구사항은 20000자 이하로 입력해 주세요. | wbs.draft |
| `wbs.draft.reviewNote` | AI가 만든 결과입니다. 반드시 검토한 뒤 반영할 항목만 고르세요. | wbs.draft |
| `wbs.draft.selectAll` | 전체 선택 | wbs.draft |
| `wbs.draft.selectNone` | 전체 해제 | wbs.draft |
| `wbs.draft.subtaskCount` | 하위작업 {count}개 | wbs.draft |
| `wbs.draft.suggestions` | 제안 | wbs.draft |
| `wbs.draft.targetDate` | 목표일 | wbs.draft |
| `wbs.draft.taskDateOrder` | 선택한 작업 '{name}'의 시작일이 종료일보다 늦습니다. | wbs.draft |
| `wbs.draft.taskDateRequired` | 선택한 작업 '{name}'의 시작일·종료일을 입력해 주세요. | wbs.draft |
| `wbs.draft.taskNameRequired` | 선택한 작업의 이름을 입력해 주세요. | wbs.draft |
| `wbs.draft.tasksSection` | 작업 ({count}) | wbs.draft |
| `wbs.draft.title` | AI WBS 초안 | wbs.draft |
| `wbs.draft.warnings` | 주의 | wbs.draft |
| `wbs.draft.waterfallOnly` | AI WBS 초안은 단계가 있는 폭포수 프로젝트에서만 쓸 수 있습니다. | wbs.draft |
| `wbs.form.assignee` | 담당자 (선택) | wbs.form |
| `wbs.form.assigneeNone` | 미배정 | wbs.form |
| `wbs.form.assigneePmOnly` | 담당자 배정은 PM만 할 수 있습니다. | wbs.form |
| `wbs.form.assigneeSelfOnly` | 담당자는 비우거나 본인만 지정할 수 있습니다. | wbs.form |
| `wbs.form.cancel` | 취소 | wbs.form |
| `wbs.form.completionCriteria` | 완료 조건 (선택) | wbs.form |
| `wbs.form.completionCriteriaTooLong` | 완료 조건은 2000자 이하로 입력해 주세요. | wbs.form |
| `wbs.form.createSubtaskTitle` | '{name}' 하위작업 추가 | wbs.form |
| `wbs.form.createTitle` | 작업 추가 | wbs.form |
| `wbs.form.dateOrder` | 시작일은 종료일보다 늦을 수 없습니다. | wbs.form |
| `wbs.form.dateRequired` | 시작일과 종료일을 입력해 주세요. | wbs.form |
| `wbs.form.derivedNote` | 하위작업이 있는 작업의 기간은 하위작업 기간으로 정해져 직접 바꿀 수 없습니다. | wbs.form |
| `wbs.form.description` | 설명 (선택) | wbs.form |
| `wbs.form.descriptionTooLong` | 설명은 2000자 이하로 입력해 주세요. | wbs.form |
| `wbs.form.editTitle` | 작업 수정 | wbs.form |
| `wbs.form.effort` | 예상 공수 (인일, 선택) | wbs.form |
| `wbs.form.effortInvalid` | 예상 공수는 0.1~99999.9인일, 소수 첫째 자리까지 입력해 주세요. | wbs.form |
| `wbs.form.endDate` | 종료일 | wbs.form |
| `wbs.form.name` | 작업명 | wbs.form |
| `wbs.form.nameRequired` | 작업명을 입력해 주세요. | wbs.form |
| `wbs.form.nameTooLong` | 작업명은 200자 이하로 입력해 주세요. | wbs.form |
| `wbs.form.phase` | 단계 | wbs.form |
| `wbs.form.phaseInherited` | 하위작업은 상위 작업의 단계를 따릅니다. | wbs.form |
| `wbs.form.priority` | 우선순위 | wbs.form |
| `wbs.form.progressDoneNote` | 완료 상태의 작업은 진척률이 100이어야 합니다. | wbs.form |
| `wbs.form.progressInvalid` | 진척률은 0~100 사이의 정수로 입력해 주세요. | wbs.form |
| `wbs.form.progressLabel` | 진척률 (0~100%) | wbs.form |
| `wbs.form.progressTitle` | '{name}' 진척률 | wbs.form |
| `wbs.form.startDate` | 시작일 | wbs.form |
| `wbs.form.submit` | 저장 | wbs.form |
| `wbs.form.submitting` | 저장 중 | wbs.form |
| `wbs.form.taskType` | 유형 | wbs.form |
| `wbs.milestone.add` | 마일스톤 추가 | wbs.milestone |
| `wbs.milestone.aiSource` | AI 초안 | wbs.milestone |
| `wbs.milestone.cancel` | 취소 | wbs.milestone |
| `wbs.milestone.columnActions` | 관리 | wbs.milestone |
| `wbs.milestone.columnDday` | D-day | wbs.milestone |
| `wbs.milestone.columnName` | 마일스톤 | wbs.milestone |
| `wbs.milestone.columnTargetDate` | 목표일 | wbs.milestone |
| `wbs.milestone.columnTasks` | 연결 작업 | wbs.milestone |
| `wbs.milestone.concurrent` | 다른 사용자가 먼저 수정했습니다. 최신 마일스톤을 다시 불러왔으니 확인한 뒤 다시 시도해 주세요. | wbs.milestone |
| `wbs.milestone.confirmDelete` | '{name}' 마일스톤을 삭제할까요? 연결 정보만 지워지고 작업은 남습니다. 되돌릴 수 없습니다. | wbs.milestone |
| `wbs.milestone.created` | '{name}' 마일스톤을 등록했습니다. | wbs.milestone |
| `wbs.milestone.createTitle` | 마일스톤 추가 | wbs.milestone |
| `wbs.milestone.dateRequired` | 목표일을 입력해 주세요. | wbs.milestone |
| `wbs.milestone.dDayLeft` | D-{days} | wbs.milestone |
| `wbs.milestone.dDayPassed` | D+{days} | wbs.milestone |
| `wbs.milestone.dDayToday` | D-day | wbs.milestone |
| `wbs.milestone.dDayUnknown` | - | wbs.milestone |
| `wbs.milestone.delayed` | 지연 | wbs.milestone |
| `wbs.milestone.delayedTitle` | 연결 작업이 끝나지 않았는데 목표일이 도래했습니다. | wbs.milestone |
| `wbs.milestone.delete` | 삭제 | wbs.milestone |
| `wbs.milestone.deleted` | '{name}' 마일스톤을 삭제했습니다. | wbs.milestone |
| `wbs.milestone.edit` | 수정 | wbs.milestone |
| `wbs.milestone.editTasks` | 연결 작업 | wbs.milestone |
| `wbs.milestone.editTitle` | 마일스톤 수정 | wbs.milestone |
| `wbs.milestone.empty` | 등록된 마일스톤이 없습니다. | wbs.milestone |
| `wbs.milestone.emptyHintPm` | 단계 종료일·오픈일처럼 기준이 되는 날짜를 마일스톤으로 추가하세요. | wbs.milestone |
| `wbs.milestone.emptyHintViewer` | PM이 마일스톤을 등록하면 여기에 표시됩니다. | wbs.milestone |
| `wbs.milestone.invalid` | 입력값을 확인해 주세요. | wbs.milestone |
| `wbs.milestone.limitExceeded` | 프로젝트 마일스톤은 최대 200개까지 등록할 수 있습니다. | wbs.milestone |
| `wbs.milestone.loadFailed` | 마일스톤을 불러오지 못했습니다. | wbs.milestone |
| `wbs.milestone.loading` | 마일스톤을 불러오는 중입니다 | wbs.milestone |
| `wbs.milestone.name` | 마일스톤명 | wbs.milestone |
| `wbs.milestone.nameRequired` | 마일스톤명을 입력해 주세요. | wbs.milestone |
| `wbs.milestone.nameTooLong` | 마일스톤명은 200자 이하로 입력해 주세요. | wbs.milestone |
| `wbs.milestone.noTasks` | 연결 작업 없음 | wbs.milestone |
| `wbs.milestone.notFound` | 마일스톤을 찾을 수 없습니다. 다른 사용자가 삭제했을 수 있어 목록을 새로 불러왔습니다. | wbs.milestone |
| `wbs.milestone.noWbsTasks` | 연결할 WBS 작업이 없습니다. 먼저 WBS에 작업을 추가하세요. | wbs.milestone |
| `wbs.milestone.pmOnly` | 마일스톤은 프로젝트 PM만 바꿀 수 있습니다. 권한이 바뀌었을 수 있어 마일스톤을 새로 불러왔습니다. | wbs.milestone |
| `wbs.milestone.retry` | 다시 시도 | wbs.milestone |
| `wbs.milestone.submit` | 저장 | wbs.milestone |
| `wbs.milestone.submitting` | 저장 중 | wbs.milestone |
| `wbs.milestone.targetDate` | 목표일 | wbs.milestone |
| `wbs.milestone.tasks` | 연결 작업 (선택) | wbs.milestone |
| `wbs.milestone.tasksHint` | WBS 작업·하위작업 중에서 고르세요. 최대 {max}개까지 연결할 수 있습니다. | wbs.milestone |
| `wbs.milestone.tasksLimitReached` | 연결 작업은 최대 {max}개까지 고를 수 있습니다. | wbs.milestone |
| `wbs.milestone.tasksSaved` | '{name}' 마일스톤의 연결 작업을 저장했습니다. | wbs.milestone |
| `wbs.milestone.tasksSelected` | {count}개 선택 | wbs.milestone |
| `wbs.milestone.tasksTitle` | '{name}' 연결 작업 | wbs.milestone |
| `wbs.milestone.taskSummary` | 미완료 {incomplete} / 연결 {linked} | wbs.milestone |
| `wbs.milestone.title` | 마일스톤 | wbs.milestone |
| `wbs.milestone.unavailable` | 이 프로젝트에서는 마일스톤을 사용할 수 없습니다. | wbs.milestone |
| `wbs.milestone.updated` | '{name}' 마일스톤을 수정했습니다. | wbs.milestone |
| `wbs.milestone.viewerNote` | 마일스톤은 PM만 추가·수정·삭제할 수 있습니다. | wbs.milestone |
| `wbs.server.addSubtask` | 하위작업 추가 | wbs.server |
| `wbs.server.addTask` | 작업 추가 | wbs.server |
| `wbs.server.aiDraftLink` | AI WBS 초안 | wbs.server |
| `wbs.server.aiSource` | AI 초안 | wbs.server |
| `wbs.server.columnActions` | 관리 | wbs.server |
| `wbs.server.columnAssignee` | 담당자 | wbs.server |
| `wbs.server.columnEffort` | 예상 공수 | wbs.server |
| `wbs.server.columnName` | 작업명 | wbs.server |
| `wbs.server.columnPeriod` | 기간 | wbs.server |
| `wbs.server.columnStatus` | 상태 | wbs.server |
| `wbs.server.concurrent` | 다른 사용자가 먼저 수정했습니다. 최신 WBS를 다시 불러왔으니 확인한 뒤 다시 시도해 주세요. | wbs.server |
| `wbs.server.confirmDelete` | '{name}'을(를) 삭제할까요? 되돌릴 수 없습니다. | wbs.server |
| `wbs.server.created` | '{name}'을(를) 등록했습니다. | wbs.server |
| `wbs.server.delete` | 삭제 | wbs.server |
| `wbs.server.deleted` | '{name}'을(를) 삭제했습니다. | wbs.server |
| `wbs.server.dependencyNote` | 선후행 관계 편집은 아직 서버와 연결하지 않았습니다. | wbs.server |
| `wbs.server.derivedProgress` | 하위작업 공수 가중 평균 | wbs.server |
| `wbs.server.derivedSchedule` | 하위작업 기간으로 산출 | wbs.server |
| `wbs.server.derivedStatus` | 하위 작업 상태 기준 자동 계산 | wbs.server |
| `wbs.server.dragCrossPhase` | 다른 단계로의 최상위 순서 이동은 지원하지 않습니다. 작업 수정에서 단계를 바꾸거나 다른 작업 아래로 옮겨 주세요. | wbs.server |
| `wbs.server.dragDescendant` | 상위 작업을 자신의 하위작업 아래로 옮길 수 없습니다. | wbs.server |
| `wbs.server.dragForbidden` | 작업 순서·계층 이동은 프로젝트 PM만 할 수 있습니다. | wbs.server |
| `wbs.server.dragHandle` | 드래그하여 이동 | wbs.server |
| `wbs.server.dragHint` | 작업 행을 끌어 같은 단계 내 순서를 바꾸거나 다른 작업 아래 하위작업으로 옮길 수 있습니다. | wbs.server |
| `wbs.server.dragMaxDepth` | 하위작업은 2단계(작업 > 하위작업)까지만 만들 수 있습니다. | wbs.server |
| `wbs.server.dragSamePosition` | 이미 같은 위치에 있습니다. | wbs.server |
| `wbs.server.edit` | 수정 | wbs.server |
| `wbs.server.editProgress` | 진척률 수정 | wbs.server |
| `wbs.server.empty` | 등록된 작업이 없습니다. | wbs.server |
| `wbs.server.emptyHintPm` | 단계별로 작업을 추가하거나 AI 초안으로 시작하세요. | wbs.server |
| `wbs.server.emptyHintViewer` | PM이 작업을 등록하면 여기에 표시됩니다. | wbs.server |
| `wbs.server.forbidden` | 이 작업을 할 권한이 없습니다. 권한이 바뀌었을 수 있어 WBS를 새로 불러왔습니다. | wbs.server |
| `wbs.server.inUse` | 하위작업·선후행 관계·마일스톤 연결이 있는 작업은 삭제할 수 없습니다. 연결을 먼저 정리해 주세요. | wbs.server |
| `wbs.server.invalid` | 입력값을 확인해 주세요. | wbs.server |
| `wbs.server.limitExceeded` | 프로젝트 WBS 항목은 최대 2,000개까지 등록할 수 있습니다. | wbs.server |
| `wbs.server.loadFailed` | WBS를 불러오지 못했습니다. | wbs.server |
| `wbs.server.loading` | WBS를 불러오는 중입니다 | wbs.server |
| `wbs.server.memberNote` | 본인이 담당한 작업만 수정하고 그 아래 하위작업을 추가할 수 있습니다. | wbs.server |
| `wbs.server.moved` | 순서를 바꿨습니다. | wbs.server |
| `wbs.server.moveDown` | 아래로 | wbs.server |
| `wbs.server.moveUp` | 위로 | wbs.server |
| `wbs.server.network` | 서버에 연결하지 못했습니다. 네트워크를 확인한 뒤 다시 시도해 주세요. | wbs.server |
| `wbs.server.noEffort` | 공수 미입력 | wbs.server |
| `wbs.server.noPhase` | 단계 미지정 | wbs.server |
| `wbs.server.notFound` | 대상 작업이나 단계를 찾을 수 없습니다. WBS를 새로 불러왔습니다. | wbs.server |
| `wbs.server.openDetail` | 업무 상세 | wbs.server |
| `wbs.server.personDays` | {value}인일 | wbs.server |
| `wbs.server.phaseDerivedSchedule` | 소속 작업 기간으로 자동 산출 | wbs.server |
| `wbs.server.phaseEffort` | 예상 공수 {effort} | wbs.server |
| `wbs.server.phasePeriod` | 기간 {period} | wbs.server |
| `wbs.server.phaseTaskCount` | 작업 {count}건 | wbs.server |
| `wbs.server.progress` | 진척률 | wbs.server |
| `wbs.server.progressSaved` | 진척률을 {value}%로 저장했습니다. | wbs.server |
| `wbs.server.reparent` | 상위 작업 변경 | wbs.server |
| `wbs.server.reparentDescription` | {name}의 위치를 변경합니다. 다른 단계의 상위 작업 아래로 옮기면 해당 단계로 함께 이동합니다. | wbs.server |
| `wbs.server.reparentRoot` | 최상위 작업으로 올리기 | wbs.server |
| `wbs.server.reparentTarget` | 이동할 상위 작업 | wbs.server |
| `wbs.server.retry` | 다시 시도 | wbs.server |
| `wbs.server.serverError` | 서버 오류로 처리하지 못했습니다. 잠시 후 다시 시도해 주세요. | wbs.server |
| `wbs.server.timeout` | 응답이 늦어 요청을 멈췄습니다. 잠시 후 다시 시도해 주세요. | wbs.server |
| `wbs.server.unassigned` | 미배정 | wbs.server |
| `wbs.server.unavailable` | 이 프로젝트에서는 WBS를 사용할 수 없습니다. 기능 구성에서 WBS를 끈 프로젝트이거나 볼 수 없는 프로젝트입니다. | wbs.server |
| `wbs.server.unknown` | 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요. | wbs.server |
| `wbs.server.updated` | '{name}'을(를) 수정했습니다. | wbs.server |
| `wbs.server.viewerNote` | 현재 역할로는 WBS를 조회만 할 수 있습니다. | wbs.server |

## BE에 이미 시드됨 (18개, V9 공통 코드 라벨)

FE가 공통 코드와 같은 값을 표시할 때 이 키를 그대로 씁니다. 등록할 필요가 없습니다.

| 라벨 키 | 한국어 기본 문구 | 구분 |
| --- | --- | --- |
| `code.PROJECT_STATUS.CANCELLED` | 취소 | 상태 · 프로젝트 |
| `code.PROJECT_STATUS.COMPLETED` | 완료 | 상태 · 프로젝트 |
| `code.PROJECT_STATUS.IN_PROGRESS` | 진행 중 | 상태 · 프로젝트 |
| `code.PROJECT_STATUS.ON_HOLD` | 보류 | 상태 · 프로젝트 |
| `code.PROJECT_STATUS.PLANNED` | 계획 | 상태 · 프로젝트 |
| `code.REQUEST_RESULT.FAILURE` | 실패 | 상태 · 요청 결과 |
| `code.REQUEST_RESULT.SUCCESS` | 성공 | 상태 · 요청 결과 |
| `code.RISK_LEVEL.CAUTION` | 주의 | 상태 · 리스크 수준 |
| `code.RISK_LEVEL.DANGER` | 위험 | 상태 · 리스크 수준 |
| `code.RISK_LEVEL.NORMAL` | 정상 | 상태 · 리스크 수준 |
| `code.TASK_STATUS.DONE` | 완료 | 상태 · 업무 |
| `code.TASK_STATUS.IN_PROGRESS` | 진행 | 상태 · 업무 |
| `code.TASK_STATUS.REVIEW` | 검토 | 상태 · 업무 |
| `code.TASK_STATUS.TODO` | 대기 | 상태 · 업무 |
| `codeGroup.PROJECT_STATUS` | 프로젝트 상태 | 공통 코드 그룹 |
| `codeGroup.REQUEST_RESULT` | 요청 처리 결과 | 공통 코드 그룹 |
| `codeGroup.RISK_LEVEL` | 위험 수준 | 공통 코드 그룹 |
| `codeGroup.TASK_STATUS` | 업무 진행 상태 | 공통 코드 그룹 |
