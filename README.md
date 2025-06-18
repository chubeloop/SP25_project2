# SIC/XE Virtual Machine & Simulator
본 프로젝트는 SIC/XE(Simplified Instructional Computer/Extra Equipment) 아키텍처를 위해 Java로 개발된 가상 머신(VM) 및 시뮬레이터입니다. 이 프로그램은 SIC/XE 어셈블러가 생성한 목적 코드(Object Code)를 로드하고, 명령어 단위로 실행하며, 그 과정을 시각적으로 모니터링할 수 있는 GUI 환경을 제공합니다.

## 주요 기능
* GUI 기반 제어 및 모니터링: 사용자가 직관적으로 프로그램을 제어하고 실행 결과를 확인할 수 있는 그래픽 사용자 인터페이스를 제공합니다.
* 프로그램을 한 명령어씩 단계별로 실행(Run One Step)하거나, 프로그램이 종료될 때까지 연속으로 실행(Run All)할 수 있습니다.
* 컨트롤 섹션 및 외부 참조 지원: CSECT로 분리된 여러 개의 컨트롤 섹션을 로드하고, EXTDEF/EXTREF와 M-레코드를 통해 외부 심볼 참조를 해결하는 링킹 로더의 기능을 지원합니다.
* 파일 기반 장치 입출력: TD, RD, WD와 같은 입출력 명령어를 지원합니다. 장치 번호(예: "05")를 실제 파일(예: "DEV05.TXT")에 매핑하여 파일 입출력을 시뮬레이션합니다.
### 실시간 상태 표시:
* 명령어 실행에 따른 레지스터(A, X, L, B, S, T, F, PC, SW) 값의 변화를 10진수와 16진수로 실시간 표시합니다.
* 계산된 유효 주소(Target Address)를 별도로 표시하여 주소 지정 방식의 결과를 쉽게 확인할 수 있습니다.
### 메모리 및 명령어 시각화:
* 로드된 목적 코드를 가상 메모리 주소와 함께 명령어 단위로 디코딩하여 보여줍니다.
* 현재 실행될 명령어(PC가 가리키는 위치)를 노란색으로 하이라이트하여 실행 흐름을 쉽게 추적할 수 있습니다.

## 프로그램 아키텍처

## 실행 흐름
* 초기화: 사용자가 VisualSimulator를 실행하면 모든 핵심 모듈(ResourceManager, SicLoader, SicSimulator)이 초기화됩니다.
* 파일 로드: 사용자가 'open' 버튼을 통해 목적 코드 파일을 선택합니다.
* 리소스 정리: VisualSimulator는 ResourceManager의 initializeResource()를 호출하여 이전 실행 상태(메모리, 레지스터 등)를 모두 초기화합니다. 동시에 이전 실행에서 생성된 출력 파일(예: "DEV05.TXT")을 삭제하여 결과가 누적되는 것을 방지합니다.
* 메모리 적재: SicLoader가 목적 코드 파일을 읽어 ResourceManager의 가상 메모리에 내용을 적재하고, M-레코드를 처리하여 주소 참조를 해결합니다.
* 시뮬레이터 준비: SicSimulator가 programLoaded()를 통해 프로그램 로드 완료를 인지하고, 프로그램 카운터(PC)를 E-레코드에 명시된 시작 주소로 설정합니다. GUI의 실행 버튼들이 활성화됩니다.
* 명령어 실행: 사용자가 'Run One Step' 또는 'Run All' 버튼을 클릭하면 SicSimulator가 실행을 시작합니다.
* SicSimulator는 현재 PC 값을 InstLuncher의 executeInstructionAt(pc) 메소드에 전달합니다.
* InstLuncher는 명령어를 실행하고, 다음 실행할 명령어의 주소(nextPC) 또는 종료 신호(NORMAL_HALT, ERROR_HALT)를 반환합니다.
* GUI 업데이트: SicSimulator는 InstLuncher로부터 받은 실행 결과를 바탕으로 로그(실행된 명령어 니모닉 등)를 생성합니다. VisualSimulator는 주기적으로 ResourceManager와 SicSimulator의 최신 상태를 가져와 레지스터, 메모리, PC 하이라이트, 로그 창 등 GUI 전체를 새로고침합니다.
* 프로그램 종료: RSUB나 특정 J 명령어 실행으로 NORMAL_HALT 신호가 반환되거나, 오류 발생으로 ERROR_HALT 신호가 반환되면 SicSimulator는 실행을 중단합니다. 이때 ResourceManager의 closeDevices()가 호출되어 모든 입출력 파일 스트림이 안전하게 닫히고, 버퍼에 남아있던 내용이 파일에 완전히 기록됩니다.

| 클래스 | 역할 |
| :--- | :--- |
| `VisualSimulator.java` | **GUI 및 메인 컨트롤러**: 사용자와의 모든 상호작용을 담당합니다. 파일 로드, 실행 제어 버튼, 레지스터/메모리/로그 표시 등 전체 UI를 구성하고, 사용자의 입력을 받아 다른 모듈에 전달합니다. |
| `SicSimulator.java` | **시뮬레이션 엔진**: 명령어 실행의 전체적인 흐름을 제어합니다. `oneStep`, `allStep` 메소드를 통해 실행을 관리하며, `InstLuncher`에게 실제 명령어 실행을 위임하고, 그 결과를 GUI에 반영하기 위한 로그를 관리합니다. |
| `InstLuncher.java` | **명령어 실행 유닛**: 개별 SIC/XE 명령어를 해석(decode)하고 실행(execute)하는 핵심 로직을 담당합니다. Opcode를 분석하고, 다양한 주소 지정 방식에 따라 유효 주소(TA)를 계산하며, 레지스터 값 변경이나 메모리 접근 등의 실제 연산을 수행합니다. |
| `SicLoader.java` | **목적 코드 로더**: 사용자가 선택한 목적 코드 파일(H, D, R, T, M, E 레코드)을 파싱하여 `ResourceManager`가 관리하는 가상 메모리에 적재합니다. 링킹 로더의 Pass 2와 유사하게 주소 수정(M-레코드 처리)을 수행합니다. |
| `ResourceManager.java` | **가상 자원 관리자**: 가상 SIC/XE 머신의 모든 자원(메모리, 레지스터, 입출력 장치, 외부 심볼 테이블 등)을 관리하는 중앙 저장소 역할을 합니다. 다른 모든 모듈은 이 클래스를 통해 자원에 접근하고 상태를 변경합니다. |
| `SymbolTable.java` | **심볼 테이블 자료구조**: `ResourceManager` 내부에서 외부 심볼 테이블(ESTAB)을 관리하기 위해 사용됩니다. `SicLoader`는 이 테이블에 외부 심볼을 등록하고, M-레코드 처리 시 주소를 참조합니다. |

