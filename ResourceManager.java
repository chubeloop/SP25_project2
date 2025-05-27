package SP25_simulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile; // 수정: RandomAccessFile 사용 예시로 변경
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ResourceManager {
	// 레지스터 번호 상수
	public static final int REG_A = 0;
	public static final int REG_X = 1;
	public static final int REG_L = 2;
	public static final int REG_B = 3;
	public static final int REG_S = 4;
	public static final int REG_T = 5;
	public static final int REG_F = 6; // F 레지스터는 double register_F로 별도 관리
	public static final int REG_PC = 8;
	public static final int REG_SW = 9;

	HashMap<String, RandomAccessFile> deviceManager = new HashMap<>(); // 수정: RandomAccessFile 사용
	byte[] memory = new byte[65536]; // char 대신 byte로 실제 메모리처럼 사용
	int[] register = new int[10];
	double register_F;

	// SymbolTable symtabList; // 로더가 직접 사용하지 않고, ESTAB을 통해 관리

	// 프로그램/섹션 정보
	private String programName = ""; // 로드된 전체 프로그램의 대표 이름 (보통 첫 CS)
	private int hRecordObjectProgramStartAddress = 0; // 첫 H레코드의 명시된 시작주소 (보통 0)
	private int programTotalLength = 0; // 로드된 모든 CS의 누적 길이
	private int actualProgramLoadAddress = 0; // 실제 메모리에 로드되는 시작 주소 (기본 0)
	private int firstInstructionAddress = 0; // E 레코드에 명시된 실행 시작 주소

	private String lastAccessedDeviceName = "";
	private HashMap<String, Integer> estab = new HashMap<>(); // External Symbol Table

	public void initializeResource() {
		Arrays.fill(memory, (byte) 0x00); // 메모리 0으로 초기화
		for (int i = 0; i < register.length; i++) {
			register[i] = 0;
		}
		register_F = 0.0;
		programName = "";
		hRecordObjectProgramStartAddress = 0;
		programTotalLength = 0;
		actualProgramLoadAddress = 0; // 실제 프로그램 로드 주소 (기본 0)
		firstInstructionAddress = 0;
		lastAccessedDeviceName = "";
		estab.clear();
		// if (symtabList != null) { symtabList.clear(); } // 로더가 직접 사용 안함
		closeAllDevices(); // 모든 열린 장치 닫기
	}

	public void closeAllDevices() { // 스켈레톤 closeDevice()에서 이름 변경
		for (RandomAccessFile raf : deviceManager.values()) {
			try {
				if (raf != null) raf.close();
			} catch (IOException e) {
				System.err.println("Error closing device stream: " + e.getMessage());
			}
		}
		deviceManager.clear();
	}


	public boolean testDevice(String devName) { // 반환 타입을 boolean으로 변경
		if (devName == null || devName.trim().isEmpty()) return false;
		lastAccessedDeviceName = devName.trim(); // 마지막 접근 장치 기록

		try {
			// STDIN, STDOUT, STDERR은 항상 준비된 것으로 간주 (파일로 리다이렉션 하지 않는 경우)
			if (devName.equalsIgnoreCase("STDIN") || devName.equals("00") ||
					devName.equalsIgnoreCase("STDOUT") || devName.equals("01") ||
					devName.equalsIgnoreCase("STDERR") || devName.equals("02")) {
				return true; // 콘솔 입출력은 항상 가능하다고 가정
			}

			if (!deviceManager.containsKey(devName)) {
				File deviceFile = new File(devName.trim());
				// 파일이 없으면 생성하지 않고, 사용 불가로 처리 (TD의 일반적 동작)
				if (!deviceFile.exists()) {
					// System.out.println("Device " + devName + " does not exist. Creating.");
					// deviceFile.createNewFile(); // TD가 파일을 생성하지는 않음
					return false; // 파일이 없으면 준비 안됨
				}
				if (!deviceFile.canRead() || !deviceFile.canWrite()){
					//System.out.println("Device " + devName + " exists but no r/w permission.");
					return false; // 읽기/쓰기 권한 없으면 준비 안됨
				}
				// RandomAccessFile raf = new RandomAccessFile(deviceFile, "rw");
				// deviceManager.put(devName, raf); // 실제 오픈은 RD/WD에서
			}
			return true; // 파일이 존재하고 접근 가능하면 준비된 것으로 간주
		} catch (Exception e) {
			System.err.println("Error testing device " + devName + ": " + e.getMessage());
			return false;
		}
	}

	public char[] readDevice(String devName, int num) {
		if (devName == null || devName.trim().isEmpty() || num <= 0) {
			if (num <= 0) return new char[0];
			return new char[0]; // 스켈레톤 반환값 null 대신 빈 배열
		}
		lastAccessedDeviceName = devName.trim();
		char[] buffer = new char[num];
		Arrays.fill(buffer, (char)0x00); // 기본값 0으로 초기화

		try {
			RandomAccessFile raf;
			if (deviceManager.containsKey(devName) && deviceManager.get(devName) != null) {
				raf = deviceManager.get(devName);
			} else {
				// STDIN 처리
				if (devName.equalsIgnoreCase("STDIN") || devName.equals("00")) {
					// 콘솔 입력은 GUI를 통해 받아야 함. 여기서는 간단히 빈 값 또는 에러.
					System.err.println("Reading from STDIN not directly supported in this basic model. Returning empty.");
					return buffer;
				}
				File deviceFile = new File(devName.trim());
				if (!deviceFile.exists() || !deviceFile.canRead()) {
					System.err.println("Device " + devName + " not found or not readable for RD.");
					return buffer; // 빈 버퍼 반환
				}
				raf = new RandomAccessFile(deviceFile, "r"); // 읽기 모드로 오픈
				deviceManager.put(devName, raf);
			}

			for (int i = 0; i < num; i++) {
				int byteRead = raf.read();
				if (byteRead == -1) { // EOF
					if (i == 0) return new char[0]; // 첫 바이트부터 EOF면 빈 배열
					return Arrays.copyOf(buffer, i); // 지금까지 읽은 것만 반환
				}
				buffer[i] = (char) (byteRead & 0xFF);
			}
			return buffer;
		} catch (IOException e) {
			System.err.println("Error reading from device " + devName + ": " + e.getMessage());
			return buffer; // 오류 시 빈 버퍼 또는 부분적으로 채워진 버퍼
		}
	}

	public void writeDevice(String devName, char[] data, int num) {
		if (devName == null || devName.trim().isEmpty() || data == null || num <= 0 || data.length < num) {
			return;
		}
		lastAccessedDeviceName = devName.trim();

		try {
			RandomAccessFile raf;
			if (deviceManager.containsKey(devName) && deviceManager.get(devName) != null) {
				raf = deviceManager.get(devName);
			} else {
				// STDOUT/STDERR 처리
				if (devName.equalsIgnoreCase("STDOUT") || devName.equals("01")) {
					System.out.print(new String(data, 0, num)); // 콘솔 표준 출력
					return;
				}
				if (devName.equalsIgnoreCase("STDERR") || devName.equals("02")) {
					System.err.print(new String(data, 0, num)); // 콘솔 표준 에러 출력
					return;
				}
				File deviceFile = new File(devName.trim());
				// 파일이 없으면 생성 (WD는 파일 생성 가능)
				raf = new RandomAccessFile(deviceFile, "rw"); // 읽기/쓰기 모드, 없으면 생성
				deviceManager.put(devName, raf);
			}
			// 파일의 현재 끝에 쓰도록 포인터 이동 (선택적, 덮어쓰려면 seek(0) 등)
			// raf.seek(raf.length());
			for (int i = 0; i < num; i++) {
				raf.write((byte)data[i]);
			}
		} catch (IOException e) {
			System.err.println("Error writing to device " + devName + ": " + e.getMessage());
		}
	}

	public byte[] getMemoryBytes(int location, int num) { // byte[] 반환으로 수정
		if (location < 0 || num < 0 || location + num > memory.length) {
			return new byte[0];
		}
		byte[] data = new byte[num];
		System.arraycopy(memory, location, data, 0, num);
		return data;
	}

	public void setMemoryBytes(int location, byte[] data, int num) { // byte[] 입력으로 수정
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
				System.err.println("Error parsing hex byte in setMemoryHex: " + byteStr);
				return; // 오류 발생 시 중단
			}
		}
	}

	public String getMemoryByteHex(int location) {
		// 프로그램 로드 범위 밖이거나 전체 메모리 범위 밖이면 공백 또는 "XX"
		if (this.programTotalLength > 0 && (location < this.actualProgramLoadAddress || location >= (this.actualProgramLoadAddress + this.programTotalLength))) {
			// 로드된 프로그램 범위 밖은 GUI에서 다르게 표시할 수 있도록 빈 문자열 또는 특정 문자 반환
			return "  "; // 예: 공백 두 칸
		}
		if (location < 0 || location >= memory.length) {
			return "XX"; // 유효하지 않은 메모리 주소
		}
		return String.format("%02X", memory[location] & 0xFF);
	}


	public int getRegister(int regNum) {
		if (regNum < 0 || regNum >= register.length || regNum == REG_F || regNum == 7 /* F와 7은 사용 안함 명시*/) {
			// System.err.println("Invalid register number for get: " + regNum);
			return 0;
		}
		return register[regNum];
	}

	public void setRegister(int regNum, int value) {
		if (regNum < 0 || regNum >= register.length || regNum == REG_F || regNum == 7) {
			// System.err.println("Invalid register number for set: " + regNum);
			return;
		}
		register[regNum] = value & 0xFFFFFF; // SIC/XE 레지스터는 24비트
	}

	public double getRegister_F() { return this.register_F; }
	public void setRegister_F(double value) { this.register_F = value; }

	// int를 3바이트 byte[]로 변환
	public byte[] intToBytes(int data) {
		byte[] result = new byte[3];
		result[0] = (byte) ((data >> 16) & 0xFF);
		result[1] = (byte) ((data >> 8) & 0xFF);
		result[2] = (byte) (data & 0xFF);
		return result;
	}

	// byte[] (주로 3바이트)를 int로 변환
	public int bytesToInt(byte[] data) {
		if (data == null) return 0;
		int value = 0;
		if (data.length == 0) return 0;

		// 빅 엔디안으로 바이트 결합
		for (int i = 0; i < data.length; i++) {
			value = (value << 8) | (data[i] & 0xFF);
		}

		// 3바이트(24비트) 값에 대한 부호 확장 (만약 최상위 비트가 1이면 음수)
		if (data.length == 3 && (data[0] & 0x80) != 0) {
			value |= 0xFF000000; // 32비트 int의 상위 바이트를 1로 채워 음수 표현
		} else if (data.length == 1 && (data[0] & 0x80) != 0){ // 1바이트 값 부호 확장
			value |= 0xFFFFFF00;
		}
		// 2바이트 값은 부호 확장 안 함 (필요시 별도 처리)
		return value;
	}


	public String getProgramName() { return programName; }
	public void setProgramName(String programName) { this.programName = (programName != null) ? programName.trim() : ""; }

	public int getHRecordObjectProgramStartAddress() { return hRecordObjectProgramStartAddress; }
	public void setHRecordObjectProgramStartAddress(int address) { this.hRecordObjectProgramStartAddress = address; }

	public int getProgramTotalLength() { return programTotalLength; } // 이름 변경
	public void setProgramTotalLength(int programTotalLength) { this.programTotalLength = programTotalLength; } // 이름 변경

	public int getActualProgramLoadAddress() { return actualProgramLoadAddress; } // 이름 변경
	public void setActualProgramLoadAddress(int actualProgramLoadAddress) { this.actualProgramLoadAddress = actualProgramLoadAddress; } // 이름 변경

	public int getFirstInstructionAddress() { return firstInstructionAddress; }
	public void setFirstInstructionAddress(int firstInstructionAddress) { this.firstInstructionAddress = firstInstructionAddress; }

	public String getLastAccessedDeviceName() { return lastAccessedDeviceName; }
	public void setLastAccessedDeviceName(String deviceName) { this.lastAccessedDeviceName = (deviceName != null) ? deviceName.trim() : ""; }

	public void addExternalSymbol(String symbol, int address) { // ESTAB에 추가
		estab.put(symbol.trim(), address);
	}
	public Integer getExternalSymbolAddress(String symbol) { // ESTAB에서 검색
		return estab.get(symbol.trim());
	}
	public HashMap<String, Integer> getEstab() { return estab; }

	// SymbolTable symtabList는 로더가 직접 사용하지 않고, ESTAB(estab 필드)으로 대체
	// public SymbolTable getSymtabList() { return symtabList; }
	// public void setSymtabList(SymbolTable symtabList) { this.symtabList = symtabList; }
}
