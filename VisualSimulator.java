package SP25_simulator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

	public VisualSimulator() {
		resourceManager = new ResourceManager();
		sicLoader = new SicLoader(resourceManager);
		sicSimulator = new SicSimulator(resourceManager);

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

	private void layoutComponents() {
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
	private void addListeners() {
		openButton.addActionListener(e -> {JFileChooser fc=new JFileChooser(".");fc.setDialogTitle("Open SIC/XE Object Code File");if(fc.showOpenDialog(VisualSimulator.this)==JFileChooser.APPROVE_OPTION){load(fc.getSelectedFile());}});
		runOneStepButton.addActionListener(e -> oneStep()); runAllButton.addActionListener(e -> allStep());
		exitButton.addActionListener(e -> {resourceManager.closeAllDevices();System.exit(0);});
	}

	public void load(File program) {
		logToGui(""); instructionCodeArea.setText("");
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

				if (loadedRegions.isEmpty() && resourceManager.getProgramTotalLength() > 0) {
					instructionCodeArea.setText("(No T-records with content found or T-regions not registered)");
				} else {
					for (MemoryRegion region : loadedRegions) {
						byte[] regionMemoryBytes = resourceManager.getMemoryBytes(region.getStartAddress(), region.getLength());
						if (regionMemoryBytes.length == 0) continue;

						int currentOffsetInRegion = 0;
						while (currentOffsetInRegion < regionMemoryBytes.length) {
							int bytesAvailableToPeek = regionMemoryBytes.length - currentOffsetInRegion;
							int bytesToPeek = Math.min(4, bytesAvailableToPeek); // Max SIC/XE instruction length

							if (bytesToPeek <= 0) break;

							byte[] instructionPrefixBytes = new byte[bytesToPeek];
							System.arraycopy(regionMemoryBytes, currentOffsetInRegion, instructionPrefixBytes, 0, bytesToPeek);

							int instructionLen = InstLuncher.getInstructionLengthFromBytes(instructionPrefixBytes);

							if (instructionLen == 0) {
								// 길이 판단 불가 시 (데이터 또는 리터럴 가능성 높음), 해당 바이트를 표시하지 않고 건너뜀.
								if (bytesAvailableToPeek > 0) {
									currentOffsetInRegion += 1; // 최소 1바이트 건너뛰고 다음 바이트에서 다시 시도
									continue;
								}
								break;
							}

							// 명령어 길이가 현재 T-레코드 영역의 남은 바이트를 초과하는지 확인
							if (currentOffsetInRegion + instructionLen > regionMemoryBytes.length) {
								// 이 경우는 T-레코드 마지막 부분에 명령어 일부만 있는 경우 (오류)
								// 표시하지 않고 현재 T-레코드 영역 처리 종료
								break;
							}

							// 유효한 길이의 명령어(또는 데이터 워드/바이트가 명령어로 해석된 경우)만 표시
							StringBuilder currentInstructionHex = new StringBuilder();
							for (int k = 0; k < instructionLen; k++) {
								currentInstructionHex.append(String.format("%02X", regionMemoryBytes[currentOffsetInRegion + k] & 0xFF));
							}
							instructionsDisplayText.append(currentInstructionHex.toString()).append("\n");
							currentOffsetInRegion += instructionLen;
						}
					}
				}
				instructionCodeArea.setText(instructionsDisplayText.toString());
				instructionCodeArea.setCaretPosition(0);

			} else { logToGui("[Error] Failed to load program details."); runOneStepButton.setEnabled(false); runAllButton.setEnabled(false); }
		} catch (Exception e) { logToGui("[Error] Load: " + e.getMessage()); e.printStackTrace(System.err); runOneStepButton.setEnabled(false); runAllButton.setEnabled(false); }
		update();
	}

	public void oneStep() {
		if (sicSimulator.isReadyToRun()) { if (!sicSimulator.oneStep()) { runOneStepButton.setEnabled(false); runAllButton.setEnabled(false); } update();
		} else { logToGui("Program not ready/finished."); runOneStepButton.setEnabled(false); runAllButton.setEnabled(false); }
	}
	public void allStep() {
		if (sicSimulator.isReadyToRun()) { logToGui("--- Starting All Step ---"); runOneStepButton.setEnabled(false); runAllButton.setEnabled(false);
			new SwingWorker<Void, Void>() {
				@Override protected Void doInBackground() throws Exception { while(sicSimulator.isReadyToRun()){ if (!sicSimulator.oneStep()) break; if (isCancelled()) break; } return null; }
				@Override protected void done() { try { get(); } catch (Exception e) { logToGui("[Error]AllStep:"+e.getMessage());e.printStackTrace(System.err); } update(); if (!sicSimulator.isReadyToRun()) { runOneStepButton.setEnabled(false); runAllButton.setEnabled(false); } logToGui("--- All Step Finished ---"); }
			}.execute();
		} else { logToGui("Program not ready/finished."); }
	}
	public void update() {
		progNameFieldH.setText(resourceManager.getProgramName()); startAddrObjFieldH.setText(String.format("%06X", resourceManager.getHRecordObjectProgramStartAddress())); progLengthFieldH.setText(String.format("%06X", resourceManager.getProgramTotalLength()));
		firstInstAddrFieldE.setText(String.format("%06X", resourceManager.getFirstInstructionAddress())); startAddrMemFieldE.setText(String.format("%06X", resourceManager.getActualProgramLoadAddress()));
		updateRegisterField(ResourceManager.REG_A, 0, 6); updateRegisterField(ResourceManager.REG_X, 1, 6); updateRegisterField(ResourceManager.REG_L, 2, 6); updateRegisterField(ResourceManager.REG_B, 3, 6); updateRegisterField(ResourceManager.REG_S, 4, 6); updateRegisterField(ResourceManager.REG_T, 5, 6);
		double fVal = resourceManager.getRegister_F(); regDecFields[6].setText(String.format("%.5e", fVal)); regHexFields[6].setText(String.format("%012X", Double.doubleToRawLongBits(fVal)).substring(0,12));
		updateRegisterField(ResourceManager.REG_PC, 7, 6); updateRegisterField(ResourceManager.REG_SW, 8, 6);
		int lastTA = (sicSimulator.instLuncher != null) ? sicSimulator.instLuncher.getLastCalculatedTA() : InstLuncher.TA_NOT_CALCULATED_YET; targetAddrField.setText((lastTA != InstLuncher.TA_NOT_CALCULATED_YET) ? String.format("%06X", lastTA) : "000000");
		deviceStatusField.setText(resourceManager.getLastAccessedDeviceName()); logArea.setText(""); if (sicSimulator.getExecutionLog() != null) { for (String guiLogEntry : sicSimulator.getExecutionLog()) { logArea.append(guiLogEntry + "\n"); } }
	}
	private void updateRegisterField(int regConst, int fieldIndex, int hexDigits) {
		if (fieldIndex < 0 || fieldIndex >= regDecFields.length || fieldIndex >= regHexFields.length) { System.err.println("VS.updateRegField: Invalid fieldIdx " + fieldIndex + " for regConst " + regConst); return; } int val = resourceManager.getRegister(regConst);
		regDecFields[fieldIndex].setText(Integer.toString(val)); regHexFields[fieldIndex].setText(String.format("%0" + hexDigits + "X", val & 0xFFFFFF));
	}
	private void logToGui(String message) {
		if (logArea.getText().length() > 10000) { try { int end = logArea.getLineEndOffset(50); logArea.replaceRange("", 0, end); } catch (Exception e) { logArea.setText(""); } } logArea.append(message + "\n");
	}
	public static void main(String[] args) {
		EventQueue.invokeLater(() -> { try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) { System.err.println("Warn: Could not set system LnF."); } VisualSimulator frame = new VisualSimulator(); frame.setVisible(true); });
	}
}
