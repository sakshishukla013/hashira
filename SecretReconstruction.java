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

        // 1. Read input.json
        String content = new String(Files.readAllBytes(Paths.get("input.json")));

        // Remove whitespace and quotes for manual parsing
        content = content.replaceAll("[\\n\\t ]", "");
        content = content.replace("\"", "");

        // -----------------------------
        // Parse n and k from "keys"
        // -----------------------------
        String keysBlock = content.split("keys:")[1];
        keysBlock = keysBlock.substring(0, keysBlock.indexOf("}"));
        keysBlock = keysBlock.replaceAll("[{}]", "");

        // Safe extraction for n
        String nPart = keysBlock.split("n:")[1].split(",")[0];
        nPart = nPart.replaceAll("[^0-9]", "");
        int n = Integer.parseInt(nPart);

        // Safe extraction for k
        String kPart = keysBlock.split("k:")[1];
        kPart = kPart.replaceAll("[^0-9]", "");
        int k = Integer.parseInt(kPart);

        // -----------------------------
        // Shares parsing
        // -----------------------------
        String afterKeys = content.split("},", 2)[1]; // everything after "},"
        afterKeys = afterKeys.replaceAll("[{}]", ""); // remove braces

        // Each share line looks like:
        // 1:base:6,value:13444211440455345511
        String[] allParts = afterKeys.split(",");

        List<Share> shares = new ArrayList<>();
        for (int i = 0; i < allParts.length;) {

            String block = allParts[i].trim();
            if (block.isEmpty()) {
                i++;
                continue;
            }

            // First item: "<index>:base:<b>"
            String[] part1 = block.split(":");
            if (part1.length < 3) {
                i++;
                continue;
            }

            int index = Integer.parseInt(part1[0]);
            int base = Integer.parseInt(part1[2]);

            // Move to the next array element for value
            i++;
            if (i >= allParts.length) break;
            String valueBlock = allParts[i].trim();

            if (!valueBlock.startsWith("value:")) {
                i++;
                continue;
            }

            String value = valueBlock.split("value:")[1];

            // Convert to BigInteger using the given base
            BigInteger y = new BigInteger(value, base);

            shares.add(new Share(BigInteger.valueOf(index), y));
            i++;
        }

        // -----------------------------
        // Compute and print secret
        // -----------------------------
        BigInteger secret = findSecret(shares, k);
        System.out.println("Secret: " + secret);
    }

    // =====================================================
    // Secret reconstruction (Lagrange interpolation)
    // =====================================================
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
