import com.sun.net.httpserver.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class SmartCalculator {

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/calculate", exchange -> {

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            String input = URLDecoder.decode(body.replace("input=", ""), "UTF-8");

            SmartCalculatorCore.steps.setLength(0);
            String result;

            try {
                if (input.contains("interest") || input.contains("percent")) {
                    result = SmartCalculatorCore.handleInterest(input);
                } else {
                    result = String.valueOf(SmartCalculatorCore.handleMath(input));
                }
            } catch (Exception e) {
                result = "Invalid Input";
            }

            SmartCalculatorCore.save(input, result);

            String json = """
            {
              "input":"%s",
              "steps":"%s",
              "result":"%s"
            }
            """.formatted(
                    input,
                    SmartCalculatorCore.steps.toString().replace("\n","\\n"),
                    result
            );

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, json.getBytes().length);
            exchange.getResponseBody().write(json.getBytes());
            exchange.close();
        });

        server.start();
        System.out.println("✅ Backend running at http://localhost:8080");
    }
}
