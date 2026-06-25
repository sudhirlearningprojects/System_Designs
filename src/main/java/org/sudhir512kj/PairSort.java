package org.sudhir512kj;

import java.util.Arrays;

public class PairSort {

    public static int[][] pairAndSort(int[] a, int[] b) {
        int n = Math.min(a.length, b.length);
        int[][] pairs = new int[n][2];
        for (int i = 0; i < n; i++) {
            pairs[i][0] = a[i];
            pairs[i][1] = b[i];
        }
        Arrays.sort(pairs, (p1, p2) -> Integer.compare(p1[0], p2[0]));
        return pairs;
    }

    public static void main(String[] args) {
        int[] a = {5, 2, 8, 1};
        int[] b = {10, 20, 30, 40};

        int[][] result = pairAndSort(a, b);

        for (int[] pair : result) {
            System.out.println("(" + pair[0] + ", " + pair[1] + ")");
        }
    }
}
