import java.math.BigInteger;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

public class SecretBlockchain {

    static class Block {
        int index;
        long timestamp;
        String nodeId;
        BigInteger x;
        BigInteger y;
        String prevHash;
        String hash;
        int nonce;

        public Block(int index, String nodeId, BigInteger x, BigInteger y, String prevHash) {
            this.index = index;
            this.nodeId = nodeId;
            this.x = x;
            this.y = y;
            this.prevHash = prevHash;
            this.timestamp = Instant.now().getEpochSecond();
            this.nonce = 0;
            this.hash = calculateHash();
        }

        public String calculateHash() {
            try {
                String input = index + nodeId + x + y + prevHash + timestamp + nonce;
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] bytes = digest.digest(input.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte b : bytes) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (Exception e) {
                throw new RuntimeException("Hashing failed", e);
            }
        }

        public void mineBlock(int difficulty) {
            String target = "0".repeat(difficulty);
            while (!hash.substring(0, difficulty).equals(target)) {
                nonce++;
                hash = calculateHash();
            }
            System.out.println("✅ Block Mined: " + hash + " by " + nodeId);
        }
    }

    static class Blockchain {
        List<Block> chain = new ArrayList<>();
        int difficulty = 3;

        public Blockchain() {
            chain.add(createGenesisBlock());
        }

        private Block createGenesisBlock() {
            return new Block(0, "Genesis", BigInteger.ZERO, BigInteger.ZERO, "0");
        }

        public void addBlock(String nodeId, BigInteger x, BigInteger y) {
            Block prev = chain.get(chain.size() - 1);
            Block newBlock = new Block(chain.size(), nodeId, x, y, prev.hash);
            newBlock.mineBlock(difficulty);
            chain.add(newBlock);
        }

        public List<Block> getValidBlocks() {
            List<Block> valid = new ArrayList<>();
            for (Block b : chain) {
                if (!b.nodeId.equals("Genesis")) valid.add(b);
            }
            return valid;
        }
    }

    static BigInteger reconstructSecret(List<Block> blocks) {
        BigInteger secret = BigInteger.ZERO;
        int k = blocks.size();

        for (int j = 0; j < k; j++) {
            BigInteger xj = blocks.get(j).x;
            BigInteger yj = blocks.get(j).y;

            BigInteger num = BigInteger.ONE;
            BigInteger den = BigInteger.ONE;

            for (int m = 0; m < k; m++) {
                if (m == j) continue;
                BigInteger xm = blocks.get(m).x;
                num = num.multiply(xm.negate());
                den = den.multiply(xj.subtract(xm));
            }

            BigInteger lj = num.multiply(den.modInverse(BigInteger.valueOf(1000000007)));
            secret = secret.add(yj.multiply(lj));
        }

        return secret.mod(BigInteger.valueOf(1000000007));
    }

    public static void main(String[] args) {
        Blockchain blockchain = new Blockchain();

        blockchain.addBlock("Node-A", BigInteger.valueOf(1), BigInteger.valueOf(10 + 15));
        blockchain.addBlock("Node-B", BigInteger.valueOf(2), BigInteger.valueOf(4 * 6));
        blockchain.addBlock("Node-C", BigInteger.valueOf(3), BigInteger.valueOf(gcd(36, 60)));
        blockchain.addBlock("Node-D", BigInteger.valueOf(4), BigInteger.valueOf(lcm(4, 5)));
        blockchain.addBlock("Node-E", BigInteger.valueOf(5), BigInteger.valueOf(100));

        List<Block> allShares = blockchain.getValidBlocks();
        List<BigInteger> secrets = new ArrayList<>();

        for (int i = 0; i < allShares.size(); i++) {
            for (int j = i + 1; j < allShares.size(); j++) {
                for (int k = j + 1; k < allShares.size(); k++) {
                    List<Block> combo = List.of(allShares.get(i), allShares.get(j), allShares.get(k));
                    BigInteger secret = reconstructSecret(combo);
                    secrets.add(secret);
                }
            }
        }

        BigInteger trueSecret = mostCommon(secrets);
        System.out.println("\n🔐 Final Reconstructed Secret: " + trueSecret);
    }

    static BigInteger gcd(int a, int b) {
        return BigInteger.valueOf(a).gcd(BigInteger.valueOf(b));
    }

    static BigInteger lcm(int a, int b) {
        BigInteger A = BigInteger.valueOf(a);
        BigInteger B = BigInteger.valueOf(b);
        return A.multiply(B).divide(A.gcd(B));
    }

    static BigInteger mostCommon(List<BigInteger> list) {
        Map<BigInteger, Integer> freq = new HashMap<>();
        for (BigInteger val : list) {
            freq.put(val, freq.getOrDefault(val, 0) + 1);
        }
        return Collections.max(freq.entrySet(), Map.Entry.comparingByValue()).getKey();
    }
}