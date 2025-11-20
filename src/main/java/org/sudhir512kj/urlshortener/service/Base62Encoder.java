package org.sudhir512kj.urlshortener.service;

public class Base62Encoder {
    
    private static final String BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BASE = BASE62.length();
    
    public static String encode(long id) {
        if (id == 0) {
            return String.valueOf(BASE62.charAt(0));
        }
        
        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            sb.append(BASE62.charAt((int)(id % BASE)));
            id /= BASE;
        }
        return sb.reverse().toString();
    }
    
    public static long decode(String shortUrl) {
        long result = 0;
        for (char c : shortUrl.toCharArray()) {
            result = result * BASE + BASE62.indexOf(c);
        }
        return result;
    }
}