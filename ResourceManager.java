package SP25_simulator;

import java.io.File;
import java.io.IOException; // createNewFile을 위해 추가
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;

public class ResourceManager {

	// ... (이전 답변의 상수, 멤버 변수, 생성자, 다른 메소드들은 동일하게 유지) ...
	public static final int REG_A = 0; public static final int REG_X = 1;
	public static final int REG_L = 2; public static final int REG_B = 3;
	public static final int REG_S = 4; public static final int REG_T = 5;
	public static final int REG_F = 6; public static final int REG_PC = 8;
	public static final int REG_SW = 9;

	HashMap<String, RandomAccessFile> deviceManager = new HashMap<>();
	byte[] memory = new byte[65536];
	int[] register = new int[10];
	double register_F;

	private String programName = "";
	private int hRecordObjectProgramStartAddress = 0;
	private int programTotalLength = 0;
	private int actualProgramLoadAddress = 0;
	private int firstInstructionAddress = 0;
	private String lastAccessedDeviceName = "";
	private HashMap<String, Integer> estab = new HashMap<>();

	public void initializeResource() { /* 이전과 동일 */
		Arrays.fill(memory, (byte) 0x00);
		for (int i = 0; i < register.length; i++) { register[i] = 0; }
		register_F = 0.0; programName = ""; hRecordObjectProgramStartAddress = 0;
		programTotalLength = 0; actualProgramLoadAddress = 0; firstInstructionAddress = 0;
		lastAccessedDeviceName = ""; estab.clear(); closeAllDevices();
	}
	public void closeAllDevices() { /* 이전과 동일 */
		for (RandomAccessFile raf : deviceManager.values()) {
			try { if (raf != null) raf.close(); }
			catch (IOException e) { System.err.println("Error closing device RAF: " + e.getMessage()); }
		}
		deviceManager.clear();
	}

	/**
	 * TD 명령어로 장치 테스트 시 호출됩니다.
	 * 장치 파일이 존재하지 않으면 새로 생성합니다.
	 * @param devName 테스트할 장치 이름 (예: "F1")
	 * @return 장치가 준비되었으면 true, 그렇지 않으면 false
	 */
	public boolean testDevice(String devName) {
		if (devName == null || devName.trim().isEmpty()) {
			System.err.println("[ResourceManager.testDevice] Device name is null or empty.");
			return false;
		}
		lastAccessedDeviceName = devName.trim();

		try {
			// 표준 입출력 장치는 항상 준비된 것으로 간주
			if (devName.equalsIgnoreCase("STDIN") || devName.equals("00") ||
					devName.equalsIgnoreCase("STDOUT") || devName.equals("01") ||
					devName.equalsIgnoreCase("STDERR") || devName.equals("02")) {
				System.out.println("[ResourceManager.testDevice] Testing standard device: " + devName + " -> Ready (true)");
				return true;
			}

			File deviceFile = new File(devName.trim());
			System.out.println("[ResourceManager.testDevice] Checking device: " + devName +
					", Path: " + deviceFile.getAbsolutePath());

			if (!deviceFile.exists()) {
				System.out.println("[ResourceManager.testDevice] File '" + deviceFile.getName() + "' does not exist. Attempting to create...");
				try {
					// 파일의 부모 디렉토리가 존재하지 않으면 생성 (선택적, 더 견고하게 만들려면)
					File parentDir = deviceFile.getParentFile();
					if (parentDir != null && !parentDir.exists()) {
						if (parentDir.mkdirs()) { // mkdris()는 여러 단계의 디렉토리 생성 [11]
							System.out.println("[ResourceManager.testDevice] Parent directory created: " + parentDir.getAbsolutePath());
						} else {
							System.err.println("[ResourceManager.testDevice] Failed to create parent directory: " + parentDir.getAbsolutePath());
							// 부모 디렉토리 생성 실패 시 파일 생성도 실패할 가능성이 높음
						}
					}

					if (deviceFile.createNewFile()) { // 파일 생성 시도 [10][12][14]
						System.out.println("[ResourceManager.testDevice] File '" + deviceFile.getName() + "' created successfully.");
						return true; // 파일 생성 성공 시 준비된 것으로 간주
					} else {
						// createNewFile()이 false를 반환하는 경우는 드물지만 (이미 존재하는데 exists()가 false인 동시성 문제 등)
						// 또는 권한 문제로 생성 실패 시 IOException 발생
						System.err.println("[ResourceManager.testDevice] Failed to create file '" + deviceFile.getName() + "' (createNewFile returned false, or already existed concurrently).");
						return false; // 파일 생성 실패
					}
				} catch (IOException e) {
					System.err.println("[ResourceManager.testDevice] IOException while creating file '" + deviceFile.getName() + "': " + e.getMessage());
					return false; // 파일 생성 중 I/O 오류
				} catch (SecurityException se) {
					System.err.println("[ResourceManager.testDevice] SecurityException while creating file '" + deviceFile.getName() + "': " + se.getMessage());
					return false; // 파일 생성 중 보안 예외
				}
			} else {
				// 파일이 이미 존재하면 준비된 것으로 간주
				System.out.println("[ResourceManager.testDevice] File '" + deviceFile.getName() + "' already exists.");
				return true;
			}
		} catch (Exception e) { // 예상치 못한 다른 예외 처리
			System.err.println("[ResourceManager.testDevice] Unexpected error testing device " + devName + ": " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	// ... (readDevice, writeDevice, 메모리 및 레지스터 관련 메소드는 이전 답변과 동일) ...
	public char[] readDevice(String devName, int num) { /* 이전과 동일 */
		if (devName == null || devName.trim().isEmpty() || num <= 0) return new char[0];
		lastAccessedDeviceName = devName.trim(); char[] buffer = new char[num]; Arrays.fill(buffer, (char)0x00);
		try {
			RandomAccessFile raf;
			if (deviceManager.containsKey(devName) && deviceManager.get(devName) != null) raf = deviceManager.get(devName);
			else {
				if (devName.equalsIgnoreCase("STDIN") || devName.equals("00")) { System.err.println("RD STDIN not impl."); return buffer; }
				File deviceFile = new File(devName.trim());
				if (!deviceFile.exists() || !deviceFile.canRead()) { System.err.println("RD Dev " + devName + " not found/readable."); setRegister(REG_A, (getRegister(REG_A) & 0xFFFF00)); return new char[0]; }
				raf = new RandomAccessFile(deviceFile, "r"); deviceManager.put(devName, raf);
			}
			byte[] byteBuffer = new byte[num]; int bytesActuallyRead = raf.read(byteBuffer, 0, num);
			if (bytesActuallyRead == -1) { setRegister(REG_A, (getRegister(REG_A) & 0xFFFF00)); return new char[0]; }
			for(int i=0; i < bytesActuallyRead; i++) buffer[i] = (char) (byteBuffer[i] & 0xFF);
			return Arrays.copyOf(buffer, bytesActuallyRead);
		} catch (IOException e) { System.err.println("RD Err " + devName + ": " + e.getMessage()); setRegister(REG_A, (getRegister(REG_A) & 0xFFFF00)); return new char[0]; }
	}
	public void writeDevice(String devName, char[] data, int num) { /* 이전과 동일 */
		if (devName == null || devName.trim().isEmpty() || data == null || num <= 0 || data.length < num) return;
		lastAccessedDeviceName = devName.trim();
		try {
			RandomAccessFile raf;
			if (deviceManager.containsKey(devName) && deviceManager.get(devName) != null) raf = deviceManager.get(devName);
			else {
				if (devName.equalsIgnoreCase("STDOUT") || devName.equals("01")) { System.out.print(new String(data, 0, num)); return; }
				if (devName.equalsIgnoreCase("STDERR") || devName.equals("02")) { System.err.print(new String(data, 0, num)); return; }
				File deviceFile = new File(devName.trim());
				// writeDevice는 파일이 없으면 생성해야 하므로, testDevice와 유사한 로직 또는 RandomAccessFile("rw")가 처리하도록 함
				// RandomAccessFile("rw")는 파일이 없으면 생성합니다.
				raf = new RandomAccessFile(deviceFile, "rw"); deviceManager.put(devName, raf);
			}
			byte[] byteData = new byte[num]; for(int i=0; i<num; i++) byteData[i] = (byte)data[i]; raf.write(byteData, 0, num);
		} catch (IOException e) { System.err.println("WD Err " + devName + ": " + e.getMessage()); }
	}
	public byte[] getMemoryBytes(int location, int num) { /* 이전과 동일 */
		if (location < 0 || num < 0 || location + num > memory.length) return new byte[0];
		byte[] data = new byte[num]; System.arraycopy(memory, location, data, 0, num); return data;
	}
	public void setMemoryBytes(int location, byte[] data, int num) { /* 이전과 동일 */
		if (location < 0 || data == null || num < 0 || location + num > memory.length || data.length < num) return;
		System.arraycopy(data, 0, memory, location, num);
	}
	public void setMemoryHex(int location, String hexString) { /* 이전과 동일 */
		if (hexString == null || hexString.length() % 2 != 0) return; int numBytes = hexString.length()/2;
		if (location < 0 || location + numBytes > memory.length) return;
		for (int i=0; i<numBytes; i++) { String byteStr = hexString.substring(i*2, i*2+2); try {memory[location+i]=(byte)Integer.parseInt(byteStr,16);}catch(NumberFormatException e){System.err.println("Err parsing hex: "+byteStr);return;}}
	}
	public String getMemoryByteHex(int location) { /* 이전과 동일 */
		if (this.programTotalLength > 0 && (location < this.actualProgramLoadAddress || location >= (this.actualProgramLoadAddress + this.programTotalLength))) return "..";
		if (location < 0 || location >= memory.length) return "XX";
		return String.format("%02X", memory[location] & 0xFF);
	}
	public int getRegister(int regNum) { /* 이전과 동일 */ if(regNum<0||regNum>=register.length||regNum==REG_F||regNum==7)return 0; return register[regNum];}
	public void setRegister(int regNum, int value) { /* 이전과 동일 */ if(regNum<0||regNum>=register.length||regNum==REG_F||regNum==7)return; register[regNum]=value&0xFFFFFF;}
	public double getRegister_F() { return this.register_F; } public void setRegister_F(double value) { this.register_F = value; }
	public byte[] intToBytes(int data) { /* 이전과 동일 */ byte[]r=new byte[3];r[0]=(byte)((data>>16)&0xFF);r[1]=(byte)((data>>8)&0xFF);r[2]=(byte)(data&0xFF);return r;}
	public int bytesToInt(byte[] data) { /* 이전과 동일 */
		if(data==null||data.length==0)return 0;int v=0;for(int i=0;i<data.length;i++)v=(v<<8)|(data[i]&0xFF);
		if(data.length==3&&(data[0]&0x80)!=0)v|=0xFF000000;else if(data.length==1&&(data[0]&0x80)!=0)v|=0xFFFFFF00;return v;
	}
	public String getProgramName(){return programName;} public void setProgramName(String programName){this.programName=(programName!=null)?programName.trim():"";}
	public int getHRecordObjectProgramStartAddress(){return hRecordObjectProgramStartAddress;} public void setHRecordObjectProgramStartAddress(int address){this.hRecordObjectProgramStartAddress=address;}
	public int getProgramTotalLength(){return programTotalLength;} public void setProgramTotalLength(int programTotalLength){this.programTotalLength=programTotalLength;}
	public int getActualProgramLoadAddress(){return actualProgramLoadAddress;} public void setActualProgramLoadAddress(int actualProgramLoadAddress){this.actualProgramLoadAddress=actualProgramLoadAddress;}
	public int getFirstInstructionAddress(){return firstInstructionAddress;} public void setFirstInstructionAddress(int firstInstructionAddress){this.firstInstructionAddress=firstInstructionAddress;}
	public String getLastAccessedDeviceName(){return lastAccessedDeviceName;} public void setLastAccessedDeviceName(String deviceName){this.lastAccessedDeviceName=(deviceName!=null)?deviceName.trim():"";}
	public void addExternalSymbol(String symbol, int address){estab.put(symbol.trim(),address);} public Integer getExternalSymbolAddress(String symbol){return estab.get(symbol.trim());}
	public HashMap<String,Integer> getEstab(){return estab;}
}
