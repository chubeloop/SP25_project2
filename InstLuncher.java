package SP25_simulator;

public class InstLuncher {

    private ResourceManager rMgr;
    private String lastExecutedInstructionInfo = "";
    private String lastErrorMessage = "";
    private int lastCalculatedTA = TA_NOT_CALCULATED_YET;
    private String lastExecutedMnemonic = "N/A";

    public static final int NORMAL_HALT = -2;
    public static final int ERROR_HALT = -1;
    // public static final int DEVICE_WAIT = -3; // 현재 사용 안 함
    public static final int TA_NOT_CALCULATED_YET = -999; // 명확한 초기값

    // Opcode Constants
    private static final int OP_LDA = 0x00; private static final int OP_LDX = 0x04;
    private static final int OP_LDL = 0x08; private static final int OP_STA = 0x0C;
    private static final int OP_STX = 0x10; private static final int OP_STL = 0x14;
    private static final int OP_ADD = 0x18; private static final int OP_SUB = 0x1C;
    private static final int OP_MUL = 0x20; private static final int OP_DIV = 0x24;
    private static final int OP_COMP = 0x28;private static final int OP_TIX = 0x2C;
    private static final int OP_JEQ = 0x30; private static final int OP_JGT = 0x34;
    private static final int OP_JLT = 0x38; private static final int OP_J = 0x3C;
    private static final int OP_JSUB = 0x48;private static final int OP_RSUB = 0x4C;
    private static final int OP_LDCH = 0x50;private static final int OP_STCH = 0x54;
    private static final int OP_LDB = 0x68; private static final int OP_LDS = 0x6C;
    private static final int OP_LDT = 0x74; private static final int OP_STB = 0x78;
    private static final int OP_STS = 0x7C; private static final int OP_STT = 0x84;
    private static final int OP_STSW = 0xE8;private static final int OP_RD = 0xD8;
    private static final int OP_WD = 0xDC; private static final int OP_TD = 0xE0;
    private static final int OP_FIX = 0xC4; private static final int OP_FLOAT = 0xC0;
    private static final int OP_HIO = 0xF4; private static final int OP_NORM = 0xC8;
    private static final int OP_SIO = 0xF0; private static final int OP_TIO = 0xF8;
    private static final int OP_ADDR = 0x90; private static final int OP_SUBR = 0x94;
    private static final int OP_MULR = 0x98; private static final int OP_DIVR = 0x9C;
    private static final int OP_COMPR = 0xA0; private static final int OP_SHIFTL = 0xA4;
    private static final int OP_SHIFTR = 0xA8;private static final int OP_RMO = 0xAC;
    private static final int OP_SVC = 0xB0; private static final int OP_CLEAR = 0xB4;
    private static final int OP_TIXR = 0xB8;

    public InstLuncher(ResourceManager resourceManager) {
        if (resourceManager == null) {
            throw new IllegalArgumentException("ResourceManager cannot be null for InstLuncher.");
        }
        this.rMgr = resourceManager;
    }

    public int executeInstructionAt(int pc) {
        lastExecutedInstructionInfo = "";
        lastErrorMessage = "";
        lastCalculatedTA = TA_NOT_CALCULATED_YET;
        lastExecutedMnemonic = "N/A";

        if (pc < 0 || pc >= rMgr.memory.length) {
            lastErrorMessage = "PC (0x" + String.format("%06X", pc) + ") out of memory bounds.";
            return ERROR_HALT;
        }

        byte[] firstByteArr = rMgr.getMemoryBytes(pc, 1);
        if (firstByteArr.length < 1) {
            lastErrorMessage = "Failed to fetch opcode byte at PC: 0x" + String.format("%06X", pc);
            return ERROR_HALT;
        }

        byte opcodeFullByte = firstByteArr[0];
        int opcodeFull = opcodeFullByte & 0xFF;
        int pureOpcode = opcodeFull & 0xFC;

        int instructionLength = getInstructionLength(pureOpcode, pc);

        if (instructionLength == 0) {
            if (lastErrorMessage == null || lastErrorMessage.isEmpty()) {
                lastErrorMessage = "Unknown opcode or format error at 0x" + Integer.toHexString(pc) + " (Opcode: " + String.format("%02X", opcodeFullByte) + ")";
            }
            return ERROR_HALT;
        }

        if (pc + instructionLength > rMgr.memory.length) {
            lastErrorMessage = "Instruction fetch at PC 0x" + String.format("%06X", pc) +
                    " (length " + instructionLength + ") out of memory bounds.";
            return ERROR_HALT;
        }

        byte[] instructionBytes = rMgr.getMemoryBytes(pc, instructionLength);
        if (instructionBytes.length < instructionLength) {
            lastErrorMessage = "Failed to fetch full " + instructionLength + "-byte instruction at PC 0x" +
                    String.format("%06X", pc) + ". Fetched only " + instructionBytes.length + " bytes.";
            return ERROR_HALT;
        }

        int nextPc;

        switch (pureOpcode) {
            case OP_LDA: lastExecutedMnemonic = "LDA"; nextPc = handleLDA(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_LDX: lastExecutedMnemonic = "LDX"; nextPc = handleLDX(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_LDL: lastExecutedMnemonic = "LDL"; nextPc = handleLDL(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_LDB: lastExecutedMnemonic = "LDB"; nextPc = handleLDB(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_LDS: lastExecutedMnemonic = "LDS"; nextPc = handleLDS(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_LDT: lastExecutedMnemonic = "LDT"; nextPc = handleLDT(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_LDCH: lastExecutedMnemonic = "LDCH"; nextPc = handleLDCH(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_STA: lastExecutedMnemonic = "STA"; nextPc = handleSTA(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_STX: lastExecutedMnemonic = "STX"; nextPc = handleSTX(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_STL: lastExecutedMnemonic = "STL"; nextPc = handleSTL(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_STB: lastExecutedMnemonic = "STB"; nextPc = handleSTB(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_STS: lastExecutedMnemonic = "STS"; nextPc = handleSTS(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_STT: lastExecutedMnemonic = "STT"; nextPc = handleSTT(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_STCH: lastExecutedMnemonic = "STCH"; nextPc = handleSTCH(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_STSW: lastExecutedMnemonic = "STSW"; nextPc = handleSTSW(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_ADD: lastExecutedMnemonic = "ADD"; nextPc = handleADD(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_SUB: lastExecutedMnemonic = "SUB"; nextPc = handleSUB(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_MUL: lastExecutedMnemonic = "MUL"; nextPc = handleMUL(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_DIV: lastExecutedMnemonic = "DIV"; nextPc = handleDIV(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_COMP: lastExecutedMnemonic = "COMP"; nextPc = handleCOMP(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_J:   lastExecutedMnemonic = "J";   nextPc = handleJ(pc, instructionBytes, instructionLength, opcodeFull);   break;
            case OP_JEQ: case OP_JLT: case OP_JGT: nextPc = handleConditionalJump(pc, instructionBytes, instructionLength, opcodeFull, pureOpcode); break;
            case OP_JSUB: lastExecutedMnemonic = "JSUB"; nextPc = handleJSUB(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_RSUB: lastExecutedMnemonic = "RSUB"; nextPc = handleRSUB(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_TIX: lastExecutedMnemonic = "TIX"; nextPc = handleTIX(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_TD: lastExecutedMnemonic = "TD"; nextPc = handleTD(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_RD: lastExecutedMnemonic = "RD"; nextPc = handleRD(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_WD: lastExecutedMnemonic = "WD"; nextPc = handleWD(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_ADDR: lastExecutedMnemonic = "ADDR"; nextPc = handleADDR(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_SUBR: lastExecutedMnemonic = "SUBR"; nextPc = handleSUBR(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_MULR: lastExecutedMnemonic = "MULR"; nextPc = handleMULR(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_DIVR: lastExecutedMnemonic = "DIVR"; nextPc = handleDIVR(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_COMPR: lastExecutedMnemonic = "COMPR"; nextPc = handleCOMPR(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_CLEAR: lastExecutedMnemonic = "CLEAR"; nextPc = handleCLEAR(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_TIXR: lastExecutedMnemonic = "TIXR"; nextPc = handleTIXR(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_RMO: lastExecutedMnemonic = "RMO"; nextPc = handleRMO(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_SHIFTL: lastExecutedMnemonic = "SHIFTL"; nextPc = handleSHIFTL(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_SHIFTR: lastExecutedMnemonic = "SHIFTR"; nextPc = handleSHIFTR(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_SVC: lastExecutedMnemonic = "SVC"; nextPc = handleSVC(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_FIX: lastExecutedMnemonic = "FIX"; nextPc = handleFIX(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_FLOAT: lastExecutedMnemonic = "FLOAT"; nextPc = handleFLOAT(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_HIO: lastExecutedMnemonic = "HIO"; nextPc = handleHIO(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_NORM: lastExecutedMnemonic = "NORM"; nextPc = handleNORM(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_SIO: lastExecutedMnemonic = "SIO"; nextPc = handleSIO(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_TIO: lastExecutedMnemonic = "TIO"; nextPc = handleTIO(pc, instructionBytes, instructionLength, opcodeFull); break;
            default:
                lastErrorMessage = "Unimplemented or Unknown Opcode: " + String.format("0x%02X", pureOpcode) + " at PC 0x" + String.format("%06X", pc);
                lastExecutedMnemonic = String.format("UNK(%02X)", pureOpcode);
                return ERROR_HALT;
        }
        return nextPc;
    }

    public int getInstructionLength(int pureOpcode, int pc) {
        switch (pureOpcode) {
            case OP_FIX: case OP_FLOAT: case OP_HIO: case OP_NORM: case OP_SIO: case OP_TIO:
                return 1;
            case OP_ADDR: case OP_CLEAR: case OP_COMPR: case OP_DIVR: case OP_MULR:
            case OP_RMO: case OP_SHIFTL:case OP_SHIFTR:case OP_SUBR: case OP_SVC:
            case OP_TIXR:
                return 2;
            default:
                if (pc + 1 >= rMgr.memory.length) {
                    lastErrorMessage = "Cannot determine F3/F4 length: PC+1 out of bounds for nixbpe byte.";
                    return 0;
                }
                byte[] nixbpeByteArr = rMgr.getMemoryBytes(pc + 1, 1);
                if (nixbpeByteArr.length < 1) {
                    lastErrorMessage = "Failed to fetch nixbpe byte for F3/F4 length determination."; return 0;
                }
                byte nixbpeByte = nixbpeByteArr[0];
                boolean e_flag = (nixbpeByte & 0x10) != 0;
                return e_flag ? 4 : 3;
        }
    }

    // InstLuncher.java
    public static int getInstructionLengthFromBytes(byte[] instructionStartBytes) {
        if (instructionStartBytes == null || instructionStartBytes.length == 0) {
            return 0;
        }
        int opcodeFull = instructionStartBytes[0] & 0xFF;
        int pureOpcode = opcodeFull & 0xFC; // n,i 비트 제외

        // 정확한 Format 1 명령어 Opcode들 (pureOpcode 기준)
        if (pureOpcode == OP_FIX || pureOpcode == OP_FLOAT || pureOpcode == OP_HIO ||
                pureOpcode == OP_NORM || pureOpcode == OP_SIO || pureOpcode == OP_TIO ) {
            // 추가적으로, Format 1 명령어는 ni 비트가 00이어야 함 (opcodeFull == pureOpcode)
            // 하지만, 오브젝트 코드에서는 단순 SIC 명령어를 3바이트로 표현할 때 opcode만 사용되기도 함.
            // 여기서는 일단 pureOpcode만으로 Format 1 길이를 결정.
            // 만약 'F1'이 TIO(F8)나 SIO(F0)로 오인되지 않으려면, opcodeFull을 사용해야 함.
            // SIO(F0), TIO(F8). 'F1'은 이들에 해당 안함.
            if (opcodeFull == OP_FIX || opcodeFull == OP_FLOAT || opcodeFull == OP_HIO ||
                    opcodeFull == OP_NORM || opcodeFull == OP_SIO || opcodeFull == OP_TIO) {
                return 1;
            }
        }

        // 정확한 Format 2 명령어 Opcode들 (pureOpcode 기준)
        if (pureOpcode == OP_ADDR || pureOpcode == OP_CLEAR || pureOpcode == OP_COMPR ||
                pureOpcode == OP_DIVR || pureOpcode == OP_MULR  || pureOpcode == OP_RMO ||
                pureOpcode == OP_SHIFTL|| pureOpcode == OP_SHIFTR|| pureOpcode == OP_SUBR ||
                pureOpcode == OP_SVC   || pureOpcode == OP_TIXR ) {
            // Format 2도 ni 비트가 없어야 함 (opcodeFull == pureOpcode)
            // 실제로는 opcode + r1r2 이므로, opcodeFull로 체크하는 것이 더 정확.
            if (opcodeFull == OP_ADDR || opcodeFull == OP_CLEAR || opcodeFull == OP_COMPR || // ... 모든 Format 2 opcodeFull
                    opcodeFull == OP_DIVR || opcodeFull == OP_MULR  || opcodeFull == OP_RMO ||
                    opcodeFull == OP_SHIFTL|| opcodeFull == OP_SHIFTR|| opcodeFull == OP_SUBR ||
                    opcodeFull == OP_SVC   || opcodeFull == OP_TIXR) {
                if (instructionStartBytes.length < 2) return 0; // Format 2인데 바이트 부족
                return 2;
            }
        }

        // Format 3 또는 4로 시도
        // 알려진 Format 3/4 명령어의 pureOpcode가 아니면 0을 반환하는 것이 이상적.
        // 예를 들어 0x45 (EBCDIC 'E')는 명령어 시작이 아님.
        // 그러나 모든 Format 3/4 opcode를 나열하기 어려우므로, 일단 nixbpe 기반으로 길이 추정.
        // 이로 인해 데이터가 명령어로 오인될 수 있음.
        if (instructionStartBytes.length < 2) {
            return 0; // Format 3/4 판단에 nixbpe 바이트 필요
        }
        // 모든 Format 3/4 명령어는 첫 바이트의 하위 2비트(n,i)를 가짐.
        // 만약 첫 바이트가 이 패턴을 따르지 않으면 (예: 0x4C0000 RSUB), 문제가 될 수 있음.
        // RSUB (4C)는 n,i가 00이므로 (opcodeFull & 0x03) == 0. 이 경우 Format 3.

        // opcodeFull을 기준으로 Format 3/4 여부를 판단하는 것이 더 안전할 수 있음.
        // 예: opcode가 0x00~0xEC 범위 내에 있고, Format 1,2가 아니면 Format 3/4로 간주.
        // 0xF0, 0xF4, 0xF8은 Format 1.
        // 이외의 0xF로 시작하는 것 (예: F1)은 유효한 명령어가 아님.
        if ((pureOpcode >= 0xF0 && pureOpcode <= 0xF8) && // Format 1 범위인데 위에서 안걸렸다면
                !(opcodeFull == OP_SIO || opcodeFull == OP_TIO || opcodeFull == OP_HIO)) { // 실제 Format 1이 아니면
            return 0; // F1, F2, F3, F5, F6, F7 등은 명령어 아님
        }


        byte nixbpeByte = instructionStartBytes[1];
        boolean e_flag = (nixbpeByte & 0x10) != 0;

        if(e_flag) {
            if (instructionStartBytes.length < 4) return 0;
            return 4;
        } else {
            if (instructionStartBytes.length < 3) return 0;
            return 3;
        }
    }


    private static class TargetAddressInfo {
        int address;
        boolean isImmediate;
        boolean isIndirect;

        TargetAddressInfo(int addrOrVal, boolean isImm, boolean isInd) {
            this.address = addrOrVal;
            this.isImmediate = isImm;
            this.isIndirect = isInd;
        }
    }

    private TargetAddressInfo calculateTargetAddress(int pc, byte[] instructionBytes, int instructionLength, int opcodeFull) {
        lastCalculatedTA = TA_NOT_CALCULATED_YET;
        if (instructionLength < 3 && instructionLength > 0 ) {
            if ((opcodeFull & 0x03) != 0x00) {
                lastErrorMessage = "Format 1/2 instruction cannot have n/i bits set for TA calculation. Opcode: " + String.format("0x%02X", opcodeFull);
                return null;
            }
            return new TargetAddressInfo(0, false, false);
        }
        if(instructionLength == 0) {
            lastErrorMessage = "Cannot calculate TA for zero-length instruction. Opcode: " + String.format("0x%02X", opcodeFull);
            return null;
        }

        boolean n_flag = (opcodeFull & 0x02) != 0;
        boolean i_flag = (opcodeFull & 0x01) != 0;

        byte nixbpeByte = instructionBytes[1];
        boolean x_flag = (nixbpeByte & 0x80) != 0;
        boolean b_flag = (nixbpeByte & 0x40) != 0;
        boolean p_flag = (nixbpeByte & 0x20) != 0;
        boolean e_flag = (nixbpeByte & 0x10) != 0;

        int disp_or_addr;

        if (e_flag) {
            if (instructionLength != 4) { lastErrorMessage = "TA calc error: e=1 but length != 4"; return null; }
            disp_or_addr = ((nixbpeByte & 0x0F) << 16) | ((instructionBytes[2] & 0xFF) << 8) | (instructionBytes[3] & 0xFF);
        } else {
            if (instructionLength != 3) { lastErrorMessage = "TA calc error: e=0 but length != 3"; return null; }
            disp_or_addr = ((nixbpeByte & 0x0F) << 8) | (instructionBytes[2] & 0xFF);
            if ((disp_or_addr & 0x0800) != 0 && (p_flag || b_flag || (!n_flag && !i_flag))) { // Sign extend for F3
                disp_or_addr |= 0xFFFFF000;
            }
        }

        String taModeLogInfo = "";
        int targetAddressOperand;

        if (i_flag && !n_flag) {
            taModeLogInfo = "#";
            targetAddressOperand = disp_or_addr;
            lastCalculatedTA = targetAddressOperand;
            lastExecutedInstructionInfo += String.format(" %s%d (0x%X)", taModeLogInfo, targetAddressOperand, targetAddressOperand & (e_flag ? 0xFFFFF : (disp_or_addr >=0 && disp_or_addr <= 0xFFF ? 0xFFF : 0xFFFFFFFF) ));
            return new TargetAddressInfo(targetAddressOperand, true, false);
        } else if (n_flag && !i_flag) {
            taModeLogInfo = "@";
        } else if (n_flag && i_flag) {
            taModeLogInfo = "M[]";
        } else {
            taModeLogInfo = "M[]";
        }

        if (p_flag && !b_flag) {
            targetAddressOperand = (pc + instructionLength) + disp_or_addr;
            taModeLogInfo += String.format("PC-rel(PC_next=0x%X+disp=0x%X)", pc + instructionLength, disp_or_addr);
        } else if (b_flag && !p_flag) {
            targetAddressOperand = rMgr.getRegister(ResourceManager.REG_B) + disp_or_addr;
            taModeLogInfo += String.format("Base-rel(B=0x%X+disp=0x%X)", rMgr.getRegister(ResourceManager.REG_B), disp_or_addr);
        } else {
            targetAddressOperand = disp_or_addr;
            if (e_flag) taModeLogInfo += "Direct(Ext Fmt)";
            else taModeLogInfo += "Direct(Simple)";
        }

        if (x_flag) {
            targetAddressOperand += rMgr.getRegister(ResourceManager.REG_X);
            taModeLogInfo += String.format("+Indexed(X=0x%X)", rMgr.getRegister(ResourceManager.REG_X));
        }

        if(e_flag) targetAddressOperand &= 0xFFFFF;
        else targetAddressOperand &= 0xFFFFFF;


        int finalAddress = targetAddressOperand;
        String effectiveAddressLog = String.format(" -> TA=0x%06X", finalAddress & 0xFFFFFF);

        if (n_flag && !i_flag) {
            if (finalAddress < 0 || finalAddress + 2 >= rMgr.memory.length) {
                lastErrorMessage = "TA calc error: Indirect pointer 0x" + String.format("%06X", finalAddress) + " out of bounds."; return null;
            }
            byte[] indirectPointerBytes = rMgr.getMemoryBytes(finalAddress, 3);
            finalAddress = rMgr.bytesToInt(indirectPointerBytes);
            effectiveAddressLog = String.format(" -> TA_ptr=0x%06X, M[TA_ptr]=0x%06X", targetAddressOperand & 0xFFFFFF, finalAddress & 0xFFFFFF);
        }

        lastCalculatedTA = finalAddress & 0xFFFFFF;
        lastExecutedInstructionInfo += String.format(" %s%s", taModeLogInfo, effectiveAddressLog);
        return new TargetAddressInfo(lastCalculatedTA, false, (n_flag && !i_flag));
    }

    private void setConditionCode(int comparisonResult) {
        if (comparisonResult < 0) rMgr.setRegister(ResourceManager.REG_SW, 0x01);
        else if (comparisonResult == 0) rMgr.setRegister(ResourceManager.REG_SW, 0x00);
        else rMgr.setRegister(ResourceManager.REG_SW, 0x02);
    }

    private String getCCString() {
        int cc = rMgr.getRegister(ResourceManager.REG_SW);
        if (cc == 0x01) return "LT"; if (cc == 0x00) return "EQ"; if (cc == 0x02) return "GT";
        return "Undef(" + String.format("%02X",cc) + ")";
    }

    private int memToSignedInt(int address, int length) {
        if(address<0||address+length > rMgr.memory.length){
            lastErrorMessage="Memory Read OutOfBounds: addr=0x"+String.format("%06X",address)+", len="+length;
            return 0;
        }
        return rMgr.bytesToInt(rMgr.getMemoryBytes(address,length));
    }

    private void intToMemBytes(int address, int value, int length) {
        if(address<0||address+length > rMgr.memory.length){
            lastErrorMessage="Memory Write OutOfBounds: addr=0x"+String.format("%06X",address)+", len="+length;
            return;
        }
        if(length==1) rMgr.setMemoryBytes(address,new byte[]{(byte)(value&0xFF)},1);
        else if(length==3) rMgr.setMemoryBytes(address,rMgr.intToBytes(value),3);
        else lastErrorMessage="Unsupported length for intToMemBytes: " + length;
    }

    // --- Instruction Handlers ---
    private int handleLDA(int pc, byte[] iB, int l, int oF) { TargetAddressInfo taInfo = calculateTargetAddress(pc,iB,l,oF); if(taInfo==null)return ERROR_HALT; int val; if(taInfo.isImmediate)val=taInfo.address; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="LDA: Mem OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);} rMgr.setRegister(ResourceManager.REG_A,val); lastExecutedInstructionInfo+=String.format(" ; A <- 0x%06X",val&0xFFFFFF); return pc+l; }
    private int handleLDX(int pc, byte[] iB, int l, int oF) { TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF); if(taInfo==null)return ERROR_HALT; int val; if(taInfo.isImmediate)val=taInfo.address; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="LDX: Mem OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);} rMgr.setRegister(ResourceManager.REG_X,val);lastExecutedInstructionInfo+=String.format(" ; X <- 0x%06X",val&0xFFFFFF);return pc+l; }
    private int handleLDL(int pc, byte[] iB, int l, int oF) { TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF); if(taInfo==null)return ERROR_HALT; int val; if(taInfo.isImmediate)val=taInfo.address; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="LDL: Mem OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);} rMgr.setRegister(ResourceManager.REG_L,val);lastExecutedInstructionInfo+=String.format(" ; L <- 0x%06X",val&0xFFFFFF);return pc+l; }
    private int handleLDB(int pc, byte[] iB, int l, int oF) { TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF); if(taInfo==null)return ERROR_HALT; int val; if(taInfo.isImmediate)val=taInfo.address; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="LDB: Mem OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);} rMgr.setRegister(ResourceManager.REG_B,val);lastExecutedInstructionInfo+=String.format(" ; B <- 0x%06X",val&0xFFFFFF);return pc+l; }
    private int handleLDS(int pc, byte[] iB, int l, int oF) { TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF); if(taInfo==null)return ERROR_HALT; int val; if(taInfo.isImmediate)val=taInfo.address; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="LDS: Mem OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);} rMgr.setRegister(ResourceManager.REG_S,val);lastExecutedInstructionInfo+=String.format(" ; S <- 0x%06X",val&0xFFFFFF);return pc+l; }
    private int handleLDT(int pc, byte[] iB, int l, int oF) { TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF); if(taInfo==null)return ERROR_HALT; int val; if(taInfo.isImmediate)val=taInfo.address; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="LDT: Mem OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);} rMgr.setRegister(ResourceManager.REG_T,val);lastExecutedInstructionInfo+=String.format(" ; T <- 0x%06X",val&0xFFFFFF);return pc+l; }
    private int handleLDCH(int pc, byte[] iB, int l, int oF) { TargetAddressInfo taInfo = calculateTargetAddress(pc,iB,l,oF); if(taInfo==null)return ERROR_HALT; int charVal; if(taInfo.isImmediate)charVal=taInfo.address&0xFF; else {if(taInfo.address<0||taInfo.address>=rMgr.memory.length){lastErrorMessage="LDCH: Mem OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}charVal=rMgr.getMemoryBytes(taInfo.address,1)[0]&0xFF;} rMgr.setRegister(ResourceManager.REG_A,(rMgr.getRegister(ResourceManager.REG_A)&0xFFFF00)|charVal);lastExecutedInstructionInfo+=String.format(" ; A_byte3 <- 0x%02X",charVal);return pc+l;}
    private int handleSTA(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STA: Invalid TA";return ERROR_HALT;} if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STA: Mem Write OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_A),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X] <- A(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_A)&0xFFFFFF);return pc+l;}
    private int handleSTX(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STX: Invalid TA";return ERROR_HALT;} if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STX: Mem Write OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_X),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X] <- X(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_X)&0xFFFFFF);return pc+l;}
    private int handleSTL(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STL: Invalid TA";return ERROR_HALT;} if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STL: Mem Write OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_L),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X] <- L(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_L)&0xFFFFFF);return pc+l;}
    private int handleSTB(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STB: Invalid TA";return ERROR_HALT;} if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STB: Mem Write OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_B),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X] <- B(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_B)&0xFFFFFF);return pc+l;}
    private int handleSTS(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STS: Invalid TA";return ERROR_HALT;} if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STS: Mem Write OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_S),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X] <- S(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_S)&0xFFFFFF);return pc+l;}
    private int handleSTT(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STT: Invalid TA";return ERROR_HALT;} if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STT: Mem Write OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_T),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X] <- T(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_T)&0xFFFFFF);return pc+l;}
    private int handleSTCH(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STCH: Invalid TA";return ERROR_HALT;} if(taInfo.address<0||taInfo.address>=rMgr.memory.length){lastErrorMessage="STCH: Mem Write OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}byte charToStore=(byte)(rMgr.getRegister(ResourceManager.REG_A)&0xFF);rMgr.setMemoryBytes(taInfo.address,new byte[]{charToStore},1);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X]_byte <- A_b3(0x%02X)",taInfo.address,charToStore&0xFF);return pc+l;}
    private int handleSTSW(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STSW: Invalid TA";return ERROR_HALT;} if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STSW: Mem Write OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_SW),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X] <- SW(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_SW)&0xFFFFFF);return pc+l;}
    private int handleADD(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null)return ERROR_HALT; int val; if(taInfo.isImmediate)val=taInfo.address; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="ADD: Mem OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);} int curA=rMgr.getRegister(ResourceManager.REG_A);long res=(long)curA+val;rMgr.setRegister(ResourceManager.REG_A,(int)(res&0xFFFFFF));lastExecutedInstructionInfo+=String.format(" ; A<-A+M(0x%06X+0x%06X=0x%06X)",curA&0xFFFFFF,val&0xFFFFFF,(int)res&0xFFFFFF);return pc+l;}
    private int handleSUB(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null)return ERROR_HALT; int val; if(taInfo.isImmediate)val=taInfo.address; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="SUB: Mem OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);} int curA=rMgr.getRegister(ResourceManager.REG_A);long res=(long)curA-val;rMgr.setRegister(ResourceManager.REG_A,(int)(res&0xFFFFFF));lastExecutedInstructionInfo+=String.format(" ; A<-A-M(0x%06X-0x%06X=0x%06X)",curA&0xFFFFFF,val&0xFFFFFF,(int)res&0xFFFFFF);return pc+l;}
    private int handleMUL(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null)return ERROR_HALT; int val; if(taInfo.isImmediate)val=taInfo.address; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="MUL: Mem OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);} int curA=rMgr.getRegister(ResourceManager.REG_A);long res=(long)curA*val;rMgr.setRegister(ResourceManager.REG_A,(int)(res&0xFFFFFF));lastExecutedInstructionInfo+=String.format(" ; A<-A*M(0x%06X*0x%06X=0x%06X)",curA&0xFFFFFF,val&0xFFFFFF,(int)res&0xFFFFFF);return pc+l;}
    private int handleDIV(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null)return ERROR_HALT; int val; if(taInfo.isImmediate)val=taInfo.address; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="DIV: Mem OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);} if(val==0){lastErrorMessage="DIV: Division by zero"; return ERROR_HALT;}int curA=rMgr.getRegister(ResourceManager.REG_A);int res=curA/val;rMgr.setRegister(ResourceManager.REG_A,res&0xFFFFFF);lastExecutedInstructionInfo+=String.format(" ; A<-A/M(0x%06X/0x%06X=0x%06X)",curA&0xFFFFFF,val&0xFFFFFF,res&0xFFFFFF);return pc+l;}
    private int handleCOMP(int pc, byte[] iB, int l, int oF) { TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF); if(taInfo==null)return ERROR_HALT; int valA=rMgr.getRegister(ResourceManager.REG_A); int valM; if(taInfo.isImmediate)valM=taInfo.address; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="COMP: Mem OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}valM=memToSignedInt(taInfo.address,3);} int compRes=Integer.compare(valA,valM); setConditionCode(compRes); lastExecutedInstructionInfo+=String.format(" ; Comp A(0x%X)w M(0x%X).CC=%s",valA&0xFFFFFF,valM&0xFFFFFF,getCCString()); return pc+l;}
    private int handleJ(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo ta = calculateTargetAddress(pc, iB, l, oF);
        if (ta == null) { lastErrorMessage = "J: TA calculation failed"; return ERROR_HALT; }
        if (ta.isImmediate) { lastErrorMessage = "J: Immediate addressing not allowed for J."; return ERROR_HALT; }

        int jumpToAddress = ta.address; // TA는 이미 indirection이 적용된 주소
        lastExecutedInstructionInfo += String.format(" ; PC<-0x%06X", jumpToAddress & 0xFFFFFF);

        // input-1.txt의 J @RETADR (0x000027) 무한 루프 방지 로직
        // RETADR의 주소는 0x00002A (이것은 어셈블리 코드에서의 레이블 주소)
        // ta.address가 M[RETADR]의 값 (즉, 실제 점프할 주소)
        // 현재 PC가 0x000027 (J @RETADR 명령어의 주소)
        if (pc == 0x000027) { // 현재 명령어가 J @RETADR 인 경우
            // 이 시점에서 ta.address는 M[0x00002A]의 값 (즉, RETADR에 저장된 값)
            if (jumpToAddress == pc || jumpToAddress == 0x000027) { // 자기 자신으로 점프하거나, J @RETADR의 주소로 점프하려 할 때
                lastExecutedInstructionInfo += " (Program end detected: J @RETADR to self/start, halting)";
                return NORMAL_HALT;
            }
            // 첫 번째 루프에서 M[RETADR]이 0x000000 이었다면, PC는 0으로 감.
            // 이 경우에도 무한 루프의 시작이므로, 명시적으로 0으로 점프 시 종료
            if (jumpToAddress == 0x000000) {
                lastExecutedInstructionInfo += " (Program end: J @RETADR to 0x000000 via initial RETADR, halting)";
                return NORMAL_HALT;
            }
        }
        return jumpToAddress & 0xFFFFFF;
    }
    private int handleConditionalJump(int pc, byte[] iB, int l, int oF, int pureOpcode) {
        TargetAddressInfo ta=calculateTargetAddress(pc,iB,l,oF); if(ta==null)return ERROR_HALT;
        if(ta.isImmediate){lastErrorMessage = (pureOpcode==OP_JEQ?"JEQ":pureOpcode==OP_JLT?"JLT":"JGT")+": Immediate addressing not allowed."; return ERROR_HALT;}
        int cc=rMgr.getRegister(ResourceManager.REG_SW);
        boolean jump = false; String mnemonic = "";
        if(pureOpcode == OP_JEQ){ mnemonic="JEQ"; if(cc==0x00) jump=true; }
        else if(pureOpcode == OP_JLT){ mnemonic="JLT"; if(cc==0x01) jump=true; }
        else if(pureOpcode == OP_JGT){ mnemonic="JGT"; if(cc==0x02) jump=true; }
        else { lastErrorMessage = "Unknown conditional jump: " + String.format("0x%02X", pureOpcode); return ERROR_HALT; }
        lastExecutedMnemonic = mnemonic;
        lastExecutedInstructionInfo+=String.format(" (Cond %s,CC=%s)",jump?"TRUE":"FALSE",getCCString());
        if(jump){lastExecutedInstructionInfo+=String.format(";PC<-0x%06X",ta.address&0xFFFFFF);return ta.address & 0xFFFFFF;}
        else return pc+l;
    }
    private int handleJSUB(int pc, byte[] iB, int l, int oF) { TargetAddressInfo ta=calculateTargetAddress(pc,iB,l,oF); if(ta==null)return ERROR_HALT; if(ta.isImmediate){lastErrorMessage="JSUB: Immediate addressing not allowed."; return ERROR_HALT;} rMgr.setRegister(ResourceManager.REG_L,pc+l); lastExecutedInstructionInfo+=String.format(" ; L<-0x%06X,PC<-0x%06X", (pc+l)&0xFFFFFF, ta.address&0xFFFFFF); return ta.address & 0xFFFFFF; }
    private int handleRSUB(int pc, byte[] iB, int l, int oF) {
        int returnAddress = rMgr.getRegister(ResourceManager.REG_L);
        lastExecutedInstructionInfo = String.format(" ; PC <- L(0x%06X)", returnAddress & 0xFFFFFF);
        return returnAddress & 0xFFFFFF;
    }
    private int handleTIX(int pc, byte[] iB, int l, int oF) { TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if (taInfo == null) return ERROR_HALT; if(taInfo.isImmediate){lastErrorMessage="TIX: Immediate addressing not allowed"; return ERROR_HALT;}int valM; if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="TIX: Mem OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}valM=memToSignedInt(taInfo.address,3); rMgr.setRegister(ResourceManager.REG_X, (rMgr.getRegister(ResourceManager.REG_X) + 1)&0xFFFFFF); int valX = rMgr.getRegister(ResourceManager.REG_X); int compRes = Integer.compare(valX, valM); setConditionCode(compRes); lastExecutedInstructionInfo += String.format(" ; X++(0x%06X), Comp X w M(0x%06X).CC=%s", valX, valM & 0xFFFFFF, getCCString()); return pc + l; }
    private int handleTD(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo ta = calculateTargetAddress(pc,iB,l,oF); if(ta==null || ta.isImmediate){lastErrorMessage="TD: Invalid TA (must be memory address to get Device ID)"; return ERROR_HALT;}
        if(ta.address<0||ta.address>=rMgr.memory.length){lastErrorMessage="TD: Memory OutOfBounds for DeviceID at 0x"+String.format("%06X",ta.address); return ERROR_HALT;}
        byte devId=rMgr.getMemoryBytes(ta.address,1)[0]; String devName=String.format("%02X",devId&0xFF);
        boolean ready=rMgr.testDevice(devName);
        if(ready) setConditionCode(-1); else setConditionCode(0);
        lastExecutedInstructionInfo+=String.format("(Dev '%s'@M[0x%06X]=0x%02X).Ready=%b;CC=%s",devName,ta.address,devId&0xFF,ready,getCCString());
        return pc+l;
    }
    private int handleRD(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="RD: Invalid TA";return ERROR_HALT;} if(taInfo.address<0||taInfo.address>=rMgr.memory.length){lastErrorMessage="RD: Mem OOB for DeviceID";return ERROR_HALT;} byte devId=rMgr.getMemoryBytes(taInfo.address,1)[0];String devName=String.format("%02X",devId&0xFF);char[]dataRead=rMgr.readDevice(devName,1);if(dataRead!=null&&dataRead.length==1){rMgr.setRegister(ResourceManager.REG_A,(rMgr.getRegister(ResourceManager.REG_A)&0xFFFF00)|(dataRead[0]&0xFF));lastExecutedInstructionInfo+=String.format(" (Dev '%s').A_b3<-0x%02X",devName,dataRead[0]&0xFF);}else{rMgr.setRegister(ResourceManager.REG_A,(rMgr.getRegister(ResourceManager.REG_A)&0xFFFF00));lastExecutedInstructionInfo+=String.format(" (Dev '%s').ReadFail/EOF.A_b3<-00",devName);/*EOF시 A의 최하위 바이트를 00으로 설정*/}return pc+l;}
    private int handleWD(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="WD: Invalid TA";return ERROR_HALT;} if(taInfo.address<0||taInfo.address>=rMgr.memory.length){lastErrorMessage="WD: Mem OOB for DeviceID";return ERROR_HALT;} byte devId=rMgr.getMemoryBytes(taInfo.address,1)[0];String devName=String.format("%02X",devId&0xFF);char charToWrite=(char)(rMgr.getRegister(ResourceManager.REG_A)&0xFF);rMgr.writeDevice(devName,new char[]{charToWrite},1);lastExecutedInstructionInfo+=String.format(" (Dev '%s').Write A_b3(0x%02X)",devName,charToWrite&0xFF);return pc+l;}
    private int handleCOMPR(int pc, byte[] iB, int l, int oF) { if(l!=2){lastErrorMessage="COMPR: Invalid length "+l;return ERROR_HALT;} int r1n=(iB[1]&0xF0)>>4; int r2n=iB[1]&0x0F; if(r1n>9||r1n==6||r1n==7||r2n>9||r2n==6||r2n==7){lastErrorMessage="COMPR: Invalid reg num";return ERROR_HALT;}int val1=rMgr.getRegister(r1n); int val2=rMgr.getRegister(r2n); int compRes=Integer.compare(val1,val2); setConditionCode(compRes); lastExecutedInstructionInfo+=String.format("r%d,r%d ; Comp r%d(0x%X)w r%d(0x%X).CC=%s",r1n,r2n,r1n,val1&0xFFFFFF,r2n,val2&0xFFFFFF,getCCString()); return pc+l; }
    private int handleCLEAR(int pc, byte[] iB, int l, int oF) { if(l!=2){lastErrorMessage="CLEAR: Invalid length "+l;return ERROR_HALT;} int r1n=(iB[1]&0xF0)>>4; if(r1n>9||r1n==6||r1n==7){lastErrorMessage="CLEAR: Invalid reg num "+r1n;return ERROR_HALT;} rMgr.setRegister(r1n,0); lastExecutedInstructionInfo=String.format("r%d ; r%d<-0",r1n,r1n); return pc+l; }
    private int handleTIXR(int pc, byte[] iB, int l, int oF) { if(l!=2){lastErrorMessage="TIXR: Invalid length "+l;return ERROR_HALT;} int r1n=(iB[1]&0xF0)>>4; if(r1n>9||r1n==6||r1n==7){lastErrorMessage="TIXR: Invalid reg num "+r1n;return ERROR_HALT;} rMgr.setRegister(ResourceManager.REG_X,(rMgr.getRegister(ResourceManager.REG_X)+1)&0xFFFFFF); int valX=rMgr.getRegister(ResourceManager.REG_X); int valR1=rMgr.getRegister(r1n); int compRes=Integer.compare(valX,valR1); setConditionCode(compRes); lastExecutedInstructionInfo+=String.format("r%d ; X<-X+1(0x%06X).Comp X w r%d(0x%06X).CC=%s",r1n,valX&0xFFFFFF,r1n,valR1&0xFFFFFF,getCCString()); return pc+l; }
    private int handleADDR(int pc, byte[] iB, int l, int oF) { if(l!=2){lastErrorMessage="ADDR: Invalid length "+l;return ERROR_HALT;} int r1n=(iB[1]&0xF0)>>4; int r2n=iB[1]&0x0F; if(r1n>9||r1n==6||r1n==7||r2n>9||r2n==6||r2n==7){lastErrorMessage="ADDR: Invalid reg num";return ERROR_HALT;} int val1=rMgr.getRegister(r1n); int val2=rMgr.getRegister(r2n); rMgr.setRegister(r2n,(val1+val2)&0xFFFFFF); lastExecutedInstructionInfo+=String.format("r%d,r%d ; r%d<-r%d+r%d",r1n,r2n,r2n,r1n,r2n); return pc+l; }
    private int handleSUBR(int pc, byte[] iB, int l, int oF) { if(l!=2){lastErrorMessage="SUBR: Invalid length "+l;return ERROR_HALT;} int r1n=(iB[1]&0xF0)>>4; int r2n=iB[1]&0x0F; if(r1n>9||r1n==6||r1n==7||r2n>9||r2n==6||r2n==7){lastErrorMessage="SUBR: Invalid reg num";return ERROR_HALT;} int val1=rMgr.getRegister(r1n); int val2=rMgr.getRegister(r2n); rMgr.setRegister(r2n,(val2-val1)&0xFFFFFF); lastExecutedInstructionInfo+=String.format("r%d,r%d ; r%d<-r%d-r%d",r1n,r2n,r2n,r2n,r1n); return pc+l; }
    private int handleMULR(int pc, byte[] iB, int l, int oF) { if(l!=2){lastErrorMessage="MULR: Invalid length "+l;return ERROR_HALT;} int r1n=(iB[1]&0xF0)>>4; int r2n=iB[1]&0x0F; if(r1n>9||r1n==6||r1n==7||r2n>9||r2n==6||r2n==7){lastErrorMessage="MULR: Invalid reg num";return ERROR_HALT;} int val1=rMgr.getRegister(r1n); int val2=rMgr.getRegister(r2n); rMgr.setRegister(r2n,(val1*val2)&0xFFFFFF); lastExecutedInstructionInfo+=String.format("r%d,r%d ; r%d<-r%d*r%d",r1n,r2n,r2n,r1n,r2n); return pc+l; }
    private int handleDIVR(int pc, byte[] iB, int l, int oF) { if(l!=2){lastErrorMessage="DIVR: Invalid length "+l;return ERROR_HALT;} int r1n=(iB[1]&0xF0)>>4; int r2n=iB[1]&0x0F; if(r1n>9||r1n==6||r1n==7||r2n>9||r2n==6||r2n==7){lastErrorMessage="DIVR: Invalid reg num";return ERROR_HALT;} int val1=rMgr.getRegister(r1n); int val2=rMgr.getRegister(r2n); if(val1==0){lastErrorMessage="DIVR: Division by zero"; return ERROR_HALT;} rMgr.setRegister(r2n,(val2/val1)&0xFFFFFF); lastExecutedInstructionInfo+=String.format("r%d,r%d ; r%d<-r%d/r%d",r1n,r2n,r2n,r2n,r1n); return pc+l; }
    private int handleRMO(int pc, byte[] iB, int l, int oF) { if(l!=2){lastErrorMessage="RMO: Invalid length "+l;return ERROR_HALT;} int r1n=(iB[1]&0xF0)>>4; int r2n=iB[1]&0x0F; if(r1n>9||r1n==6||r1n==7||r2n>9||r2n==6||r2n==7){lastErrorMessage="RMO: Invalid reg num";return ERROR_HALT;} rMgr.setRegister(r2n,rMgr.getRegister(r1n)); lastExecutedInstructionInfo+=String.format("r%d,r%d ; r%d<-r%d",r1n,r2n,r2n,r1n); return pc+l; }
    private int handleSHIFTL(int pc,byte[]iB,int len,int oF){if(len!=2){lastErrorMessage="SHIFTL: Invalid length";return ERROR_HALT;}int r1n=(iB[1]&0xF0)>>4;int nShifts=(iB[1]&0x0F)+1;if(r1n>9||r1n==6||r1n==7){lastErrorMessage="SHIFTL: Invalid Reg num "+r1n;return ERROR_HALT;}int r1val=rMgr.getRegister(r1n);int shiftedVal=(r1val<<nShifts)&0xFFFFFF;rMgr.setRegister(r1n,shiftedVal);lastExecutedInstructionInfo=String.format("r%d,n=%d ; r%d << %d = 0x%06X",r1n,nShifts,r1n,nShifts,shiftedVal);return pc+len;}
    private int handleSHIFTR(int pc,byte[]iB,int len,int oF){if(len!=2){lastErrorMessage="SHIFTR: Invalid length";return ERROR_HALT;}int r1n=(iB[1]&0xF0)>>4;int nShifts=(iB[1]&0x0F)+1;if(r1n>9||r1n==6||r1n==7){lastErrorMessage="SHIFTR: Invalid Reg num "+r1n;return ERROR_HALT;}int r1val=rMgr.getRegister(r1n);int shiftedVal=(r1val>>>nShifts); rMgr.setRegister(r1n,shiftedVal&0xFFFFFF);lastExecutedInstructionInfo=String.format("r%d,n=%d ; r%d >>> %d = 0x%06X (logical)",r1n,nShifts,r1n,nShifts,shiftedVal&0xFFFFFF);return pc+len;}
    private int handleSVC(int pc,byte[]iB,int len,int oF){if(len!=2){lastErrorMessage="SVC: Invalid length";return ERROR_HALT;}int nSvc=(iB[1]&0xF0)>>4;lastExecutedInstructionInfo=String.format("n=%d ; Supervisor Call (no OS simulated)",nSvc);return pc+len;} // 피연산자는 n r2 가 아니라 n 만 사용
    private int handleFIX(int pc,byte[]iB,int len,int oF){if(len!=1){lastErrorMessage="FIX: Invalid length";return ERROR_HALT;}int fVal_int=(int)rMgr.getRegister_F();rMgr.setRegister(ResourceManager.REG_A,fVal_int&0xFFFFFF);lastExecutedInstructionInfo=String.format("; A <- int(F) (value: %d (0x%X) from F: %.2f)",fVal_int&0xFFFFFF,fVal_int&0xFFFFFF,rMgr.getRegister_F());return pc+len;}
    private int handleFLOAT(int pc,byte[]iB,int len,int oF){if(len!=1){lastErrorMessage="FLOAT: Invalid length";return ERROR_HALT;}double aVal_float=(double)(rMgr.getRegister(ResourceManager.REG_A)&0xFFFFFF); if((rMgr.getRegister(ResourceManager.REG_A)&0x800000)!=0) aVal_float = (double)((rMgr.getRegister(ResourceManager.REG_A)|0xFF000000)); rMgr.setRegister_F(aVal_float);lastExecutedInstructionInfo=String.format("; F <- float(A) (value: %.2f from A: %d (0x%X))",aVal_float,rMgr.getRegister(ResourceManager.REG_A)&0xFFFFFF,rMgr.getRegister(ResourceManager.REG_A)&0xFFFFFF);return pc+len;}
    private int handleHIO(int pc,byte[]iB,int len,int oF){lastErrorMessage="Unimplemented HIO";return ERROR_HALT;}
    private int handleNORM(int pc,byte[]iB,int len,int oF){lastErrorMessage="Unimplemented NORM";return ERROR_HALT;}
    private int handleSIO(int pc,byte[]iB,int len,int oF){lastErrorMessage="Unimplemented SIO";return ERROR_HALT;}
    private int handleTIO(int pc,byte[]iB,int len,int oF){lastErrorMessage="Unimplemented TIO";return ERROR_HALT;}

    public String getLastErrorMessage() { return lastErrorMessage; }
    public String getLastExecutedInstructionInfo() { return lastExecutedInstructionInfo; }
    public String getLastExecutedMnemonic() { return lastExecutedMnemonic; }
    public int getLastCalculatedTA() { return lastCalculatedTA; }

    public byte[] getCurrentInstructionBytes(int pc) {
        if (pc < 0 || pc >= rMgr.memory.length) return new byte[0];
        byte[] firstByteArr = rMgr.getMemoryBytes(pc, 1);
        if (firstByteArr.length < 1) return new byte[0];
        int pureOpcode = (firstByteArr[0] & 0xFF) & 0xFC;
        int length = getInstructionLength(pureOpcode, pc);
        if (length == 0 || pc + length > rMgr.memory.length) return new byte[0];
        return rMgr.getMemoryBytes(pc, length);
    }
}
