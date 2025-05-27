package SP25_simulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList; // M 레코드 임시 저장용
import java.util.List;    // M 레코드 임시 저장용

public class SicLoader {
	ResourceManager rMgr;
	private int currentCsLoadAddress = 0;
	private int programTotalCumulativeLength = 0; // 모든 CS의 누적 길이
	private boolean firstExecutionAddressSet = false;

	private List<MRecordTemp> modificationRecordsBuffer = new ArrayList<>();

	// M 레코드 임시 저장을 위한 내부 클래스
	private static class MRecordTemp {
		int csRelativeAddress;
		int lengthHalfBytes;
		char operation;
		String symbolName;
		int csLoadAddressAtTimeOfRecord; // 이 M 레코드가 속한 CS의 로드 시작 주소

		MRecordTemp(int addr, int len, char op, String sym, int csStart) {
			this.csRelativeAddress = addr;
			this.lengthHalfBytes = len;
			this.operation = op;
			this.symbolName = sym;
			this.csLoadAddressAtTimeOfRecord = csStart;
		}
	}

	public SicLoader(ResourceManager resourceManager) {
		if (resourceManager == null) {
			throw new IllegalArgumentException("ResourceManager cannot be null for SicLoader.");
		}
		this.rMgr = resourceManager;
	}

	public void setResourceManager(ResourceManager resourceManager) { // 스켈레톤 호환성
		if (resourceManager == null) {
			throw new IllegalArgumentException("ResourceManager cannot be null.");
		}
		this.rMgr = resourceManager;
	}

	public void load(File objectCodeFile) {
		if (objectCodeFile == null || !objectCodeFile.exists() || !objectCodeFile.isFile()) {
			System.err.println("Object code file is invalid or does not exist: " +
					(objectCodeFile != null ? objectCodeFile.getPath() : "null"));
			return;
		}
		if (rMgr == null) {
			System.err.println("ResourceManager is not set in SicLoader. Cannot load.");
			return;
		}

		rMgr.initializeResource(); // 로드 전 리소스 초기화
		this.currentCsLoadAddress = rMgr.getActualProgramLoadAddress(); // 일반적으로 0에서 시작
		this.programTotalCumulativeLength = 0;
		this.firstExecutionAddressSet = false;
		this.modificationRecordsBuffer.clear();

		String line;
		String currentCsName = "";
		int currentCsDeclaredLength = 0;

		try (BufferedReader reader = new BufferedReader(new FileReader(objectCodeFile))) {
			while ((line = reader.readLine()) != null) {
				line = line.trim(); // 앞뒤 공백 제거
				if (line.isEmpty()) continue;

				// 탭을 공백으로 대체 (목적 코드 파일에 탭이 있는 경우 대비)
				line = line.replace('\t', ' ');

				char recordType = line.charAt(0);

				switch (recordType) {
					case 'H':
						if (line.length() < 1 + 6 + 6 + 6) { // H + 이름(6) + 시작(6) + 길이(6) = 19
							System.err.println("Malformed H record (too short, needs 19 chars): " + line);
							continue;
						}
						currentCsName = line.substring(1, 7).trim();
						// int csStartAddrInObj = Integer.parseInt(line.substring(7, 13).trim(), 16); // 사용 안함
						currentCsDeclaredLength = Integer.parseInt(line.substring(13, 19).trim(), 16);

						if (rMgr.getProgramName().isEmpty()) { // 첫 번째 CS
							rMgr.setProgramName(currentCsName);
							rMgr.setHRecordObjectProgramStartAddress(Integer.parseInt(line.substring(7, 13).trim(), 16));
							// 실제 로드 주소는 currentCsLoadAddress (기본 0)
							// rMgr.setActualProgramLoadAddress(this.currentCsLoadAddress); // 이미 생성자에서 0으로 초기화됨
						}
						rMgr.addExternalSymbol(currentCsName, this.currentCsLoadAddress); // ESTAB에 CS 이름과 로드 주소 저장
						break;

					case 'D':
						if (line.length() < 1 + 6 + 6) { // D + 심볼(6) + 주소(6) = 13
							System.err.println("Malformed D record (too short): " + line);
							continue;
						}
						for (int i = 1; i < line.length(); i += 12) {
							if (i + 12 > line.length()) {
								System.err.println("Malformed D record (segment length problem): " + line.substring(i));
								break;
							}
							String defSymbol = line.substring(i, i + 6).trim();
							int defAddrRelative = Integer.parseInt(line.substring(i + 6, i + 12).trim(), 16);
							rMgr.addExternalSymbol(defSymbol, this.currentCsLoadAddress + defAddrRelative);
						}
						break;

					case 'R':
						// EXTREF 심볼. M 레코드 처리 시 ESTAB에서 조회. 여기서 특별히 저장할 필요는 없음.
						break;

					case 'T':
						if (line.length() < 1 + 6 + 2) { // T + 시작주소(6) + 길이(2) = 9
							System.err.println("Malformed T record (too short): " + line);
							continue;
						}
						int tRecStartAddrRelative = Integer.parseInt(line.substring(1, 7).trim(), 16);
						int tRecLengthBytes = Integer.parseInt(line.substring(7, 9).trim(), 16);

						if (line.length() < 9 + tRecLengthBytes * 2) {
							System.err.println("Malformed T record (object code length mismatch): " + line +
									" Expected hex length: " + tRecLengthBytes * 2 +
									" Got: " + (line.length() - 9));
							continue;
						}
						String objectCodeHex = line.substring(9, 9 + tRecLengthBytes * 2);
						int actualMemAddr = this.currentCsLoadAddress + tRecStartAddrRelative;
						rMgr.setMemoryHex(actualMemAddr, objectCodeHex);
						break;

					case 'M':
						if (line.length() < 1 + 6 + 2 + 1 + 1) { // M + 주소(6) + 길이(2) + 연산자(1) + 심볼(1) = 11
							System.err.println("Malformed M record (too short): " + line);
							continue;
						}
						int modAddrRelativeCS = Integer.parseInt(line.substring(1, 7).trim(), 16);
						int modLengthHalfBytes = Integer.parseInt(line.substring(7, 9).trim(), 16);
						char operation = line.charAt(9);
						String symbolName = line.substring(10).trim();

						modificationRecordsBuffer.add(new MRecordTemp(modAddrRelativeCS, modLengthHalfBytes, operation, symbolName, this.currentCsLoadAddress));
						break;

					case 'E':
						if (!firstExecutionAddressSet) {
							if (line.length() > 1) { // 주소값이 명시된 경우
								if (line.length() < 1 + 6) {
									System.err.println("Malformed E record (address too short): " + line);
								} else {
									int execAddrRelativeCS = Integer.parseInt(line.substring(1, 7).trim(), 16);
									rMgr.setFirstInstructionAddress(this.currentCsLoadAddress + execAddrRelativeCS);
									firstExecutionAddressSet = true;
								}
							} else { // 주소값 없이 'E'만 온 경우 (이 CS의 시작 주소를 의미)
								rMgr.setFirstInstructionAddress(this.currentCsLoadAddress);
								firstExecutionAddressSet = true;
							}
						}
						// 현재 CS 로딩 완료, 다음 CS를 위한 주소 업데이트
						this.programTotalCumulativeLength += currentCsDeclaredLength;
						this.currentCsLoadAddress = rMgr.getActualProgramLoadAddress() + this.programTotalCumulativeLength;
						// currentCsName, currentCsDeclaredLength는 다음 H 레코드에서 갱신
						break;
					default:
						System.err.println("Unknown record type encountered: '" + recordType + "' in line: " + line);
						break;
				}
			}

			// --- 모든 T 레코드 로드 후 M 레코드 적용 ---
			for (MRecordTemp mRec : modificationRecordsBuffer) {
				Integer symbolAbsoluteAddress = rMgr.getExternalSymbolAddress(mRec.symbolName);
				if (symbolAbsoluteAddress == null) {
					System.err.println(String.format("Loader M-Record Error: Symbol '%s' not found in ESTAB for M-rec (RelAddr %04X, CSStart %06X)",
							mRec.symbolName, mRec.csRelativeAddress, mRec.csLoadAddressAtTimeOfRecord));
					continue;
				}

				int actualModMemoryAddr = mRec.csLoadAddressAtTimeOfRecord + mRec.csRelativeAddress;
				int numBytesToModify = (mRec.lengthHalfBytes + 1) / 2; // 5->3, 6->3

				if (actualModMemoryAddr < 0 || actualModMemoryAddr + numBytesToModify > rMgr.memory.length) {
					System.err.println(String.format("Loader M-Record Error: Memory address 0x%06X out of bounds for modification.", actualModMemoryAddr));
					continue;
				}

				byte[] originalValueBytes = rMgr.getMemoryBytes(actualModMemoryAddr, numBytesToModify);
				if (originalValueBytes.length < numBytesToModify) {
					System.err.println("Error: Could not read memory for M record at " + String.format("0x%06X", actualModMemoryAddr));
					continue;
				}

				long originalValSegment = 0;
				// M 레코드는 특정 비트만 수정해야 함.
				// modLengthHalfBytes: 05 (20비트), 06 (24비트)
				if (mRec.lengthHalfBytes == 5) { // 하위 20비트 (메모리의 3바이트 중 하위 2.5 바이트 부분)
					originalValSegment = ( (long)(originalValueBytes[0] & 0x0F) << 16) |
							( (long)(originalValueBytes[1] & 0xFF) << 8  ) |
							( (long)(originalValueBytes[2] & 0xFF)       );
				} else if (mRec.lengthHalfBytes == 6) { // 24비트 전체
					originalValSegment = ( (long)(originalValueBytes[0] & 0xFF) << 16) |
							( (long)(originalValueBytes[1] & 0xFF) << 8  ) |
							( (long)(originalValueBytes[2] & 0xFF)       );
				} else {
					System.err.println("Unsupported M-record length: " + mRec.lengthHalfBytes);
					continue;
				}


				long modifiedValSegment;
				if (mRec.operation == '+') {
					modifiedValSegment = originalValSegment + symbolAbsoluteAddress;
				} else {
					modifiedValSegment = originalValSegment - symbolAbsoluteAddress;
				}

				// 수정된 값을 다시 바이트 배열로 변환하여 메모리에 씀
				byte[] newValueBytes = new byte[numBytesToModify];
				if (mRec.lengthHalfBytes == 5) { // 20비트 수정
					// 원래 값의 상위 4비트(originalValueBytes[0]의 상위 니블)는 유지
					newValueBytes[0] = (byte) ((originalValueBytes[0] & 0xF0) | ((modifiedValSegment >> 16) & 0x0F));
					newValueBytes[1] = (byte) ((modifiedValSegment >> 8) & 0xFF);
					newValueBytes[2] = (byte) (modifiedValSegment & 0xFF);
				} else { // 24비트 수정
					modifiedValSegment &= 0xFFFFFFL; // 24비트 마스크
					newValueBytes[0] = (byte) ((modifiedValSegment >> 16) & 0xFF);
					newValueBytes[1] = (byte) ((modifiedValSegment >> 8) & 0xFF);
					newValueBytes[2] = (byte) (modifiedValSegment & 0xFF);
				}
				rMgr.setMemoryBytes(actualModMemoryAddr, newValueBytes, numBytesToModify);
				// System.out.println(String.format("M-Applied: @%06X val %s%s(%06X) origSeg %06X newSeg %06X", actualModMemoryAddr, Character.toString(mRec.operation), mRec.symbolName, symbolAbsoluteAddress, originalValSegment, modifiedValSegment & 0xFFFFFFL));
			}
			// --- M 레코드 적용 완료 ---


			if (!firstExecutionAddressSet && rMgr.getProgramName() != null && !rMgr.getProgramName().isEmpty()) {
				rMgr.setFirstInstructionAddress(rMgr.getActualProgramLoadAddress());
			}
			rMgr.setProgramTotalLength(this.programTotalCumulativeLength);

		} catch (IOException e) {
			System.err.println("Error reading object code file '" + objectCodeFile.getPath() + "': " + e.getMessage());
			e.printStackTrace();
		} catch (NumberFormatException e) {
			System.err.println("Error parsing number in object code: " + e.getMessage() + ". Check logs for offending line.");
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("An unexpected error occurred during loading: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
