package br.jm.music.utils;

/**
 * Created by Jorge on 23/07/2015.
 */
public class StringUtils {

    public static String getReversedString(){
        // A reversed public key piece (from 14 to 60)
        String text = "u5OFArtnQx2v9XT0aeqackGcbiima8qacoaafeqab0W9gI";

        char[] chars = text.toCharArray();
        int indexOfLastChar = chars.length - 1;
        for(int i = 0; i < chars.length/2; i++){
            char temp = chars[i];
            chars[i] = chars[indexOfLastChar - i ];
            chars[indexOfLastChar - i] = temp;
        }

        return new String(chars);
    }

    public static String swapCases(String s){
        // Get byte sequence to play with.
        byte[] bytes = s.getBytes();

        // Swap upper and lower case letters.
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] >= 'A' && bytes[i] <= 'Z')
                bytes[i] = (byte) ('a' + (bytes[i] - 'A'));
            else if (bytes[i] >= 'a' && bytes[i] <= 'z')
                bytes[i] = (byte) ('A' + (bytes[i] - 'a'));
        }

        // Assign back to string.
        return new String(bytes);
    }

}
