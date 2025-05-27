package SP25_simulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SicLoader {
	ResourceManager rMgr;
	private int currentCsLoadAddress = 0;
	private int programTotalCumulativeLength = 0;
	private boolean firstExecutionAddressSet = false;
	private List<MRecordTemp> modificationRecordsBuffer = new ArrayList<>(); // 타입 명시

	private static class MRecordTemp { // 제공된 파일[3]과 동일
		int csRelativeAddress;
		int lengthHalfBytes;
		char operation;
		String symbolName;
		int csLoadAddressAtTimeOfRecord;
		MRecordTemp(int addr, int len, char op, String sym, int csStart) {
			this.csRelativeAddress = addr; this.lengthHalfBytes = len;
			this.operation = op; this.symbolName = sym;
			this.csLoadAddressAtTimeOfRecord = csStart;
		}
	}

	public SicLoader(ResourceManager resourceManager) { // 제공된 파일[3]과 동일
		if (resourceManager == null) {
			throw new IllegalArgumentException("ResourceManager cannot be null for SicLoader.");
		}
		this.rMgr = resourceManager;
	}

	public void setResourceManager(ResourceManager resourceManager) { // 제공된 파일[3]과 동일
		if (resourceManager == null) {
			throw new IllegalArgumentException("ResourceManager cannot be null.");
		}
		this.rMgr = resourceManager;
	}

	public void load(File objectCodeFile) { // 제공된 파일[3]의 로직 사용
		if (objectCodeFile == null || !objectCodeFile.exists() || !objectCodeFile.isFile()) {
			System.err.println("Object code file is invalid: " + (objectCodeFile != null ? objectCodeFile.getPath() : "null"));
			return;
		}
		if (rMgr == null) {
			System.err.println("ResourceManager not set in SicLoader."); return;
		}

		rMgr.initializeResource();
		this.currentCsLoadAddress = rMgr.getActualProgramLoadAddress(); // rMgr에서 로드 시작 주소 가져옴
		this.programTotalCumulativeLength = 0;
		this.firstExecutionAddressSet = false;
		this.modificationRecordsBuffer.clear();

		String line;
		String currentCsName = "";
		int currentCsDeclaredLength = 0;
		boolean firstHRecordProcessed = false; // 첫 H 레코드인지 판별

		try (BufferedReader reader = new BufferedReader(new FileReader(objectCodeFile))) {
			while ((line = reader.readLine()) != null) {
				line = line.trim().replace('\t', ' ');
				if (line.isEmpty()) continue;
				char recordType = line.charAt(0);

				switch (recordType) {
					case 'H':
						if (line.length() < 19) { /* 오류 처리 */ continue; }
						currentCsName = line.substring(1, 7).trim();
						int csObjStartAddr = Integer.parseInt(line.substring(7, 13).trim(), 16);
						currentCsDeclaredLength = Integer.parseInt(line.substring(13, 19).trim(), 16);

						if (!firstHRecordProcessed) {
							rMgr.setProgramName(currentCsName);
							rMgr.setHRecordObjectProgramStartAddress(csObjStartAddr);
							// rMgr.setActualProgramLoadAddress(this.currentCsLoadAddress); // 이미 0으로 초기화됨
							firstHRecordProcessed = true;
						}
						rMgr.addExternalSymbol(currentCsName, this.currentCsLoadAddress);
						break;
					case 'D':
						if (line.length() < 13) { /* 오류 처리 */ continue; }
						for (int i = 1; i < line.length(); i += 12) {
							if (i + 12 > line.length()) { /* 오류 처리 */ break; }
							String defSymbol = line.substring(i, i + 6).trim();
							int defAddrRelative = Integer.parseInt(line.substring(i + 6, i + 12).trim(), 16);
							rMgr.addExternalSymbol(defSymbol, this.currentCsLoadAddress + defAddrRelative);
						}
						break;
					case 'R': /* R 레코드 처리 (현재는 무시) */ break;
					case 'T':
						if (line.length() < 9) { /* 오류 처리 */ continue; }
						int tRecStartAddrRelative = Integer.parseInt(line.substring(1, 7).trim(), 16);
						int tRecLengthBytes = Integer.parseInt(line.substring(7, 9).trim(), 16);
						if (line.length() < 9 + tRecLengthBytes * 2) { /* 오류 처리 */ continue; }
						String objectCodeHex = line.substring(9, 9 + tRecLengthBytes * 2);
						int actualMemAddr = this.currentCsLoadAddress + tRecStartAddrRelative;
						rMgr.setMemoryHex(actualMemAddr, objectCodeHex);
						break;
					case 'M':
						if (line.length() < 11) { /* 오류 처리 */ continue; }
						int modAddrRelativeCS = Integer.parseInt(line.substring(1, 7).trim(), 16);
						int modLengthHalfBytes = Integer.parseInt(line.substring(7, 9).trim(), 16);
						char operation = line.charAt(9);
						String symbolName = line.substring(10).trim();
						modificationRecordsBuffer.add(new MRecordTemp(modAddrRelativeCS, modLengthHalfBytes, operation, symbolName, this.currentCsLoadAddress));
						break;
					case 'E':
						if (!firstExecutionAddressSet) {
							if (line.length() > 1) {
								if (line.length() < 7) { /* 오류 처리 */ }
								else {
									int execAddrRelativeCS = Integer.parseInt(line.substring(1, 7).trim(), 16);
									rMgr.setFirstInstructionAddress(this.currentCsLoadAddress + execAddrRelativeCS);
									firstExecutionAddressSet = true;
								}
							} else {
								rMgr.setFirstInstructionAddress(this.currentCsLoadAddress); // 이 CS의 시작 주소
								firstExecutionAddressSet = true;
							}
						}
						this.programTotalCumulativeLength += currentCsDeclaredLength;
						this.currentCsLoadAddress = rMgr.getActualProgramLoadAddress() + this.programTotalCumulativeLength;
						break;
					default: /* 오류 처리 */ break;
				}
			}

			// M 레코드 적용 (제공된 파일[3] 로직)
			for (MRecordTemp mRec : modificationRecordsBuffer) {
				Integer symbolAbsoluteAddress = rMgr.getExternalSymbolAddress(mRec.symbolName);
				if (symbolAbsoluteAddress == null) { /* 오류 처리 */ continue; }
				int actualModMemoryAddr = mRec.csLoadAddressAtTimeOfRecord + mRec.csRelativeAddress;
				int numBytesToModify = (mRec.lengthHalfBytes + 1) / 2;
				if (actualModMemoryAddr < 0 || actualModMemoryAddr + numBytesToModify > rMgr.memory.length) { /* 오류 */ continue; }

				byte[] originalValueBytes = rMgr.getMemoryBytes(actualModMemoryAddr, numBytesToModify);
				if (originalValueBytes.length < numBytesToModify) { /* 오류 */ continue; }

				long originalValSegment = 0;
				if (mRec.lengthHalfBytes == 5) {
					originalValSegment = ( (long)(originalValueBytes[0] & 0x0F) << 16) |
							( (long)(originalValueBytes[1] & 0xFF) << 8 ) |
							( (long)(originalValueBytes[2] & 0xFF) );
				} else if (mRec.lengthHalfBytes == 6) {
					originalValSegment = ( (long)(originalValueBytes[0] & 0xFF) << 16) |
							( (long)(originalValueBytes[1] & 0xFF) << 8 ) |
							( (long)(originalValueBytes[2] & 0xFF) );
				} else { /* 오류 */ continue; }

				long modifiedValSegment = (mRec.operation == '+') ? (originalValSegment + symbolAbsoluteAddress)
						: (originalValSegment - symbolAbsoluteAddress);
				byte[] newValueBytes = new byte[numBytesToModify];
				if (mRec.lengthHalfBytes == 5) {
					newValueBytes[0] = (byte) ((originalValueBytes[0] & 0xF0) | ((modifiedValSegment >> 16) & 0x0F));
					newValueBytes[1] = (byte) ((modifiedValSegment >> 8) & 0xFF);
					newValueBytes[2] = (byte) (modifiedValSegment & 0xFF);
				} else {
					modifiedValSegment &= 0xFFFFFFL;
					newValueBytes[0] = (byte) ((modifiedValSegment >> 16) & 0xFF);
					newValueBytes[1] = (byte) ((modifiedValSegment >> 8) & 0xFF);
					newValueBytes[2] = (byte) (modifiedValSegment & 0xFF);
				}
				rMgr.setMemoryBytes(actualModMemoryAddr, newValueBytes, numBytesToModify);
			}

			if (!firstExecutionAddressSet && rMgr.getProgramName() != null && !rMgr.getProgramName().isEmpty()) {
				rMgr.setFirstInstructionAddress(rMgr.getActualProgramLoadAddress());
			}
			rMgr.setProgramTotalLength(this.programTotalCumulativeLength);

		} catch (IOException | NumberFormatException e) {
			System.err.println("Error during SicLoader.load: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
