package net.pixeldreamstudios.morequesttypes.util;

public enum ComparisonMode {
    EQUALS("=="),
    GREATER_THAN(">"),
    LESS_THAN("<"),
    GREATER_OR_EQUAL(">="),
    LESS_OR_EQUAL("<="),
    RANGE(">", "<"),
    RANGE_EQUAL(">=", "<="),
    RANGE_EQUAL_FIRST(">=", "<"),
    RANGE_EQUAL_SECOND(">", "<=");

    private final String firstSymbol;
    private final String secondSymbol;

    ComparisonMode(String symbol) {
        this.firstSymbol = symbol;
        this.secondSymbol = null;
    }

    ComparisonMode(String firstSymbol, String secondSymbol) {
        this.firstSymbol = firstSymbol;
        this.secondSymbol = secondSymbol;
    }

    public String getFirstSymbol() {
        return firstSymbol;
    }

    public String getSecondSymbol() {
        return secondSymbol;
    }

    public boolean isRange() {
        return secondSymbol != null;
    }

    public String getTranslationKey() {
        return "morequesttypes.comparison." + name().toLowerCase();
    }

    public boolean compare(int value, int first, int second) {
        return ComparisonManager.compare(value, this, first, second);
    }
}