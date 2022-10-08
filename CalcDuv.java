/*

Source code from: https://github.com/gdmn/duv

Calculate duv for x and y, numbers can be comma or dot separated:
    java CalcDuv.java 0,4525 0,4037
    result: -0,0019
If input is greater than one, it will be automatically divided by 10000:
    java CalcDuv.java 4525 4037
    result: -0,0019
Calculate duv for values read from console (standard input):
    java CalcDuv.java
You can input more than one value pair, it is useful for copy-pasting from any spreadsheet application, for example LibreOffice Calc:
    java CalcDuv.java
    4356	4118
    4377	4101
    0,0033
    0,0023

Run in podman:
    podman run -ti --rm -e MAVEN_CONFIG=/var/maven/.m2 -v "$PWD":/var/maven/project -w /var/maven/project docker.io/maven:3-openjdk-17 java CalcDuv.java

Run in docker:
    docker run -ti --rm -e MAVEN_CONFIG=/var/maven/.m2 -v "$PWD":/var/maven/project -w /var/maven/project -u "$(id -u):$(id -g)" maven:3-openjdk-17 java CalcDuv.java

It uses algorithm from https://www.waveformlighting.com/tech/calculate-duv-from-cie-1931-xy-coordinates

*/

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;

import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;

public class CalcDuv {

    private final LinkedList<Double> buffer;
    private final DecimalFormat decimalFormat;

    CalcDuv() {
        buffer = new LinkedList<>();
        decimalFormat = new DecimalFormat();
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        decimalFormat.setDecimalFormatSymbols(symbols);
    }

    private double calc(Double[] xy) {
        double x = xy[0];
        double y = xy[1];
        double u = (4 * x) / (-2 * x + 12 * y + 3);
        double v = (6 * y) / (-2 * x + 12 * y + 3);
        double k6 = -0.00616793;
        double k5 = 0.0893944;
        double k4 = -0.5179722;
        double k3 = 1.5317403;
        double k2 = -2.4243787;
        double k1 = 1.925865;
        double k0 = -0.471106;
        double lfp = Math.sqrt(Math.pow((u - 0.292), 2) + Math.pow((v - 0.24), 2));
        double a = Math.acos((u - 0.292) / lfp);
        double lbb = k6 * Math.pow(a, 6) + k5 * Math.pow(a, 5) + k4 * Math.pow(a, 4) + k3 * Math.pow(a, 3) + k2 * Math.pow(a, 2) + k1 * a + k0;
        return lfp - lbb;
    }

    private void put(LinkedList<Double> buffer, Double item) {
        buffer.add(item);
    }

    private Double[] pop(LinkedList<Double> buffer) {
        return Optional.ofNullable(buffer)
                .filter(list -> list.size() >= 2)
                .map(list -> new Double[]{list.pop(), list.pop()})
                .orElse(null);
    }

    private void parse(LinkedList<Double> buffer, String input) {
        Optional.ofNullable(input)
                .map(String::trim)
                .map(string -> string.replace(',', '.'))
                .map(string -> string.split("[ \\s]+"))
                .stream()
                .flatMap(Arrays::stream)
                .filter(not(String::isEmpty))
                .map(s -> {
                    try {
                        return decimalFormat.parse(s);
                    } catch (ParseException e) {
                        System.err.println(e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(Number::doubleValue)
                .map(value -> value > 1 ? value / 10000 : value)
                .forEach(value -> this.put(buffer, value));
    }

    private void print(double result) {
        System.out.printf("%.4f%n", result);
    }

    void onInput(String input) {
        parse(buffer, input);
        Double[] doubles;
        while (nonNull(doubles = pop(buffer))) {
            print(calc(doubles));
        }
    }

    public static void main(String[] args) {
        CalcDuv app = new CalcDuv();
        if (nonNull(args) && args.length > 0) {
            app.onInput(String.join(" ", args));
        } else {
            Scanner input = new Scanner(System.in);
            while (input.hasNextLine()) {
                app.onInput(input.nextLine());
            }
        }
    }
}
