package SP25_simulator;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Highlighter;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

// 명령어 목록 표시 및 현재 PC 포인팅을 위한 간단한 내부 클래스
class InstructionDisplayItem {
	int startAddress;       // 이 명령어가 메모리에서 시작하는 실제 주소
	String objectCodeHex;   // 표시될 오브젝트 코드 문자열
	int originalLineNumber; // JTextArea에서의 원래 줄 번호 (하이라이트용)

	public InstructionDisplayItem(int startAddress, String objectCodeHex, int originalLineNumber) {
		this.startAddress = startAddress;
		this.objectCodeHex = objectCodeHex;
		this.originalLineNumber = originalLineNumber;
	}
}

public class VisualSimulator extends JFrame {
	ResourceManager resourceManager;
	SicLoader sicLoader;
	SicSimulator sicSimulator;
	private File currentObjectCodeFile = null;

	private JButton openButton, runOneStepButton, runAllButton, exitButton;
	private JTextField fileNameField;
	private JTextField progNameFieldH, startAddrObjFieldH, progLengthFieldH;
	private JTextField firstInstAddrFieldE, startAddrMemFieldE;
	private JLabel[] regLabels = new JLabel[9];
	private JTextField[] regDecFields = new JTextField[9];
	private JTextField[] regHexFields = new JTextField[9];
	private JTextField targetAddrField;
	private JTextArea instructionCodeArea;
	private JTextField deviceStatusField;
	private JTextArea logArea;

	// 명령어 목록 및 하이라이트 관련 필드
	private List<InstructionDisplayItem> instructionDisplayList;
	private Highlighter.HighlightPainter currentPcHighlightPainter;
	private Object lastHighlightTag = null;

	public VisualSimulator() {
		resourceManager = new ResourceManager();
		sicLoader = new SicLoader(resourceManager);
		sicSimulator = new SicSimulator(resourceManager);
		instructionDisplayList = new ArrayList<>();
		currentPcHighlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW); // 하이라이트 색상

		setTitle("SIC/XE Simulator (SP25_Project2)");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		initComponents();
		layoutComponents();
		addListeners();
		pack();
		setMinimumSize(getPreferredSize());
		setLocationRelativeTo(null);
		runOneStepButton.setEnabled(false);
		runAllButton.setEnabled(false);
		update();
	}

	private void initComponents() {
		// ... (이전과 동일) ...
		fileNameField = new JTextField(20); fileNameField.setEditable(false); openButton = new JButton("open");
		progNameFieldH = new JTextField(6); progNameFieldH.setEditable(false); progNameFieldH.setHorizontalAlignment(JTextField.CENTER);
		startAddrObjFieldH = new JTextField(6); startAddrObjFieldH.setEditable(false); startAddrObjFieldH.setHorizontalAlignment(JTextField.CENTER);
		progLengthFieldH = new JTextField(6); progLengthFieldH.setEditable(false); progLengthFieldH.setHorizontalAlignment(JTextField.CENTER);
		firstInstAddrFieldE = new JTextField(6); firstInstAddrFieldE.setEditable(false); firstInstAddrFieldE.setHorizontalAlignment(JTextField.CENTER);
		startAddrMemFieldE = new JTextField(6); startAddrMemFieldE.setEditable(false); startAddrMemFieldE.setHorizontalAlignment(JTextField.CENTER);
		String[] regNamesForLabels = {"A (#0)", "X (#1)", "L (#2)", "B (#3)", "S (#4)", "T (#5)", "F (#6)", "PC (#8)", "SW (#9)"};
		for (int i = 0; i < regLabels.length; i++) { regLabels[i] = new JLabel(regNamesForLabels[i]); regDecFields[i] = new JTextField(7); regDecFields[i].setEditable(false); regDecFields[i].setHorizontalAlignment(JTextField.RIGHT); regHexFields[i] = new JTextField(6); regHexFields[i].setEditable(false); regHexFields[i].setHorizontalAlignment(JTextField.RIGHT); }
		targetAddrField = new JTextField(6); targetAddrField.setEditable(false); targetAddrField.setHorizontalAlignment(JTextField.RIGHT);

		instructionCodeArea = new JTextArea(15, 10);
		instructionCodeArea.setEditable(false); instructionCodeArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		instructionCodeArea.setLineWrap(false);
		DefaultCaret instructionCaret = (DefaultCaret)instructionCodeArea.getCaret();
		instructionCaret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

		deviceStatusField = new JTextField(4); deviceStatusField.setEditable(false); deviceStatusField.setHorizontalAlignment(JTextField.CENTER);
		runOneStepButton = new JButton("실행(1step)"); runAllButton = new JButton("실행 (all)"); exitButton = new JButton("종료");
		logArea = new JTextArea(10, 15); logArea.setEditable(false); logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		DefaultCaret logCaret = (DefaultCaret)logArea.getCaret(); logCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
	}

	private void layoutComponents() { /* 이전과 동일 */
		setLayout(new GridBagLayout()); GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(3,5,3,5); gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 0; gbc.gridy = 0; add(new JLabel("FileName :"), gbc); gbc.gridx = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; add(fileNameField, gbc); gbc.gridx = 3; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; add(openButton, gbc);
		JPanel hPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5,2)); hPanel.setBorder(BorderFactory.createTitledBorder("H (Header Record)")); hPanel.add(new JLabel("Program name:")); hPanel.add(progNameFieldH); hPanel.add(new JLabel("Start Address(obj):")); hPanel.add(startAddrObjFieldH); hPanel.add(new JLabel("Length:")); hPanel.add(progLengthFieldH); gbc.gridy = 1; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.HORIZONTAL; add(hPanel, gbc);
		JPanel ePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5,2)); ePanel.setBorder(BorderFactory.createTitledBorder("E (End Record)")); ePanel.add(new JLabel("First instruction Addr:")); ePanel.add(firstInstAddrFieldE); ePanel.add(new JLabel("Start Address in Memory:")); ePanel.add(startAddrMemFieldE); gbc.gridy = 2; add(ePanel, gbc);
		JPanel leftPanel = new JPanel(new BorderLayout()); JPanel regPanel = new JPanel(new GridBagLayout()); regPanel.setBorder(BorderFactory.createTitledBorder("Register")); GridBagConstraints rGbc = new GridBagConstraints(); rGbc.anchor = GridBagConstraints.WEST; rGbc.insets = new Insets(1,3,1,3); rGbc.gridy = 0; rGbc.gridx = 1; regPanel.add(new JLabel("Dec"), rGbc); rGbc.gridx = 2; regPanel.add(new JLabel("Hex"), rGbc); for(int i=0; i<regLabels.length; i++) { rGbc.gridy = i+1; rGbc.gridx = 0; rGbc.fill = GridBagConstraints.HORIZONTAL; regPanel.add(regLabels[i], rGbc); rGbc.gridx = 1; rGbc.fill = GridBagConstraints.NONE; regPanel.add(regDecFields[i], rGbc); rGbc.gridx = 2; regPanel.add(regHexFields[i], rGbc); } leftPanel.add(regPanel, BorderLayout.NORTH); gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.gridheight = 2; gbc.fill = GridBagConstraints.VERTICAL; gbc.anchor = GridBagConstraints.NORTHWEST; gbc.weightx = 0.3; add(leftPanel, gbc);
		JPanel rightPanel = new JPanel(new GridBagLayout()); GridBagConstraints rpGbc = new GridBagConstraints(); rpGbc.fill = GridBagConstraints.HORIZONTAL; rpGbc.anchor = GridBagConstraints.NORTHWEST; rpGbc.insets = new Insets(2,2,2,2); rpGbc.weightx = 1.0;
		rpGbc.gridx=0; rpGbc.gridy=0; rpGbc.gridwidth=1; rpGbc.fill = GridBagConstraints.NONE; rpGbc.anchor = GridBagConstraints.EAST; rightPanel.add(new JLabel("Target Address :"), rpGbc); rpGbc.gridx=1; rpGbc.gridwidth=1; rpGbc.fill = GridBagConstraints.HORIZONTAL; rpGbc.anchor = GridBagConstraints.WEST; rightPanel.add(targetAddrField, rpGbc);
		rpGbc.gridx=0; rpGbc.gridy=1; rpGbc.gridwidth=1; rpGbc.fill = GridBagConstraints.NONE; rpGbc.anchor = GridBagConstraints.NORTHEAST; rightPanel.add(new JLabel("Instructions :"), rpGbc); JScrollPane instructionScrollPane = new JScrollPane(instructionCodeArea); instructionScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED); instructionScrollPane.setPreferredSize(new Dimension(180, 120)); rpGbc.gridx=1; rpGbc.gridy=1; rpGbc.gridwidth=1; rpGbc.fill = GridBagConstraints.BOTH; rpGbc.weighty=0.5; rightPanel.add(instructionScrollPane, rpGbc);
		rpGbc.gridx=0; rpGbc.gridy=2; rpGbc.gridwidth=1; rpGbc.fill = GridBagConstraints.NONE; rpGbc.anchor = GridBagConstraints.EAST; rpGbc.weighty=0; rightPanel.add(new JLabel("사용중인 장치:"), rpGbc); rpGbc.gridx=1; rpGbc.gridy=2; rpGbc.gridwidth=1; rpGbc.fill = GridBagConstraints.HORIZONTAL; rpGbc.anchor = GridBagConstraints.WEST; rightPanel.add(deviceStatusField, rpGbc);
		JPanel logPanelContainer = new JPanel(new BorderLayout()); logPanelContainer.setBorder(BorderFactory.createTitledBorder("Log")); JScrollPane logScrollPane = new JScrollPane(logArea); logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED); logPanelContainer.add(logScrollPane, BorderLayout.CENTER); rpGbc.gridx=0; rpGbc.gridy=3; rpGbc.gridwidth=2; rpGbc.fill = GridBagConstraints.BOTH; rpGbc.weighty=0.5; rightPanel.add(logPanelContainer, rpGbc);
		gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 3; gbc.gridheight = 2; gbc.fill = GridBagConstraints.BOTH; gbc.anchor = GridBagConstraints.NORTHWEST; gbc.weightx = 0.7; gbc.weighty = 1.0; add(rightPanel, gbc);
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5)); buttonPanel.add(runOneStepButton); buttonPanel.add(runAllButton); buttonPanel.add(exitButton); gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 4; gbc.gridheight = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.CENTER; gbc.weightx = 1.0; gbc.weighty = 0; add(buttonPanel, gbc);
	}
	private void addListeners() { /* 이전과 동일 */
		openButton.addActionListener(e -> {JFileChooser fc=new JFileChooser(".");fc.setDialogTitle("Open SIC/XE Object Code File");if(fc.showOpenDialog(VisualSimulator.this)==JFileChooser.APPROVE_OPTION){load(fc.getSelectedFile());}});
		runOneStepButton.addActionListener(e -> oneStep()); runAllButton.addActionListener(e -> allStep());
		exitButton.addActionListener(e -> {resourceManager.closeAllDevices();System.exit(0);});
	}

	public void load(File program) {
		logToGui("");
		instructionCodeArea.setText("");
		instructionDisplayList.clear(); // 새 파일 로드 시 이전 목록 초기화
		removeCurrentPcHighlight(); // 이전 하이라이트 제거

		if (program == null) { logToGui("[Error] Program file is null."); return; }
		currentObjectCodeFile = program; fileNameField.setText(program.getName());
		try {
			resourceManager.initializeResource();
			sicLoader.load(program);
			sicSimulator.programLoaded();

			if (resourceManager.getProgramName() != null && !resourceManager.getProgramName().isEmpty()) {
				runOneStepButton.setEnabled(true); runAllButton.setEnabled(true);
				List<String> initLogs = sicSimulator.getExecutionLog();
				if(!initLogs.isEmpty()) { for(String logEntry : initLogs) { logToGui(logEntry); } }
				else { logToGui("Program '" + program.getName() + "' loaded. PC: " + String.format("0x%06X", resourceManager.getRegister(ResourceManager.REG_PC))); }

				StringBuilder instructionsDisplayText = new StringBuilder();
				List<MemoryRegion> loadedRegions = resourceManager.getTRecordLoadedRegions();
				int currentLineNumberForHighlight = 0;

				if (loadedRegions.isEmpty() && resourceManager.getProgramTotalLength() > 0) {
					instructionCodeArea.setText("(No T-records with content or T-regions not registered)");
				} else {
					for (MemoryRegion region : loadedRegions) {
						byte[] regionMemoryBytes = resourceManager.getMemoryBytes(region.getStartAddress(), region.getLength());
						if (regionMemoryBytes.length == 0) continue;

						int currentOffsetInRegion = 0;
						while (currentOffsetInRegion < regionMemoryBytes.length) {
							int actualMemoryAddressForThisInstruction = region.getStartAddress() + currentOffsetInRegion;
							int bytesAvailableToPeek = regionMemoryBytes.length - currentOffsetInRegion;
							int bytesToPeek = Math.min(4, bytesAvailableToPeek);

							if (bytesToPeek <= 0) break;

							byte[] instructionPrefixBytes = new byte[bytesToPeek];
							System.arraycopy(regionMemoryBytes, currentOffsetInRegion, instructionPrefixBytes, 0, bytesToPeek);

							int instructionLen = InstLuncher.getInstructionLengthFromBytes(instructionPrefixBytes);

							if (instructionLen == 0) {
								if (bytesAvailableToPeek > 0) {
									currentOffsetInRegion += 1;
									continue;
								}
								break;
							}

							if (currentOffsetInRegion + instructionLen > regionMemoryBytes.length) {
								break;
							}

							StringBuilder currentInstructionHex = new StringBuilder();
							for (int k = 0; k < instructionLen; k++) {
								currentInstructionHex.append(String.format("%02X", regionMemoryBytes[currentOffsetInRegion + k] & 0xFF));
							}
							String hexString = currentInstructionHex.toString();
							instructionsDisplayText.append(hexString).append("\n");
							instructionDisplayList.add(new InstructionDisplayItem(actualMemoryAddressForThisInstruction, hexString, currentLineNumberForHighlight));
							currentLineNumberForHighlight++;

							currentOffsetInRegion += instructionLen;
						}
					}
				}
				instructionCodeArea.setText(instructionsDisplayText.toString());
				instructionCodeArea.setCaretPosition(0);
				// 로드 후 첫 PC 위치 하이라이트
				highlightCurrentPc();

			} else { logToGui("[Error] Failed to load program details."); runOneStepButton.setEnabled(false); runAllButton.setEnabled(false); }
		} catch (Exception e) { logToGui("[Error] Load: " + e.getMessage()); e.printStackTrace(System.err); runOneStepButton.setEnabled(false); runAllButton.setEnabled(false); }
		update(); // update는 레지스터 값 등을 표시하므로 load 후 호출
	}

	private void highlightCurrentPc() {
		removeCurrentPcHighlight(); // 이전 하이라이트 제거
		int currentPc = resourceManager.getRegister(ResourceManager.REG_PC);
		for (InstructionDisplayItem item : instructionDisplayList) {
			if (item.startAddress == currentPc) {
				try {
					int start = instructionCodeArea.getLineStartOffset(item.originalLineNumber);
					int end = instructionCodeArea.getLineEndOffset(item.originalLineNumber);
					if (end > start) end--; // 개행 문자 제외
					lastHighlightTag = instructionCodeArea.getHighlighter().addHighlight(start, end, currentPcHighlightPainter);
					instructionCodeArea.setCaretPosition(start); // 캐럿도 해당 위치로 이동
					instructionCodeArea.scrollRectToVisible(instructionCodeArea.modelToView(start)); // 해당 줄이 보이도록 스크롤
				} catch (Exception e) {
					// System.err.println("Highlighting error: " + e.getMessage());
				}
				break;
			}
		}
	}

	private void removeCurrentPcHighlight() {
		if (lastHighlightTag != null) {
			instructionCodeArea.getHighlighter().removeHighlight(lastHighlightTag);
			lastHighlightTag = null;
		}
	}

	public void oneStep() {
		if (sicSimulator.isReadyToRun()) {
			if (!sicSimulator.oneStep()) {
				runOneStepButton.setEnabled(false);
				runAllButton.setEnabled(false);
			}
			update(); // 레지스터 등 GUI 업데이트
			highlightCurrentPc(); // PC 변경 후 하이라이트 업데이트
		} else {
			logToGui("Program not ready/finished.");
			runOneStepButton.setEnabled(false);
			runAllButton.setEnabled(false);
		}
	}

	public void allStep() {
		if (sicSimulator.isReadyToRun()) {
			logToGui("--- Starting All Step ---");
			runOneStepButton.setEnabled(false); // 실행 중에는 버튼 비활성화
			runAllButton.setEnabled(false);

			new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() throws Exception {
					while(sicSimulator.isReadyToRun()){
						if (!sicSimulator.oneStep()) break;
						// 각 스텝 후 GUI 업데이트 및 하이라이트를 Event Dispatch Thread에서 처리
						SwingUtilities.invokeLater(() -> {
							update();
							highlightCurrentPc();
						});
						if (isCancelled()) break;
						// Thread.sleep(50); // 너무 빠르면 GUI 업데이트가 안보일 수 있으므로 약간의 딜레이 (선택적)
					}
					return null;
				}
				@Override
				protected void done() {
					try { get(); } catch (Exception e) { logToGui("[Error]AllStep:"+e.getMessage());e.printStackTrace(System.err); }
					// 최종 상태 업데이트
					SwingUtilities.invokeLater(() -> {
						update();
						highlightCurrentPc();
						if (!sicSimulator.isReadyToRun()) {
							runOneStepButton.setEnabled(false);
							runAllButton.setEnabled(false);
						} else {
							runOneStepButton.setEnabled(true); // 아직 실행 가능하면 버튼 다시 활성화
							runAllButton.setEnabled(true);
						}
						logToGui("--- All Step Finished ---");
					});
				}
			}.execute();
		} else { logToGui("Program not ready/finished."); }
	}

	public void update() {
		// ... (기존 update 내용 동일) ...
		progNameFieldH.setText(resourceManager.getProgramName()); startAddrObjFieldH.setText(String.format("%06X", resourceManager.getHRecordObjectProgramStartAddress())); progLengthFieldH.setText(String.format("%06X", resourceManager.getProgramTotalLength()));
		firstInstAddrFieldE.setText(String.format("%06X", resourceManager.getFirstInstructionAddress())); startAddrMemFieldE.setText(String.format("%06X", resourceManager.getActualProgramLoadAddress()));
		updateRegisterField(ResourceManager.REG_A, 0, 6); updateRegisterField(ResourceManager.REG_X, 1, 6); updateRegisterField(ResourceManager.REG_L, 2, 6); updateRegisterField(ResourceManager.REG_B, 3, 6); updateRegisterField(ResourceManager.REG_S, 4, 6); updateRegisterField(ResourceManager.REG_T, 5, 6);
		double fVal = resourceManager.getRegister_F(); regDecFields[6].setText(String.format("%.5e", fVal)); regHexFields[6].setText(String.format("%012X", Double.doubleToRawLongBits(fVal)).substring(0,12));
		updateRegisterField(ResourceManager.REG_PC, 7, 6); updateRegisterField(ResourceManager.REG_SW, 8, 6);
		int lastTA = (sicSimulator.instLuncher != null) ? sicSimulator.instLuncher.getLastCalculatedTA() : InstLuncher.TA_NOT_CALCULATED_YET; targetAddrField.setText((lastTA != InstLuncher.TA_NOT_CALCULATED_YET) ? String.format("%06X", lastTA) : "000000");
		deviceStatusField.setText(resourceManager.getLastAccessedDeviceName());

		// LogArea 업데이트는 SicSimulator에서 직접 하지 않고, 여기서 getExecutionLog()를 통해 가져와서 표시
		logArea.setText(""); // 기존 로그 지우기
		if (sicSimulator.getExecutionLog() != null) {
			for (String guiLogEntry : sicSimulator.getExecutionLog()) {
				logArea.append(guiLogEntry + "\n");
			}
		}
	}

	private void updateRegisterField(int regConst, int fieldIndex, int hexDigits) { /* 이전과 동일 */
		if (fieldIndex < 0 || fieldIndex >= regDecFields.length || fieldIndex >= regHexFields.length) { System.err.println("VS.updateRegField: Invalid fieldIdx " + fieldIndex + " for regConst " + regConst); return; } int val = resourceManager.getRegister(regConst);
		regDecFields[fieldIndex].setText(Integer.toString(val)); regHexFields[fieldIndex].setText(String.format("%0" + hexDigits + "X", val & 0xFFFFFF));
	}
	private void logToGui(String message) { /* 이전과 동일 */
		if (logArea.getText().length() > 10000) { try { int end = logArea.getLineEndOffset(50); logArea.replaceRange("", 0, end); } catch (Exception e) { logArea.setText(""); } } logArea.append(message + "\n");
	}
	public static void main(String[] args) { /* 이전과 동일 */
		EventQueue.invokeLater(() -> { try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) { System.err.println("Warn: Could not set system LnF."); } VisualSimulator frame = new VisualSimulator(); frame.setVisible(true); });
	}
}
