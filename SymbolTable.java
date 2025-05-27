package SP25_simulator;

import java.util.ArrayList;
import java.util.HashMap;

public class SymbolTable {
	private HashMap<String, Integer> table;

	public SymbolTable() {
		table = new HashMap<>();
	}

	public void putSymbol(String symbol, int address) {
		if (symbol == null || symbol.trim().isEmpty()) {
			System.err.println("Error: Symbol name cannot be null or empty.");
			return;
		}
		String trimmedSymbol = symbol.trim();
		if (table.containsKey(trimmedSymbol)) {
			System.err.println("Error: Symbol '" + trimmedSymbol + "' already exists. Use modifySymbol().");
		} else {
			table.put(trimmedSymbol, address);
		}
	}

	public void modifySymbol(String symbol, int newaddress) {
		if (symbol == null || symbol.trim().isEmpty()) return;
		String trimmedSymbol = symbol.trim();
		if (table.containsKey(trimmedSymbol)) {
			table.put(trimmedSymbol, newaddress);
		} else {
			System.err.println("Error: Symbol '" + trimmedSymbol + "' not found. Cannot modify.");
		}
	}

	public int search(String symbol) {
		if (symbol == null || symbol.trim().isEmpty()) return -1;
		Integer address = table.get(symbol.trim());
		return (address != null) ? address : -1;
	}

	public boolean containsSymbol(String symbol) {
		if (symbol == null || symbol.trim().isEmpty()) return false;
		return table.containsKey(symbol.trim());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("SymbolTable Contents:\n");
		if (table.isEmpty()) sb.append("(empty)\n");
		else table.forEach((key, value) -> sb.append(String.format(" %-10s : 0x%06X (%d)\n", key, value, value)));
		return sb.toString();
	}

	public void clear() {
		table.clear();
	}
}
