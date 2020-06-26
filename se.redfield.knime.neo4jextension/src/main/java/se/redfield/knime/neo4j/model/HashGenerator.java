/**
 *
 */
package se.redfield.knime.neo4j.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Vyacheslav Soldatov <vyacheslav.soldatov@inbox.ru>
 *
 */
public class HashGenerator {
    private static byte[] SALT = {-60, -29, 124, 102, -77, 4, 87, 69, -95, 121, -89, -24, -113, 84, -59, -39};

    /**
     * Default constructor.
     */
    private HashGenerator() {
        super();
    }

    /**
     * @param str string.
     * @return MD5 signature for given string.
     */
    public static String generateHash(final String str) {
        String hash = null;
        try {
            // Create MessageDigest instance for MD5
            final MessageDigest md = MessageDigest.getInstance("MD5");
            // Add password bytes to digest
            md.update(SALT);

            // Get the hash's bytes
            final byte[] bytes = md.digest(str.getBytes());

            // This bytes[] has bytes in decimal format;
            // Convert it to hexadecimal format
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            // Get complete hashed string in hex format
            hash = sb.toString();
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return hash;
    }

    /**
     * @param args
     */
    public static void main(final String[] args) {
        System.out.println(generateHash("password"));
    }
}
