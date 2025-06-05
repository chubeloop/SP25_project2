package SP25_simulator;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

// T 레코드가 로드한 메모리 영역을 나타내는 간단한 클래스
class MemoryRegion {
	int startAddress;
	int length;

	public MemoryRegion(int startAddress, int length) {
		this.startAddress = startAddress;
		this.length = length;
	}

	public int getStartAddress() { return startAddress; }
	public int getLength() { return length; }
}

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

	HashMap<String, RandomAccessFile> deviceManager;
	byte[] memory;
	int[] register;
	double register_F;

	private String programName;
	private int hRecordObjectProgramStartAddress;
	private int programTotalLength; // H 레코드 기준 전체 길이 (RESW/RESB 포함)
	private int actualProgramLoadAddress;
	private int firstInstructionAddress;
	private String lastAccessedDeviceName;
	private HashMap<String, Integer> estab;
	private List<MemoryRegion> tRecordLoadedRegions; // T 레코드로 실제 데이터가 로드된 영역

	public ResourceManager() {
		this.memory = new byte[1 << 16];
		this.register = new int[10];
		this.deviceManager = new HashMap<>();
		this.estab = new HashMap<>();
		this.tRecordLoadedRegions = new ArrayList<>();
		initializeResource();
	}

	public void initializeResource() {
		Arrays.fill(memory, (byte) 0x00);
		for (int i = 0; i < register.length; i++) { register[i] = 0; }
		register_F = 0.0; programName = ""; hRecordObjectProgramStartAddress = 0;
		programTotalLength = 0; actualProgramLoadAddress = 0; firstInstructionAddress = 0;
		lastAccessedDeviceName = "";
		if (estab != null) estab.clear(); else estab = new HashMap<>();
		if (tRecordLoadedRegions != null) tRecordLoadedRegions.clear(); else tRecordLoadedRegions = new ArrayList<>();
		closeDevices();
	}

	public void closeDevices() {
		for (RandomAccessFile raf : deviceManager.values()) {
			try { if (raf != null) raf.close(); }
			catch (IOException e) { System.err.println("Error closing device RAF: " + e.getMessage()); }
		}
		deviceManager.clear();
	}

	public boolean testDevice(String devName) {
		if (devName == null || devName.trim().isEmpty()) { return false; }
		lastAccessedDeviceName = devName.trim();
		try {
			if (devName.equalsIgnoreCase("STDIN") || devName.equals("00") ||
					devName.equalsIgnoreCase("STDOUT") || devName.equals("01") ||
					devName.equalsIgnoreCase("STDERR") || devName.equals("02")) {
				return true;
			}
			File deviceFile = new File(devName.trim());
			if (!deviceFile.exists()) {
				try {
					File parentDir = deviceFile.getParentFile();
					if (parentDir != null && !parentDir.exists()) { if (!parentDir.mkdirs()) { /* ignore */ } }
					if (deviceFile.createNewFile()) { return true; }
					else { return deviceFile.exists(); }
				} catch (IOException | SecurityException e) { return false; }
			} else { return true; }
		} catch (Exception e) { return false; }
	}

	public char[] readDevice(String devName, int num) {
		if (devName == null || devName.trim().isEmpty() || num <= 0) return new char[0];
		lastAccessedDeviceName = devName.trim(); char[] buffer = new char[num]; Arrays.fill(buffer, (char)0x00);
		try {
			RandomAccessFile raf;
			if (deviceManager.containsKey(devName) && deviceManager.get(devName) != null) raf = deviceManager.get(devName);
			else {
				if (devName.equalsIgnoreCase("STDIN") || devName.equals("00")) { return buffer; }
				File deviceFile = new File(devName.trim());
				if (!deviceFile.exists() || !deviceFile.canRead()) { return new char[0]; }
				raf = new RandomAccessFile(deviceFile, "r"); deviceManager.put(devName, raf);
			}
			byte[] byteBuffer = new byte[num]; int bytesActuallyRead = raf.read(byteBuffer, 0, num);
			if (bytesActuallyRead == -1) { return new char[0]; }
			for(int i=0; i < bytesActuallyRead; i++) buffer[i] = (char) (byteBuffer[i] & 0xFF);
			return Arrays.copyOf(buffer, bytesActuallyRead);
		} catch (IOException e) { return new char[0]; }
	}

	public void writeDevice(String devName, char[] data, int num) {
		if (devName == null || devName.trim().isEmpty() || data == null || num <= 0 || data.length < num) return;
		lastAccessedDeviceName = devName.trim();
		try {
			RandomAccessFile raf;
			if (deviceManager.containsKey(devName) && deviceManager.get(devName) != null) raf = deviceManager.get(devName);
			else {
				if (devName.equalsIgnoreCase("STDOUT") || devName.equals("01")) { System.out.print(new String(data, 0, num)); return; }
				if (devName.equalsIgnoreCase("STDERR") || devName.equals("02")) { System.err.print(new String(data, 0, num)); return; }
				File deviceFile = new File(devName.trim());
				raf = new RandomAccessFile(deviceFile, "rw"); deviceManager.put(devName, raf);
			}
			byte[] byteData = new byte[num]; for(int i=0; i<num; i++) byteData[i] = (byte)data[i]; raf.write(byteData, 0, num);
		} catch (IOException e) { System.err.println("[ResourceManager.writeDevice] Error writing: " + e.getMessage()); }
	}

	public byte[] getMemory(int location, int num) {
		if (location < 0 || num <= 0 || location + num > memory.length) {
			return new byte[0];
		}
		byte[] data = new byte[num];
		System.arraycopy(memory, location, data, 0, num);
		return data;
	}

	public void setMemory(int location, byte[] data, int num) {
		if (location < 0 || data == null || num < 0 || location + num > memory.length || data.length < num) return;
		System.arraycopy(data, 0, memory, location, num);
	}

	public void setMemoryHex(int location, String hexString) {
		if (hexString == null || hexString.length() % 2 != 0) return;
		int numBytes = hexString.length() / 2;
		if (location < 0 || location + numBytes > memory.length) return;
		for (int i = 0; i < numBytes; i++) {
			String byteStr = hexString.substring(i * 2, i * 2 + 2);
			try { memory[location + i] = (byte) Integer.parseInt(byteStr, 16); }
			catch (NumberFormatException e) { System.err.println("[ResourceManager.setMemoryHex] Error parsing: "+byteStr); return; }
		}
	}

	public int getRegister(int regNum) {
		if(regNum<0||regNum>=register.length||regNum==REG_F||regNum==7) return 0;
		return register[regNum];
	}
	public void setRegister(int regNum, int value) {
		if(regNum<0||regNum>=register.length||regNum==REG_F||regNum==7) return;
		register[regNum]=value&0xFFFFFF;
	}
	public double getRegister_F() { return this.register_F; }
	public void setRegister_F(double value) { this.register_F = value; }
	public byte[] intToBytes(int data) { byte[]r=new byte[3];r[0]=(byte)((data>>16)&0xFF);r[1]=(byte)((data>>8)&0xFF);r[2]=(byte)(data&0xFF);return r;}
	public int byteToInt(byte[] data) {
		if(data==null||data.length==0)return 0; int v=0; for(int i=0;i<data.length;i++)v=(v<<8)|(data[i]&0xFF);
		if(data.length==3&&(data[0]&0x80)!=0)v|=0xFF000000; else if(data.length==1&&(data[0]&0x80)!=0)v|=0xFFFFFF00; return v;
	}
	public String getProgramName(){return programName;} public void setProgramName(String programName){this.programName=(programName!=null)?programName.trim():"";}
	public int getHRecordObjectProgramStartAddress(){return hRecordObjectProgramStartAddress;} public void setHRecordObjectProgramStartAddress(int address){this.hRecordObjectProgramStartAddress=address;}
	public int getProgramTotalLength(){return programTotalLength;} public void setProgramTotalLength(int programTotalLength){this.programTotalLength=programTotalLength;}
	public int getActualProgramLoadAddress(){return actualProgramLoadAddress;} public void setActualProgramLoadAddress(int actualProgramLoadAddress){this.actualProgramLoadAddress=actualProgramLoadAddress;}
	public int getFirstInstructionAddress(){return firstInstructionAddress;} public void setFirstInstructionAddress(int firstInstructionAddress){this.firstInstructionAddress=firstInstructionAddress;}
	public String getLastAccessedDeviceName(){return lastAccessedDeviceName;} public void setLastAccessedDeviceName(String deviceName){this.lastAccessedDeviceName=(deviceName!=null)?deviceName.trim():"";}
	public void addExternalSymbol(String symbol, int address){if(symbol!=null&&!symbol.trim().isEmpty())estab.put(symbol.trim(),address);}
	public Integer getExternalSymbolAddress(String symbol){if(symbol==null||symbol.trim().isEmpty())return null;return estab.get(symbol.trim());}
	public HashMap<String,Integer> getEstab(){return estab;}

	// T-레코드로 로드된 영역 정보 추가 및 조회
	public void addTRecordLoadedRegion(int startAddress, int length) {
		if (this.tRecordLoadedRegions != null && length > 0) {
			this.tRecordLoadedRegions.add(new MemoryRegion(startAddress, length));
		}
	}
	public List<MemoryRegion> getTRecordLoadedRegions() {
		return new ArrayList<>(this.tRecordLoadedRegions); // 방어적 복사
	}

	public char[] intToChar(int data) {
		char[] result = new char[3];
		// data의 상위 8비트 (가장 왼쪽 바이트)
		result[0] = (char) ((data >> 16) & 0xFF);
		// data의 중간 8비트
		result[1] = (char) ((data >> 8) & 0xFF);
		// data의 하위 8비트 (가장 오른쪽 바이트)
		result[2] = (char) (data & 0xFF);
		return result;
	}
}