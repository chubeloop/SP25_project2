package SP25_simulator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SicSimulator {
	ResourceManager rMgr;
	InstLuncher instLuncher; // 이름 일관성 유지 (또는 InstLauncher로 통일)
	private boolean isProgramLoaded = false; // 로드 완료 및 실행 가능 상태
	private List<String> executionLog; // 로그 저장

	// 실행된 명령어 수 (디버깅용)
	// private int instructionsExecuted = 0;

	public SicSimulator(ResourceManager resourceManager) {
		if (resourceManager == null) {
			throw new IllegalArgumentException("ResourceManager cannot be null for SicSimulator.");
		}
		this.rMgr = resourceManager;
		this.instLuncher = new InstLuncher(this.rMgr); // InstLuncher 인스턴스 생성
		this.executionLog = new ArrayList<>();
	}

	/**
	 * 프로그램이 ResourceManager에 로드된 후 호출되어 시뮬레이터 상태를 초기화합니다.
	 * PC를 프로그램 실행 시작 주소로 설정합니다.
	 */
	public void programLoaded() { // 파라미터 File program 제거
		if (rMgr.getProgramName() == null || rMgr.getProgramName().isEmpty()) {
			addLog("Error: Program not properly loaded by SicLoader (no program name).");
			isProgramLoaded = false;
			return;
		}
		// PC를 로더가 결정한 첫 번째 명령어 주소로 설정
		rMgr.setRegister(ResourceManager.REG_PC, rMgr.getFirstInstructionAddress());
		isProgramLoaded = true;
		// instructionsExecuted = 0;
		executionLog.clear(); // 이전 로그 삭제
		addLog("Program '" + rMgr.getProgramName() + "' initialized for execution. PC set to " +
				String.format("0x%06X", rMgr.getRegister(ResourceManager.REG_PC)) + ".");
	}


	/**
	 * 1개의 instruction이 수행된 모습을 보인다.
	 * @return 실행이 계속될 수 있으면 true, 프로그램이 중단(halt)되면 false.
	 */
	public boolean oneStep() {
		if (!isProgramLoaded) {
			addLog("Cannot execute: No program loaded or program has ended.");
			return false; // 실행 불가
		}

		int pc = rMgr.getRegister(ResourceManager.REG_PC);

		// PC 유효성 검사 (프로그램 범위 내)
		// 프로그램 실제 로드 주소와 전체 길이를 사용해야 함
		int programActualStart = rMgr.getActualProgramLoadAddress();
		int programEnd = programActualStart + rMgr.getProgramTotalLength();

		if (pc < programActualStart || pc >= programEnd) {
			// RSUB 등으로 정상 종료된 경우 PC가 범위를 벗어날 수 있음 (예: L 레지스터 값이 이상할 때)
			// InstLauncher.NORMAL_HALT 등으로 이미 처리되었을 것이므로, 여기선 추가 로그만 남김.
			if (rMgr.getProgramTotalLength() > 0) { // 길이가 0인 프로그램은 시작하자마자 종료될 수 있음
				addLog("Program Counter (0x" + String.format("%06X", pc) +
						") is out of loaded program bounds [" + String.format("0x%06X", programActualStart) +
						" - " + String.format("0x%06X", programEnd-1) + "]. Halting simulation.");
			} else {
				addLog("Program has zero length. Halting simulation.");
			}
			isProgramLoaded = false; // 실행 중단
			return false;
		}

		// InstLuncher를 통해 명령어 실행
		int nextPc = instLuncher.executeInstructionAt(pc);

		if (nextPc == InstLuncher.ERROR_HALT) {
			addLog("Error executing instruction at PC: 0x" + String.format("%06X", pc) +
					". " + instLuncher.getLastErrorMessage() + " Halting simulation.");
			isProgramLoaded = false; // 실행 중단
			return false;
		} else if (nextPc == InstLuncher.NORMAL_HALT) {
			addLog("Program terminated normally by instruction: " + instLuncher.getLastExecutedInstructionInfo() +
					" at PC: 0x" + String.format("%06X", pc) + ". Halting simulation.");
			isProgramLoaded = false; // 실행 중단
			return false;
		} else {
			// 성공적으로 실행됨, PC 업데이트
			String logMessage = String.format("PC: 0x%06X. Executed: %s. Next PC: 0x%06X",
					pc, instLuncher.getLastExecutedInstructionInfo(), nextPc);
			addLog(logMessage);
			rMgr.setRegister(ResourceManager.REG_PC, nextPc);
			// instructionsExecuted++;
			return true; // 계속 실행 가능
		}
	}

	/**
	 * 남은 모든 instruction이 수행된 모습을 보인다.
	 */
	public void allStep() {
		if (!isProgramLoaded) {
			addLog("Cannot execute 'allStep': No program loaded or program has ended.");
			return;
		}
		addLog("--- Starting All Step Execution ---");
		int maxSteps = 10000; // 무한 루프 방지용 최대 스텝
		int stepsTaken = 0;
		while (isProgramLoaded && stepsTaken < maxSteps) {
			if (!oneStep()) { // oneStep이 false를 반환하면 중단
				break;
			}
			stepsTaken++;
		}

		if (isProgramLoaded && stepsTaken >= maxSteps) {
			addLog("AllStep execution stopped after " + maxSteps + " instructions (potential infinite loop).");
			isProgramLoaded = false; // 안전을 위해 중단 처리
		} else if (!isProgramLoaded) {
			addLog("AllStep execution finished after " + stepsTaken + " step(s).");
		} else {
			addLog("AllStep execution loop ended. Steps: " + stepsTaken); // 정상 종료 외의 상황
		}
		addLog("--- All Step Execution Finished ---");
	}

	/**
	 * 각 단계를 수행할 때 마다 관련된 기록을 남기도록 한다.
	 */
	public void addLog(String log) {
		executionLog.add(log);
		System.out.println("[SIM_LOG] " + log); // 디버깅을 위해 콘솔에도 출력
	}

	public List<String> getExecutionLog() {
		return new ArrayList<>(executionLog); // 로그 리스트의 복사본 반환
	}

	/**
	 * 시뮬레이터가 프로그램을 실행할 준비가 되었는지 확인합니다.
	 * @return 프로그램이 로드되어 실행 가능하면 true, 아니면 false.
	 */
	public boolean isReadyToRun() {
		return isProgramLoaded;
	}
}
