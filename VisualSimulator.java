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
	// ... (이전 답변의 멤버 변수, 생성자, initComponents, layoutComponents, addListeners, load, oneStep, allStep, updateRegisterField, logToGui, main 메소드 그대로 사용) ...
	// VisualSimulator.java의 이전 답변(첨부 파일 [6] 기반 + GUI 목표[9] 반영 버전)의 코드를 여기에 그대로 사용합니다.
	// 단, update() 메소드에서 로그를 가져오는 부분을 SicSimulator.getExecutionLog()로 수정하고,
	// 해당 로그가 이미 니모닉만 포함하도록 SicSimulator.addLogForGui()가 사용되었으므로,
	// VisualSimulator.update()의 로그 처리부는 다음과 같이 단순화될 수 있습니다:

	// VisualSimulator.update() 메소드 내 Log 처리 부분:
    /*
    logArea.setText("");
    if (sicSimulator.getExecutionLog() != null) {
        for (String guiLogEntry : sicSimulator.getExecutionLog()) { // getExecutionLog가 GUI용 로그 반환
            logArea.append(guiLogEntry + "\n");
        }
    }
    */
	// (이전 답변의 VisualSimulator.java 전체 코드를 여기에 붙여넣습니다.)
	// (이전 답변의 VisualSimulator.java는 이미 GUI 목표[9]를 최대한 반영하도록 수정되었습니다.)
	// 이전 답변의 VisualSimulator.java를 여기에 붙여넣습니다.

	ResourceManager resourceManager;
	SicLoader sicLoader;
	SicSimulator sicSimulator;

	private JButton openButton, runOneStepButton, runAllButton, exitButton;
	private JTextField fileNameField;
	private JTextField progNameFieldH, startAddrObjFieldH, progLengthFieldH;
	private JTextField firstInstAddrFieldE, startAddrMemFieldE;
	private JLabel[] regLabels = new JLabel[9];
	private JTextField[] regDecFields = new JTextField[9];
	private JTextField[] regHexFields = new JTextField[9];
	private JTextField targetAddrField;
	private JTextField instructionCodeField; // 목표 이미지처럼 한 줄 오브젝트 코드 표시
	private JTextField deviceStatusField;
	private JTextArea logArea; // 목표 이미지처럼 니모닉 리스트 표시
	private File currentObjectCodeFile = null;

	public VisualSimulator() {
		resourceManager = new ResourceManager();
		sicLoader = new SicLoader(resourceManager);
		sicSimulator = new SicSimulator(resourceManager);

		setTitle("SIC/XE Simulator (SP25_Project2)"); // PDF[7] 타이틀
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		initComponents();
		layoutComponents(); // 레이아웃은 목표 이미지[9]에 맞게
		addListeners();
		pack();
		setLocationRelativeTo(null);
		runOneStepButton.setEnabled(false);
		runAllButton.setEnabled(false);
		update(); // 초기 화면
	}

	private void initComponents() {
		fileNameField = new JTextField(15); fileNameField.setEditable(false);
		openButton = new JButton("open");

		progNameFieldH = new JTextField(6); progNameFieldH.setEditable(false); progNameFieldH.setHorizontalAlignment(JTextField.CENTER);
		startAddrObjFieldH = new JTextField(6); startAddrObjFieldH.setEditable(false); startAddrObjFieldH.setHorizontalAlignment(JTextField.CENTER);
		progLengthFieldH = new JTextField(6); progLengthFieldH.setEditable(false); progLengthFieldH.setHorizontalAlignment(JTextField.CENTER);

		firstInstAddrFieldE = new JTextField(6); firstInstAddrFieldE.setEditable(false); firstInstAddrFieldE.setHorizontalAlignment(JTextField.CENTER);
		startAddrMemFieldE = new JTextField(6); startAddrMemFieldE.setEditable(false); startAddrMemFieldE.setHorizontalAlignment(JTextField.CENTER);

		String[] regNamesShort = {"A", "X", "L", "B", "S", "T", "F", "PC", "SW"};
		int[] regConstIndices = {0, 1, 2, 3, 4, 5, 6, 8, 9}; // 실제 rMgr.register 배열 인덱스 (F는 6이지만 별도)
		for (int i = 0; i < regNamesShort.length; i++) {
			regLabels[i] = new JLabel(regNamesShort[i] + " (#" + regConstIndices[i] + ")");
			regDecFields[i] = new JTextField(5); regDecFields[i].setEditable(false); regDecFields[i].setHorizontalAlignment(JTextField.RIGHT);
			regHexFields[i] = new JTextField(6); regHexFields[i].setEditable(false); regHexFields[i].setHorizontalAlignment(JTextField.RIGHT);
		}

		targetAddrField = new JTextField(6); targetAddrField.setEditable(false); targetAddrField.setHorizontalAlignment(JTextField.RIGHT);
		instructionCodeField = new JTextField(12); instructionCodeField.setEditable(false); instructionCodeField.setFont(new Font("Monospaced", Font.PLAIN, 12));
		deviceStatusField = new JTextField(3); deviceStatusField.setEditable(false); deviceStatusField.setHorizontalAlignment(JTextField.CENTER);

		runOneStepButton = new JButton("실행(1step)");
		runAllButton = new JButton("실행 (all)");
		exitButton = new JButton("종료");

		logArea = new JTextArea(10, 12);
		logArea.setEditable(false);
		logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		DefaultCaret caret = (DefaultCaret)logArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
	}

	private void layoutComponents() { // 목표 이미지[9]와 유사한 레이아웃
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(3,3,3,3);
		gbc.anchor = GridBagConstraints.WEST;

		gbc.gridx=0; gbc.gridy=0; add(new JLabel("FileName :"), gbc);
		gbc.gridx=1; gbc.gridwidth=2; gbc.fill=GridBagConstraints.HORIZONTAL; add(fileNameField, gbc);
		gbc.gridx=3; gbc.gridwidth=1; gbc.fill=GridBagConstraints.NONE; add(openButton, gbc);

		JPanel hPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3,0));
		hPanel.setBorder(BorderFactory.createTitledBorder("H (Header Record)"));
		hPanel.add(new JLabel("Program name:")); hPanel.add(progNameFieldH);
		hPanel.add(new JLabel(" Start Address of Object Program:")); hPanel.add(startAddrObjFieldH);
		hPanel.add(new JLabel(" Length of Program:")); hPanel.add(progLengthFieldH);
		gbc.gridy=1; gbc.gridwidth=4; gbc.fill=GridBagConstraints.HORIZONTAL; add(hPanel, gbc);

		JPanel ePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3,0));
		ePanel.setBorder(BorderFactory.createTitledBorder("E (End Record)"));
		ePanel.add(new JLabel("Address of First instruction:")); ePanel.add(firstInstAddrFieldE);
		ePanel.add(new JLabel(" Start Address in Memory:")); ePanel.add(startAddrMemFieldE);
		gbc.gridy=2; add(ePanel, gbc);

		JPanel regPanel = new JPanel(new GridBagLayout());
		regPanel.setBorder(BorderFactory.createTitledBorder("Register"));
		GridBagConstraints rGbc = new GridBagConstraints();
		rGbc.anchor = GridBagConstraints.WEST; rGbc.insets = new Insets(1,3,1,3);
		rGbc.gridy=0; rGbc.gridx=1; regPanel.add(new JLabel("Dec"), rGbc);
		rGbc.gridx=2; regPanel.add(new JLabel("Hex"), rGbc);
		for(int i=0; i<regLabels.length; i++) {
			rGbc.gridy=i+1; rGbc.gridx=0; regPanel.add(regLabels[i], rGbc);
			rGbc.gridx=1; regPanel.add(regDecFields[i], rGbc);
			rGbc.gridx=2; regPanel.add(regHexFields[i], rGbc);
		}
		gbc.gridx=0; gbc.gridy=3; gbc.gridwidth=1; gbc.gridheight=3;
		gbc.fill=GridBagConstraints.VERTICAL; gbc.anchor=GridBagConstraints.NORTHWEST; add(regPanel, gbc);

		JPanel rightTopPanel = new JPanel(new GridBagLayout());
		GridBagConstraints rtGbc = new GridBagConstraints();
		rtGbc.anchor = GridBagConstraints.WEST; rtGbc.insets = new Insets(1,5,1,5);
		rtGbc.gridx=0; rtGbc.gridy=0; rightTopPanel.add(new JLabel("Target Address :"), rtGbc);
		rtGbc.gridx=1; rtGbc.fill=GridBagConstraints.HORIZONTAL; rtGbc.weightx=1.0; rightTopPanel.add(targetAddrField, rtGbc); // weightx 추가
		rtGbc.gridx=0; rtGbc.gridy=1; rtGbc.fill=GridBagConstraints.NONE; rtGbc.weightx=0; rightTopPanel.add(new JLabel("Instructions :"), rtGbc);
		rtGbc.gridx=1; rtGbc.fill=GridBagConstraints.HORIZONTAL; rtGbc.weightx=1.0; rightTopPanel.add(instructionCodeField, rtGbc);
		rtGbc.gridx=0; rtGbc.gridy=2; rtGbc.fill=GridBagConstraints.NONE; rtGbc.weightx=0; rightTopPanel.add(new JLabel("사용중인 장치"), rtGbc);
		rtGbc.gridx=1; rtGbc.fill=GridBagConstraints.HORIZONTAL; rtGbc.weightx=1.0; rightTopPanel.add(deviceStatusField, rtGbc);
		gbc.gridx=1; gbc.gridy=3; gbc.gridwidth=1; gbc.gridheight=1; // gridwidth 변경
		gbc.fill=GridBagConstraints.HORIZONTAL; gbc.anchor=GridBagConstraints.NORTHWEST; gbc.weightx=0.6; // weightx 추가
		add(rightTopPanel, gbc);

		JPanel logPanel = new JPanel(new BorderLayout()); // Log를 오른쪽 Registers 아래에 배치
		logPanel.setBorder(BorderFactory.createTitledBorder("Log"));
		logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
		logPanel.setPreferredSize(new Dimension(180, 150)); // 로그 영역 크기
		gbc.gridx=1; gbc.gridy=4; gbc.gridwidth=1; gbc.gridheight=1; // 위치 변경
		gbc.weighty=1.0; gbc.fill=GridBagConstraints.BOTH; add(logPanel, gbc);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 2));
		buttonPanel.add(runOneStepButton); buttonPanel.add(runAllButton); buttonPanel.add(exitButton);
		gbc.gridx=0; gbc.gridy=6; gbc.gridwidth=4; gbc.gridheight=1; // 맨 아래 전체 너비
		gbc.weighty=0; gbc.fill=GridBagConstraints.HORIZONTAL; gbc.anchor=GridBagConstraints.CENTER; add(buttonPanel, gbc);
	}


	private void addListeners() { /* 이전 답변과 동일 */
		openButton.addActionListener(e -> {
			JFileChooser fc = new JFileChooser("."); fc.setDialogTitle("Open Object Code");
			if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { load(fc.getSelectedFile()); }
		});
		runOneStepButton.addActionListener(e -> oneStep());
		runAllButton.addActionListener(e -> allStep());
		exitButton.addActionListener(e -> { resourceManager.closeAllDevices(); System.exit(0); });
	}

	public void load(File program) { /* 이전 답변과 유사, SicSimulator.addLogForGui 사용 */
		logToGui("");
		if (program == null) { logToGui("[Error] Program file is null."); return; }
		currentObjectCodeFile = program; fileNameField.setText(program.getName());
		try {
			sicLoader.load(program);
			sicSimulator.programLoaded(); // 이 내부에서 초기 로그 추가
			if (resourceManager.getProgramName() != null && !resourceManager.getProgramName().isEmpty()) {
				runOneStepButton.setEnabled(true); runAllButton.setEnabled(true);
				// SicSimulator의 programLoaded가 이미 GUI용 로그를 추가함.
			} else { logToGui("[Error] Load failed."); runOneStepButton.setEnabled(false); runAllButton.setEnabled(false); }
		} catch (Exception e) { logToGui("[Error] Load: " + e.getMessage()); e.printStackTrace(); }
		update();
	}

	public void oneStep() { /* SicSimulator.oneStep() 호출, update() */
		if (sicSimulator.isReadyToRun()) {
			if (!sicSimulator.oneStep()) { runOneStepButton.setEnabled(false); runAllButton.setEnabled(false); }
			update(); // SicSimulator.oneStep() 내부에서 GUI용 로그(니모닉)가 추가됨
		} else { logToGui("Program not ready/finished."); runOneStepButton.setEnabled(false); runAllButton.setEnabled(false); }
	}

	public void allStep() { /* SwingWorker 사용, update() */
		if (sicSimulator.isReadyToRun()) {
			logToGui("--- All Step Start ---");
			runOneStepButton.setEnabled(false); runAllButton.setEnabled(false);
			new SwingWorker<Void,Void>() {
				@Override protected Void doInBackground() { sicSimulator.allStep(); return null; }
				@Override protected void done() {
					try{get();}catch(Exception e){logToGui("[Error]AllStep:"+e.getMessage());e.printStackTrace();}
					update(); logToGui("--- All Step Finish ---");
				}
			}.execute();
		} else { logToGui("Program not ready/finished."); }
	}

	public void update() {
		progNameFieldH.setText(resourceManager.getProgramName());
		startAddrObjFieldH.setText(String.format("%06X", resourceManager.getHRecordObjectProgramStartAddress()));
		progLengthFieldH.setText(String.format("%06X", resourceManager.getProgramTotalLength()));
		firstInstAddrFieldE.setText(String.format("%06X", resourceManager.getFirstInstructionAddress()));
		startAddrMemFieldE.setText(String.format("%06X", resourceManager.getActualProgramLoadAddress()));

		updateRegisterField(ResourceManager.REG_A, 0, 6); updateRegisterField(ResourceManager.REG_X, 1, 6);
		updateRegisterField(ResourceManager.REG_L, 2, 6); updateRegisterField(ResourceManager.REG_B, 3, 6);
		updateRegisterField(ResourceManager.REG_S, 4, 6); updateRegisterField(ResourceManager.REG_T, 5, 6);
		double fVal = resourceManager.getRegister_F();
		regDecFields[6].setText(String.format("%.5e", fVal)); // 목표 이미지[9]는 F가 3.00000e+00 (A와 동일값?)
		regHexFields[6].setText(String.format("%06X", 0));     // 목표 이미지[9]는 F Hex 000000
		updateRegisterField(ResourceManager.REG_PC, 7, 6);
		updateRegisterField(ResourceManager.REG_SW, 8, 6);

		int lastTA = (sicSimulator.instLuncher != null) ? sicSimulator.instLuncher.getLastCalculatedTA() : InstLuncher.TA_NOT_CALCULATED_YET;
		targetAddrField.setText((lastTA != InstLuncher.TA_NOT_CALCULATED_YET) ? String.format("%06X", lastTA) : "000000");

		instructionCodeField.setText("");
		if (sicSimulator.isReadyToRun() && sicSimulator.instLuncher != null) {
			int pc = resourceManager.getRegister(ResourceManager.REG_PC);
			byte[] instBytes = sicSimulator.instLuncher.getCurrentInstructionBytes(pc);
			if (instBytes != null && instBytes.length > 0) {
				StringBuilder sb = new StringBuilder();
				for (byte b : instBytes) sb.append(String.format("%02X", b & 0xFF));
				instructionCodeField.setText(sb.toString());
			} else if (sicSimulator.isReadyToRun()){ // 프로그램은 실행 중인데 명령어 못가져오면 (종료 직전)
				instructionCodeField.setText("(halted)");
			} else { // 아예 로드 안됐거나 종료
				instructionCodeField.setText("(idle)");
			}
		}
		deviceStatusField.setText(resourceManager.getLastAccessedDeviceName());

		logArea.setText("");
		if (sicSimulator.getExecutionLog() != null) {
			for (String guiLog : sicSimulator.getExecutionLog()) { // SicSimulator가 GUI용 로그(니모닉)만 저장
				logArea.append(guiLog + "\n");
			}
		}
	}

	private void updateRegisterField(int regConst, int fieldIndex, int hexDigits) {
		int val = resourceManager.getRegister(regConst);
		regDecFields[fieldIndex].setText(Integer.toString(val));
		regHexFields[fieldIndex].setText(String.format("%0" + hexDigits + "X", val & 0xFFFFFF));
	}

	private void logToGui(String message) {
		if (logArea.getText().length() > 10000) {
			try { int end = logArea.getLineEndOffset(20); logArea.replaceRange("", 0, end); }
			catch (Exception e){logArea.setText("");}
		}
		logArea.append(message + "\n");
	}

	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
			catch (Exception e) { System.err.println("LnF Error"); }
			VisualSimulator frame = new VisualSimulator();
			frame.setVisible(true);
		});
	}
}

