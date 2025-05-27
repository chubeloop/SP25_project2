package SP25_simulator;

// instruction에 따라 동작을 수행하는 메소드를 정의하는 클래스
// (클래스 이름 InstLuncher -> InstLauncher로 변경 권장)
public class InstLuncher {
    private ResourceManager rMgr;
    private String lastExecutedInstructionInfo = "";
    private String lastErrorMessage = "";
    private int lastCalculatedTA = -1; // TA_NOT_CALCULATED_YET
    private String lastExecutedMnemonic = "";


    public static final int NORMAL_HALT = -2; // Indicates normal program termination (e.g., RSUB)
    public static final int ERROR_HALT = -1;  // Indicates an error causing termination
    public static final int TA_NOT_CALCULATED_YET = -1; // Initial value for lastCalculatedTA


    // Opcode Constants (from 명세서 and SIC/XE standard)
    // Format 3/4
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
    private static final int OP_STS = 0x7C; private static final int OP_STT = 0x80; // STT is 0x84, not 0x80
    // STT is 0x84
    private static final int OP_STSW = 0xE8;private static final int OP_RD = 0xD8;
    private static final int OP_WD = 0xDC;  private static final int OP_TD = 0xE0;

    // Format 1
    private static final int OP_FIX = 0xC4; private static final int OP_FLOAT = 0xC0;
    private static final int OP_HIO = 0xF4; private static final int OP_NORM = 0xC8;
    private static final int OP_SIO = 0xF0; private static final int OP_TIO = 0xF8;

    // Format 2
    private static final int OP_ADDR = 0x90;  private static final int OP_SUBR = 0x94;
    private static final int OP_MULR = 0x98;  private static final int OP_DIVR = 0x9C;
    private static final int OP_COMPR = 0xA0; private static final int OP_SHIFTL = 0xA4;
    private static final int OP_SHIFTR = 0xA8;private static final int OP_RMO = 0xAC;
    private static final int OP_SVC = 0xB0;   private static final int OP_CLEAR = 0xB4;
    private static final int OP_TIXR = 0xB8;

    public InstLuncher(ResourceManager resourceManager) {
        if (resourceManager == null) {
            throw new IllegalArgumentException("ResourceManager cannot be null for InstLauncher.");
        }
        this.rMgr = resourceManager;
    }

    /**
     * Executes the instruction at the given Program Counter (PC).
     * @param pc The current value of the Program Counter.
     * @return The PC for the next instruction, or NORMAL_HALT/ERROR_HALT.
     */
    public int executeInstructionAt(int pc) {
        lastExecutedInstructionInfo = "";
        lastErrorMessage = "";
        lastCalculatedTA = TA_NOT_CALCULATED_YET;
        lastExecutedMnemonic = "N/A";

        if (pc < 0 || pc >= rMgr.memory.length) {
            lastErrorMessage = "PC (0x" + String.format("%06X", pc) + ") out of memory bounds.";
            return ERROR_HALT;
        }

        byte opcodeFullByte = rMgr.getMemoryBytes(pc, 1)[0];
        int opcodeFull = opcodeFullByte & 0xFF;
        int pureOpcode = opcodeFull & 0xFC; // Mask out n and i bits

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
        if (instructionBytes.length < instructionLength) { // Should not happen if above check passes
            lastErrorMessage = "Failed to fetch full " + instructionLength + "-byte instruction at PC 0x" +
                    String.format("%06X", pc) + ". Fetched only " + instructionBytes.length + " bytes.";
            return ERROR_HALT;
        }

        int nextPc = pc + instructionLength; // Default next PC

        // Dispatch to specific instruction handlers
        switch (pureOpcode) {
            case OP_LDA:   lastExecutedMnemonic = "LDA";   nextPc = handleLDA(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_LDX:   lastExecutedMnemonic = "LDX";   nextPc = handleLDX(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_LDL:   lastExecutedMnemonic = "LDL";   nextPc = handleLDL(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_LDB:   lastExecutedMnemonic = "LDB";   nextPc = handleLDB(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_LDS:   lastExecutedMnemonic = "LDS";   nextPc = handleLDS(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_LDT:   lastExecutedMnemonic = "LDT";   nextPc = handleLDT(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_LDCH:  lastExecutedMnemonic = "LDCH";  nextPc = handleLDCH(pc, instructionBytes, instructionLength, opcodeFull); break;

            case OP_STA:   lastExecutedMnemonic = "STA";   nextPc = handleSTA(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_STX:   lastExecutedMnemonic = "STX";   nextPc = handleSTX(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_STL:   lastExecutedMnemonic = "STL";   nextPc = handleSTL(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_STB:   lastExecutedMnemonic = "STB";   nextPc = handleSTB(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_STS:   lastExecutedMnemonic = "STS";   nextPc = handleSTS(pc, instructionBytes, instructionLength, opcodeFull); break;
            case 0x84:     lastExecutedMnemonic = "STT";   nextPc = handleSTT(pc, instructionBytes, instructionLength, opcodeFull); break; // STT is 0x84
            case OP_STCH:  lastExecutedMnemonic = "STCH";  nextPc = handleSTCH(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_STSW:  lastExecutedMnemonic = "STSW";  nextPc = handleSTSW(pc, instructionBytes, instructionLength, opcodeFull); break;


            case OP_ADD:   lastExecutedMnemonic = "ADD";   nextPc = handleADD(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_SUB:   lastExecutedMnemonic = "SUB";   nextPc = handleSUB(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_MUL:   lastExecutedMnemonic = "MUL";   nextPc = handleMUL(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_DIV:   lastExecutedMnemonic = "DIV";   nextPc = handleDIV(pc, instructionBytes, instructionLength, opcodeFull); break;

            case OP_COMP:  lastExecutedMnemonic = "COMP";  nextPc = handleCOMP(pc, instructionBytes, instructionLength, opcodeFull); break;

            case OP_J:     lastExecutedMnemonic = "J";     nextPc = handleJ(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_JEQ:
            case OP_JLT:
            case OP_JGT:   /* Mnemonic set in handler */  nextPc = handleConditionalJump(pc, instructionBytes, instructionLength, opcodeFull, pureOpcode); break;
            case OP_JSUB:  lastExecutedMnemonic = "JSUB";  nextPc = handleJSUB(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_RSUB:  lastExecutedMnemonic = "RSUB";  nextPc = handleRSUB(pc, instructionBytes, instructionLength, opcodeFull); break;

            case OP_TIX:   lastExecutedMnemonic = "TIX";   nextPc = handleTIX(pc, instructionBytes, instructionLength, opcodeFull); break;

            case OP_TD:    lastExecutedMnemonic = "TD";    nextPc = handleTD(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_RD:    lastExecutedMnemonic = "RD";    nextPc = handleRD(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_WD:    lastExecutedMnemonic = "WD";    nextPc = handleWD(pc, instructionBytes, instructionLength, opcodeFull); break;

            // Format 2
            case OP_ADDR:  lastExecutedMnemonic = "ADDR";  nextPc = handleADDR(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_SUBR:  lastExecutedMnemonic = "SUBR";  nextPc = handleSUBR(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_MULR:  lastExecutedMnemonic = "MULR";  nextPc = handleMULR(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_DIVR:  lastExecutedMnemonic = "DIVR";  nextPc = handleDIVR(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_COMPR: lastExecutedMnemonic = "COMPR"; nextPc = handleCOMPR(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_CLEAR: lastExecutedMnemonic = "CLEAR"; nextPc = handleCLEAR(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_TIXR:  lastExecutedMnemonic = "TIXR";  nextPc = handleTIXR(pc, instructionBytes, instructionLength, opcodeFull); break;
            case OP_RMO:   lastExecutedMnemonic = "RMO";   nextPc = handleRMO(pc, instructionBytes, instructionLength, opcodeFull); break;
            // SHIFTL, SHIFTR, SVC, FIX, FLOAT, HIO, SIO, TIO, NORM not implemented yet

            default:
                lastErrorMessage = "Unimplemented or Unknown Opcode: " + String.format("0x%02X", pureOpcode);
                lastExecutedInstructionInfo = String.format("Unknown Opcode [0x%02X] at 0x%06X. Length: %d", pureOpcode, pc, instructionLength);
                lastExecutedMnemonic = String.format("UNK(%02X)", pureOpcode);
                return ERROR_HALT;
        }
        return nextPc;
    }

    /**
     * Determines the length of an instruction based on its opcode and e-bit.
     * @param pureOpcode Opcode with n,i bits masked (e.g., opcode & 0xFC)
     * @param pc Current Program Counter (to fetch nixbpe byte for e-bit check if needed)
     * @return Instruction length in bytes (1, 2, 3, or 4), or 0 if unknown.
     */
    private int getInstructionLength(int pureOpcode, int pc) {
        switch (pureOpcode) {
            // Format 1
            case OP_FIX: case OP_FLOAT: case OP_HIO: case OP_NORM: case OP_SIO: case OP_TIO:
                return 1;
            // Format 2
            case OP_ADDR: case OP_CLEAR: case OP_COMPR: case OP_DIVR: case OP_MULR:
            case OP_RMO:  case OP_SHIFTL:case OP_SHIFTR:case OP_SUBR: case OP_SVC:
            case OP_TIXR:
                return 2;
            // Format 3/4 (Most opcodes)
            default:
                // Need to check e-bit from the nixbpe byte (pc+1)
                if (pc + 1 >= rMgr.memory.length) {
                    lastErrorMessage = "Cannot determine F3/F4 length: PC+1 out of bounds for nixbpe byte.";
                    return 0; // Error
                }
                byte nixbpeByte = rMgr.getMemoryBytes(pc + 1, 1)[0];
                boolean e_flag = (nixbpeByte & 0x10) != 0;
                return e_flag ? 4 : 3;
        }
    }

    /**
     * Helper class to store Target Address calculation results.
     */
    private static class TargetAddressInfo {
        int address;      // Final effective memory address, or register number for Format 2
        int value;        // For immediate mode, this holds the value. For others, could be fetched val.
        boolean isImmediate;
        boolean isRegisterToRegister; // For Format 2

        // Constructor for memory/immediate addressing
        TargetAddressInfo(int addrOrVal, boolean isImm) {
            this.isImmediate = isImm;
            if (isImm) {
                this.value = addrOrVal;
                this.address = -1; // Not a memory address
            } else {
                this.address = addrOrVal;
                this.value = 0; // Needs to be fetched from memory if not immediate
            }
            this.isRegisterToRegister = false;
        }

        // Constructor for Format 2 (r1, r2)
        TargetAddressInfo(int r1, int r2) {
            this.address = r1; // Store r1 in 'address' field
            this.value = r2;   // Store r2 in 'value' field
            this.isImmediate = false;
            this.isRegisterToRegister = true;
        }
    }

    /**
     * Calculates the Target Address (TA) for Format 3 and Format 4 instructions.
     * @return TargetAddressInfo object, or null if error.
     */
    private TargetAddressInfo calculateTargetAddress(int pc, byte[] instructionBytes, int instructionLength, int opcodeFull) {
        lastCalculatedTA = TA_NOT_CALCULATED_YET; // Reset
        if (instructionLength < 3) {
            lastErrorMessage = "calculateTargetAddress called for non-Format 3/4 instruction (length " + instructionLength + ").";
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

        if (e_flag) { // Format 4
            if (instructionLength != 4) {
                lastErrorMessage = "e_flag is set, but instruction length is not 4. PC: " + String.format("0x%06X", pc);
                return null;
            }
            disp_or_addr = ((nixbpeByte & 0x0F) << 16) | // Lower 4 bits of nixbpeByte are upper 4 bits of 20-bit address
                    ((instructionBytes[2] & 0xFF) << 8) |
                    (instructionBytes[3] & 0xFF);
        } else { // Format 3
            if (instructionLength != 3) {
                lastErrorMessage = "e_flag is not set, but instruction length is not 3. PC: " + String.format("0x%06X", pc);
                return null;
            }
            disp_or_addr = ((nixbpeByte & 0x0F) << 8) | (instructionBytes[2] & 0xFF);
            // Sign-extend 12-bit displacement if b or p is set (or if it's used as a direct address that could be negative in SIC)
            if (b_flag || p_flag || (!n_flag && !i_flag && !e_flag)) { // For SIC direct or relative
                if ((disp_or_addr & 0x0800) != 0) { // If 12th bit is 1 (negative)
                    disp_or_addr |= 0xFFFFF000; // Sign-extend to 32-bit
                }
            }
        }

        int targetAddress = 0;
        String taModeInfo = "";

        if (i_flag && !n_flag) { // Immediate addressing (#)
            taModeInfo = "Immediate";
            lastCalculatedTA = disp_or_addr; // For immediate, TA is the value itself
            lastExecutedInstructionInfo += String.format(" #%d (0x%X)", disp_or_addr, disp_or_addr & (e_flag ? 0xFFFFF : 0xFFF) );
            return new TargetAddressInfo(disp_or_addr, true);
        } else if (n_flag && !i_flag) { // Indirect addressing (@)
            taModeInfo = "Indirect";
        } else if (n_flag && i_flag) { // Simple addressing (direct for operand fetch)
            taModeInfo = "Simple";
        } else { // n=0, i=0 (SIC compatible simple/direct addressing)
            taModeInfo = "Simple (SIC)";
            // For SIC mode, disp_or_addr (15 bits if x=0) is the target address
            // Here, n=0, i=0, e=0 implies disp_or_addr is 12 bits from Format 3
            // targetAddress will be disp_or_addr itself, possibly indexed
        }

        // Calculate base target address before indexing and indirection
        if (p_flag && !b_flag) { // PC-relative
            targetAddress = (pc + instructionLength) + disp_or_addr; // disp_or_addr is signed
            taModeInfo += String.format(" PC-rel (PC_next=0x%X + disp=0x%X)", pc + instructionLength, disp_or_addr);
        } else if (b_flag && !p_flag) { // Base-relative
            targetAddress = rMgr.getRegister(ResourceManager.REG_B) + disp_or_addr; // disp_or_addr is signed
            taModeInfo += String.format(" Base-rel (B=0x%X + disp=0x%X)", rMgr.getRegister(ResourceManager.REG_B), disp_or_addr);
        } else { // Direct addressing (e_flag or no b/p flags)
            targetAddress = disp_or_addr; // disp_or_addr is 20-bit (e=1) or 12-bit (e=0, no b/p)
            if (e_flag) taModeInfo += " Direct (Ext Fmt)";
            else taModeInfo += " Direct (Simple/SIC)";
        }

        if (x_flag) {
            targetAddress += rMgr.getRegister(ResourceManager.REG_X);
            taModeInfo += String.format(" + Indexed (X=0x%X)", rMgr.getRegister(ResourceManager.REG_X));
        }

        targetAddress &= 0xFFFFFF; // Ensure 24-bit address space for memory
        lastCalculatedTA = targetAddress; // Store calculated TA before indirection for logging

        if (n_flag && !i_flag) { // Indirect: targetAddress currently holds the address of the actual target address
            if (targetAddress < 0 || targetAddress + 2 >= rMgr.memory.length) { // Assuming 3-byte address pointer
                lastErrorMessage = "Memory access out of bounds for indirect address lookup at M[0x" + String.format("%06X", targetAddress) + "]";
                return null;
            }
            byte[] indirectPointerBytes = rMgr.getMemoryBytes(targetAddress, 3);
            int finalTargetAddress = rMgr.bytesToInt(indirectPointerBytes);
            lastExecutedInstructionInfo += String.format(" %s, TA_ptr=0x%06X, M[TA_ptr]=0x%06X", taModeInfo, targetAddress, finalTargetAddress & 0xFFFFFF);
            targetAddress = finalTargetAddress & 0xFFFFFF;
            lastCalculatedTA = targetAddress; // Update TA for logging
        } else {
            lastExecutedInstructionInfo += String.format(" %s, TA=0x%06X", taModeInfo, targetAddress);
        }

        // Final validation of TA for memory access
        if (targetAddress < 0 || targetAddress >= rMgr.memory.length ) {
            // Allow TA to be == memory.length for certain ops like STCH to a buffer end,
            // but fetching from it would be an error.
            // Let individual handlers check bounds for their specific access length.
            // lastErrorMessage = "Final target address 0x" + String.format("%06X", targetAddress) + " is out of memory bounds.";
            // return null;
        }
        return new TargetAddressInfo(targetAddress, false);
    }

    // --- Helper Methods ---
    private void setConditionCode(int comparisonResult) {
        // CC < (LT) -> SW = -1 (or other convention, e.g. 0b01)
        // CC = (EQ) -> SW =  0 (or other convention, e.g. 0b00)
        // CC > (GT) -> SW =  1 (or other convention, e.g. 0b10)
        if (comparisonResult < 0) rMgr.setRegister(ResourceManager.REG_SW, 0x01); // LT for SIC/XE book
        else if (comparisonResult == 0) rMgr.setRegister(ResourceManager.REG_SW, 0x00); // EQ
        else rMgr.setRegister(ResourceManager.REG_SW, 0x02); // GT
    }

    private String getCCString() {
        int cc = rMgr.getRegister(ResourceManager.REG_SW);
        if (cc == 0x01) return "LT";
        if (cc == 0x00) return "EQ";
        if (cc == 0x02) return "GT";
        return "Undef(" + String.format("%02X",cc) + ")";
    }

    // Reads 'length' bytes from memory at 'address' and converts to a signed int
    private int memToSignedInt(int address, int length) {
        if (address < 0 || address + length > rMgr.memory.length) {
            lastErrorMessage = "Memory read out of bounds: addr=0x" + String.format("%06X", address) + ", len=" + length;
            // This should ideally halt. For now, return 0 or throw.
            return 0; // Or throw exception
        }
        byte[] data = rMgr.getMemoryBytes(address, length);
        return rMgr.bytesToInt(data); // ResourceManager.bytesToInt handles sign extension for 3-byte
    }

    // Converts an int 'value' to 'length' bytes and writes to memory at 'address'
    private void intToMemBytes(int address, int value, int length) {
        if (address < 0 || address + length > rMgr.memory.length) {
            lastErrorMessage = "Memory write out of bounds: addr=0x" + String.format("%06X", address) + ", len=" + length;
            return; // Or throw exception
        }
        byte[] data = rMgr.intToBytes(value); // Assumes intToBytes returns 3 bytes
        if (length == 1) {
            rMgr.setMemoryBytes(address, new byte[]{(byte)(value & 0xFF)}, 1);
        } else if (length == 3) {
            rMgr.setMemoryBytes(address, data, 3);
        } else {
            // Handle other lengths if necessary, or restrict.
            lastErrorMessage = "Unsupported length for intToMemBytes: " + length;
        }
    }

    // --- Individual Instruction Handlers ---
    private int handleLDA(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        int operandValue;
        if (taInfo.isImmediate) {
            operandValue = taInfo.value;
        } else {
            if (taInfo.address < 0 || taInfo.address + 2 >= rMgr.memory.length) { // Check for 3-byte read
                lastErrorMessage = "LDA: Memory access out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
            }
            operandValue = memToSignedInt(taInfo.address, 3);
        }
        rMgr.setRegister(ResourceManager.REG_A, operandValue);
        lastExecutedInstructionInfo += String.format(" ; A <- 0x%06X", operandValue & 0xFFFFFF);
        return pc + length;
    }
    private int handleLDX(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        int operandValue;
        if (taInfo.isImmediate) {
            operandValue = taInfo.value;
        } else {
            if (taInfo.address < 0 || taInfo.address + 2 >= rMgr.memory.length) {
                lastErrorMessage = "LDX: Memory access out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
            }
            operandValue = memToSignedInt(taInfo.address, 3);
        }
        rMgr.setRegister(ResourceManager.REG_X, operandValue);
        lastExecutedInstructionInfo += String.format(" ; X <- 0x%06X", operandValue & 0xFFFFFF);
        return pc + length;
    }
    private int handleLDL(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        int operandValue;
        if (taInfo.isImmediate) {
            operandValue = taInfo.value;
        } else {
            if (taInfo.address < 0 || taInfo.address + 2 >= rMgr.memory.length) {
                lastErrorMessage = "LDL: Memory access out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
            }
            operandValue = memToSignedInt(taInfo.address, 3);
        }
        rMgr.setRegister(ResourceManager.REG_L, operandValue);
        lastExecutedInstructionInfo += String.format(" ; L <- 0x%06X", operandValue & 0xFFFFFF);
        return pc + length;
    }
    private int handleLDB(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        int operandValue;
        if (taInfo.isImmediate) {
            operandValue = taInfo.value;
        } else {
            if (taInfo.address < 0 || taInfo.address + 2 >= rMgr.memory.length) {
                lastErrorMessage = "LDB: Memory access out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
            }
            operandValue = memToSignedInt(taInfo.address, 3);
        }
        rMgr.setRegister(ResourceManager.REG_B, operandValue);
        lastExecutedInstructionInfo += String.format(" ; B <- 0x%06X", operandValue & 0xFFFFFF);
        return pc + length;
    }
    private int handleLDS(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        int operandValue;
        if (taInfo.isImmediate) {
            operandValue = taInfo.value;
        } else {
            if (taInfo.address < 0 || taInfo.address + 2 >= rMgr.memory.length) {
                lastErrorMessage = "LDS: Memory access out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
            }
            operandValue = memToSignedInt(taInfo.address, 3);
        }
        rMgr.setRegister(ResourceManager.REG_S, operandValue);
        lastExecutedInstructionInfo += String.format(" ; S <- 0x%06X", operandValue & 0xFFFFFF);
        return pc + length;
    }
    private int handleLDT(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        lastExecutedMnemonic = "LDT";
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        int operandValue;
        if (taInfo.isImmediate) {
            operandValue = taInfo.value; // 즉시값을 바로 사용
            lastExecutedInstructionInfo += String.format(" ; T <- ImmediateValue (0x%06X)", operandValue & 0xFFFFFF);
        } else {
            if (taInfo.address < 0 || taInfo.address + 2 >= rMgr.memory.length) {
                lastErrorMessage = "LDT: Memory access out of bounds for operand: 0x" + String.format("%06X", taInfo.address);
                return ERROR_HALT;
            }
            operandValue = memToSignedInt(taInfo.address, 3);
            lastExecutedInstructionInfo += String.format(" ; T <- M[0x%06X] (Value: 0x%06X)", taInfo.address, operandValue & 0xFFFFFF);
        }
        rMgr.setRegister(ResourceManager.REG_T, operandValue);
        return pc + length;
    }

    private int handleLDCH(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        int charValue;
        if (taInfo.isImmediate) { // LDCH #imm not standard, but if supported
            charValue = taInfo.value & 0xFF;
        } else {
            if (taInfo.address < 0 || taInfo.address >= rMgr.memory.length) {
                lastErrorMessage = "LDCH: Memory access out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
            }
            charValue = rMgr.getMemoryBytes(taInfo.address, 1)[0] & 0xFF;
        }
        int regA = rMgr.getRegister(ResourceManager.REG_A);
        rMgr.setRegister(ResourceManager.REG_A, (regA & 0xFFFF00) | charValue); // Store in rightmost byte of A
        lastExecutedInstructionInfo += String.format(" ; A_byte3 <- 0x%02X", charValue);
        return pc + length;
    }


    private int handleSTA(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        if (taInfo.isImmediate) { lastErrorMessage = "STA cannot use immediate addressing."; return ERROR_HALT; }
        if (taInfo.address < 0 || taInfo.address + 2 >= rMgr.memory.length) {
            lastErrorMessage = "STA: Memory access out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
        }
        intToMemBytes(taInfo.address, rMgr.getRegister(ResourceManager.REG_A), 3);
        lastExecutedInstructionInfo += String.format(" ; M[0x%06X] <- A (0x%06X)", taInfo.address, rMgr.getRegister(ResourceManager.REG_A) & 0xFFFFFF);
        return pc + length;
    }
    private int handleSTX(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        if (taInfo.isImmediate) { lastErrorMessage = "STX cannot use immediate addressing."; return ERROR_HALT; }
        if (taInfo.address < 0 || taInfo.address + 2 >= rMgr.memory.length) {
            lastErrorMessage = "STX: Memory access out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
        }
        intToMemBytes(taInfo.address, rMgr.getRegister(ResourceManager.REG_X), 3);
        lastExecutedInstructionInfo += String.format(" ; M[0x%06X] <- X (0x%06X)", taInfo.address, rMgr.getRegister(ResourceManager.REG_X) & 0xFFFFFF);
        return pc + length;
    }
    private int handleSTL(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        if (taInfo.isImmediate) { lastErrorMessage = "STL cannot use immediate addressing."; return ERROR_HALT; }
        if (taInfo.address < 0 || taInfo.address + 2 >= rMgr.memory.length) {
            lastErrorMessage = "STL: Memory access out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
        }
        intToMemBytes(taInfo.address, rMgr.getRegister(ResourceManager.REG_L), 3);
        lastExecutedInstructionInfo += String.format(" ; M[0x%06X] <- L (0x%06X)", taInfo.address, rMgr.getRegister(ResourceManager.REG_L) & 0xFFFFFF);
        return pc + length;
    }
    private int handleSTB(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        if (taInfo.isImmediate) { lastErrorMessage = "STB cannot use immediate addressing."; return ERROR_HALT; }
        if (taInfo.address < 0 || taInfo.address + 2 >= rMgr.memory.length) {
            lastErrorMessage = "STB: Memory access out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
        }
        intToMemBytes(taInfo.address, rMgr.getRegister(ResourceManager.REG_B), 3);
        lastExecutedInstructionInfo += String.format(" ; M[0x%06X] <- B (0x%06X)", taInfo.address, rMgr.getRegister(ResourceManager.REG_B) & 0xFFFFFF);
        return pc + length;
    }
    private int handleSTS(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        if (taInfo.isImmediate) { lastErrorMessage = "STS cannot use immediate addressing."; return ERROR_HALT; }
        if (taInfo.address < 0 || taInfo.address + 2 >= rMgr.memory.length) {
            lastErrorMessage = "STS: Memory access out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
        }
        intToMemBytes(taInfo.address, rMgr.getRegister(ResourceManager.REG_S), 3);
        lastExecutedInstructionInfo += String.format(" ; M[0x%06X] <- S (0x%06X)", taInfo.address, rMgr.getRegister(ResourceManager.REG_S) & 0xFFFFFF);
        return pc + length;
    }
    private int handleSTT(int pc, byte[] instructionBytes, int length, int opcodeFull) { // Opcode 0x84
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        if (taInfo.isImmediate) { lastErrorMessage = "STT cannot use immediate addressing."; return ERROR_HALT; }
        if (taInfo.address < 0 || taInfo.address + 2 >= rMgr.memory.length) {
            lastErrorMessage = "STT: Memory access out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
        }
        intToMemBytes(taInfo.address, rMgr.getRegister(ResourceManager.REG_T), 3);
        lastExecutedInstructionInfo += String.format(" ; M[0x%06X] <- T (0x%06X)", taInfo.address, rMgr.getRegister(ResourceManager.REG_T) & 0xFFFFFF);
        return pc + length;
    }
    private int handleSTCH(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        if (taInfo.isImmediate) { lastErrorMessage = "STCH cannot use immediate addressing."; return ERROR_HALT; }
        if (taInfo.address < 0 || taInfo.address >= rMgr.memory.length) {
            lastErrorMessage = "STCH: Memory access out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
        }
        byte charToStore = (byte) (rMgr.getRegister(ResourceManager.REG_A) & 0xFF); // Rightmost byte of A
        rMgr.setMemoryBytes(taInfo.address, new byte[]{charToStore}, 1);
        lastExecutedInstructionInfo += String.format(" ; M[0x%06X] <- A_byte3 (0x%02X)", taInfo.address, charToStore & 0xFF);
        return pc + length;
    }
    private int handleSTSW(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        if (taInfo.isImmediate) { lastErrorMessage = "STSW cannot use immediate addressing."; return ERROR_HALT; }
        if (taInfo.address < 0 || taInfo.address + 2 >= rMgr.memory.length) {
            lastErrorMessage = "STSW: Memory access out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
        }
        intToMemBytes(taInfo.address, rMgr.getRegister(ResourceManager.REG_SW), 3);
        lastExecutedInstructionInfo += String.format(" ; M[0x%06X] <- SW (0x%06X)", taInfo.address, rMgr.getRegister(ResourceManager.REG_SW) & 0xFFFFFF);
        return pc + length;
    }


    private int handleADD(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        int operandValue;
        if (taInfo.isImmediate) {
            operandValue = taInfo.value;
        } else {
            if (taInfo.address < 0 || taInfo.address + 2 >= rMgr.memory.length) {
                lastErrorMessage = "ADD: Memory access out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
            }
            operandValue = memToSignedInt(taInfo.address, 3);
        }
        int currentA = rMgr.getRegister(ResourceManager.REG_A);
        long result = (long)currentA + operandValue; // Use long for intermediate to check overflow if needed
        rMgr.setRegister(ResourceManager.REG_A, (int)(result & 0xFFFFFF)); // Store 24-bit result
        lastExecutedInstructionInfo += String.format(" ; A <- A + M (0x%06X + 0x%06X = 0x%06X)",
                currentA & 0xFFFFFF, operandValue & 0xFFFFFF, (int)result & 0xFFFFFF);
        return pc + length;
    }
    private int handleSUB(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        int operandValue;
        if (taInfo.isImmediate) {
            operandValue = taInfo.value;
        } else {
            if (taInfo.address < 0 || taInfo.address + 2 >= rMgr.memory.length) {
                lastErrorMessage = "SUB: Memory access out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
            }
            operandValue = memToSignedInt(taInfo.address, 3);
        }
        int currentA = rMgr.getRegister(ResourceManager.REG_A);
        long result = (long)currentA - operandValue;
        rMgr.setRegister(ResourceManager.REG_A, (int)(result & 0xFFFFFF));
        lastExecutedInstructionInfo += String.format(" ; A <- A - M (0x%06X - 0x%06X = 0x%06X)",
                currentA & 0xFFFFFF, operandValue & 0xFFFFFF, (int)result & 0xFFFFFF);
        return pc + length;
    }
    private int handleMUL(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        int operandValue;
        if (taInfo.isImmediate) {
            operandValue = taInfo.value;
        } else {
            if (taInfo.address < 0 || taInfo.address + 2 >= rMgr.memory.length) {
                lastErrorMessage = "MUL: Memory access out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
            }
            operandValue = memToSignedInt(taInfo.address, 3);
        }
        int currentA = rMgr.getRegister(ResourceManager.REG_A);
        long result = (long)currentA * operandValue; // Potential 48-bit result, SIC/XE A is 24-bit
        rMgr.setRegister(ResourceManager.REG_A, (int)(result & 0xFFFFFF)); // Store lower 24 bits
        lastExecutedInstructionInfo += String.format(" ; A <- A * M (0x%06X * 0x%06X = 0x%06X)",
                currentA & 0xFFFFFF, operandValue & 0xFFFFFF, (int)result & 0xFFFFFF);
        return pc + length;
    }
    private int handleDIV(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        int operandValue;
        if (taInfo.isImmediate) {
            operandValue = taInfo.value;
        } else {
            if (taInfo.address < 0 || taInfo.address + 2 >= rMgr.memory.length) {
                lastErrorMessage = "DIV: Memory access out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
            }
            operandValue = memToSignedInt(taInfo.address, 3);
        }
        if (operandValue == 0) { lastErrorMessage = "DIV: Division by zero."; return ERROR_HALT; }
        int currentA = rMgr.getRegister(ResourceManager.REG_A);
        int quotient = currentA / operandValue;
        // int remainder = currentA % operandValue; // SIC/XE DIV also sets remainder, often in L or X
        rMgr.setRegister(ResourceManager.REG_A, quotient & 0xFFFFFF);
        lastExecutedInstructionInfo += String.format(" ; A <- A / M (0x%06X / 0x%06X = 0x%06X)",
                currentA & 0xFFFFFF, operandValue & 0xFFFFFF, quotient & 0xFFFFFF);
        return pc + length;
    }


    private int handleCOMP(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        int operandValue;
        if (taInfo.isImmediate) {
            operandValue = taInfo.value;
        } else {
            if (taInfo.address < 0 || taInfo.address + 2 >= rMgr.memory.length) {
                lastErrorMessage = "COMP: Memory access out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
            }
            operandValue = memToSignedInt(taInfo.address, 3);
        }
        int currentA_signed = rMgr.getRegister(ResourceManager.REG_A); // Already signed if from memToSignedInt
        setConditionCode(Integer.compare(currentA_signed, operandValue));
        lastExecutedInstructionInfo += String.format(" ; Compare A (0x%06X) with Val (0x%06X). CC=%s",
                currentA_signed & 0xFFFFFF, operandValue & 0xFFFFFF, getCCString());
        return pc + length;
    }

    private int handleJ(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        if (taInfo.isImmediate) { lastErrorMessage = "J cannot use immediate addressing."; return ERROR_HALT; }
        lastExecutedInstructionInfo += String.format(" ; PC <- 0x%06X", taInfo.address);
        return taInfo.address; // Return new PC
    }

    private int handleConditionalJump(int pc, byte[] instructionBytes, int length, int opcodeFull, int pureOpcode) {
        String mnemonic = "";
        boolean conditionMet = false;
        int ccVal = rMgr.getRegister(ResourceManager.REG_SW); // SIC/XE CC: 00(EQ), 01(LT), 10(GT)

        switch(pureOpcode) {
            case OP_JEQ: mnemonic = "JEQ"; if (ccVal == 0x00) conditionMet = true; break;
            case OP_JLT: mnemonic = "JLT"; if (ccVal == 0x01) conditionMet = true; break;
            case OP_JGT: mnemonic = "JGT"; if (ccVal == 0x02) conditionMet = true; break;
            default: lastErrorMessage = "Unknown conditional jump opcode: " + String.format("0x%02X", pureOpcode); return ERROR_HALT;
        }
        lastExecutedMnemonic = mnemonic; // Set mnemonic here

        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        if (taInfo.isImmediate) { lastErrorMessage = mnemonic + " cannot use immediate addressing."; return ERROR_HALT; }

        if (conditionMet) {
            lastExecutedInstructionInfo += String.format(" (Cond TRUE, CC=%s) ; PC <- 0x%06X", getCCString(), taInfo.address);
            return taInfo.address; // Jump
        } else {
            lastExecutedInstructionInfo += String.format(" (Cond FALSE, CC=%s) ; PC no change by JMP", getCCString());
            return pc + length; // No jump
        }
    }

    private int handleJSUB(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        if (taInfo.isImmediate) { lastErrorMessage = "JSUB cannot use immediate addressing."; return ERROR_HALT; }
        rMgr.setRegister(ResourceManager.REG_L, pc + length); // Store return address (next instruction) in L
        lastExecutedInstructionInfo += String.format(" ; L <- 0x%06X, PC <- 0x%06X", pc + length, taInfo.address);
        return taInfo.address; // Jump to subroutine
    }

    private int handleRSUB(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        // RSUB is Format 3 but has no operand. TA calculation is not needed.
        if (length != 3) { lastErrorMessage = "RSUB: Invalid instruction length (not 3)."; return ERROR_HALT; }
        lastExecutedInstructionInfo = "RSUB"; // Set here as it's simple
        int returnAddress = rMgr.getRegister(ResourceManager.REG_L);
        lastExecutedInstructionInfo += String.format(" ; PC <- L (0x%06X)", returnAddress & 0xFFFFFF);
        // For SIC/XE, RSUB often implies program end if it's the main routine's RSUB.
        // Signalling NORMAL_HALT for the simulator to handle.
        // The simulator could then set PC to returnAddress if needed, or just halt.
        // If this RSUB is the end of the *entire* program, it should halt.
        // For simplicity, we'll assume this RSUB might be a program end signal.
        // However, a more robust simulator might check if L points to a valid OS return or specific halt address.
        // Here, we return NORMAL_HALT, and SicSimulator should probably stop.
        // If we want RSUB to actually jump, we'd return `returnAddress`.
        // Let's make RSUB jump to L, and let the simulator decide to halt if L is e.g. 0 or out of bounds.
        // To match typical behavior where RSUB signals a halt if it's the "main" return:
        // We can return a special value that SicSimulator interprets as a "halt after this instruction".
        // For now, let's treat it as a jump to L, and if L is 0 or something, it will likely halt.
        // If the loaded program has a proper END record with a start address for the *first* CS,
        // an RSUB from the "main" routine might have L pointing to that initial start or a loader-defined exit.
        // Let's simply jump to L. If L is 0, it will execute from 0 (maybe loader stub or error).
        // For the provided input, RSUB in COPY CS should return to where JSUB to RDREC/WRREC happened.
        // If it's the final RSUB of the main program, L might be 0 or some OS entry.
        // The prompt asks to implement ALL instructions. RSUB jumps to (L).
        // The problem description doesn't specify OS interaction or halt conditions beyond PC bounds.
        return returnAddress & 0xFFFFFF; // Jump to content of L
    }

    private int handleTIX(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        if (taInfo.isImmediate) { lastErrorMessage = "TIX cannot use immediate addressing."; return ERROR_HALT; }
        int valX = rMgr.getRegister(ResourceManager.REG_X);
        valX = (valX + 1) & 0xFFFFFF;
        rMgr.setRegister(ResourceManager.REG_X, valX);

        if (taInfo.address < 0 || taInfo.address + 2 >= rMgr.memory.length) {
            lastErrorMessage = "TIX: Memory access out of bounds for operand: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
        }
        int operandM = memToSignedInt(taInfo.address, 3);
        setConditionCode(Integer.compare(valX, operandM)); // Compare X with M
        lastExecutedInstructionInfo += String.format(" ; X <- X+1 (0x%X), Compare X with M[0x%06X] (0x%X). CC=%s",
                valX, taInfo.address, operandM & 0xFFFFFF, getCCString());
        return pc + length;
    }


    private int handleTD(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        if (taInfo.isImmediate) { lastErrorMessage = "TD cannot use immediate addressing."; return ERROR_HALT; }

        if (taInfo.address < 0 || taInfo.address >= rMgr.memory.length) {
            lastErrorMessage = "TD: Memory address for device ID out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
        }
        byte deviceIdByte = rMgr.getMemoryBytes(taInfo.address, 1)[0];
        String deviceName = String.format("%02X", deviceIdByte & 0xFF);

        boolean ready = rMgr.testDevice(deviceName);
        // SIC/XE 명세: TD는 CC를 설정. < (준비됨, ready to read/write), = (사용중/준비안됨/busy)
        // JEQ devbusy -> CC = 0 (EQ) 일 때 점프 (즉, device is busy)
        // JLT devready -> CC < 0 (LT) 일 때 점프 (즉, device is ready)
        if (ready) {
            setConditionCode(-1); // CC < (Ready) -> SW (CC bits) set to indicate LT
        } else {
            setConditionCode(0);  // CC = (Not Ready / Busy) -> SW (CC bits) set to indicate EQ
        }
        lastExecutedInstructionInfo += String.format(" (Test Dev: '%s' at M[0x%06X]=0x%02X). Ready=%b ; CC set to %s",
                deviceName, taInfo.address, deviceIdByte & 0xFF, ready, getCCString());
        return pc + length;
    }

    private int handleRD(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        if (taInfo.isImmediate) { lastErrorMessage = "RD cannot use immediate addressing."; return ERROR_HALT; }
        if (taInfo.address < 0 || taInfo.address >= rMgr.memory.length) {
            lastErrorMessage = "RD: Memory address for device ID out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
        }
        byte deviceIdByte = rMgr.getMemoryBytes(taInfo.address, 1)[0];
        String deviceName = String.format("%02X", deviceIdByte & 0xFF);

        char[] dataRead = rMgr.readDevice(deviceName, 1); // Read 1 byte
        if (dataRead != null && dataRead.length == 1) {
            int currentA = rMgr.getRegister(ResourceManager.REG_A);
            rMgr.setRegister(ResourceManager.REG_A, (currentA & 0xFFFF00) | (dataRead[0] & 0xFF)); // Store in rightmost byte of A
            lastExecutedInstructionInfo += String.format(" (Read Dev: '%s' at M[0x%06X]=0x%02X) ; A_byte3 <- 0x%02X",
                    deviceName, taInfo.address, deviceIdByte & 0xFF, dataRead[0] & 0xFF);
        } else {
            lastExecutedInstructionInfo += String.format(" (Read Dev: '%s' at M[0x%06X]=0x%02X) ; Read failed or EOF. A not changed.",
                    deviceName, taInfo.address, deviceIdByte & 0xFF);
            // Optionally set A to 0 or handle error condition
        }
        return pc + length;
    }

    private int handleWD(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        TargetAddressInfo taInfo = calculateTargetAddress(pc, instructionBytes, length, opcodeFull);
        if (taInfo == null) return ERROR_HALT;
        if (taInfo.isImmediate) { lastErrorMessage = "WD cannot use immediate addressing."; return ERROR_HALT; }
        if (taInfo.address < 0 || taInfo.address >= rMgr.memory.length) {
            lastErrorMessage = "WD: Memory address for device ID out of bounds: 0x" + String.format("%06X", taInfo.address); return ERROR_HALT;
        }
        byte deviceIdByte = rMgr.getMemoryBytes(taInfo.address, 1)[0];
        String deviceName = String.format("%02X", deviceIdByte & 0xFF);

        char dataToWrite = (char) (rMgr.getRegister(ResourceManager.REG_A) & 0xFF); // Rightmost byte of A
        rMgr.writeDevice(deviceName, new char[]{dataToWrite}, 1);
        lastExecutedInstructionInfo += String.format(" (Write Dev: '%s' at M[0x%06X]=0x%02X) ; Write A_byte3 (0x%02X)",
                deviceName, taInfo.address, deviceIdByte & 0xFF, dataToWrite & 0xFF);
        return pc + length;
    }

    // --- Format 2 Handlers ---
    private int handleADDR(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        if (length != 2) { lastErrorMessage = "ADDR: Invalid length."; return ERROR_HALT; }
        int r1_num = (instructionBytes[1] & 0xF0) >> 4;
        int r2_num = instructionBytes[1] & 0x0F;
        int valR1 = rMgr.getRegister(r1_num);
        int valR2 = rMgr.getRegister(r2_num);
        rMgr.setRegister(r2_num, (valR2 + valR1) & 0xFFFFFF);
        lastExecutedInstructionInfo = String.format("ADDR r%d, r%d ; r%d <- r%d + r%d (0x%X + 0x%X = 0x%X)",
                r1_num, r2_num, r2_num, r2_num, r1_num,
                valR2 & 0xFFFFFF, valR1 & 0xFFFFFF, rMgr.getRegister(r2_num) & 0xFFFFFF);
        return pc + length;
    }
    private int handleSUBR(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        if (length != 2) { lastErrorMessage = "SUBR: Invalid length."; return ERROR_HALT; }
        int r1_num = (instructionBytes[1] & 0xF0) >> 4;
        int r2_num = instructionBytes[1] & 0x0F;
        int valR1 = rMgr.getRegister(r1_num);
        int valR2 = rMgr.getRegister(r2_num);
        rMgr.setRegister(r2_num, (valR2 - valR1) & 0xFFFFFF);
        lastExecutedInstructionInfo = String.format("SUBR r%d, r%d ; r%d <- r%d - r%d (0x%X - 0x%X = 0x%X)",
                r1_num, r2_num, r2_num, r2_num, r1_num,
                valR2 & 0xFFFFFF, valR1 & 0xFFFFFF, rMgr.getRegister(r2_num) & 0xFFFFFF);
        return pc + length;
    }
    private int handleMULR(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        if (length != 2) { lastErrorMessage = "MULR: Invalid length."; return ERROR_HALT; }
        int r1_num = (instructionBytes[1] & 0xF0) >> 4;
        int r2_num = instructionBytes[1] & 0x0F;
        int valR1 = rMgr.getRegister(r1_num);
        int valR2 = rMgr.getRegister(r2_num);
        long result = (long)valR2 * valR1; // Can be 48-bit
        rMgr.setRegister(r2_num, (int)(result & 0xFFFFFF)); // Store lower 24 bits in r2
        lastExecutedInstructionInfo = String.format("MULR r%d, r%d ; r%d <- r%d * r%d (0x%X * 0x%X = 0x%X)",
                r1_num, r2_num, r2_num, r2_num, r1_num,
                valR2 & 0xFFFFFF, valR1 & 0xFFFFFF, rMgr.getRegister(r2_num) & 0xFFFFFF);
        return pc + length;
    }
    private int handleDIVR(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        if (length != 2) { lastErrorMessage = "DIVR: Invalid length."; return ERROR_HALT; }
        int r1_num = (instructionBytes[1] & 0xF0) >> 4;
        int r2_num = instructionBytes[1] & 0x0F;
        int valR1 = rMgr.getRegister(r1_num);
        int valR2 = rMgr.getRegister(r2_num);
        if (valR1 == 0) { lastErrorMessage = "DIVR: Division by zero."; return ERROR_HALT; }
        rMgr.setRegister(r2_num, (valR2 / valR1) & 0xFFFFFF);
        // Remainder usually goes to r1 for DIVR, or A for DIV
        // rMgr.setRegister(r1_num, (valR2 % valR1) & 0xFFFFFF); // If r1 stores remainder
        lastExecutedInstructionInfo = String.format("DIVR r%d, r%d ; r%d <- r%d / r%d (0x%X / 0x%X = 0x%X)",
                r1_num, r2_num, r2_num, r2_num, r1_num,
                valR2 & 0xFFFFFF, valR1 & 0xFFFFFF, rMgr.getRegister(r2_num) & 0xFFFFFF);
        return pc + length;
    }
    private int handleCOMPR(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        if (length != 2) { lastErrorMessage = "COMPR: Invalid length."; return ERROR_HALT; }
        int r1_num = (instructionBytes[1] & 0xF0) >> 4;
        int r2_num = instructionBytes[1] & 0x0F;
        int valR1_signed = rMgr.getRegister(r1_num);
        int valR2_signed = rMgr.getRegister(r2_num);
        setConditionCode(Integer.compare(valR1_signed, valR2_signed));
        lastExecutedInstructionInfo = String.format("COMPR r%d, r%d ; Compare r%d (0x%X) with r%d (0x%X). CC=%s",
                r1_num, r2_num, r1_num, valR1_signed & 0xFFFFFF, r2_num, valR2_signed & 0xFFFFFF, getCCString());
        return pc + length;
    }
    private int handleCLEAR(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        if (length != 2) { lastErrorMessage = "CLEAR: Invalid length."; return ERROR_HALT; }
        int r1_num = (instructionBytes[1] & 0xF0) >> 4;
        rMgr.setRegister(r1_num, 0);
        lastExecutedInstructionInfo = String.format("CLEAR r%d ; r%d <- 0", r1_num, r1_num);
        return pc + length;
    }
    private int handleTIXR(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        if (length != 2) { lastErrorMessage = "TIXR: Invalid length."; return ERROR_HALT; }
        int r1_num = (instructionBytes[1] & 0xF0) >> 4; // Operand register TIXR r1
        int valX = rMgr.getRegister(ResourceManager.REG_X);
        valX = (valX + 1) & 0xFFFFFF; // Increment X
        rMgr.setRegister(ResourceManager.REG_X, valX);
        int valR1_signed = rMgr.getRegister(r1_num); // Compare X with (r1)
        setConditionCode(Integer.compare(valX, valR1_signed));
        lastExecutedInstructionInfo = String.format("TIXR r%d ; X <- X+1 (0x%X), Compare X with r%d (0x%X). CC=%s",
                r1_num, valX, r1_num, valR1_signed & 0xFFFFFF, getCCString());
        return pc + length;
    }
    private int handleRMO(int pc, byte[] instructionBytes, int length, int opcodeFull) {
        if (length != 2) { lastErrorMessage = "RMO: Invalid length."; return ERROR_HALT; }
        int r1_num = (instructionBytes[1] & 0xF0) >> 4;
        int r2_num = instructionBytes[1] & 0x0F;
        rMgr.setRegister(r2_num, rMgr.getRegister(r1_num));
        lastExecutedInstructionInfo = String.format("RMO r%d, r%d ; r%d <- r%d (0x%X)",
                r1_num, r2_num, r2_num, r1_num, rMgr.getRegister(r1_num) & 0xFFFFFF);
        return pc + length;
    }


    public String getLastExecutedInstructionInfo() { return lastExecutedInstructionInfo; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public int getLastCalculatedTA() { return lastCalculatedTA; }
    public String getLastExecutedMnemonic() { return lastExecutedMnemonic; }

} // End of InstLauncher class
