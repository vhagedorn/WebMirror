package me.vadim.archive.util;

/**
 * Useful utility methods for controlling the length & adding padding to strings.
 *
 * @link <a href="https://gist.github.com/Abdallah-Abdelazim/3c2ee83c04f22d4c2edc75cf5e3a5a35">Source</a>
 * @author Abdallah Abdelazim
 */
public final class StrPad {

    /**
     * Prevent instantiation.
     */
    private StrPad() {

    }

    /**
     * Limit the number of characters in a string to a given length or left pad it with {@code spaces}
     * if it's smaller.
     * @param text the text to process.
     * @param length the desired length.
     * @return the text truncated from the right if its length is greater than the given length,
     * or pad it with spaces from the left if its length is smaller.
     */
    public static String leftPad(String text, int length) {
        return String.format("%" + length + "." + length + "s", text);
    }

    /**
     * Limit the number of characters in a string to a given length or left pad it with a given padding character
     * if it's smaller.
     * @param text the text to process.
     * @param length the desired length.
     * @param padding the custom padding character.
     * @return the text truncated from the right if its length is greater than the given length,
     * or pad it with the given padding character from the left if its length is smaller.
     * @see StrPad#leftPad(String, int)
     */
    public static String leftPad(String text, int length, char padding) {
        return leftPad(text, length).replace(' ', padding);
    }

    /**
     * Limit the number of characters in a string to a given length or right pad it with {@code spaces}
     * if it's smaller.
     * @param text the text to process.
     * @param length the desired length.
     * @return the text truncated from the right if its length is greater than the given length,
     * or pad it with spaces from the right if its length is smaller.
     */
    public static String rightPad(String text, int length) {
        return String.format("%-" + length + "." + length + "s", text);
    }

    /**
     * Limit the number of characters in a string to a given length or right pad it with a given padding character
     * if it's smaller.
     * @param text the text to process.
     * @param length the desired length.
     * @param padding the custom padding character.
     * @return the text truncated from the right if its length is greater than the given length,
     * or pad it with the given padding character from the right if its length is smaller.
     * @see StrPad#rightPad(String, int)
     */
    public static String rightPad(String text, int length, char padding) {
        return rightPad(text, length).replace(' ', padding);
    }

}