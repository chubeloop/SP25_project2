package SP25_simulator;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class ResourceManager {

	public static final int REG_A = 0;
	public static final int REG_X = 1;
	public static final int REG_L = 2;
	public static final int REG_B = 3;
	public static final int REG_S = 4;
	public static final int REG_T = 5;
	public static final int REG_F = 6;
	public static final int REG_PC = 8;
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

	public void initializeResource() {
		Arrays.fill(memory, (byte) 0x00);
		for (int i = 0; i < register.length; i++) {
			register[i] = 0;
		}
		register_F = 0.0;
		programName = "";
		hRecordObjectProgramStartAddress = 0;
		programTotalLength = 0;
		actualProgramLoadAddress = 0;
		firstInstructionAddress = 0;
		lastAccessedDeviceName = "";
		estab.clear();
		closeAllDevices();
	}

	public void closeAllDevices() { // 스켈레톤 closeDevice()에서 이름 변경
		for (RandomAccessFile raf : deviceManager.values()) {
			try {
				if (raf != null) raf.close();
			} catch (IOException e) {
				System.err.println("Error closing device RAF: " + e.getMessage());
			}
		}
		deviceManager.clear();
	}

	public boolean testDevice(String devName) {
		if (devName == null || devName.trim().isEmpty()) return false;
		lastAccessedDeviceName = devName.trim();
		try {
			if (devName.equalsIgnoreCase("STDIN") || devName.equals("00") ||
					devName.equalsIgnoreCase("STDOUT") || devName.equals("01") ||
					devName.equalsIgnoreCase("STDERR") || devName.equals("02")) {
				return true;
			}
			File deviceFile = new File(devName.trim());
			// TD는 파일 존재 여부 및 접근 권한만 체크. 실제 open은 RD/WD에서.
			if (!deviceFile.exists()) {
				//System.err.println("Device " + devName + " does not exist for TD test.");
				return false; // 파일 없으면 준비 안됨
			}
			// 파일이 존재하면 준비된 것으로 간주 (읽기/쓰기 권한은 RD/WD에서 확인)
			return true;
		} catch (Exception e) {
			System.err.println("Error testing device " + devName + ": " + e.getMessage());
			return false;
		}
	}

	public char[] readDevice(String devName, int num) { // InstLuncher는 char[] 반환을 기대
		if (devName == null || devName.trim().isEmpty() || num <= 0) {
			return new char[0];
		}
		lastAccessedDeviceName = devName.trim();
		char[] buffer = new char[num];
		Arrays.fill(buffer, (char)0x00);

		try {
			RandomAccessFile raf;
			if (deviceManager.containsKey(devName) && deviceManager.get(devName) != null) {
				raf = deviceManager.get(devName);
			} else {
				if (devName.equalsIgnoreCase("STDIN") || devName.equals("00")) {
					System.err.println("Reading from STDIN not directly supported. Returning empty.");
					return buffer;
				}
				File deviceFile = new File(devName.trim());
				if (!deviceFile.exists() || !deviceFile.canRead()) {
					System.err.println("Device " + devName + " not found or not readable for RD.");
					setRegister(REG_A, (getRegister(REG_A) & 0xFFFF00)); // EOF처럼 A의 최하위 00
					return new char[0]; // 빈 배열 반환
				}
				raf = new RandomAccessFile(deviceFile, "r");
				deviceManager.put(devName, raf);
			}

			byte[] byteBuffer = new byte[num];
			int bytesActuallyRead = raf.read(byteBuffer, 0, num);

			if (bytesActuallyRead == -1) { // EOF
				setRegister(REG_A, (getRegister(REG_A) & 0xFFFF00)); // A의 최하위 바이트 00
				return new char[0];
			}
			for(int i=0; i < bytesActuallyRead; i++) {
				buffer[i] = (char) (byteBuffer[i] & 0xFF);
			}
			return Arrays.copyOf(buffer, bytesActuallyRead);

		} catch (IOException e) {
			System.err.println("Error reading from device " + devName + ": " + e.getMessage());
			setRegister(REG_A, (getRegister(REG_A) & 0xFFFF00)); // 오류 시에도 A 최하위 00
			return new char[0]; // 오류 시 빈 배열 반환
		}
	}

	public void writeDevice(String devName, char[] data, int num) { // InstLuncher는 char[] 데이터를 줌
		if (devName == null || devName.trim().isEmpty() || data == null || num <= 0 || data.length < num) {
			return;
		}
		lastAccessedDeviceName = devName.trim();
		try {
			RandomAccessFile raf;
			if (deviceManager.containsKey(devName) && deviceManager.get(devName) != null) {
				raf = deviceManager.get(devName);
			} else {
				if (devName.equalsIgnoreCase("STDOUT") || devName.equals("01")) {
					System.out.print(new String(data, 0, num)); return;
				}
				if (devName.equalsIgnoreCase("STDERR") || devName.equals("02")) {
					System.err.print(new String(data, 0, num)); return;
				}
				File deviceFile = new File(devName.trim());
				raf = new RandomAccessFile(deviceFile, "rw"); // 없으면 생성
				deviceManager.put(devName, raf);
			}
			// raf.seek(raf.length()); // 파일 끝에 쓰려면
			byte[] byteData = new byte[num];
			for(int i=0; i<num; i++) byteData[i] = (byte)data[i];
			raf.write(byteData, 0, num);
		} catch (IOException e) {
			System.err.println("Error writing to device " + devName + ": " + e.getMessage());
		}
	}

	public byte[] getMemoryBytes(int location, int num) {
		if (location < 0 || num < 0 || location + num > memory.length) {
			return new byte[0];
		}
		byte[] data = new byte[num];
		System.arraycopy(memory, location, data, 0, num);
		return data;
	}

	public void setMemoryBytes(int location, byte[] data, int num) {
		if (location < 0 || data == null || num < 0 || location + num > memory.length || data.length < num) {
			return;
		}
		System.arraycopy(data, 0, memory, location, num);
	}

	public void setMemoryHex(int location, String hexString) {
		if (hexString == null || hexString.length() % 2 != 0) return;
		int numBytes = hexString.length() / 2;
		if (location < 0 || location + numBytes > memory.length) return;
		for (int i = 0; i < numBytes; i++) {
			String byteStr = hexString.substring(i * 2, i * 2 + 2);
			try {
				memory[location + i] = (byte) Integer.parseInt(byteStr, 16);
			} catch (NumberFormatException e) {
				System.err.println("Error parsing hex byte in setMemoryHex: " + byteStr); return;
			}
		}
	}

	public String getMemoryByteHex(int location) {
		// 로드된 프로그램 범위 밖인지 확인
		if (this.programTotalLength > 0 &&
				(location < this.actualProgramLoadAddress || location >= (this.actualProgramLoadAddress + this.programTotalLength))) {
			return ".."; // PDF 4.2의 로드 결과 화면처럼 로드 안된 부분은 다르게 표시 (예: .. 또는 공백)
		}
		if (location < 0 || location >= memory.length) {
			return "XX"; // 완전한 메모리 범위 밖
		}
		return String.format("%02X", memory[location] & 0xFF);
	}

	public int getRegister(int regNum) {
		if (regNum < 0 || regNum >= register.length || regNum == REG_F || regNum == 7) {
			return 0;
		}
		return register[regNum];
	}

	public void setRegister(int regNum, int value) {
		if (regNum < 0 || regNum >= register.length || regNum == REG_F || regNum == 7) {
			return;
		}
		register[regNum] = value & 0xFFFFFF;
	}

	public double getRegister_F() { return this.register_F; }
	public void setRegister_F(double value) { this.register_F = value; }

	public byte[] intToBytes(int data) {
		byte[] result = new byte[3];
		result[0] = (byte) ((data >> 16) & 0xFF);
		result[1] = (byte) ((data >> 8) & 0xFF);
		result[2] = (byte) (data & 0xFF);
		return result;
	}

	public int bytesToInt(byte[] data) {
		if (data == null) return 0;
		int value = 0;
		if (data.length == 0) return 0;
		for (int i = 0; i < data.length; i++) {
			value = (value << 8) | (data[i] & 0xFF);
		}
		if (data.length == 3 && (data[0] & 0x80) != 0) {
			value |= 0xFF000000;
		} else if (data.length == 1 && (data[0] & 0x80) != 0){
			value |= 0xFFFFFF00;
		}
		return value;
	}

	public String getProgramName() { return programName; }
	public void setProgramName(String programName) { this.programName = (programName != null) ? programName.trim() : ""; }
	public int getHRecordObjectProgramStartAddress() { return hRecordObjectProgramStartAddress; }
	public void setHRecordObjectProgramStartAddress(int address) { this.hRecordObjectProgramStartAddress = address; }
	public int getProgramTotalLength() { return programTotalLength; }
	public void setProgramTotalLength(int programTotalLength) { this.programTotalLength = programTotalLength; }
	public int getActualProgramLoadAddress() { return actualProgramLoadAddress; }
	public void setActualProgramLoadAddress(int actualProgramLoadAddress) { this.actualProgramLoadAddress = actualProgramLoadAddress; }
	public int getFirstInstructionAddress() { return firstInstructionAddress; }
	public void setFirstInstructionAddress(int firstInstructionAddress) { this.firstInstructionAddress = firstInstructionAddress; }
	public String getLastAccessedDeviceName() { return lastAccessedDeviceName; }
	public void setLastAccessedDeviceName(String deviceName) { this.lastAccessedDeviceName = (deviceName != null) ? deviceName.trim() : ""; }
	public void addExternalSymbol(String symbol, int address) { estab.put(symbol.trim(), address); }
	public Integer getExternalSymbolAddress(String symbol) { return estab.get(symbol.trim()); }
	public HashMap<String,Integer> getEstab() { return estab; } // 타입 명시
}
