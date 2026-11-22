# CoreviaAI_BE 개발 도구 지침

작업을 시작할 때 `README.md`와 [공통 코드·다국어·테마 개발 규칙](docs/development/code-i18n-theme-rules.md)을 읽고 적용한다. 상위 CoreviaAI 프로젝트의 `PROJECT.md`·`development-guidelines/`·`AGENTS.md`가 있으면 매 작업마다 원본을 직접 읽고 우선순위를 따른다. 저장소·Jira·Confluence 주소는 `PROJECT.md`에서 확인한다.

- 기준 요구사항은 전사관리 3안과 3.3 권한 매트릭스다. 대상 Jira 이슈·에픽·인수 기준을 읽고 구현한다. 충돌·보안 결정의 모호함은 임의로 정하지 않는다.
- NFR-20: 화면 문구는 DB 다국어 라벨 키만 참조하고 코드 표시·선택지는 공통 코드 API를 사용한다. component별 문구·상태 배열·번역 사전·색상 하드코딩을 만들지 않는다.
- 코드 값·라벨 키는 고정이다. BE enum·정책은 업무 계약이며 표시 문구와 분리한다. 새 상태·전이·권한은 관리 데이터 변경만으로 추가하지 않는다.
- 지원 언어 `ko/en/zh-TW`, 번역 누락은 한국어 fallback. 문구는 텍스트로 출력한다. 날짜·숫자는 locale API를 쓴다.
- 테마는 공통 디자인 token과 소유자에서 적용한다. 상태는 텍스트·아이콘을 함께 제공한다. FR-ADM-12는 S이며 규칙 추가를 테마 기능 완료로 표현하지 않는다.
- PR에서 화면 하드코딩 문구·상태값 0건 증거를 남긴다. UI 코드가 없으면 해당 없음으로 기록한다. 이 BE의 규칙 파일만으로 FE 검증 완료를 주장하지 않는다.
- Java 기준은 `build.gradle` toolchain(Java 17)이다. Gradle Wrapper로 해당 동작 테스트·검증을 수행한다. 새 라이브러리 없이 해결 가능하면 의존성을 추가하지 않는다.
- 새 작업 브랜치에서 commit까지만 수행한다. 원격 push·원격 브랜치 생성/삭제 금지. commit·PR에 AI 도구 표기나 `Co-Authored-By`를 넣지 않는다. Jira 상태는 바꾸지 않는다.

완료 보고에는 대상 Jira·FR/NFR, 테스트·검수 증거, 문서 변경, 미완료·해당 없음 사유를 남긴다.
