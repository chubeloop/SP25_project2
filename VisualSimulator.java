package SP25_simulator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

public class VisualSimulator extends JFrame {
	ResourceManager resourceManager; // final로 선언하고 생성자에서 초기화 권장
	SicLoader sicLoader;
	SicSimulator sicSimulator;

	// GUI Components (이전 답변의 VisualSimulatorFrame 내용과 거의 동일)
	private JButton openButton, runOneStepButton, runAllButton, exitButton;
	private JTextField fileNameField;
	private JTextField progNameFieldH, startAddrObjFieldH, progLengthFieldH;
	private JTextField firstInstAddrFieldE, startAddrMemFieldE; // 실제 프로그램 시작 주소 (메모리 기준)
	private JLabel[] regLabels = new JLabel[9];
	private JTextField[] regDecFields = new JTextField[9];
	private JTextField[] regHexFields = new JTextField[9];
	private JTextField targetAddrField; // InstLuncher에서 계산된 마지막 TA 표시용
	private JTextArea instructionArea; // 메모리 뷰 또는 디스어셈블된 명령어 목록
	private JTextField deviceStatusField; // 마지막 접근 장치
	private JTextArea logArea;

	private File currentObjectCodeFile = null;

	public VisualSimulator() {
		resourceManager = new ResourceManager();
		sicLoader = new SicLoader(resourceManager);
		sicSimulator = new SicSimulator(resourceManager);

		setTitle("SIC/XE Simulator (SP25)"); // 명세서 타이틀
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		initComponents();
		layoutComponents(); // 이전 답변의 layoutComponents() 사용
		addListeners();     // 이전 답변의 addListeners() 사용
		pack();
		setLocationRelativeTo(null);
		// setResizable(false); // 크기 조절 가능하게 두는 것이 좋을 수 있음

		// 초기 버튼 상태: 로드 전에는 실행 버튼 비활성화
		runOneStepButton.setEnabled(false);
		runAllButton.setEnabled(false);
		update(); // 초기 빈 화면 업데이트
	}

	private void initComponents() {
		// FileName & Open Button
		fileNameField = new JTextField(20); fileNameField.setEditable(false);
		openButton = new JButton("Open Object File"); // 버튼 텍스트 명확히

		// H (Header Record)
		progNameFieldH = new JTextField(8); progNameFieldH.setEditable(false); progNameFieldH.setHorizontalAlignment(JTextField.CENTER);
		startAddrObjFieldH = new JTextField(8); startAddrObjFieldH.setEditable(false);startAddrObjFieldH.setHorizontalAlignment(JTextField.CENTER);
		progLengthFieldH = new JTextField(8); progLengthFieldH.setEditable(false);progLengthFieldH.setHorizontalAlignment(JTextField.CENTER);

		// E (End Record) / Effective Start
		firstInstAddrFieldE = new JTextField(8); firstInstAddrFieldE.setEditable(false);firstInstAddrFieldE.setHorizontalAlignment(JTextField.CENTER);
		startAddrMemFieldE = new JTextField(8); startAddrMemFieldE.setEditable(false); startAddrMemFieldE.setHorizontalAlignment(JTextField.CENTER);


		// Register
		String[] regNames = {"A (#0)", "X (#1)", "L (#2)", "B (#3)", "S (#4)", "T (#5)", "F (#6)", "PC (#8)", "SW (#9)"};
		for (int i = 0; i < regNames.length; i++) {
			regLabels[i] = new JLabel(regNames[i]);
			regDecFields[i] = new JTextField(7); regDecFields[i].setEditable(false); regDecFields[i].setHorizontalAlignment(JTextField.RIGHT);
			regHexFields[i] = new JTextField(7); regHexFields[i].setEditable(false); regHexFields[i].setHorizontalAlignment(JTextField.RIGHT);
		}

		// Target Address
		targetAddrField = new JTextField(8); targetAddrField.setEditable(false); targetAddrField.setHorizontalAlignment(JTextField.RIGHT);

		// Instructions (Memory Area)
		instructionArea = new JTextArea(15, 40); // 행/열 크기 조정
		instructionArea.setEditable(false);
		instructionArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

		// 사용중인 장치
		deviceStatusField = new JTextField(10); deviceStatusField.setEditable(false);

		// 실행 버튼
		runOneStepButton = new JButton("Execute 1 Step");
		runAllButton = new JButton("Execute All Steps");
		exitButton = new JButton("Exit Program");

		// Log
		logArea = new JTextArea(10, 60); // 행/열 크기 조정
		logArea.setEditable(false);
		logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		DefaultCaret caret = (DefaultCaret) logArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); // 자동 스크롤
	}


	private void layoutComponents() { // 상세 레이아웃은 GUI 예시 그림 참고하여 GridBagLayout 등으로 구성
		setLayout(new BorderLayout(10, 10));
		((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

		// Top Panel: File Open
		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		topPanel.add(new JLabel("Object Code File:"));
		topPanel.add(fileNameField);
		topPanel.add(openButton);
		add(topPanel, BorderLayout.NORTH);

		// Center Panel: Left (H, E, Registers) and Right (Target, Instructions, Device, Buttons)
		JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 0)); // 좌우 분할

		// Left Panel
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

		JPanel hRecordPanel = new JPanel(new GridLayout(0, 2, 5, 2));
		hRecordPanel.setBorder(BorderFactory.createTitledBorder("H (Header Record)"));
		hRecordPanel.add(new JLabel("Program name:")); hRecordPanel.add(progNameFieldH);
		hRecordPanel.add(new JLabel("Start Address (Obj):")); hRecordPanel.add(startAddrObjFieldH);
		hRecordPanel.add(new JLabel("Length of Program (Hex):")); hRecordPanel.add(progLengthFieldH);
		leftPanel.add(hRecordPanel);

		JPanel eRecordPanel = new JPanel(new GridLayout(0, 2, 5, 2));
		eRecordPanel.setBorder(BorderFactory.createTitledBorder("E (End Record) / Effective Start"));
		eRecordPanel.add(new JLabel("First Instruction Addr (E):")); eRecordPanel.add(firstInstAddrFieldE);
		eRecordPanel.add(new JLabel("Actual Load Addr (Mem):")); eRecordPanel.add(startAddrMemFieldE);
		leftPanel.add(eRecordPanel);

		JPanel registerPanel = new JPanel(new GridBagLayout());
		registerPanel.setBorder(BorderFactory.createTitledBorder("Registers"));
		GridBagConstraints rGbc = new GridBagConstraints();
		rGbc.anchor = GridBagConstraints.WEST; rGbc.insets = new Insets(1, 3, 1, 3);
		rGbc.gridy = 0; rGbc.gridx = 1; registerPanel.add(new JLabel("Decimal"), rGbc);
		rGbc.gridx = 2; registerPanel.add(new JLabel("Hex (24b)"), rGbc);
		for (int i = 0; i < regLabels.length; i++) {
			rGbc.gridy = i + 1;
			rGbc.gridx = 0; registerPanel.add(regLabels[i], rGbc);
			rGbc.gridx = 1; registerPanel.add(regDecFields[i], rGbc);
			rGbc.gridx = 2; registerPanel.add(regHexFields[i], rGbc);
		}
		leftPanel.add(registerPanel);
		leftPanel.add(Box.createVerticalGlue()); // 남는 공간 채우기
		centerPanel.add(leftPanel);

		// Right Panel
		JPanel rightPanel = new JPanel(new BorderLayout(5,5));

		JPanel rightTopInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		rightTopInfoPanel.add(new JLabel("Target Addr:"));
		rightTopInfoPanel.add(targetAddrField);
		rightTopInfoPanel.add(new JLabel("Device:"));
		rightTopInfoPanel.add(deviceStatusField);
		rightPanel.add(rightTopInfoPanel, BorderLayout.NORTH);

		JPanel instructionPanel = new JPanel(new BorderLayout());
		instructionPanel.setBorder(BorderFactory.createTitledBorder("Instructions (Memory View)"));
		JScrollPane instructionScrollPane = new JScrollPane(instructionArea);
		instructionScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		instructionPanel.add(instructionScrollPane, BorderLayout.CENTER);
		rightPanel.add(instructionPanel, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.add(runOneStepButton);
		buttonPanel.add(runAllButton);
		buttonPanel.add(exitButton);
		rightPanel.add(buttonPanel, BorderLayout.SOUTH);
		centerPanel.add(rightPanel);

		add(centerPanel, BorderLayout.CENTER);

		// Log Panel
		JPanel logPanel = new JPanel(new BorderLayout());
		logPanel.setBorder(BorderFactory.createTitledBorder("Log (Execution Trace)"));
		JScrollPane logScrollPane = new JScrollPane(logArea);
		logPanel.add(logScrollPane, BorderLayout.CENTER);
		logPanel.setPreferredSize(new Dimension(100, 150)); // 높이 조절
		add(logPanel, BorderLayout.SOUTH);
	}


	private void addListeners() {
		openButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser("."); // 현재 디렉토리에서 시작
				fileChooser.setDialogTitle("Open SIC/XE Object Code File");
				int result = fileChooser.showOpenDialog(VisualSimulator.this);
				if (result == JFileChooser.APPROVE_OPTION) {
					currentObjectCodeFile = fileChooser.getSelectedFile();
					fileNameField.setText(currentObjectCodeFile.getName());
					// VisualSimulator의 load 호출 (내부적으로 sicLoader, sicSimulator.programLoaded 호출)
					load(currentObjectCodeFile);
				}
			}
		});

		runOneStepButton.addActionListener(e -> oneStep());
		runAllButton.addActionListener(e -> allStep()); // SwingWorker 사용 권장
		exitButton.addActionListener(e -> {
			resourceManager.closeAllDevices(); // 모든 장치 닫기
			System.exit(0);
		});
	}

	/**
	 * 프로그램 로드 명령을 전달한다.
	 */
	public void load(File program) { // VisualSimulator의 멤버 메서드
		logArea.setText(""); // 로그 초기화
		if (program == null) {
			log("[Error] Program file is null.");
			return;
		}
		this.currentObjectCodeFile = program;
		this.fileNameField.setText(program.getName());

		try {
			// 1. ResourceManager 초기화 (SicLoader.load 전에 수행)
			// resourceManager.initializeResource(); // SicLoader.load() 내부에서 호출됨

			// 2. SicLoader를 통해 목적 코드 로드
			sicLoader.load(program); // 이 내부에서 rMgr.initializeResource() 호출

			// 3. SicSimulator에 로드 완료 알림 (PC 설정 등)
			sicSimulator.programLoaded();

			if (resourceManager.getProgramName() != null && !resourceManager.getProgramName().isEmpty()) {
				runOneStepButton.setEnabled(true);
				runAllButton.setEnabled(true);
				log("Program '" + program.getName() + "' loaded successfully by SicLoader.");
				log("Simulator initialized. PC is at " + String.format("0x%06X", resourceManager.getRegister(ResourceManager.REG_PC)));
			} else {
				log("[Error] Failed to load program '" + program.getName() + "'. Check loader output (console).");
				runOneStepButton.setEnabled(false);
				runAllButton.setEnabled(false);
			}
		} catch (Exception e) {
			log("[Error] During program loading: " + e.getMessage());
			e.printStackTrace(System.err); // 콘솔에 상세 스택 트레이스 출력
			runOneStepButton.setEnabled(false);
			runAllButton.setEnabled(false);
		}
		update(); // 로드 후 화면 갱신
	}

	/**
	 * 하나의 명령어만 수행할 것을 SicSimulator에 요청한다.
	 */
	public void oneStep() { // VisualSimulator의 멤버 메서드
		if (sicSimulator.isReadyToRun()) {
			if (!sicSimulator.oneStep()) { // oneStep이 false 반환 시 (종료 또는 오류)
				runOneStepButton.setEnabled(false);
				runAllButton.setEnabled(false);
				log("Execution finished or halted by oneStep. Final PC: " + String.format("0x%06X", resourceManager.getRegister(ResourceManager.REG_PC)));
			}
			update(); // 한 스텝 실행 후 화면 갱신
		} else {
			log("Program not ready or already finished. Cannot execute oneStep.");
			runOneStepButton.setEnabled(false); // 실행 불가 상태면 버튼 비활성화
			runAllButton.setEnabled(false);
		}
	}

	/**
	 * 남아있는 모든 명령어를 수행할 것을 SicSimulator에 요청한다.
	 */
	public void allStep() { // VisualSimulator의 멤버 메서드
		if (sicSimulator.isReadyToRun()) {
			log("Starting allStep execution...");
			runOneStepButton.setEnabled(false); // 실행 중에는 1스텝 버튼 비활성화
			runAllButton.setEnabled(false);     // 실행 중에는 all스텝 버튼 비활성화

			new SwingWorker<Void, String>() {
				@Override
				protected Void doInBackground() throws Exception {
					sicSimulator.allStep(); // 백그라운드에서 실행
					return null;
				}

				@Override
				protected void done() {
					try {
						get(); // 예외가 발생했으면 여기서 처리
					} catch (Exception e) {
						log("[Error] Exception during allStep execution: " + e.getMessage());
						e.printStackTrace(System.err);
					}
					update(); // 모든 스텝 실행 후 화면 갱신
					// allStep 후에는 보통 프로그램이 종료되므로 버튼은 비활성화 유지
					// runOneStepButton.setEnabled(sicSimulator.isReadyToRun());
					// runAllButton.setEnabled(sicSimulator.isReadyToRun());
					log("AllStep execution finished or halted.");
				}
			}.execute();
		} else {
			log("Program not ready or already finished. Cannot execute allStep.");
		}
	}


	/**
	 * 화면을 최신값으로 갱신하는 역할을 수행한다.
	 */
	public void update() { // VisualSimulator의 멤버 메서드
		// H 레코드 정보
		progNameFieldH.setText(resourceManager.getProgramName());
		startAddrObjFieldH.setText(String.format("%06X", resourceManager.getHRecordObjectProgramStartAddress()));
		progLengthFieldH.setText(String.format("%06X", resourceManager.getProgramTotalLength()));

		// E 레코드 정보 / 실제 시작 주소
		firstInstAddrFieldE.setText(String.format("%06X", resourceManager.getFirstInstructionAddress()));
		startAddrMemFieldE.setText(String.format("%06X", resourceManager.getActualProgramLoadAddress()));

		// 레지스터 값
		String[] regHexFormat = {"%06X", "%06X", "%06X", "%06X", "%06X", "%06X", "%012X", "%06X", "%06X"}; // F는 48비트(12 hex)
		int[] regConsts = {
				ResourceManager.REG_A, ResourceManager.REG_X, ResourceManager.REG_L,
				ResourceManager.REG_B, ResourceManager.REG_S, ResourceManager.REG_T,
				-1, /* F는 별도 처리 */ ResourceManager.REG_PC, ResourceManager.REG_SW
		};

		for (int i = 0; i < regLabels.length; i++) {
			if (i == 6) { // F 레지스터 (인덱스 6)
				double fVal = resourceManager.getRegister_F();
				regDecFields[i].setText(String.format("%.6e", fVal)); // 지수형 10진수
				// SIC/XE F는 48비트. long으로 변환 후 16진수 12자리 표시
				long fBits = Double.doubleToLongBits(fVal); // 실제로는 SIC/XE 48비트 변환 필요
				regHexFields[i].setText(String.format("%012X", fBits).substring(0,12)); // 상위 12자리만 표시 (임시)
			} else {
				int val = resourceManager.getRegister(regConsts[i]);
				regDecFields[i].setText(Integer.toString(val));
				regHexFields[i].setText(String.format(regHexFormat[i], val));
			}
		}

		// Target Address
		if (sicSimulator.instLuncher != null) { // instLuncher가 null일 수 있음
			int lastTA = sicSimulator.instLuncher.getLastCalculatedTA();
			if (lastTA != InstLuncher.TA_NOT_CALCULATED_YET) { // TA가 계산된 경우
				targetAddrField.setText(String.format("%06X", lastTA));
			} else {
				targetAddrField.setText("------"); // 아직 계산 안됨
			}
		}


		// Instructions (Memory Area) - 간단한 메모리 덤프 (16진수)
		StringBuilder memContent = new StringBuilder();
		int currentPCForHighlight = resourceManager.getRegister(ResourceManager.REG_PC);
		int memViewStart = Math.max(0, currentPCForHighlight - (16*4)); // PC 주변 4줄 위부터
		memViewStart = (memViewStart / 16) * 16; // 16바이트 정렬

		int linesToDisplay = 15; // 화면에 표시할 메모리 라인 수
		int bytesPerLine = 16;

		instructionArea.setText(""); // 이전 내용 지우기
		for (int line = 0; line < linesToDisplay; line++) {
			int lineStartAddr = memViewStart + line * bytesPerLine;
			if (lineStartAddr >= resourceManager.memory.length) break;

			memContent.append(String.format("%05X0: ", lineStartAddr / 16)); // 주소 표시 (XXXXX0 형태)
			for (int offset = 0; offset < bytesPerLine; offset++) {
				int addr = lineStartAddr + offset;
				if (addr < resourceManager.memory.length) {
					if (addr >= resourceManager.getActualProgramLoadAddress() && addr < resourceManager.getActualProgramLoadAddress() + resourceManager.getProgramTotalLength()){
						memContent.append(resourceManager.getMemoryByteHex(addr));
					} else {
						memContent.append(".."); // 프로그램 범위 밖은 .. 으로 표시
					}
				} else {
					memContent.append("  "); // 메모리 범위 밖
				}
				if ((offset + 1) % 4 == 0) memContent.append("  "); // 4바이트마다 큰 공백
				else memContent.append(" ");
			}
			// ASCII 표현 (선택적)
			// memContent.append("  |  ");
			// for (int offset = 0; offset < bytesPerLine; offset++) { ... }
			memContent.append("\n");
		}
		instructionArea.setText(memContent.toString());
		instructionArea.setCaretPosition(0); // 맨 위로 스크롤

		// "사용중인 장치"
		deviceStatusField.setText(resourceManager.getLastAccessedDeviceName().isEmpty() ? "N/A" : resourceManager.getLastAccessedDeviceName());

		// Log (명령어 수행 관련)
		StringBuilder logBuilder = new StringBuilder();
		if (sicSimulator.getExecutionLog() != null) {
			List<String> logs = sicSimulator.getExecutionLog();
			// 최근 로그만 표시 (예: 마지막 20개)
			int logStart = Math.max(0, logs.size() - 20);
			for (int i = logStart; i < logs.size(); i++) {
				logBuilder.append(logs.get(i)).append("\n");
			}
		}
		logArea.setText(logBuilder.toString());
		if (logArea.getDocument().getLength() > 0) { // 자동 스크롤
			logArea.setCaretPosition(logArea.getDocument().getLength());
		}
	}

	private void log(String message) { // VisualSimulator 내부 로그 추가용
		if (logArea.getText().length() > 20000) { // 로그 너무 길어지면 일부 삭제 (앞부분)
			try {
				int end = logArea.getLineEndOffset(50); // 약 50줄 삭제
				logArea.replaceRange("", 0, end);
			} catch (Exception e) { logArea.setText("");}
		}
		logArea.append(message + "\n");
		System.out.println("[GUI_LOG] " + message); // 콘솔에도 출력 (디버깅용)
	}


	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					// 시스템 기본 LookAndFeel 사용 (더 나은 UI)
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					System.err.println("Warning: Could not set system LookAndFeel.");
				}
				VisualSimulator frame = new VisualSimulator();
				frame.setVisible(true);
			}
		});
	}
}
