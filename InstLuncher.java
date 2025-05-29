package SP25_simulator;

public class InstLuncher {

    private ResourceManager rMgr;
    private String lastExecutedInstructionInfo = "";
    private String lastErrorMessage = "";
    private int lastCalculatedTA = TA_NOT_CALCULATED_YET;
    private String lastExecutedMnemonic = "N/A";

    public static final int NORMAL_HALT = -2;
    public static final int ERROR_HALT = -1;
    public static final int TA_NOT_CALCULATED_YET = -1;

    // Opcode Constants (SIC/XE 표준 및 제공된 파일[4] 참조)
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
    private static final int OP_STS = 0x7C; private static final int OP_STT = 0x84; // Corrected (was 0x80 in file[4] comments)
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
            // lastErrorMessage is set by getInstructionLength
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
            case OP_STT: lastExecutedMnemonic = "STT"; nextPc = handleSTT(pc, instructionBytes, instructionLength, opcodeFull); break;
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
            default: // Format 3 or 4
                if (pc + 1 >= rMgr.memory.length) {
                    lastErrorMessage = "Cannot determine F3/F4 length: PC+1 out of bounds for nixbpe byte.";
                    return 0; // Error
                }
                byte[] nixbpeByteArr = rMgr.getMemoryBytes(pc + 1, 1);
                if (nixbpeByteArr.length < 1) {
                    lastErrorMessage = "Failed to fetch nixbpe byte for F3/F4 length determination."; return 0; // Error
                }
                byte nixbpeByte = nixbpeByteArr[0];
                boolean e_flag = (nixbpeByte & 0x10) != 0; // Check e bit for format 4
                return e_flag ? 4 : 3;
        }
    }

    private static class TargetAddressInfo {
        int address;
        int value;
        boolean isImmediate;
        boolean isRegisterToRegister; // For Format 2

        TargetAddressInfo(int addrOrVal, boolean isImm) {
            this.isImmediate = isImm;
            if (isImm) {
                this.value = addrOrVal; this.address = -1; // No memory address for immediate
            } else {
                this.address = addrOrVal; this.value = 0; // Value will be fetched from memory if needed
            }
            this.isRegisterToRegister = false;
        }
        TargetAddressInfo(int r1, int r2) { // For Format 2 register-register ops
            this.address = r1; // operand1 register number
            this.value = r2;   // operand2 register number
            this.isImmediate = false;
            this.isRegisterToRegister = true;
        }
    }

    private TargetAddressInfo calculateTargetAddress(int pc, byte[] instructionBytes, int instructionLength, int opcodeFull) {
        lastCalculatedTA = TA_NOT_CALCULATED_YET; // Reset before calculation

        // Format 3/4 specific TA calculation
        if (instructionLength < 3) {
            lastErrorMessage = "calculateTargetAddress called for non-Format 3/4 instruction (length " + instructionLength + "). Opcode: " + String.format("0x%02X", opcodeFull & 0xFC);
            return null; // Not a Format 3 or 4 instruction, or error
        }

        boolean n_flag = (opcodeFull & 0x02) != 0; // Indirect
        boolean i_flag = (opcodeFull & 0x01) != 0; // Immediate

        byte nixbpeByte = instructionBytes[1];
        boolean x_flag = (nixbpeByte & 0x80) != 0; // Indexed
        boolean b_flag = (nixbpeByte & 0x40) != 0; // Base-relative
        boolean p_flag = (nixbpeByte & 0x20) != 0; // PC-relative
        boolean e_flag = (nixbpeByte & 0x10) != 0; // Extended format

        int disp_or_addr;

        if (e_flag) { // Format 4: 20-bit address
            if (instructionLength != 4) { lastErrorMessage = "TA calc error: e=1 but length!=4"; return null; }
            disp_or_addr = ((nixbpeByte & 0x0F) << 16) |   // Lower 4 bits of nixbpe
                    ((instructionBytes[2] & 0xFF) << 8) |
                    (instructionBytes[3] & 0xFF);
        } else { // Format 3: 12-bit displacement
            if (instructionLength != 3) { lastErrorMessage = "TA calc error: e=0 but length!=3"; return null; }
            disp_or_addr = ((nixbpeByte & 0x0F) << 8) | (instructionBytes[2] & 0xFF);
            // Sign-extend 12-bit displacement if it's negative (for PC/Base relative or direct if b=p=0)
            if (b_flag || p_flag || (!n_flag && !i_flag && !e_flag)) { // Check if it's displacement
                if ((disp_or_addr & 0x0800) != 0) { // If 12th bit (sign bit) is 1
                    disp_or_addr |= 0xFFFFF000; // Sign extend to 32-bit
                }
            }
        }

        String taModeLogInfo = "";
        int targetAddress = 0;

        // 1. Determine addressing mode and calculate TA before indirection
        if (i_flag && !n_flag) { // Immediate addressing (i=1, n=0)
            taModeLogInfo = "Immediate";
            lastCalculatedTA = disp_or_addr; // The value itself is the operand
            lastExecutedInstructionInfo += String.format(" #%d (0x%X)", disp_or_addr, disp_or_addr & (e_flag ? 0xFFFFF : 0xFFF) ); // Log immediate value
            return new TargetAddressInfo(disp_or_addr, true);
        } else if (n_flag && !i_flag) { // Indirect addressing (i=0, n=1)
            taModeLogInfo = "Indirect";
            // TA calculation continues below, then indirection is applied
        } else if (n_flag && i_flag)  { // Simple addressing (i=1, n=1) - SIC compatible
            taModeLogInfo = "Simple";
        } else { // Simple addressing (i=0, n=0) - This case should not occur for SIC/XE Format 3/4 if b or p is set.
            // If b=0 and p=0, it's direct addressing (disp_or_addr is the TA).
            taModeLogInfo = "Simple (SIC Direct if b=p=0)";
        }

        // 2. Calculate base for TA (PC-relative, Base-relative, or Direct)
        if (p_flag && !b_flag) { // PC-relative (p=1, b=0)
            targetAddress = (pc + instructionLength) + disp_or_addr;
            taModeLogInfo += String.format(" PC-rel(PC_next=0x%X+disp=0x%X)", pc + instructionLength, disp_or_addr);
        } else if (b_flag && !p_flag) { // Base-relative (b=1, p=0)
            targetAddress = rMgr.getRegister(ResourceManager.REG_B) + disp_or_addr;
            taModeLogInfo += String.format(" Base-rel(B=0x%X+disp=0x%X)", rMgr.getRegister(ResourceManager.REG_B), disp_or_addr);
        } else { // Direct addressing (b=0, p=0) or SIC simple (n=1,i=1 but not immediate)
            targetAddress = disp_or_addr; // disp_or_addr is the absolute address (or part of it for F4)
            if (e_flag) taModeLogInfo += " Direct(Ext Fmt)";
            else taModeLogInfo += " Direct(Simple/SIC)";
        }

        // 3. Apply indexing if x_flag is set
        if (x_flag) {
            targetAddress += rMgr.getRegister(ResourceManager.REG_X);
            taModeLogInfo += String.format("+Indexed(X=0x%X)", rMgr.getRegister(ResourceManager.REG_X));
        }
        targetAddress &= 0xFFFFFF; // Ensure 24-bit address for SIC/XE (though F4 uses 20-bit in instruction)
        lastCalculatedTA = targetAddress; // Store TA before indirection
        String effectiveAddressLog = String.format("TA=0x%06X", targetAddress);

        // 4. Apply indirection if n_flag is set (and not immediate)
        if (n_flag && !i_flag) { // Indirect addressing
            if (targetAddress < 0 || targetAddress + 2 >= rMgr.memory.length) {
                lastErrorMessage = "TA calc error: Indirect pointer 0x" + String.format("%06X",targetAddress) + " out of bounds.";
                return null;
            }
            byte[] indirectPointerBytes = rMgr.getMemoryBytes(targetAddress, 3);
            int finalTargetAddress = rMgr.bytesToInt(indirectPointerBytes);
            effectiveAddressLog = String.format("TA_ptr=0x%06X, M[TA_ptr]=0x%06X", targetAddress, finalTargetAddress & 0xFFFFFF);
            targetAddress = finalTargetAddress & 0xFFFFFF; // Final TA after indirection
            lastCalculatedTA = targetAddress; // Store final TA
        }

        lastExecutedInstructionInfo += String.format(" %s -> %s", taModeLogInfo, effectiveAddressLog);

        // Final check for TA validity
        if (targetAddress < 0 || targetAddress >= rMgr.memory.length ) {
            lastErrorMessage = "Calculated TA 0x" + String.format("%06X", targetAddress) + " out of memory bounds.";
            // return null; // Optionally halt if final TA is invalid (STCH/LDCH might use smaller part of mem)
        }
        return new TargetAddressInfo(targetAddress, false);
    }

    private void setConditionCode(int comparisonResult) {
        if (comparisonResult < 0) rMgr.setRegister(ResourceManager.REG_SW, 0x01);      // LT (CC < as per SIC/XE book p.49)
        else if (comparisonResult == 0) rMgr.setRegister(ResourceManager.REG_SW, 0x00); // EQ (CC =)
        else rMgr.setRegister(ResourceManager.REG_SW, 0x02);      // GT (CC >)
    }

    private String getCCString() {
        int cc = rMgr.getRegister(ResourceManager.REG_SW);
        if (cc == 0x01) return "LT";
        if (cc == 0x00) return "EQ";
        if (cc == 0x02) return "GT";
        return "Undef(" + String.format("%02X",cc) + ")";
    }

    private int memToSignedInt(int address, int length) {
        if(address<0||address+length > rMgr.memory.length){
            lastErrorMessage="Memory Read OutOfBounds: addr=0x"+String.format("%06X",address)+", len="+length;
            return 0; // Or throw exception
        }
        return rMgr.bytesToInt(rMgr.getMemoryBytes(address,length));
    }

    private void intToMemBytes(int address, int value, int length) {
        if(address<0||address+length > rMgr.memory.length){
            lastErrorMessage="Memory Write OutOfBounds: addr=0x"+String.format("%06X",address)+", len="+length;
            return; // Or throw exception
        }
        if(length==1) rMgr.setMemoryBytes(address,new byte[]{(byte)(value&0xFF)},1);
        else if(length==3) rMgr.setMemoryBytes(address,rMgr.intToBytes(value),3); // Assumes intToBytes returns 3 bytes
        else lastErrorMessage="Unsupported length for intToMemBytes: " + length;
    }

    // --- Individual Instruction Handlers ---
    // (All handlers log details to lastExecutedInstructionInfo)

    private int handleLDA(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc,iB,l,oF); if(taInfo==null)return ERROR_HALT;
        int val; if(taInfo.isImmediate)val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="LDA: Memory access for operand OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);}
        rMgr.setRegister(ResourceManager.REG_A,val);
        System.out.println("[InstLuncher.handleLDA] After setRegister for A, value in ResourceManager: " + rMgr.getRegister(ResourceManager.REG_A));
        lastExecutedInstructionInfo+=String.format(" ; A <- 0x%06X",val&0xFFFFFF);
        return pc+l;
    }
    private int handleLDX(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc,iB,l,oF); if(taInfo==null)return ERROR_HALT;
        int val; if(taInfo.isImmediate)val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="LDX: Memory access for operand OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);}
        rMgr.setRegister(ResourceManager.REG_X,val);lastExecutedInstructionInfo+=String.format(" ; X <- 0x%06X",val&0xFFFFFF);return pc+l;
    }
    private int handleLDL(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc,iB,l,oF); if(taInfo==null)return ERROR_HALT;
        int val; if(taInfo.isImmediate)val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="LDL: Memory access for operand OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);}
        rMgr.setRegister(ResourceManager.REG_L,val);lastExecutedInstructionInfo+=String.format(" ; L <- 0x%06X",val&0xFFFFFF);return pc+l;
    }
    private int handleLDB(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc,iB,l,oF); if(taInfo==null)return ERROR_HALT;
        int val; if(taInfo.isImmediate)val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="LDB: Memory access for operand OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);}
        rMgr.setRegister(ResourceManager.REG_B,val);lastExecutedInstructionInfo+=String.format(" ; B <- 0x%06X",val&0xFFFFFF);return pc+l;
    }
    private int handleLDS(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc,iB,l,oF); if(taInfo==null)return ERROR_HALT;
        int val; if(taInfo.isImmediate)val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="LDS: Memory access for operand OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);}
        rMgr.setRegister(ResourceManager.REG_S,val);lastExecutedInstructionInfo+=String.format(" ; S <- 0x%06X",val&0xFFFFFF);return pc+l;
    }
    private int handleLDT(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc,iB,l,oF); if(taInfo==null)return ERROR_HALT;
        int val; if(taInfo.isImmediate)val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="LDT: Memory access for operand OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);}
        rMgr.setRegister(ResourceManager.REG_T,val);lastExecutedInstructionInfo+=String.format(" ; T <- 0x%06X",val&0xFFFFFF);return pc+l;
    }
    private int handleLDCH(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc,iB,l,oF); if(taInfo==null)return ERROR_HALT;
        int charVal; if(taInfo.isImmediate)charVal=taInfo.value&0xFF; else {if(taInfo.address<0||taInfo.address>=rMgr.memory.length){lastErrorMessage="LDCH: Memory access for operand OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;}charVal=rMgr.getMemoryBytes(taInfo.address,1)[0]&0xFF;}
        rMgr.setRegister(ResourceManager.REG_A,(rMgr.getRegister(ResourceManager.REG_A)&0xFFFF00)|charVal);lastExecutedInstructionInfo+=String.format(" ; A_byte3 <- 0x%02X",charVal);return pc+l;
    }
    private int handleSTA(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STA: Invalid TA (must be memory address)";return ERROR_HALT;} if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STA: Memory write OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;} intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_A),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X] <- A(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_A)&0xFFFFFF);return pc+l;}
    private int handleSTX(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STX: Invalid TA";return ERROR_HALT;} if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STX: Memory write OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;} intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_X),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X] <- X(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_X)&0xFFFFFF);return pc+l;}
    private int handleSTL(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STL: Invalid TA";return ERROR_HALT;} if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STL: Memory write OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;} intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_L),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X] <- L(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_L)&0xFFFFFF);return pc+l;}
    private int handleSTB(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STB: Invalid TA";return ERROR_HALT;} if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STB: Memory write OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;} intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_B),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X] <- B(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_B)&0xFFFFFF);return pc+l;}
    private int handleSTS(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STS: Invalid TA";return ERROR_HALT;} if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STS: Memory write OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;} intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_S),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X] <- S(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_S)&0xFFFFFF);return pc+l;}
    private int handleSTT(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STT: Invalid TA";return ERROR_HALT;} if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STT: Memory write OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;} intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_T),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X] <- T(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_T)&0xFFFFFF);return pc+l;}
    private int handleSTCH(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STCH: Invalid TA";return ERROR_HALT;} if(taInfo.address<0||taInfo.address>=rMgr.memory.length){lastErrorMessage="STCH: Memory write OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;} byte charToStore=(byte)(rMgr.getRegister(ResourceManager.REG_A)&0xFF);rMgr.setMemoryBytes(taInfo.address,new byte[]{charToStore},1);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X] <- A_byte3(0x%02X)",taInfo.address,charToStore&0xFF);return pc+l;}
    private int handleSTSW(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="STSW: Invalid TA";return ERROR_HALT;} if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="STSW: Memory write OOB @0x"+String.format("%06X",taInfo.address);return ERROR_HALT;} intToMemBytes(taInfo.address,rMgr.getRegister(ResourceManager.REG_SW),3);lastExecutedInstructionInfo+=String.format(" ; M[0x%06X] <- SW(0x%06X)",taInfo.address,rMgr.getRegister(ResourceManager.REG_SW)&0xFFFFFF);return pc+l;}
    private int handleADD(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null)return ERROR_HALT; int val; if(taInfo.isImmediate)val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="ADD: Mem OOB";return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);} int curA=rMgr.getRegister(ResourceManager.REG_A);long res=(long)curA+val;rMgr.setRegister(ResourceManager.REG_A,(int)(res&0xFFFFFF));lastExecutedInstructionInfo+=String.format(" ; A<-A+M(0x%06X+0x%06X=0x%06X)",curA&0xFFFFFF,val&0xFFFFFF,(int)res&0xFFFFFF);return pc+l;}
    private int handleSUB(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null)return ERROR_HALT; int val; if(taInfo.isImmediate)val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="SUB: Mem OOB";return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);} int curA=rMgr.getRegister(ResourceManager.REG_A);long res=(long)curA-val;rMgr.setRegister(ResourceManager.REG_A,(int)(res&0xFFFFFF));lastExecutedInstructionInfo+=String.format(" ; A<-A-M(0x%06X-0x%06X=0x%06X)",curA&0xFFFFFF,val&0xFFFFFF,(int)res&0xFFFFFF);return pc+l;}
    private int handleMUL(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null)return ERROR_HALT; int val; if(taInfo.isImmediate)val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="MUL: Mem OOB";return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);} int curA=rMgr.getRegister(ResourceManager.REG_A);long res=(long)curA*val;rMgr.setRegister(ResourceManager.REG_A,(int)(res&0xFFFFFF));lastExecutedInstructionInfo+=String.format(" ; A<-A*M(0x%06X*0x%06X=0x%06X)",curA&0xFFFFFF,val&0xFFFFFF,(int)res&0xFFFFFF);return pc+l;}
    private int handleDIV(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null)return ERROR_HALT; int val; if(taInfo.isImmediate)val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="DIV: Mem OOB";return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);} if(val==0){lastErrorMessage="DIV: Division by zero";return ERROR_HALT;}int curA=rMgr.getRegister(ResourceManager.REG_A);int quot=curA/val;rMgr.setRegister(ResourceManager.REG_A,quot&0xFFFFFF);lastExecutedInstructionInfo+=String.format(" ; A<-A/M(0x%06X/0x%06X=0x%06X)",curA&0xFFFFFF,val&0xFFFFFF,quot&0xFFFFFF);return pc+l;}
    private int handleCOMP(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null)return ERROR_HALT; int val; if(taInfo.isImmediate)val=taInfo.value; else {if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="COMP: Mem OOB";return ERROR_HALT;}val=memToSignedInt(taInfo.address,3);} int curA_s=rMgr.getRegister(ResourceManager.REG_A);setConditionCode(Integer.compare(curA_s,val));lastExecutedInstructionInfo+=String.format(" ; CompA(0x%06X)wVal(0x%06X).CC=%s",curA_s&0xFFFFFF,val&0xFFFFFF,getCCString());return pc+l;}
    private int handleJ(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="J: Invalid TA";return ERROR_HALT;} lastExecutedInstructionInfo+=String.format(" ; PC<-0x%06X",taInfo.address);return taInfo.address;}
    private int handleConditionalJump(int pc, byte[] iB, int l, int oF, int pO) {String mne="";boolean cond=false;int cc=rMgr.getRegister(ResourceManager.REG_SW);switch(pO){case OP_JEQ:mne="JEQ";if(cc==0x00)cond=true;break;case OP_JLT:mne="JLT";if(cc==0x01)cond=true;break;case OP_JGT:mne="JGT";if(cc==0x02)cond=true;break;default:lastErrorMessage="Unknown CondJump Opcode";return ERROR_HALT;} lastExecutedMnemonic=mne;TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage=mne+": Invalid TA";return ERROR_HALT;} if(cond){lastExecutedInstructionInfo+=String.format(" (Cond TRUE,CC=%s);PC<-0x%06X",getCCString(),taInfo.address);return taInfo.address;} else{lastExecutedInstructionInfo+=String.format(" (Cond FALSE,CC=%s)",getCCString());return pc+l;}}
    private int handleJSUB(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="JSUB: Invalid TA";return ERROR_HALT;} rMgr.setRegister(ResourceManager.REG_L,pc+l);lastExecutedInstructionInfo+=String.format(" ; L<-0x%06X,PC<-0x%06X",(pc+l)&0xFFFFFF,taInfo.address);return taInfo.address;}
    private int handleRSUB(int pc, byte[] iB, int l, int oF) {if(l!=3){lastErrorMessage="RSUB: Invalid length";return ERROR_HALT;}lastExecutedInstructionInfo=" ; Return from subroutine";return NORMAL_HALT;}
    private int handleTIX(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="TIX: Invalid TA";return ERROR_HALT;} int valX=rMgr.getRegister(ResourceManager.REG_X);valX=(valX+1)&0xFFFFFF;rMgr.setRegister(ResourceManager.REG_X,valX); if(taInfo.address<0||taInfo.address+2>=rMgr.memory.length){lastErrorMessage="TIX: Mem OOB";return ERROR_HALT;}int valM=memToSignedInt(taInfo.address,3);setConditionCode(Integer.compare(valX,valM));lastExecutedInstructionInfo+=String.format(" ; X++(0x%06X),CompXwM(0x%06X).CC=%s",valX,valM&0xFFFFFF,getCCString());return pc+l;}
    private int handleTD(int pc, byte[] iB, int l, int oF) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc,iB,l,oF);
        if(taInfo==null||taInfo.isImmediate){
            lastErrorMessage="TD: Invalid TA (must be memory address to get Device ID)";
            return ERROR_HALT;
        }
        if(taInfo.address<0||taInfo.address>=rMgr.memory.length){
            lastErrorMessage="TD: Memory OutOfBounds for DeviceID at 0x"+String.format("%06X",taInfo.address);
            return ERROR_HALT;
        }
        byte devId=rMgr.getMemoryBytes(taInfo.address,1)[0];
        String devName=String.format("%02X",devId&0xFF);

        boolean ready=rMgr.testDevice(devName); // 이 호출의 결과가 중요합니다.

        // 디버깅 로그 추가
        System.out.println("[InstLuncher.handleTD] Device: " + devName + ", Ready: " + ready);

        if(ready) setConditionCode(-1); // LT (ready, CC <)
        else setConditionCode(0);  // EQ (not ready, CC =)

        lastExecutedInstructionInfo+=String.format(" (Dev '%s'@M[0x%06X]=0x%02X).Ready=%b;CC=%s",devName,taInfo.address,devId&0xFF,ready,getCCString());
        return pc+l;
    }    private int handleRD(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="RD: Invalid TA";return ERROR_HALT;} if(taInfo.address<0||taInfo.address>=rMgr.memory.length){lastErrorMessage="RD: Mem OOB for DeviceID";return ERROR_HALT;} byte devId=rMgr.getMemoryBytes(taInfo.address,1)[0];String devName=String.format("%02X",devId&0xFF);char[]dataRead=rMgr.readDevice(devName,1);if(dataRead!=null&&dataRead.length==1){rMgr.setRegister(ResourceManager.REG_A,(rMgr.getRegister(ResourceManager.REG_A)&0xFFFF00)|(dataRead[0]&0xFF));lastExecutedInstructionInfo+=String.format(" (Dev '%s').A_b3<-0x%02X",devName,dataRead[0]&0xFF);}else{lastExecutedInstructionInfo+=String.format(" (Dev '%s').ReadFail/EOF.A_b3<-00",devName);rMgr.setRegister(ResourceManager.REG_A,(rMgr.getRegister(ResourceManager.REG_A)&0xFFFF00));}return pc+l;}
    private int handleWD(int pc, byte[] iB, int l, int oF) {TargetAddressInfo taInfo=calculateTargetAddress(pc,iB,l,oF);if(taInfo==null||taInfo.isImmediate){lastErrorMessage="WD: Invalid TA";return ERROR_HALT;} if(taInfo.address<0||taInfo.address>=rMgr.memory.length){lastErrorMessage="WD: Mem OOB for DeviceID";return ERROR_HALT;} byte devId=rMgr.getMemoryBytes(taInfo.address,1)[0];String devName=String.format("%02X",devId&0xFF);char dataToWrite=(char)(rMgr.getRegister(ResourceManager.REG_A)&0xFF);rMgr.writeDevice(devName,new char[]{dataToWrite},1);lastExecutedInstructionInfo+=String.format(" (Dev '%s').Write A_b3(0x%02X)",devName,dataToWrite&0xFF);return pc+l;}
    private int handleADDR(int pc, byte[] iB, int l, int oF) {if(l!=2){lastErrorMessage="ADDR: Invalid length";return ERROR_HALT;}int r1n=(iB[1]&0xF0)>>4;int r2n=iB[1]&0x0F;if(r1n>9||r1n==6||r1n==7||r2n>9||r2n==6||r2n==7){lastErrorMessage="ADDR: Invalid Reg num";return ERROR_HALT;}int v1=rMgr.getRegister(r1n);int v2=rMgr.getRegister(r2n);rMgr.setRegister(r2n,(v2+v1)&0xFFFFFF);lastExecutedInstructionInfo=String.format("r%d,r%d;r%d<-r%d+r%d(0x%X+0x%X=0x%X)",r1n,r2n,r2n,r2n,r1n,v2&0xFFFFFF,v1&0xFFFFFF,rMgr.getRegister(r2n)&0xFFFFFF);return pc+l;}
    private int handleSUBR(int pc, byte[] iB, int l, int oF) {if(l!=2){lastErrorMessage="SUBR: Invalid length";return ERROR_HALT;}int r1n=(iB[1]&0xF0)>>4;int r2n=iB[1]&0x0F;if(r1n>9||r1n==6||r1n==7||r2n>9||r2n==6||r2n==7){lastErrorMessage="SUBR: Invalid Reg num";return ERROR_HALT;}int v1=rMgr.getRegister(r1n);int v2=rMgr.getRegister(r2n);rMgr.setRegister(r2n,(v2-v1)&0xFFFFFF);lastExecutedInstructionInfo=String.format("r%d,r%d;r%d<-r%d-r%d(0x%X-0x%X=0x%X)",r1n,r2n,r2n,r2n,r1n,v2&0xFFFFFF,v1&0xFFFFFF,rMgr.getRegister(r2n)&0xFFFFFF);return pc+l;}
    private int handleMULR(int pc, byte[] iB, int l, int oF) {if(l!=2){lastErrorMessage="MULR: Invalid length";return ERROR_HALT;}int r1n=(iB[1]&0xF0)>>4;int r2n=iB[1]&0x0F;if(r1n>9||r1n==6||r1n==7||r2n>9||r2n==6||r2n==7){lastErrorMessage="MULR: Invalid Reg num";return ERROR_HALT;}int v1=rMgr.getRegister(r1n);int v2=rMgr.getRegister(r2n);long res=(long)v2*v1;rMgr.setRegister(r2n,(int)(res&0xFFFFFF));lastExecutedInstructionInfo=String.format("r%d,r%d;r%d<-r%d*r%d(0x%X*0x%X=0x%X)",r1n,r2n,r2n,r2n,r1n,v2&0xFFFFFF,v1&0xFFFFFF,(int)res&0xFFFFFF);return pc+l;}
    private int handleDIVR(int pc, byte[] iB, int l, int oF) {if(l!=2){lastErrorMessage="DIVR: Invalid length";return ERROR_HALT;}int r1n=(iB[1]&0xF0)>>4;int r2n=iB[1]&0x0F;if(r1n>9||r1n==6||r1n==7||r2n>9||r2n==6||r2n==7){lastErrorMessage="DIVR: Invalid Reg num";return ERROR_HALT;}int v1=rMgr.getRegister(r1n);int v2=rMgr.getRegister(r2n);if(v1==0){lastErrorMessage="DIVR: Division by zero";return ERROR_HALT;}rMgr.setRegister(r2n,(v2/v1)&0xFFFFFF);lastExecutedInstructionInfo=String.format("r%d,r%d;r%d<-r%d/r%d(0x%X/0x%X=0x%X)",r1n,r2n,r2n,r2n,r1n,v2&0xFFFFFF,v1&0xFFFFFF,(v2/v1)&0xFFFFFF);return pc+l;}
    private int handleCOMPR(int pc, byte[] iB, int l, int oF) {if(l!=2){lastErrorMessage="COMPR: Invalid length";return ERROR_HALT;}int r1n=(iB[1]&0xF0)>>4;int r2n=iB[1]&0x0F;if(r1n>9||r1n==6||r1n==7||r2n>9||r2n==6||r2n==7){lastErrorMessage="COMPR: Invalid Reg num";return ERROR_HALT;}int v1s=rMgr.getRegister(r1n);int v2s=rMgr.getRegister(r2n);setConditionCode(Integer.compare(v1s,v2s));lastExecutedInstructionInfo=String.format("r%d,r%d;Comp r%d(0x%X)w r%d(0x%X).CC=%s",r1n,r2n,r1n,v1s&0xFFFFFF,r2n,v2s&0xFFFFFF,getCCString());return pc+l;}
    private int handleCLEAR(int pc, byte[] iB, int l, int oF) {
        if(l!=2){lastErrorMessage="CLEAR: Invalid length";return ERROR_HALT;}
        int r1n=(iB[1]&0xF0)>>4; // 상위 4비트가 레지스터 번호
        if(r1n>9||r1n==ResourceManager.REG_F||r1n==7){ // 유효한 레지스터 번호인지 확인 (0-5, 8, 9)
            lastErrorMessage="CLEAR: Invalid Register number " + r1n;
            return ERROR_HALT;
        }
        rMgr.setRegister(r1n,0); // 해당 레지스터를 0으로 설정
        System.out.println("[InstLuncher.handleCLEAR] After setRegister for r" + r1n + ", value in ResourceManager: " + rMgr.getRegister(r1n));
        lastExecutedInstructionInfo=String.format("r%d;r%d<-0",r1n,r1n);
        return pc+l;
    }
    private int handleTIXR(int pc, byte[] iB, int l, int oF) {if(l!=2){lastErrorMessage="TIXR: Invalid length";return ERROR_HALT;}int r1n=(iB[1]&0xF0)>>4;if(r1n>9||r1n==6||r1n==7){lastErrorMessage="TIXR: Invalid Reg num";return ERROR_HALT;}int valX=rMgr.getRegister(ResourceManager.REG_X);valX=(valX+1)&0xFFFFFF;rMgr.setRegister(ResourceManager.REG_X,valX);int valR1s=rMgr.getRegister(r1n);setConditionCode(Integer.compare(valX,valR1s));lastExecutedInstructionInfo=String.format("r%d;X++(0x%06X),CompXw r%d(0x%06X).CC=%s",r1n,valX,r1n,valR1s&0xFFFFFF,getCCString());return pc+l;}
    private int handleRMO(int pc, byte[] iB, int l, int oF) {if(l!=2){lastErrorMessage="RMO: Invalid length";return ERROR_HALT;}int r1n=(iB[1]&0xF0)>>4;int r2n=iB[1]&0x0F;if(r1n>9||r1n==6||r1n==7||r2n>9||r2n==6||r2n==7){lastErrorMessage="RMO: Invalid Reg num";return ERROR_HALT;}rMgr.setRegister(r2n,rMgr.getRegister(r1n));lastExecutedInstructionInfo=String.format("r%d,r%d;r%d<-r%d(0x%X)",r1n,r2n,r2n,r1n,rMgr.getRegister(r1n)&0xFFFFFF);return pc+l;}
    private int handleSHIFTL(int pc,byte[]iB,int len,int oF){if(len!=2){lastErrorMessage="SHIFTL: Invalid length";return ERROR_HALT;}int r1n=(iB[0]&0xF0)>>4;int n=(iB[1]&0x0F);if(r1n>9||r1n==6||r1n==7){lastErrorMessage="SHIFTL: Invalid Reg num";return ERROR_HALT;}int r1val=rMgr.getRegister(r1n);int shiftedVal=(r1val<<(n+1))&0xFFFFFF;/* Add more logic for bits shifted out if needed */rMgr.setRegister(r1n,shiftedVal);lastExecutedInstructionInfo=String.format("r%d,n=%d ; r%d << %d = 0x%06X",r1n,n+1,r1n,n+1,shiftedVal);return pc+len;}
    private int handleSHIFTR(int pc,byte[]iB,int len,int oF){if(len!=2){lastErrorMessage="SHIFTR: Invalid length";return ERROR_HALT;}int r1n=(iB[0]&0xF0)>>4;int n=(iB[1]&0x0F);if(r1n>9||r1n==6||r1n==7){lastErrorMessage="SHIFTR: Invalid Reg num";return ERROR_HALT;}int r1val=rMgr.getRegister(r1n);int shiftedVal=(r1val>>(n+1));/* Logical shift */rMgr.setRegister(r1n,shiftedVal&0xFFFFFF);lastExecutedInstructionInfo=String.format("r%d,n=%d ; r%d >> %d = 0x%06X (logical)",r1n,n+1,r1n,n+1,shiftedVal&0xFFFFFF);return pc+len;}
    private int handleSVC(int pc,byte[]iB,int len,int oF){if(len!=2){lastErrorMessage="SVC: Invalid length";return ERROR_HALT;}int n=(iB[0]&0xF0)>>4;lastExecutedInstructionInfo=String.format("n=%d ; Supervisor Call (not fully simulated)",n);return pc+len;}
    private int handleFIX(int pc,byte[]iB,int len,int oF){if(len!=1){lastErrorMessage="FIX: Invalid length";return ERROR_HALT;}int fVal_int=(int)rMgr.getRegister_F();rMgr.setRegister(ResourceManager.REG_A,fVal_int);lastExecutedInstructionInfo=String.format("; A <- int(F) (value: %d from F: %.2f)",fVal_int,rMgr.getRegister_F());return pc+len;}
    private int handleFLOAT(int pc,byte[]iB,int len,int oF){if(len!=1){lastErrorMessage="FLOAT: Invalid length";return ERROR_HALT;}double aVal_float=(double)rMgr.getRegister(ResourceManager.REG_A);rMgr.setRegister_F(aVal_float);lastExecutedInstructionInfo=String.format("; F <- float(A) (value: %.2f from A: %d)",aVal_float,rMgr.getRegister(ResourceManager.REG_A));return pc+len;}
    private int handleHIO(int pc,byte[]iB,int len,int oF){lastExecutedMnemonic="HIO";lastErrorMessage="Unimplemented HIO";return ERROR_HALT;}
    private int handleNORM(int pc,byte[]iB,int len,int oF){lastExecutedMnemonic="NORM";lastErrorMessage="Unimplemented NORM";return ERROR_HALT;}
    private int handleSIO(int pc,byte[]iB,int len,int oF){lastExecutedMnemonic="SIO";lastErrorMessage="Unimplemented SIO";return ERROR_HALT;}
    private int handleTIO(int pc,byte[]iB,int len,int oF){lastExecutedMnemonic="TIO";lastErrorMessage="Unimplemented TIO";return ERROR_HALT;}

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

    // InstLuncher.java 안에 추가

    /**
     * 메모리 주소(PC) 없이, 주어진 바이트 배열(명령어의 시작 부분)만으로 명령어 길이를 추정합니다.
     * Format 1, 2는 첫 바이트로, Format 3/4는 두 번째 바이트의 e-bit로 판단합니다.
     * @param instructionStartBytes 명령어의 시작 바이트들 (최소 1바이트, Format 3/4 판단 시 최소 2바이트 필요)
     * @return 추정된 명령어 길이 (1, 2, 3, 또는 4). 오류 시 0.
     */
    public static int getInstructionLengthFromBytes(byte[] instructionStartBytes) {
        if (instructionStartBytes == null || instructionStartBytes.length == 0) {
            return 0; // 잘못된 입력
        }

        int opcodeFull = instructionStartBytes[0] & 0xFF;
        int pureOpcode = opcodeFull & 0xFC; // 하위 2비트(n,i) 제외

        switch (pureOpcode) {
            // Format 1 (1 byte)
            case OP_FIX: case OP_FLOAT: case OP_HIO: case OP_NORM: case OP_SIO: case OP_TIO:
                return 1;
            // Format 2 (2 bytes)
            case OP_ADDR: case OP_CLEAR: case OP_COMPR: case OP_DIVR: case OP_MULR:
            case OP_RMO: case OP_SHIFTL: case OP_SHIFTR: case OP_SUBR: case OP_SVC:
            case OP_TIXR:
                return 2;
            default: // Format 3 or 4
                if (instructionStartBytes.length < 2) {
                    // Format 3/4 여부 판단에 nixbpe 바이트가 필요하지만 충분한 바이트가 제공되지 않음.
                    // 이 경우, 기본적으로 Format 3으로 가정하거나, 오류로 처리할 수 있음.
                    // 실제 사용 시에는 최소 2바이트를 전달해야 함.
                    // 여기서는 안전하게 3으로 가정하거나, 호출하는 쪽에서 바이트 길이를 보장해야 함.
                    // 만약 이 함수가 로드된 전체 메모리 스트림을 순회하며 호출된다면,
                    // instructionStartBytes는 항상 현재 위치부터 최소 2바이트를 포함해야 함.
                    // System.err.println("getInstructionLengthFromBytes: Insufficient bytes for Format 3/4 check, assuming 3.");
                    // return 3; // 또는 오류로 0 반환
                    return 0; // 최소 2바이트가 없으면 길이 판단 불가
                }
                byte nixbpeByte = instructionStartBytes[1];
                boolean e_flag = (nixbpeByte & 0x10) != 0; // Check e bit for format 4
                return e_flag ? 4 : 3;
        }
    }

}
