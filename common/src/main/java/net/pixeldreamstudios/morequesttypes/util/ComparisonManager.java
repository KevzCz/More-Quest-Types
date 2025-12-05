package net.pixeldreamstudios.morequesttypes.util;

public final class ComparisonManager {
    private ComparisonManager() {}

    public static boolean compare(int value, ComparisonMode mode, int firstNumber, int secondNumber) {
        int validatedSecond = validateNumbers(mode, firstNumber, secondNumber);

        return switch (mode) {
            case EQUALS -> value == firstNumber;
            case GREATER_THAN -> value > firstNumber;
            case LESS_THAN -> value < firstNumber;
            case GREATER_OR_EQUAL -> value >= firstNumber;
            case LESS_OR_EQUAL -> value <= firstNumber;
            case RANGE -> value > firstNumber && value < validatedSecond;
            case RANGE_EQUAL -> value >= firstNumber && value <= validatedSecond;
            case RANGE_EQUAL_FIRST -> value >= firstNumber && value < validatedSecond;
            case RANGE_EQUAL_SECOND -> value > firstNumber && value <= validatedSecond;
        };
    }

    public static String getDescription(ComparisonMode mode, int firstNumber, int secondNumber) {
        int validatedSecond = validateNumbers(mode, firstNumber, secondNumber);

        return switch (mode) {
            case EQUALS -> "= " + firstNumber;
            case GREATER_THAN -> "> " + firstNumber;
            case LESS_THAN -> "< " + firstNumber;
            case GREATER_OR_EQUAL -> "≥ " + firstNumber;
            case LESS_OR_EQUAL -> "≤ " + firstNumber;
            case RANGE -> firstNumber + " < x < " + validatedSecond;
            case RANGE_EQUAL -> firstNumber + " ≤ x ≤ " + validatedSecond;
            case RANGE_EQUAL_FIRST -> firstNumber + " ≤ x < " + validatedSecond;
            case RANGE_EQUAL_SECOND -> firstNumber + " < x ≤ " + validatedSecond;
        };
    }

    public static int validateNumbers(ComparisonMode mode, int firstNumber, int secondNumber) {
        if (mode.isRange()) {
            if (secondNumber <= firstNumber) {
                return firstNumber + 10;
            }
        }
        return secondNumber;
    }

    public static long getMaxProgress(ComparisonMode mode, int firstNumber, int secondNumber) {
        if (mode.isRange()) {
            return 1;
        }
        return Math.max(1, firstNumber);
    }
}