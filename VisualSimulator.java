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

	// GUI Components
	private JButton openButton, runOneStepButton, runAllButton, exitButton;
	private JTextField fileNameField;
	private JTextField progNameFieldH, startAddrObjFieldH, progLengthFieldH;
	private JTextField firstInstAddrFieldE, startAddrMemFieldE;
	private JLabel[] regLabels = new JLabel[9];
	private JTextField[] regDecFields = new JTextField[9];
	private JTextField[] regHexFields = new JTextField[9];
	private JTextField targetAddrField;
	private JTextArea instructionCodeArea; // JTextArea로 변경됨
	private JTextField deviceStatusField;
	private JTextArea logArea;

	private List<String> instructionCodeHistory;

	public VisualSimulator() {
		resourceManager = new ResourceManager();
		sicLoader = new SicLoader(resourceManager);
		sicSimulator = new SicSimulator(resourceManager);
		instructionCodeHistory = new ArrayList<>();

		setTitle("SIC/XE Simulator (SP25_Project2)");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		initComponents();
		layoutComponents();
		addListeners();
		pack();
		setLocationRelativeTo(null);
		runOneStepButton.setEnabled(false);
		runAllButton.setEnabled(false);
		update();
	}

	private void initComponents() {
		// ... (이전 답변과 동일) ...
		fileNameField = new JTextField(15); fileNameField.setEditable(false);
		openButton = new JButton("open");
		progNameFieldH = new JTextField(6); progNameFieldH.setEditable(false); progNameFieldH.setHorizontalAlignment(JTextField.CENTER);
		startAddrObjFieldH = new JTextField(6); startAddrObjFieldH.setEditable(false); startAddrObjFieldH.setHorizontalAlignment(JTextField.CENTER);
		progLengthFieldH = new JTextField(6); progLengthFieldH.setEditable(false); progLengthFieldH.setHorizontalAlignment(JTextField.CENTER);
		firstInstAddrFieldE = new JTextField(6); firstInstAddrFieldE.setEditable(false); firstInstAddrFieldE.setHorizontalAlignment(JTextField.CENTER);
		startAddrMemFieldE = new JTextField(6); startAddrMemFieldE.setEditable(false); startAddrMemFieldE.setHorizontalAlignment(JTextField.CENTER);
		String[] regNamesShort = {"A", "X", "L", "B", "S", "T", "F", "PC", "SW"};
		int[] regConstIndices = {0, 1, 2, 3, 4, 5, 6, 8, 9};
		for (int i = 0; i < regNamesShort.length; i++) {
			regLabels[i] = new JLabel(regNamesShort[i] + " (#" + regConstIndices[i] + ")");
			regDecFields[i] = new JTextField(5); regDecFields[i].setEditable(false); regDecFields[i].setHorizontalAlignment(JTextField.RIGHT);
			regHexFields[i] = new JTextField(6); regHexFields[i].setEditable(false); regHexFields[i].setHorizontalAlignment(JTextField.RIGHT);
		}
		targetAddrField = new JTextField(6); targetAddrField.setEditable(false); targetAddrField.setHorizontalAlignment(JTextField.RIGHT);
		instructionCodeArea = new JTextArea(5, 12); // JTextArea
		instructionCodeArea.setEditable(false);
		instructionCodeArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		instructionCodeArea.setLineWrap(true);
		instructionCodeArea.setWrapStyleWord(true);
		DefaultCaret instructionCaret = (DefaultCaret)instructionCodeArea.getCaret();
		instructionCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		deviceStatusField = new JTextField(3); deviceStatusField.setEditable(false); deviceStatusField.setHorizontalAlignment(JTextField.CENTER);
		runOneStepButton = new JButton("실행(1step)");
		runAllButton = new JButton("실행 (all)");
		exitButton = new JButton("종료");
		logArea = new JTextArea(10, 12);
		logArea.setEditable(false);
		logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		DefaultCaret logCaret = (DefaultCaret)logArea.getCaret();
		logCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
	}

	private void layoutComponents() {
		// ... (이전 답변과 동일, instructionCodeArea를 JScrollPane에 담아 배치) ...
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(3,3,3,3); gbc.anchor = GridBagConstraints.WEST;
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
		rtGbc.gridx=1; rtGbc.fill=GridBagConstraints.HORIZONTAL; rtGbc.weightx=1.0; rightTopPanel.add(targetAddrField, rtGbc);
		rtGbc.gridx=0; rtGbc.gridy=1; rtGbc.fill=GridBagConstraints.NONE; rtGbc.weightx=0; rightTopPanel.add(new JLabel("Instructions :"), rtGbc);
		JScrollPane instructionScrollPane = new JScrollPane(instructionCodeArea);
		instructionScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED); // 필요할 때만 스크롤바
		instructionScrollPane.setPreferredSize(new Dimension(100, 80));
		rtGbc.gridx=1; rtGbc.fill=GridBagConstraints.BOTH; rtGbc.weightx=1.0; rtGbc.weighty=0.5; rightTopPanel.add(instructionScrollPane, rtGbc); // fill BOTH, weighty 추가
		rtGbc.gridx=0; rtGbc.gridy=2; rtGbc.fill=GridBagConstraints.NONE; rtGbc.weightx=0; rtGbc.weighty=0; rightTopPanel.add(new JLabel("사용중인 장치"), rtGbc);
		rtGbc.gridx=1; rtGbc.fill=GridBagConstraints.HORIZONTAL; rtGbc.weightx=1.0; rightTopPanel.add(deviceStatusField, rtGbc);
		gbc.gridx=1; gbc.gridy=3; gbc.gridwidth=1; gbc.gridheight=1;
		gbc.fill=GridBagConstraints.BOTH; gbc.anchor=GridBagConstraints.NORTHWEST; gbc.weightx=0.6; gbc.weighty=0.5; add(rightTopPanel, gbc);
		JPanel logPanel = new JPanel(new BorderLayout());
		logPanel.setBorder(BorderFactory.createTitledBorder("Log"));
		logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
		logPanel.setPreferredSize(new Dimension(180, 150));
		gbc.gridx=1; gbc.gridy=4; gbc.gridwidth=1; gbc.gridheight=1;
		gbc.weighty=1.0; gbc.fill=GridBagConstraints.BOTH; add(logPanel, gbc);
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 2));
		buttonPanel.add(runOneStepButton); buttonPanel.add(runAllButton); buttonPanel.add(exitButton);
		gbc.gridx=0; gbc.gridy=6; gbc.gridwidth=4; gbc.gridheight=1; // Buttons at the very bottom
		gbc.weighty=0; gbc.fill=GridBagConstraints.HORIZONTAL; gbc.anchor=GridBagConstraints.CENTER; add(buttonPanel, gbc);
	}

	private void addListeners() {
		// ... (이전 답변과 동일) ...
		openButton.addActionListener(e -> {JFileChooser fc=new JFileChooser(".");fc.setDialogTitle("Open Object Code");if(fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION){load(fc.getSelectedFile());}});
		runOneStepButton.addActionListener(e -> oneStep());
		runAllButton.addActionListener(e -> allStep());
		exitButton.addActionListener(e -> { resourceManager.closeAllDevices(); System.exit(0); });
	}

	public void load(File program) {
		logToGui("");
		instructionCodeHistory.clear();
		instructionCodeArea.setText("");

		if (program == null) { logToGui("[Error] Program file is null."); return; }
		currentObjectCodeFile = program; fileNameField.setText(program.getName());
		try {
			sicLoader.load(program);
			sicSimulator.programLoaded();
			if (resourceManager.getProgramName() != null && !resourceManager.getProgramName().isEmpty()) {
				runOneStepButton.setEnabled(true); runAllButton.setEnabled(true);
				List<String> initLogs = sicSimulator.getExecutionLog();
				if(!initLogs.isEmpty()) { for(String log : initLogs) logToGui(log); }
				else { logToGui("Program '" + program.getName() + "' loaded. PC: " + String.format("0x%06X", resourceManager.getRegister(ResourceManager.REG_PC)));}
			} else { logToGui("[Error] Load failed."); runOneStepButton.setEnabled(false); runAllButton.setEnabled(false); }
		} catch (Exception e) { logToGui("[Error] Load: " + e.getMessage()); e.printStackTrace(); }
		update();
	}

	public void oneStep() {
		if (sicSimulator.isReadyToRun()) {
			if (sicSimulator.instLuncher != null) {
				int pc = resourceManager.getRegister(ResourceManager.REG_PC);
				byte[] instBytes = sicSimulator.instLuncher.getCurrentInstructionBytes(pc);
				if (instBytes != null && instBytes.length > 0) {
					StringBuilder sb = new StringBuilder();
					for (byte b : instBytes) sb.append(String.format("%02X", b & 0xFF));
					// *** 주소값 없이 오브젝트 코드만 히스토리에 추가 ***
					instructionCodeHistory.add(sb.toString());
				}
			}

			if (!sicSimulator.oneStep()) {
				runOneStepButton.setEnabled(false); runAllButton.setEnabled(false);
			}
			update();
		} else {
			logToGui("Program not ready or finished.");
			runOneStepButton.setEnabled(false); runAllButton.setEnabled(false);
		}
	}

	public void allStep() {
		if (sicSimulator.isReadyToRun()) {
			logToGui("--- All Step Start ---");
			runOneStepButton.setEnabled(false); runAllButton.setEnabled(false);
			new SwingWorker<Void,Void>() {
				@Override protected Void doInBackground() throws Exception {
					while(sicSimulator.isReadyToRun()){
						if (sicSimulator.instLuncher != null) {
							int pc = resourceManager.getRegister(ResourceManager.REG_PC);
							byte[] instBytes = sicSimulator.instLuncher.getCurrentInstructionBytes(pc);
							if (instBytes != null && instBytes.length > 0) {
								StringBuilder sb = new StringBuilder();
								for (byte b : instBytes) sb.append(String.format("%02X", b & 0xFF));
								// *** 주소값 없이 오브젝트 코드만 히스토리에 추가 ***
								instructionCodeHistory.add(sb.toString());
							} else {
								instructionCodeHistory.add("(fetch_err/end)"); // 주소 없이
								break;
							}
						}
						if (!sicSimulator.oneStep()) break;
						if (isCancelled()) break;
					}
					return null;
				}
				@Override protected void done() {
					try{get();}catch(Exception e){logToGui("[Error]AllStep:"+e.getMessage());e.printStackTrace();}
					update();
					runOneStepButton.setEnabled(false); runAllButton.setEnabled(false);
					logToGui("--- All Step Finish ---");
				}
			}.execute();
		} else {
			logToGui("Program not ready or finished.");
		}
	}

	public void update() {
		// H, E 레코드, 레지스터, TargetAddress, DeviceStatus 업데이트는 이전과 동일
		progNameFieldH.setText(resourceManager.getProgramName());
		startAddrObjFieldH.setText(String.format("%06X", resourceManager.getHRecordObjectProgramStartAddress()));
		progLengthFieldH.setText(String.format("%06X", resourceManager.getProgramTotalLength()));
		firstInstAddrFieldE.setText(String.format("%06X", resourceManager.getFirstInstructionAddress()));
		startAddrMemFieldE.setText(String.format("%06X", resourceManager.getActualProgramLoadAddress()));
		updateRegisterField(ResourceManager.REG_A,0,6);updateRegisterField(ResourceManager.REG_X,1,6);
		updateRegisterField(ResourceManager.REG_L,2,6);updateRegisterField(ResourceManager.REG_B,3,6);
		updateRegisterField(ResourceManager.REG_S,4,6);updateRegisterField(ResourceManager.REG_T,5,6);
		double fVal=resourceManager.getRegister_F();regDecFields[6].setText(String.format("%.5e",fVal));regHexFields[6].setText(String.format("%06X",0));
		updateRegisterField(ResourceManager.REG_PC,7,6);updateRegisterField(ResourceManager.REG_SW,8,6);
		int lastTA=(sicSimulator.instLuncher!=null)?sicSimulator.instLuncher.getLastCalculatedTA():InstLuncher.TA_NOT_CALCULATED_YET;
		targetAddrField.setText((lastTA!=InstLuncher.TA_NOT_CALCULATED_YET)?String.format("%06X",lastTA):"000000");
		deviceStatusField.setText(resourceManager.getLastAccessedDeviceName());

		// Instruction Code Area 업데이트 (주소 없이 오브젝트 코드만 누적)
		StringBuilder instHistoryText = new StringBuilder();
		for (String objectCodeOnly : instructionCodeHistory) {
			instHistoryText.append(objectCodeOnly).append("\n");
		}
		instructionCodeArea.setText(instHistoryText.toString());
		// instructionCodeArea.setCaretPosition(instructionCodeArea.getDocument().getLength()); // 자동 스크롤은 DefaultCaret

		// Log Area 업데이트 (니모닉 로그)
		logArea.setText("");
		if (sicSimulator.getExecutionLog() != null) {
			for (String guiLog : sicSimulator.getExecutionLog()) {
				logArea.append(guiLog + "\n");
			}
		}
	}

	private void updateRegisterField(int regConst,int fieldIndex,int hexDigits){
		int val=resourceManager.getRegister(regConst);
		regDecFields[fieldIndex].setText(Integer.toString(val));
		regHexFields[fieldIndex].setText(String.format("%0"+hexDigits+"X",val&0xFFFFFF));
	}

	private void logToGui(String message){
		if (logArea.getText().length() > 10000) {
			try { int end = logArea.getLineEndOffset(20); logArea.replaceRange("", 0, end); }
			catch (Exception e){logArea.setText("");}
		}
		logArea.append(message + "\n");
	}

	public static void main(String[] args){
		EventQueue.invokeLater(()->{
			try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
			catch(Exception e){System.err.println("LnF Error");}
			VisualSimulator frame=new VisualSimulator();
			frame.setVisible(true);
		});
	}
}

