import java.sql.*;
import java.util.*;
import java.util.regex.*;

public class SmartCalculatorCore {

    public static boolean DEGREE_MODE = true;
    public static StringBuilder steps = new StringBuilder();
    public static final String DB_URL = "jdbc:sqlite:calculator.db";

    static {
        createTable();
    }

    // ================= DATABASE =================
    public static void createTable() {
        try (Connection c = DriverManager.getConnection(DB_URL);
             Statement s = c.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS calculations(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  input TEXT,
                  steps TEXT,
                  result TEXT,
                  time DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);
        } catch (Exception ignored) {}
    }

    public static void save(String input, String result) {
        try (Connection c = DriverManager.getConnection(DB_URL);
             PreparedStatement p = c.prepareStatement(
                     "INSERT INTO calculations(input,steps,result) VALUES(?,?,?)")) {
            p.setString(1, input);
            p.setString(2, steps.toString());
            p.setString(3, result);
            p.execute();
        } catch (Exception ignored) {}
    }

    // ================= INTEREST =================
    public static String handleInterest(String text) {

        List<Double> nums = new ArrayList<>();
        Matcher m = Pattern.compile("\\d+").matcher(text);
        while (m.find()) nums.add(Double.parseDouble(m.group()));

        if (nums.size() < 3) return "Invalid Interest Input";

        double p = nums.get(0);
        double r = nums.get(1);
        double t = nums.get(2);

        if (text.contains("compound")) {
            double amount = p * Math.pow((1 + r / 100), t);
            double ci = amount - p;
            steps.append("Compound Interest = ").append(ci).append("\n");
            steps.append("Total Amount = ").append(amount).append("\n");
            return String.valueOf(amount);
        } else {
            double si = (p * r * t) / 100;
            steps.append("Simple Interest = ").append(si).append("\n");
            steps.append("Total Amount = ").append(p + si).append("\n");
            return String.valueOf(p + si);
        }
    }

    // ================= MATH =================
    public static double handleMath(String input) {
        String exp = preprocess(input);
        return evaluate(exp);
    }

    public static String preprocess(String e) {
        e = e.replaceAll("\\s+", "");
        return replaceTrig(e);
    }

    public static String replaceTrig(String e) {

        Pattern p = Pattern.compile("(sin|cos|tan|sec|cosec|cot)\\(([^)]+)\\)");
        Matcher m = p.matcher(e);
        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            String f = m.group(1);
            double v = Double.parseDouble(m.group(2));
            double a = DEGREE_MODE ? Math.toRadians(v) : v;
            double r = switch (f) {
                case "sin" -> Math.sin(a);
                case "cos" -> Math.cos(a);
                case "tan" -> Math.tan(a);
                case "sec" -> 1 / Math.cos(a);
                case "cosec" -> 1 / Math.sin(a);
                default -> 1 / Math.tan(a);
            };
            steps.append(f).append("(").append(v).append(")=").append(r).append("\n");
            m.appendReplacement(sb, Double.toString(r));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static double evaluate(String e) {

        Stack<Double> v = new Stack<>();
        Stack<Character> o = new Stack<>();

        for (int i = 0; i < e.length(); i++) {
            char c = e.charAt(i);

            if (Character.isDigit(c) || c == '.') {
                StringBuilder sb = new StringBuilder();
                while (i < e.length() &&
                        (Character.isDigit(e.charAt(i)) || e.charAt(i) == '.'))
                    sb.append(e.charAt(i++));
                i--;
                v.push(Double.parseDouble(sb.toString()));
            } else if ("+-*/".indexOf(c) >= 0) {
                while (!o.isEmpty()) apply(v, o.pop());
                o.push(c);
            }
        }
        while (!o.isEmpty()) apply(v, o.pop());
        return v.pop();
    }

    static void apply(Stack<Double> v, char o) {
        double b = v.pop(), a = v.pop();
        double r = switch (o) {
            case '+' -> a + b;
            case '-' -> a - b;
            case '*' -> a * b;
            default -> a / b;
        };
        steps.append(a).append(o).append(b).append("=").append(r).append("\n");
        v.push(r);
    }
}
