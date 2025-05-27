package SP25_simulator;

// instruction에 따라 동작을 수행하는 메소드를 정의하는 클래스
public class InstLuncher {

    private ResourceManager rMgr;
    private String lastExecutedInstructionInfo = ""; // 상세 로그 (주소지정방식, TA, 값 등)
    private String lastErrorMessage = "";
    private int lastCalculatedTA = TA_NOT_CALCULATED_YET; // VisualSimulator의 TargetAddress 필드용
    private String lastExecutedMnemonic = "N/A";           // VisualSimulator의 Log 영역용 니모닉

    public static final int NORMAL_HALT = -2; // 정상 프로그램 종료 (예: RSUB이 OS로 복귀)
    public static final int ERROR_HALT = -1;  // 오류로 인한 실행 중단
    public static final int TA_NOT_CALCULATED_YET = -1; // lastCalculatedTA 초기값

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
    private static final int OP_STS = 0x7C; private static final int OP_STT = 0x84; // STT is 0x84
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
            lastErrorMessage = "Unknown opcode or error determining instruction length for opcode: " +
                    String.format("0x%02X at PC 0x%06X", pureOpcode, pc);
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

        int nextPc = pc + instructionLength;

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
            case OP_STT: lastExecutedMnemonic = "STT"; nextPc = handleSTT(pc, instructionBytes, instructionLength, opcodeFull); break; // 0x84
            case OP_STCH: lastExecutedMnemonic = "STCH"; nextPc = handleSTCH(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_STSW: lastExecutedMnemonic = "STSW"; nextPc = handleSTSW(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_ADD: lastExecutedMnemonic = "ADD"; nextPc = handleADD(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_SUB: lastExecutedMnemonic = "SUB"; nextPc = handleSUB(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_MUL: lastExecutedMnemonic = "MUL"; nextPc = handleMUL(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_DIV: lastExecutedMnemonic = "DIV"; nextPc = handleDIV(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_COMP: lastExecutedMnemonic = "COMP"; nextPc = handleCOMP(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_J: lastExecutedMnemonic = "J"; nextPc = handleJ(pc, instructionBytes, instructionLength, opcodeFull); break;
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
                lastErrorMessage = "Unimplemented or Unknown Opcode: " + String.format("0x%02X", pureOpcode);
                lastExecutedInstructionInfo = String.format("Unknown Opcode [0x%02X] at 0x%06X. Length: %d", pureOpcode, pc, instructionLength);
                lastExecutedMnemonic = String.format("UNK(%02X)", pureOpcode);
                return ERROR_HALT;
        }
        return nextPc;
    }

    private int getInstructionLength(int pureOpcode, int pc) {
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

    private static class TargetAddressInfo {
        int address; int value; boolean isImmediate; boolean isRegisterToRegister;
        TargetAddressInfo(int addrOrVal, boolean isImm) {this.isImmediate = isImm; if (isImm) {this.value = addrOrVal; this.address = -1;} else {this.address = addrOrVal; this.value = 0;} this.isRegisterToRegister = false;}
        TargetAddressInfo(int r1, int r2) {this.address = r1; this.value = r2; this.isImmediate = false; this.isRegisterToRegister = true;}
    }

    private TargetAddressInfo calculateTargetAddress(int pc, byte[] instructionBytes, int instructionLength, int opcodeFull) {
        lastCalculatedTA = TA_NOT_CALCULATED_YET;
        if (instructionLength < 3) { lastErrorMessage = "TA calc error: Not F3/F4"; return null; }
        boolean n_f = (opcodeFull&2)!=0, i_f = (opcodeFull&1)!=0; byte nix = instructionBytes[1];
        boolean x_f = (nix&0x80)!=0, b_f = (nix&0x40)!=0, p_f = (nix&0x20)!=0, e_f = (nix&0x10)!=0;
        int d_a;
        if(e_f){if(instructionLength!=4){lastErrorMessage="TA e=1 len!=4";return null;}d_a=((nix&0xF)<<16)|((instructionBytes[2]&0xFF)<<8)|(instructionBytes[3]&0xFF);}
        else{if(instructionLength!=3){lastErrorMessage="TA e=0 len!=3";return null;}d_a=((nix&0xF)<<8)|(instructionBytes[2]&0xFF);if(b_f||p_f||(!n_f&&!i_f&&!e_f)){if((d_a&0x800)!=0)d_a|=0xFFFFF000;}}
        String taLogInfo = ""; int ta=0; // taLogInfo는 lastExecutedInstructionInfo에 추가될 상세 주소지정방식 문자열
        if(i_f&&!n_f){taLogInfo="Immediate";lastCalculatedTA=d_a;lastExecutedInstructionInfo+=String.format(" #%d(0x%X)",d_a,d_a&(e_f?0xFFFFF:0xFFF));return new TargetAddressInfo(d_a,true);}
        else if(n_f&&!i_f)taLogInfo="Indirect"; else if(n_f&&i_f)taLogInfo="Simple"; else taLogInfo="Simple (SIC)";

        if(p_f&&!b_f){ta=(pc+instructionLength)+d_a;taLogInfo+=String.format(" PC-rel(PC_next=0x%X+disp=0x%X)",pc+instructionLength,d_a);}
        else if(b_f&&!p_f){ta=rMgr.getRegister(ResourceManager.REG_B)+d_a;taLogInfo+=String.format(" Base-rel(B=0x%X+disp=0x%X)",rMgr.getRegister(ResourceManager.REG_B),d_a);}
        else{ta=d_a;if(e_f)taLogInfo+=" Direct(Ext Fmt)";else taLogInfo+=" Direct(Simple/SIC)";}
        if(x_f){ta+=rMgr.getRegister(ResourceManager.REG_X);taLogInfo+=String.format("+Indexed(X=0x%X)",rMgr.getRegister(ResourceManager.REG_X));}
        ta&=0xFFFFFF; lastCalculatedTA=ta; String effAddrLog=String.format("TA=0x%06X",ta);
        if(n_f&&!i_f){if(ta<0||ta+2>=rMgr.memory.length){lastErrorMessage="TA Ind OOB @"+String.format("0x%06X",ta);return null;}byte[]ptr=rMgr.getMemoryBytes(ta,3);int finalTa=rMgr.bytesToInt(ptr);effAddrLog=String.format("TA_ptr=0x%06X,M[]=0x%06X",ta,finalTa&0xFFFFFF);ta=finalTa&0xFFFFFF;lastCalculatedTA=ta;}
        lastExecutedInstructionInfo+=String.format(" %s -> %s",taLogInfo,effAddrLog);
        return new TargetAddressInfo(ta,false);
    }

    private void setConditionCode(int comparisonResult) {
        if (comparisonResult < 0) rMgr.setRegister(ResourceManager.REG_SW, 0x01);      // LT (SIC/XE Book p.49: CC <)
        else if (comparisonResult == 0) rMgr.setRegister(ResourceManager.REG_SW, 0x00); // EQ (CC =)
        else rMgr.setRegister(ResourceManager.REG_SW, 0x02);      // GT (CC >)
    }
    private String getCCString() {
        int cc = rMgr.getRegister(ResourceManager.REG_SW);
        if (cc == 0x01) return "LT"; if (cc == 0x00) return "EQ"; if (cc == 0x02) return "GT";
        return "Undef(" + String.format("%02X",cc) + ")";
    }
    private int memToSignedInt(int address, int length) {
        if(address<0||address+length>rMgr.memory.length){lastErrorMessage="MemReadOOB: 0x"+String.format("%06X",address);return 0;}
        return rMgr.bytesToInt(rMgr.getMemoryBytes(address,length));
    }
    private void intToMemBytes(int address, int value, int length) {
        if(address<0||address+length>rMgr.memory.length){lastErrorMessage="MemWriteOOB: 0x"+String.format("%06X",address);return;}
        if(length==1)rMgr.setMemoryBytes(address,new byte[]{(byte)(value&0xFF)},1);
        else if(length==3)rMgr.setMemoryBytes(address,rMgr.intToBytes(value),3);
        else lastErrorMessage="Unsupported len for intToMemBytes";
    }

    // --- Individual Instruction Handlers ---
    private int handleLDA(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if (taInfo == null) return ERROR_HALT;
        int val; if(taInfo.isImmediate) val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="LDA MemOOB";return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);}
        rMgr.setRegister(ResourceManager.REG_A,val);lastExecutedInstructionInfo+=String.format(" ; A <- 0x%06X",val&0xFFFFFF);return pc+l;
    }
    private int handleLDX(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if (taInfo == null) return ERROR_HALT;
        int val; if(taInfo.isImmediate) val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="LDX MemOOB";return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);}
        rMgr.setRegister(ResourceManager.REG_X,val);lastExecutedInstructionInfo+=String.format(" ; X <- 0x%06X",val&0xFFFFFF);return pc+l;
    }
    private int handleLDL(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if (taInfo == null) return ERROR_HALT;
        int val; if(taInfo.isImmediate) val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="LDL MemOOB";return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);}
        rMgr.setRegister(ResourceManager.REG_L,val);lastExecutedInstructionInfo+=String.format(" ; L <- 0x%06X",val&0xFFFFFF);return pc+l;
    }
    private int handleLDB(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if (taInfo == null) return ERROR_HALT;
        int val; if(taInfo.isImmediate) val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="LDB MemOOB";return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);}
        rMgr.setRegister(ResourceManager.REG_B,val);lastExecutedInstructionInfo+=String.format(" ; B <- 0x%06X",val&0xFFFFFF);return pc+l;
    }
    private int handleLDS(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if (taInfo == null) return ERROR_HALT;
        int val; if(taInfo.isImmediate) val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="LDS MemOOB";return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);}
        rMgr.setRegister(ResourceManager.REG_S,val);lastExecutedInstructionInfo+=String.format(" ; S <- 0x%06X",val&0xFFFFFF);return pc+l;
    }
    private int handleLDT(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if (taInfo == null) return ERROR_HALT;
        int val; if(taInfo.isImmediate) val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="LDT MemOOB";return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);}
        rMgr.setRegister(ResourceManager.REG_T,val);lastExecutedInstructionInfo+=String.format(" ; T <- 0x%06X",val&0xFFFFFF);return pc+l;
    }
    private int handleLDCH(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if (taInfo == null) return ERROR_HALT;
        int charVal; if(taInfo.isImmediate) charVal=taInfo.value&0xFF; else {if(taInfo.address<0||taInfo.address>=rMgr.memory.length){lastErrorMessage="LDCH MemOOB";return ERROR_HALT;}charVal=rMgr.getMemoryBytes(taInfo.address,1)[0]&0xFF;}
        rMgr.setRegister(ResourceManager.REG_A,(rMgr.getRegister(ResourceManager.REG_A)&0xFFFF00)|charVal);lastExecutedInstructionInfo+=String.format(" ; A_byte3<-0x%02X",charVal);return pc+l;
    }
    private int handleSTA(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STA:No TA or Imm";return ERROR_HALT;}
        if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STA MemOOB";return ERROR_HALT;}
        intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_A),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X]<-A(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_A)&0xFFFFFF);return pc+l;
    }
    private int handleSTX(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STX:No TA or Imm";return ERROR_HALT;}
        if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STX MemOOB";return ERROR_HALT;}
        intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_X),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X]<-X(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_X)&0xFFFFFF);return pc+l;
    }
    private int handleSTL(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STL:No TA or Imm";return ERROR_HALT;}
        if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STL MemOOB";return ERROR_HALT;}
        intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_L),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X]<-L(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_L)&0xFFFFFF);return pc+l;
    }
    private int handleSTB(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STB:No TA or Imm";return ERROR_HALT;}
        if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STB MemOOB";return ERROR_HALT;}
        intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_B),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X]<-B(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_B)&0xFFFFFF);return pc+l;
    }
    private int handleSTS(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STS:No TA or Imm";return ERROR_HALT;}
        if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STS MemOOB";return ERROR_HALT;}
        intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_S),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X]<-S(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_S)&0xFFFFFF);return pc+l;
    }
    private int handleSTT(int pc, byte[] iB, int l, int oF) { // Opcode 0x84
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STT:No TA or Imm";return ERROR_HALT;}
        if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STT MemOOB";return ERROR_HALT;}
        intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_T),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X]<-T(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_T)&0xFFFFFF);return pc+l;
    }
    private int handleSTCH(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STCH:No TA or Imm";return ERROR_HALT;}
        if(taInfo.address<0||taInfo.address>=rMgr.memory.length){lastErrorMessage="STCH MemOOB";return ERROR_HALT;}
        byte charToStore=(byte)(rMgr.getRegister(ResourceManager.REG_A)&0xFF);rMgr.setMemoryBytes(taInfo.address,new byte[]{charToStore},1);
        lastExecutedInstructionInfo+=String.format(" ; M[0x%06X]<-A_byte3(0x%02X)",taInfo.address,charToStore&0xFF);return pc+l;
    }
    private int handleSTSW(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STSW:No TA or Imm";return ERROR_HALT;}
        if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STSW MemOOB";return ERROR_HALT;}
        intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_SW),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X]<-SW(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_SW)&0xFFFFFF);return pc+l;
    }
    private int handleADD(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if (taInfo == null) return ERROR_HALT;
        int val; if(taInfo.isImmediate) val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="ADD MemOOB";return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);}
        int curA=rMgr.getRegister(ResourceManager.REG_A);long res=(long)curA+val;rMgr.setRegister(ResourceManager.REG_A,(int)(res&0xFFFFFF));
        lastExecutedInstructionInfo+=String.format(" ; A<-A+M(0x%06X+0x%06X=0x%06X)",curA&0xFFFFFF,val&0xFFFFFF,(int)res&0xFFFFFF);return pc+l;
    }
    private int handleSUB(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if (taInfo == null) return ERROR_HALT;
        int val; if(taInfo.isImmediate) val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="SUB MemOOB";return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);}
        int curA=rMgr.getRegister(ResourceManager.REG_A);long res=(long)curA-val;rMgr.setRegister(ResourceManager.REG_A,(int)(res&0xFFFFFF));
        lastExecutedInstructionInfo+=String.format(" ; A<-A-M(0x%06X-0x%06X=0x%06X)",curA&0xFFFFFF,val&0xFFFFFF,(int)res&0xFFFFFF);return pc+l;
    }
    private int handleMUL(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if (taInfo == null) return ERROR_HALT;
        int val; if(taInfo.isImmediate) val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="MUL MemOOB";return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);}
        int curA=rMgr.getRegister(ResourceManager.REG_A);long res=(long)curA*val;rMgr.setRegister(ResourceManager.REG_A,(int)(res&0xFFFFFF)); // SIC/XE MUL result fits in A
        lastExecutedInstructionInfo+=String.format(" ; A<-A*M(0x%06X*0x%06X=0x%06X)",curA&0xFFFFFF,val&0xFFFFFF,(int)res&0xFFFFFF);return pc+l;
    }
    private int handleDIV(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if (taInfo == null) return ERROR_HALT;
        int val; if(taInfo.isImmediate) val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="DIV MemOOB";return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);}
        if(val==0){lastErrorMessage="DIV: Division by zero";return ERROR_HALT;}int curA=rMgr.getRegister(ResourceManager.REG_A);int quot=curA/val;rMgr.setRegister(ResourceManager.REG_A,quot&0xFFFFFF);
        lastExecutedInstructionInfo+=String.format(" ; A<-A/M(0x%06X/0x%06X=0x%06X)",curA&0xFFFFFF,val&0xFFFFFF,quot&0xFFFFFF);return pc+l;
    }
    private int handleCOMP(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if (taInfo == null) return ERROR_HALT;
        int val; if(taInfo.isImmediate) val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="COMP MemOOB";return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);}
        int curA_s=rMgr.getRegister(ResourceManager.REG_A);setConditionCode(Integer.compare(curA_s,val));
        lastExecutedInstructionInfo+=String.format(" ; CompA(0x%06X) w Val(0x%06X).CC=%s",curA_s&0xFFFFFF,val&0xFFFFFF,getCCString());return pc+l;
    }
    private int handleJ(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if(taInfo==null||taInfo.isImmediate){lastErrorMessage="J:No TA or Imm";return ERROR_HALT;}
        lastExecutedInstructionInfo+=String.format(" ; PC<-0x%06X",taInfo.address);return taInfo.address;
    }
    private int handleConditionalJump(int pc, byte[] iB, int l, int oF, int pO) {
        String mne="";boolean cond=false;int cc=rMgr.getRegister(ResourceManager.REG_SW);
        switch(pO){case OP_JEQ:mne="JEQ";if(cc==0x00)cond=true;break;case OP_JLT:mne="JLT";if(cc==0x01)cond=true;break;case OP_JGT:mne="JGT";if(cc==0x02)cond=true;break;default:return ERROR_HALT;}
        lastExecutedMnemonic=mne;TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage=mne+":No TA or Imm";return ERROR_HALT;}
        if(cond){lastExecutedInstructionInfo+=String.format(" (Cond TRUE,CC=%s);PC<-0x%06X",getCCString(),taInfo.address);return taInfo.address;}
        else{lastExecutedInstructionInfo+=String.format(" (Cond FALSE,CC=%s)",getCCString());return pc+l;}
    }
    private int handleJSUB(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if(taInfo==null||taInfo.isImmediate){lastErrorMessage="JSUB:No TA or Imm";return ERROR_HALT;}
        rMgr.setRegister(ResourceManager.REG_L,pc+l);lastExecutedInstructionInfo+=String.format(" ; L<-0x%06X,PC<-0x%06X",(pc+l),taInfo.address);return taInfo.address;
    }
    // RSUB 수정: 정상 종료를 위해 NORMAL_HALT 반환 (무한 루프 가능성 줄임)
    private int handleRSUB(int pc, byte[] iB, int l, int oF) {
        if (l != 3) {lastErrorMessage="RSUB: Invalid length"; return ERROR_HALT;}
        lastExecutedInstructionInfo = " ; Return from subroutine";
        // int retAddr = rMgr.getRegister(ResourceManager.REG_L);
        // lastExecutedInstructionInfo += String.format(" (normally PC <- L (0x%06X))", retAddr & 0xFFFFFF);
        // return retAddr & 0xFFFFFF; // 기존: L로 점프
        return NORMAL_HALT; // 시뮬레이터에 정상 종료 알림
    }
    private int handleTIX(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if (taInfo == null) return ERROR_HALT;
        int valX=rMgr.getRegister(ResourceManager.REG_X);valX=(valX+1)&0xFFFFFF;rMgr.setRegister(ResourceManager.REG_X,valX);
        int valM; if(taInfo.isImmediate) valM=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="TIX MemOOB";return ERROR_HALT;}valM=memToSignedInt(taInfo.address,3);}
        setConditionCode(Integer.compare(valX,valM));
        lastExecutedInstructionInfo+=String.format(" ; X++(0x%06X),CompX w M(0x%06X).CC=%s",valX,valM&0xFFFFFF,getCCString());return pc+l;
    }
    private int handleTD(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if(taInfo==null||taInfo.isImmediate){lastErrorMessage="TD:No TA or Imm";return ERROR_HALT;}
        if(taInfo.address<0||taInfo.address>=rMgr.memory.length){lastErrorMessage="TD MemOOB";return ERROR_HALT;}
        byte devId=rMgr.getMemoryBytes(taInfo.address,1)[0];String devName=String.format("%02X",devId&0xFF);
        boolean ready=rMgr.testDevice(devName);if(ready)setConditionCode(-1);else setConditionCode(0); // LT=ready, EQ=not_ready
        lastExecutedInstructionInfo+=String.format(" (Dev '%s'@M[0x%06X]=0x%02X).Ready=%b;CC=%s",devName,taInfo.address,devId&0xFF,ready,getCCString());return pc+l;
    }
    private int handleRD(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if(taInfo==null||taInfo.isImmediate){lastErrorMessage="RD:No TA or Imm";return ERROR_HALT;}
        if(taInfo.address<0||taInfo.address>=rMgr.memory.length){lastErrorMessage="RD MemOOB DevName";return ERROR_HALT;}
        byte devId=rMgr.getMemoryBytes(taInfo.address,1)[0];String devName=String.format("%02X",devId&0xFF);
        char[] dataRead = rMgr.readDevice(devName,1); // ResourceManager.readDevice는 char[] 반환
        if(dataRead!=null&&dataRead.length==1){rMgr.setRegister(ResourceManager.REG_A,(rMgr.getRegister(ResourceManager.REG_A)&0xFFFF00)|(dataRead[0]&0xFF));lastExecutedInstructionInfo+=String.format(" (Dev '%s').A_b3<-0x%02X",devName,dataRead[0]&0xFF);}
        else{lastExecutedInstructionInfo+=String.format(" (Dev '%s').ReadFail/EOF.A_b3<-00",devName);rMgr.setRegister(ResourceManager.REG_A,(rMgr.getRegister(ResourceManager.REG_A)&0xFFFF00));} // EOF시 A 최하위 00
        return pc+l;
    }
    private int handleWD(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, iB, l, oF); if(taInfo==null||taInfo.isImmediate){lastErrorMessage="WD:No TA or Imm";return ERROR_HALT;}
        if(taInfo.address<0||taInfo.address>=rMgr.memory.length){lastErrorMessage="WD MemOOB DevName";return ERROR_HALT;}
        byte devId=rMgr.getMemoryBytes(taInfo.address,1)[0];String devName=String.format("%02X",devId&0xFF);
        char dataToWrite=(char)(rMgr.getRegister(ResourceManager.REG_A)&0xFF);rMgr.writeDevice(devName,new char[]{dataToWrite},1);
        lastExecutedInstructionInfo+=String.format(" (Dev '%s').Write A_b3(0x%02X)",devName,dataToWrite&0xFF);return pc+l;
    }
    private int handleADDR(int pc, byte[] iB, int l, int oF) {
        if(l!=2){lastErrorMessage="ADDR:Invalid len";return ERROR_HALT;}int r1n=(iB[1]&0xF0)>>4;int r2n=iB[1]&0x0F;
        int v1=rMgr.getRegister(r1n);int v2=rMgr.getRegister(r2n);rMgr.setRegister(r2n,(v2+v1)&0xFFFFFF);
        lastExecutedInstructionInfo=String.format("r%d,r%d;r%d<-r%d+r%d(0x%X+0x%X=0x%X)",r1n,r2n,r2n,r2n,r1n,v2&0xFFFFFF,v1&0xFFFFFF,rMgr.getRegister(r2n)&0xFFFFFF);return pc+l;
    }
    private int handleSUBR(int pc, byte[] iB, int l, int oF) {
        if(l!=2){lastErrorMessage="SUBR:Invalid len";return ERROR_HALT;}int r1n=(iB[1]&0xF0)>>4;int r2n=iB[1]&0x0F;
        int v1=rMgr.getRegister(r1n);int v2=rMgr.getRegister(r2n);rMgr.setRegister(r2n,(v2-v1)&0xFFFFFF);
        lastExecutedInstructionInfo=String.format("r%d,r%d;r%d<-r%d-r%d(0x%X-0x%X=0x%X)",r1n,r2n,r2n,r2n,r1n,v2&0xFFFFFF,v1&0xFFFFFF,rMgr.getRegister(r2n)&0xFFFFFF);return pc+l;
    }
    private int handleMULR(int pc, byte[] iB, int l, int oF) {
        if(l!=2){lastErrorMessage="MULR:Invalid len";return ERROR_HALT;}int r1n=(iB[1]&0xF0)>>4;int r2n=iB[1]&0x0F;
        int v1=rMgr.getRegister(r1n);int v2=rMgr.getRegister(r2n);long res=(long)v2*v1;rMgr.setRegister(r2n,(int)(res&0xFFFFFF));
        lastExecutedInstructionInfo=String.format("r%d,r%d;r%d<-r%d*r%d(0x%X*0x%X=0x%X)",r1n,r2n,r2n,r2n,r1n,v2&0xFFFFFF,v1&0xFFFFFF,(int)res&0xFFFFFF);return pc+l;
    }
    private int handleDIVR(int pc, byte[] iB, int l, int oF) {
        if(l!=2){lastErrorMessage="DIVR:Invalid len";return ERROR_HALT;}int r1n=(iB[1]&0xF0)>>4;int r2n=iB[1]&0x0F;
        int v1=rMgr.getRegister(r1n);int v2=rMgr.getRegister(r2n);if(v1==0){lastErrorMessage="DIVR:Div by zero";return ERROR_HALT;}rMgr.setRegister(r2n,(v2/v1)&0xFFFFFF);
        lastExecutedInstructionInfo=String.format("r%d,r%d;r%d<-r%d/r%d(0x%X/0x%X=0x%X)",r1n,r2n,r2n,r2n,r1n,v2&0xFFFFFF,v1&0xFFFFFF,(v2/v1)&0xFFFFFF);return pc+l;
    }
    private int handleCOMPR(int pc, byte[] iB, int l, int oF) {
        if(l!=2){lastErrorMessage="COMPR:Invalid len";return ERROR_HALT;}int r1n=(iB[1]&0xF0)>>4;int r2n=iB[1]&0x0F;
        int v1s=rMgr.getRegister(r1n);int v2s=rMgr.getRegister(r2n);setConditionCode(Integer.compare(v1s,v2s));
        lastExecutedInstructionInfo=String.format("r%d,r%d;Comp r%d(0x%X) w r%d(0x%X).CC=%s",r1n,r2n,r1n,v1s&0xFFFFFF,r2n,v2s&0xFFFFFF,getCCString());return pc+l;
    }
    private int handleCLEAR(int pc, byte[] iB, int l, int oF) {
        if(l!=2){lastErrorMessage="CLEAR:Invalid len";return ERROR_HALT;}int r1n=(iB[1]&0xF0)>>4;rMgr.setRegister(r1n,0);
        lastExecutedInstructionInfo=String.format("r%d;r%d<-0",r1n,r1n);return pc+l;
    }
    private int handleTIXR(int pc, byte[] iB, int l, int oF) {
        if(l!=2){lastErrorMessage="TIXR:Invalid len";return ERROR_HALT;}int r1n=(iB[1]&0xF0)>>4;
        int valX=rMgr.getRegister(ResourceManager.REG_X);valX=(valX+1)&0xFFFFFF;rMgr.setRegister(ResourceManager.REG_X,valX);
        int valR1s=rMgr.getRegister(r1n);setConditionCode(Integer.compare(valX,valR1s));
        lastExecutedInstructionInfo=String.format("r%d;X++(0x%06X),CompX w r%d(0x%06X).CC=%s",r1n,valX,r1n,valR1s&0xFFFFFF,getCCString());return pc+l;
    }
    private int handleRMO(int pc, byte[] iB, int l, int oF) {
        if(l!=2){lastErrorMessage="RMO:Invalid len";return ERROR_HALT;}int r1n=(iB[1]&0xF0)>>4;int r2n=iB[1]&0x0F;
        rMgr.setRegister(r2n,rMgr.getRegister(r1n));lastExecutedInstructionInfo=String.format("r%d,r%d;r%d<-r%d(0x%X)",r1n,r2n,r2n,r1n,rMgr.getRegister(r1n)&0xFFFFFF);return pc+l;
    }
    // Format 1 (Unimplemented for now as not in input-1.txt, add if needed)
    private int handleFIX(int pc,byte[]iB,int len,int oF){lastExecutedMnemonic="FIX";lastErrorMessage="Unimpl";return ERROR_HALT;}
    private int handleFLOAT(int pc,byte[]iB,int len,int oF){lastExecutedMnemonic="FLOAT";lastErrorMessage="Unimpl";return ERROR_HALT;}
    private int handleHIO(int pc,byte[]iB,int len,int oF){lastExecutedMnemonic="HIO";lastErrorMessage="Unimpl";return ERROR_HALT;}
    private int handleNORM(int pc,byte[]iB,int len,int oF){lastExecutedMnemonic="NORM";lastErrorMessage="Unimpl";return ERROR_HALT;}
    private int handleSIO(int pc,byte[]iB,int len,int oF){lastExecutedMnemonic="SIO";lastErrorMessage="Unimpl";return ERROR_HALT;}
    private int handleTIO(int pc,byte[]iB,int len,int oF){lastExecutedMnemonic="TIO";lastErrorMessage="Unimpl";return ERROR_HALT;}
    // Other Format 2 (Unimplemented for now)
    private int handleSHIFTL(int pc,byte[]iB,int len,int oF){lastExecutedMnemonic="SHIFTL";lastErrorMessage="Unimpl";return ERROR_HALT;}
    private int handleSHIFTR(int pc,byte[]iB,int len,int oF){lastExecutedMnemonic="SHIFTR";lastErrorMessage="Unimpl";return ERROR_HALT;}
    private int handleSVC(int pc,byte[]iB,int len,int oF){lastExecutedMnemonic="SVC";lastErrorMessage="Unimpl";return ERROR_HALT;}


    public String getLastExecutedInstructionInfo() { return lastExecutedInstructionInfo; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public int getLastCalculatedTA() { return lastCalculatedTA; }
    public String getLastExecutedMnemonic() { return lastExecutedMnemonic; }

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
