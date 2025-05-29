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
	private List<MRecordTemp> modificationRecordsBuffer;

	private static class MRecordTemp {
		int csRelativeAddress; int lengthHalfBytes; char operation; String symbolName; int csLoadAddressAtTimeOfRecord;
		MRecordTemp(int addr, int len, char op, String sym, int csStart) {this.csRelativeAddress=addr;this.lengthHalfBytes=len;this.operation=op;this.symbolName=sym;this.csLoadAddressAtTimeOfRecord=csStart;}
	}

	public SicLoader(ResourceManager resourceManager) {
		if (resourceManager == null) throw new IllegalArgumentException("RM cannot be null for SicLoader.");
		this.rMgr = resourceManager;
		this.modificationRecordsBuffer = new ArrayList<>();
	}

	public void load(File objectCodeFile) {
		if (objectCodeFile == null || !objectCodeFile.exists() || !objectCodeFile.isFile()) { System.err.println("SicLoader: Obj file invalid: " + (objectCodeFile != null ? objectCodeFile.getPath() : "null")); return; }
		if (rMgr == null) { System.err.println("SicLoader: RM not init."); return; }

		this.currentCsLoadAddress = rMgr.getActualProgramLoadAddress();
		this.programTotalCumulativeLength = 0;
		this.firstExecutionAddressSet = false;
		this.modificationRecordsBuffer.clear();

		String line; String currentCsName = ""; int currentCsDeclaredLength = 0; boolean firstHRecordProcessed = false;

		try (BufferedReader reader = new BufferedReader(new FileReader(objectCodeFile))) {
			while ((line = reader.readLine()) != null) {
				line = line.trim().replace('\t', ' '); if (line.isEmpty()) continue;
				char recordType = line.charAt(0);
				switch (recordType) {
					case 'H':
						if (line.length() < 19) { System.err.println("SicLoader: Malformed H: " + line); continue; }
						currentCsName = line.substring(1, 7).trim();
						int csObjStartAddr = Integer.parseInt(line.substring(7, 13).trim(), 16);
						currentCsDeclaredLength = Integer.parseInt(line.substring(13, 19).trim(), 16);
						if (!firstHRecordProcessed) { rMgr.setProgramName(currentCsName); rMgr.setHRecordObjectProgramStartAddress(csObjStartAddr); firstHRecordProcessed = true; }
						rMgr.addExternalSymbol(currentCsName, this.currentCsLoadAddress);
						break;
					case 'D':
						if (line.length() < 13) { System.err.println("SicLoader: Malformed D: " + line); continue; }
						for (int i=1; i < line.length(); i+=12) { if(i+12 > line.length()) break; String defSym=line.substring(i,i+6).trim(); int defAddrRel=Integer.parseInt(line.substring(i+6,i+12).trim(),16); rMgr.addExternalSymbol(defSym, this.currentCsLoadAddress + defAddrRel); }
						break;
					case 'R': break;
					case 'T':
						if (line.length() < 9) { System.err.println("SicLoader: Malformed T (short): " + line); continue; }
						int tRecStartAddrRelative = Integer.parseInt(line.substring(1, 7).trim(), 16);
						int tRecLengthBytes = Integer.parseInt(line.substring(7, 9).trim(), 16);
						if (line.length() < 9 + tRecLengthBytes * 2) { System.err.println("SicLoader: Malformed T (len mismatch): " + line); continue; }
						String objectCodeHex = line.substring(9, 9 + tRecLengthBytes * 2);
						int actualMemAddr = this.currentCsLoadAddress + tRecStartAddrRelative;
						rMgr.setMemoryHex(actualMemAddr, objectCodeHex);
						rMgr.addTRecordLoadedRegion(actualMemAddr, tRecLengthBytes); // T-레코드 로드 영역 정보 추가
						break;
					case 'M':
						if (line.length() < 11) { System.err.println("SicLoader: Malformed M: " + line); continue; }
						modificationRecordsBuffer.add(new MRecordTemp(Integer.parseInt(line.substring(1,7).trim(),16),Integer.parseInt(line.substring(7,9).trim(),16),line.charAt(9),line.substring(10).trim(),this.currentCsLoadAddress));
						break;
					case 'E':
						if (!firstExecutionAddressSet) { if (line.length() > 1) { if (line.length() < 7) {} else {rMgr.setFirstInstructionAddress(this.currentCsLoadAddress + Integer.parseInt(line.substring(1, 7).trim(), 16)); firstExecutionAddressSet = true;} } else { rMgr.setFirstInstructionAddress(rMgr.getActualProgramLoadAddress()); firstExecutionAddressSet = true; } }
						this.programTotalCumulativeLength += currentCsDeclaredLength; this.currentCsLoadAddress = rMgr.getActualProgramLoadAddress() + this.programTotalCumulativeLength;
						currentCsName = ""; currentCsDeclaredLength = 0;
						break;
					default: System.err.println("SicLoader: Unknown record type '" + recordType + "': " + line); break;
				}
			}
			for (MRecordTemp mRec : modificationRecordsBuffer) {
				Integer symAbsAddr = rMgr.getExternalSymbolAddress(mRec.symbolName); if (symAbsAddr == null) {System.err.println("SicLoader: MRec Err - Sym '"+mRec.symbolName+"' not found."); continue; }
				int actModMemAddr = mRec.csLoadAddressAtTimeOfRecord + mRec.csRelativeAddress;
				// *** numBytesToModify 선언 및 초기화 위치 수정/확인 ***
				int numBytesToModify = (mRec.lengthHalfBytes + 1) / 2;
				if (actModMemAddr < 0 || actModMemAddr + numBytesToModify > rMgr.memory.length) {System.err.println("SicLoader: MRec Addr OOB 0x"+Integer.toHexString(actModMemAddr)); continue; }
				byte[] origBytes = rMgr.getMemoryBytes(actModMemAddr, numBytesToModify); if(origBytes.length<numBytesToModify){System.err.println("SicLoader: MRec - Read orig failed 0x"+Integer.toHexString(actModMemAddr));continue;}
				long origValSeg = 0;
				if (mRec.lengthHalfBytes == 5) { origValSeg = ((long)(origBytes[0]&0x0F)<<16)|((long)(origBytes[1]&0xFF)<<8)|((long)(origBytes[2]&0xFF)); }
				else if (mRec.lengthHalfBytes == 6) { origValSeg = ((long)(origBytes[0]&0xFF)<<16)|((long)(origBytes[1]&0xFF)<<8)|((long)(origBytes[2]&0xFF)); }
				else { System.err.println("SicLoader: MRec - Invalid lenHB: " + mRec.lengthHalfBytes); continue;}
				long modValSeg = (mRec.operation=='+')?(origValSeg+symAbsAddr):(origValSeg-symAbsAddr);
				byte[] newBytes = new byte[numBytesToModify]; // 여기서 numBytesToModify 사용
				if(mRec.lengthHalfBytes==5){newBytes[0]=(byte)((origBytes[0]&0xF0)|((modValSeg>>16)&0x0F));newBytes[1]=(byte)((modValSeg>>8)&0xFF);newBytes[2]=(byte)(modValSeg&0xFF);}
				else {modValSeg&=0xFFFFFFL;newBytes[0]=(byte)((modValSeg>>16)&0xFF);newBytes[1]=(byte)((modValSeg>>8)&0xFF);newBytes[2]=(byte)(modValSeg&0xFF);}
				rMgr.setMemoryBytes(actModMemAddr, newBytes, numBytesToModify); // 마지막 인자도 numBytesToModify
			}
			if (!firstExecutionAddressSet && rMgr.getProgramName() != null && !rMgr.getProgramName().isEmpty()) { rMgr.setFirstInstructionAddress(rMgr.getActualProgramLoadAddress()); }
			rMgr.setProgramTotalLength(this.programTotalCumulativeLength);
		} catch (IOException | NumberFormatException e) { System.err.println("SicLoader: Error during load: " + e.getMessage()); e.printStackTrace();
		} catch (Exception e) { System.err.println("SicLoader: Unexpected error: " + e.getMessage()); e.printStackTrace(); }
	}
}
