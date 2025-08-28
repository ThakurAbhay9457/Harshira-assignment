package com.example.secretfinder;

import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SecretFinder {

    // Using a Java record for an immutable Point data structure
    public record Point(BigInteger x, BigInteger y) {}

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -jar secret-finder.jar <path_to_json_file>");
            return;
        }

        try {
            // 1. Read the Test Case (Input) from a separate JSON file
            String content = new String(Files.readAllBytes(Paths.get(args[0])));
            JSONObject data = new JSONObject(content);

            JSONObject keysInfo = data.getJSONObject("keys");
            int n = keysInfo.getInt("n");
            int k = keysInfo.getInt("k");

            List<Point> points = new ArrayList<>();
            for (String key : data.keySet()) {
                if (key.equals("keys")) {
                    continue;
                }

                JSONObject pointData = data.getJSONObject(key);
                BigInteger x = new BigInteger(key);

                // 2. Decode the Y Values
                String yEncoded = pointData.getString("value");
                int base = Integer.parseInt(pointData.getString("base"));
                BigInteger y = new BigInteger(yEncoded, base); // BigInteger handles decoding natively

                points.add(new Point(x, y));
            }

            // Sort points by x-coordinate to ensure we use the first k points consistently
            points.sort(Comparator.comparing(Point::x));

            // 3. Find the Secret (C)
            BigInteger secret = findSecret(points, k);

            System.out.println("Successfully parsed " + n + " roots.");
            System.out.println("Minimum roots required (k): " + k);
            System.out.println("----------------------------------");
            System.out.println("The calculated secret (C) is: " + secret);

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Finds the secret (the polynomial's value at x=0) using Lagrange Interpolation.
     * P(0) = Sum_{j=0}^{k-1} y_j * L_j(0)
     * L_j(0) = Product_{m=0, m!=j}^{k-1} (-x_m) / (x_j - x_m)
     *
     * @param allPoints A list of all available points (x, y).
     * @param k The number of points to use for interpolation.
     * @return The calculated secret, P(0), as a BigInteger.
     */
    private static BigInteger findSecret(List<Point> allPoints, int k) {
        if (allPoints.size() < k) {
            throw new IllegalArgumentException("Not enough points to solve for the secret.");
        }

        List<Point> points = allPoints.subList(0, k); // Use the first k points
        BigInteger secret = BigInteger.ZERO;

        for (int j = 0; j < k; j++) {
            Point currentPoint = points.get(j);
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            for (int m = 0; m < k; m++) {
                if (j == m) {
                    continue;
                }
                Point otherPoint = points.get(m);
                // numerator *= -x_m
                numerator = numerator.multiply(otherPoint.x().negate());
                // denominator *= (x_j - x_m)
                denominator = denominator.multiply(currentPoint.x().subtract(otherPoint.x()));
            }

            // term = y_j * (numerator / denominator)
            BigInteger term = currentPoint.y().multiply(numerator).divide(denominator);
            secret = secret.add(term);
        }

        return secret;
    }
}
