import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SecretReconstruction {

    static class Share {
        BigInteger x, y;
        Share(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }

    public static void main(String[] args) throws Exception {
        // 1. Read the JSON file into a string
        String content = new String(Files.readAllBytes(Paths.get("input.json")));

        // 2. Remove unnecessary characters
        content = content.replaceAll("[{}\\n\\t ]", "");
        content = content.replace("\"", "");  // remove quotes

        // 3. Extract n and k
        int n = Integer.parseInt(content.split("n:")[1].split(",")[0]);
        int k = Integer.parseInt(content.split("k:")[1].split(",")[0]);

        // 4. Extract shares part
        String sharesPart = content.split("shares:")[1];
        sharesPart = sharesPart.replaceAll("[{}]", "");

        // Debug output to see how shares look
        System.out.println("DEBUG sharesPart before splitting: " + sharesPart);

        String[] pairs = sharesPart.split(",");

        List<Share> shares = new ArrayList<>();
        for (String p : pairs) {
            if (p == null) continue;
            p = p.trim();
            if (p.isEmpty()) continue;

            // Debug each piece
            System.out.println("DEBUG pair: " + p);

            String[] kv = p.split(":");
            if (kv.length != 2) {
                System.out.println("Skipping malformed pair: " + p);
                continue;
            }

            String xStr = kv[0].trim();
            String yStr = kv[1].trim();

            // Debug before parsing
            System.out.println("Parsing x=" + xStr + " y=" + yStr);

            try {
                BigInteger x = new BigInteger(xStr);
                BigInteger y = new BigInteger(yStr);
                shares.add(new Share(x, y));
            } catch (Exception ex) {
                System.out.println("Invalid number in pair: " + p);
            }
        }

        // 5. Find and print the secret
        BigInteger secret = findSecret(shares, k);
        System.out.println("Secret: " + secret);
    }

    static BigInteger findSecret(List<Share> shares, int k) {
        List<List<Share>> comb = new ArrayList<>();
        combinations(shares, k, 0, new ArrayList<>(), comb);

        Map<String, Integer> freq = new HashMap<>();
        for (List<Share> subset : comb) {
            BigInteger s = lagrangeInterpolation(subset);
            String key = s.toString();
            freq.put(key, freq.getOrDefault(key, 0) + 1);
        }

        String maxKey = null;
        int maxCount = 0;
        for (var e : freq.entrySet()) {
            if (e.getValue() > maxCount) {
                maxCount = e.getValue();
                maxKey = e.getKey();
            }
        }
        return new BigInteger(maxKey);
    }

    static void combinations(List<Share> arr, int k, int idx,
                             List<Share> curr, List<List<Share>> result) {
        if (curr.size() == k) {
            result.add(new ArrayList<>(curr));
            return;
        }
        if (idx == arr.size()) return;
        curr.add(arr.get(idx));
        combinations(arr, k, idx + 1, curr, result);
        curr.remove(curr.size() - 1);
        combinations(arr, k, idx + 1, curr, result);
    }

    static BigInteger lagrangeInterpolation(List<Share> subset) {
        BigInteger secret = BigInteger.ZERO;
        int k = subset.size();

        for (int i = 0; i < k; i++) {
            BigInteger term = subset.get(i).y;
            for (int j = 0; j < k; j++) {
                if (i != j) {
                    BigInteger num = subset.get(j).x.negate();
                    BigInteger den = subset.get(i).x.subtract(subset.get(j).x);
                    term = term.multiply(num).divide(den);
                }
            }
            secret = secret.add(term);
        }
        return secret;
    }
}
