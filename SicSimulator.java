package SP25_simulator;

import java.util.ArrayList;
import java.util.List; // 추가

public class SicSimulator {
	ResourceManager rMgr;
	InstLuncher instLuncher;
	private boolean isProgramLoaded = false;
	private List<String> executionLog; // String 타입 명시

	public SicSimulator(ResourceManager resourceManager) {
		// ... (파일[4]과 동일) ...
		this.rMgr = resourceManager;
		this.instLuncher = new InstLuncher(this.rMgr);
		this.executionLog = new ArrayList<>(); // 타입 명시
	}

	public void programLoaded() {
		// ... (파일[4]과 동일) ...
		rMgr.setRegister(ResourceManager.REG_PC, rMgr.getFirstInstructionAddress());
		isProgramLoaded = true;
		executionLog.clear();
		addLogForGui("Program '" + rMgr.getProgramName() + "' loaded. PC: " + String.format("0x%06X", rMgr.getRegister(ResourceManager.REG_PC)));
	}

	public boolean oneStep() {
		if (!isProgramLoaded) return false;
		int pc = rMgr.getRegister(ResourceManager.REG_PC);
		// ... (PC 유효성 검사 - 파일[4]과 동일) ...
		if (pc < rMgr.getActualProgramLoadAddress() || pc >= (rMgr.getActualProgramLoadAddress() + rMgr.getProgramTotalLength())) {
			if (rMgr.getProgramTotalLength() > 0) { /* 로그 생략 (GUI에서 처리) */ }
			isProgramLoaded = false; return false;
		}

		int nextPc = instLuncher.executeInstructionAt(pc);
		if (nextPc == InstLuncher.ERROR_HALT) {
			addLogForConsole("Error at PC 0x" + String.format("%06X", pc) + ": " + instLuncher.getLastErrorMessage());
			addLogForGui("Error: " + instLuncher.getLastExecutedMnemonic()); // GUI엔 니모닉과 에러
			isProgramLoaded = false; return false;
		} else if (nextPc == InstLuncher.NORMAL_HALT) {
			addLogForConsole("Halt at PC 0x" + String.format("%06X", pc) + " by " + instLuncher.getLastExecutedMnemonic());
			addLogForGui(instLuncher.getLastExecutedMnemonic()); // GUI엔 니모닉
			isProgramLoaded = false; return false;
		} else {
			addLogForConsole(String.format("PC:0x%06X->0x%06X. %s %s", pc, nextPc, instLuncher.getLastExecutedMnemonic(), instLuncher.getLastExecutedInstructionInfo()));
			addLogForGui(instLuncher.getLastExecutedMnemonic()); // GUI엔 니모닉만
			rMgr.setRegister(ResourceManager.REG_PC, nextPc);
			return true;
		}
	}

	public void allStep() {
		// ... (파일[4]과 동일, 내부 oneStep() 호출 시 로그는 위와 같이 처리됨) ...
		if (!isProgramLoaded) return;
		int maxSteps = 100000; int stepsTaken = 0;
		while(isProgramLoaded && stepsTaken < maxSteps) { if(!oneStep()) break; stepsTaken++; }
		if (isProgramLoaded && stepsTaken >= maxSteps) { addLogForGui("Max steps reached!"); isProgramLoaded=false; }
	}

	// GUI용 로그 (니모닉 위주)와 콘솔용 상세 로그 분리
	private void addLogForGui(String log) {
		executionLog.add(log);
	}
	private void addLogForConsole(String log) {
		System.out.println("[SIM_CONSOLE_LOG] " + log);
		// GUI에도 동일한 로그를 남길지, 아니면 GUI는 addLogForGui만 사용할지 결정.
		// 여기서는 콘솔에만 상세 로그를 남기고, GUI 로그는 addLogForGui를 통해 관리.
	}

	// VisualSimulator에서 사용
	public List<String> getExecutionLog() {
		return new ArrayList<>(executionLog); // 복사본 반환
	}
	public boolean isReadyToRun() { return isProgramLoaded; }
}
