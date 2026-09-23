package com.abuasad.calculator;

import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvExpression;
    private TextView tvResult;

    private String currentInput = "0";
    private BigDecimal firstOperand = null;
    private String pendingOperator = null;
    private boolean isOperatorJustPressed = false;
    private boolean isCalculationFinished = false;

    private static final int MAX_INPUT_LENGTH = 16;
    private static final int DIVISION_SCALE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = findViewById(R.id.tvExpression);
        tvResult = findViewById(R.id.tvResult);

        setupNumberButtons();
        setupOperatorButtons();
        setupActionButtons();
        updateDisplay();
    }

    private void setupNumberButtons() {
        int[] numButtonIds = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };

        View.OnClickListener numListener = v -> {
            provideFeedback(v);
            Button btn = (Button) v;
            String digit = btn.getText().toString();
            appendDigit(digit);
        };

        for (int id : numButtonIds) {
            findViewById(id).setOnClickListener(numListener);
        }

        findViewById(R.id.btnDot).setOnClickListener(v -> {
            provideFeedback(v);
            appendDot();
        });
    }

    private void setupOperatorButtons() {
        findViewById(R.id.btnAdd).setOnClickListener(v -> {
            provideFeedback(v);
            handleOperator("+");
        });
        findViewById(R.id.btnSubtract).setOnClickListener(v -> {
            provideFeedback(v);
            handleOperator("−");
        });
        findViewById(R.id.btnMultiply).setOnClickListener(v -> {
            provideFeedback(v);
            handleOperator("×");
        });
        findViewById(R.id.btnDivide).setOnClickListener(v -> {
            provideFeedback(v);
            handleOperator("÷");
        });
        findViewById(R.id.btnEquals).setOnClickListener(v -> {
            provideFeedback(v);
            calculateEquals();
        });
    }

    private void setupActionButtons() {
        findViewById(R.id.btnClear).setOnClickListener(v -> {
            provideFeedback(v);
            clearAll();
        });

        findViewById(R.id.btnBackspace).setOnClickListener(v -> {
            provideFeedback(v);
            handleBackspace();
        });

        findViewById(R.id.btnPlusMinus).setOnClickListener(v -> {
            provideFeedback(v);
            toggleSign();
        });

        findViewById(R.id.btnPercent).setOnClickListener(v -> {
            provideFeedback(v);
            applyPercent();
        });
    }

    private void appendDigit(String digit) {
        if (isCalculationFinished) {
            clearAll();
        }

        if (isOperatorJustPressed) {
            currentInput = digit;
            isOperatorJustPressed = false;
        } else {
            if ("0".equals(currentInput)) {
                currentInput = digit;
            } else if (currentInput.length() < MAX_INPUT_LENGTH) {
                currentInput += digit;
            }
        }
        updateDisplay();
    }

    private void appendDot() {
        if (isCalculationFinished) {
            clearAll();
        }

        if (isOperatorJustPressed) {
            currentInput = "0.";
            isOperatorJustPressed = false;
        } else if (!currentInput.contains(".")) {
            currentInput += ".";
        }
        updateDisplay();
    }

    private void handleOperator(String operator) {
        if (isCalculationFinished) {
            firstOperand = parseBigDecimal(currentInput);
            pendingOperator = operator;
            isCalculationFinished = false;
            isOperatorJustPressed = true;
            tvExpression.setText(formatNumber(firstOperand) + " " + operator);
            return;
        }

        if (firstOperand == null) {
            firstOperand = parseBigDecimal(currentInput);
            pendingOperator = operator;
            isOperatorJustPressed = true;
            tvExpression.setText(formatNumber(firstOperand) + " " + operator);
        } else if (isOperatorJustPressed) {
            // Change operator if user clicks another operator without entering a number
            pendingOperator = operator;
            tvExpression.setText(formatNumber(firstOperand) + " " + operator);
        } else {
            // Consecutive operation: evaluate previous step first
            BigDecimal secondOperand = parseBigDecimal(currentInput);
            BigDecimal intermediateResult = executeOperation(firstOperand, pendingOperator, secondOperand);
            if (intermediateResult == null) {
                showDivisionByZeroError();
                return;
            }
            firstOperand = intermediateResult;
            pendingOperator = operator;
            currentInput = formatNumber(intermediateResult);
            isOperatorJustPressed = true;
            tvExpression.setText(formatNumber(firstOperand) + " " + operator);
            tvResult.setText(currentInput);
        }
    }

    private void calculateEquals() {
        if (firstOperand == null || pendingOperator == null || isOperatorJustPressed) {
            return;
        }

        BigDecimal secondOperand = parseBigDecimal(currentInput);
        BigDecimal result = executeOperation(firstOperand, pendingOperator, secondOperand);

        if (result == null) {
            showDivisionByZeroError();
            return;
        }

        tvExpression.setText(formatNumber(firstOperand) + " " + pendingOperator + " " + formatNumber(secondOperand) + " =");
        currentInput = formatNumber(result);
        tvResult.setText(currentInput);

        firstOperand = null;
        pendingOperator = null;
        isCalculationFinished = true;
        isOperatorJustPressed = false;
    }

    private BigDecimal executeOperation(BigDecimal a, String op, BigDecimal b) {
        try {
            switch (op) {
                case "+":
                    return a.add(b);
                case "−":
                    return a.subtract(b);
                case "×":
                    return a.multiply(b);
                case "÷":
                    if (b.compareTo(BigDecimal.ZERO) == 0) {
                        return null; // Division by zero
                    }
                    return a.divide(b, DIVISION_SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
                default:
                    return b;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private void toggleSign() {
        if (isCalculationFinished) {
            firstOperand = null;
            pendingOperator = null;
            isCalculationFinished = false;
        }

        if (!"0".equals(currentInput) && !currentInput.isEmpty()) {
            if (currentInput.startsWith("-")) {
                currentInput = currentInput.substring(1);
            } else {
                currentInput = "-" + currentInput;
            }
            updateDisplay();
        }
    }

    private void applyPercent() {
        try {
            BigDecimal val = parseBigDecimal(currentInput);
            BigDecimal percentVal = val.divide(new BigDecimal("100"), DIVISION_SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
            currentInput = formatNumber(percentVal);
            updateDisplay();
        } catch (Exception ignored) {
        }
    }

    private void handleBackspace() {
        if (isCalculationFinished) {
            clearAll();
            return;
        }

        if (isOperatorJustPressed) {
            return;
        }

        if (currentInput.length() > 1) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
            if ("-".equals(currentInput)) {
                currentInput = "0";
            }
        } else {
            currentInput = "0";
        }
        updateDisplay();
    }

    private void clearAll() {
        currentInput = "0";
        firstOperand = null;
        pendingOperator = null;
        isOperatorJustPressed = false;
        isCalculationFinished = false;
        tvExpression.setText("");
        updateDisplay();
    }

    private void showDivisionByZeroError() {
        tvExpression.setText("");
        tvResult.setText(getString(R.string.error_divide_zero));
        currentInput = "0";
        firstOperand = null;
        pendingOperator = null;
        isOperatorJustPressed = false;
        isCalculationFinished = true;
    }

    private void updateDisplay() {
        tvResult.setText(formatDisplayNumber(currentInput));
    }

    private BigDecimal parseBigDecimal(String str) {
        try {
            return new BigDecimal(str);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String formatNumber(BigDecimal bd) {
        if (bd == null) return "0";
        bd = bd.stripTrailingZeros();
        return bd.toPlainString();
    }

    private String formatDisplayNumber(String input) {
        if (input == null || input.isEmpty() || "0".equals(input)) {
            return "0";
        }
        if (input.endsWith(".")) {
            String integerPart = input.substring(0, input.length() - 1);
            return formatIntegerWithCommas(integerPart) + ".";
        }
        if (input.contains(".")) {
            String[] parts = input.split("\\.");
            String intPart = formatIntegerWithCommas(parts[0]);
            String decPart = parts.length > 1 ? parts[1] : "";
            return intPart + "." + decPart;
        }
        return formatIntegerWithCommas(input);
    }

    private String formatIntegerWithCommas(String str) {
        try {
            boolean isNegative = str.startsWith("-");
            String clean = isNegative ? str.substring(1) : str;
            if (clean.isEmpty()) return isNegative ? "-" : "0";
            BigDecimal bd = new BigDecimal(clean);
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            DecimalFormat formatter = new DecimalFormat("#,###", symbols);
            String formatted = formatter.format(bd);
            return isNegative ? "-" + formatted : formatted;
        } catch (Exception e) {
            return str;
        }
    }

    private void provideFeedback(View view) {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception ignored) {
        }
    }
}
