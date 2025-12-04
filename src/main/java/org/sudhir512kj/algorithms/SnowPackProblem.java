package org.sudhir512kj.algorithms;

import java.util.*;

/**
 * Snow Pack Problem (Trapping Rain Water)
 * 
 * Given an array representing mountain heights, calculate how much snow can be trapped between peaks.
 * 
 * Example:
 * Input: [3, 0, 2, 0, 4]
 * Visual:
 *       █
 *   █ ░░█
 *   █░█░█
 * ─────────
 * Output: 7 (░ represents trapped snow)
 */
public class SnowPackProblem {

    /**
     * Solution 1: Brute Force
     * Time: O(n²), Space: O(1)
     */
    public static int trapSnowBruteForce(int[] heights) {
        if (heights == null || heights.length < 3) return 0;
        
        int totalSnow = 0;
        int n = heights.length;
        
        for (int i = 1; i < n - 1; i++) {
            int leftMax = 0;
            for (int j = 0; j <= i; j++) {
                leftMax = Math.max(leftMax, heights[j]);
            }
            
            int rightMax = 0;
            for (int j = i; j < n; j++) {
                rightMax = Math.max(rightMax, heights[j]);
            }
            
            totalSnow += Math.min(leftMax, rightMax) - heights[i];
        }
        
        return totalSnow;
    }

    /**
     * Solution 2: Dynamic Programming
     * Time: O(n), Space: O(n)
     */
    public static int trapSnowDP(int[] heights) {
        if (heights == null || heights.length < 3) return 0;
        
        int n = heights.length;
        int[] leftMax = new int[n];
        int[] rightMax = new int[n];
        
        leftMax[0] = heights[0];
        for (int i = 1; i < n; i++) {
            leftMax[i] = Math.max(leftMax[i - 1], heights[i]);
        }
        
        rightMax[n - 1] = heights[n - 1];
        for (int i = n - 2; i >= 0; i--) {
            rightMax[i] = Math.max(rightMax[i + 1], heights[i]);
        }
        
        int totalSnow = 0;
        for (int i = 0; i < n; i++) {
            totalSnow += Math.min(leftMax[i], rightMax[i]) - heights[i];
        }
        
        return totalSnow;
    }

    /**
     * Solution 3: Two Pointers (Most Optimal)
     * Time: O(n), Space: O(1)
     */
    public static int trapSnowTwoPointers(int[] heights) {
        if (heights == null || heights.length < 3) return 0;
        
        int left = 0, right = heights.length - 1;
        int leftMax = 0, rightMax = 0;
        int totalSnow = 0;
        
        while (left < right) {
            if (heights[left] < heights[right]) {
                if (heights[left] >= leftMax) {
                    leftMax = heights[left];
                } else {
                    totalSnow += leftMax - heights[left];
                }
                left++;
            } else {
                if (heights[right] >= rightMax) {
                    rightMax = heights[right];
                } else {
                    totalSnow += rightMax - heights[right];
                }
                right--;
            }
        }
        
        return totalSnow;
    }

    /**
     * Solution 4: Stack-Based
     * Time: O(n), Space: O(n)
     */
    public static int trapSnowStack(int[] heights) {
        if (heights == null || heights.length < 3) return 0;
        
        Stack<Integer> stack = new Stack<>();
        int totalSnow = 0;
        
        for (int i = 0; i < heights.length; i++) {
            while (!stack.isEmpty() && heights[i] > heights[stack.peek()]) {
                int top = stack.pop();
                if (stack.isEmpty()) break;
                
                int distance = i - stack.peek() - 1;
                int boundedHeight = Math.min(heights[i], heights[stack.peek()]) - heights[top];
                totalSnow += distance * boundedHeight;
            }
            stack.push(i);
        }
        
        return totalSnow;
    }

    /**
     * Solution 5: With Details
     */
    public static SnowPackResult trapSnowWithDetails(int[] heights) {
        if (heights == null || heights.length < 3) {
            return new SnowPackResult(0, new ArrayList<>());
        }
        
        int n = heights.length;
        int[] leftMax = new int[n];
        int[] rightMax = new int[n];
        List<SnowSegment> segments = new ArrayList<>();
        
        leftMax[0] = heights[0];
        for (int i = 1; i < n; i++) {
            leftMax[i] = Math.max(leftMax[i - 1], heights[i]);
        }
        
        rightMax[n - 1] = heights[n - 1];
        for (int i = n - 2; i >= 0; i--) {
            rightMax[i] = Math.max(rightMax[i + 1], heights[i]);
        }
        
        int totalSnow = 0;
        for (int i = 0; i < n; i++) {
            int waterLevel = Math.min(leftMax[i], rightMax[i]);
            int snowAtPosition = waterLevel - heights[i];
            totalSnow += snowAtPosition;
            
            if (snowAtPosition > 0) {
                segments.add(new SnowSegment(i, heights[i], waterLevel, snowAtPosition));
            }
        }
        
        return new SnowPackResult(totalSnow, segments);
    }

    public static String visualize(int[] heights) {
        if (heights == null || heights.length == 0) return "";
        
        int maxHeight = Arrays.stream(heights).max().orElse(0);
        int n = heights.length;
        
        int[] leftMax = new int[n];
        int[] rightMax = new int[n];
        
        leftMax[0] = heights[0];
        for (int i = 1; i < n; i++) {
            leftMax[i] = Math.max(leftMax[i - 1], heights[i]);
        }
        
        rightMax[n - 1] = heights[n - 1];
        for (int i = n - 2; i >= 0; i--) {
            rightMax[i] = Math.max(rightMax[i + 1], heights[i]);
        }
        
        int[] waterLevel = new int[n];
        for (int i = 0; i < n; i++) {
            waterLevel[i] = Math.min(leftMax[i], rightMax[i]);
        }
        
        StringBuilder sb = new StringBuilder();
        for (int level = maxHeight; level > 0; level--) {
            for (int i = 0; i < n; i++) {
                if (heights[i] >= level) {
                    sb.append("█");
                } else if (waterLevel[i] >= level) {
                    sb.append("░");
                } else {
                    sb.append(" ");
                }
            }
            sb.append("\n");
        }
        
        sb.append("─".repeat(n)).append("\n");
        for (int i = 0; i < n; i++) {
            sb.append(i % 10);
        }
        
        return sb.toString();
    }

    public static class SnowPackResult {
        public final int totalSnow;
        public final List<SnowSegment> segments;
        
        public SnowPackResult(int totalSnow, List<SnowSegment> segments) {
            this.totalSnow = totalSnow;
            this.segments = segments;
        }
        
        @Override
        public String toString() {
            return String.format("Total Snow: %d units\nSegments: %s", totalSnow, segments);
        }
    }
    
    public static class SnowSegment {
        public final int position;
        public final int groundHeight;
        public final int waterLevel;
        public final int snowDepth;
        
        public SnowSegment(int position, int groundHeight, int waterLevel, int snowDepth) {
            this.position = position;
            this.groundHeight = groundHeight;
            this.waterLevel = waterLevel;
            this.snowDepth = snowDepth;
        }
        
        @Override
        public String toString() {
            return String.format("[Pos:%d, Ground:%d, Water:%d, Snow:%d]", 
                position, groundHeight, waterLevel, snowDepth);
        }
    }

    public static void main(String[] args) {
        int[][] testCases = {
            {3, 0, 2, 0, 4},
            {0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1},
            {4, 2, 0, 3, 2, 5},
            {5, 4, 3, 2, 1},
            {1, 2, 3, 4, 5},
            {3, 3, 3, 3},
            {5, 2, 1, 2, 1, 5},
            {},
            {5},
            {5, 5},
        };
        
        for (int i = 0; i < testCases.length; i++) {
            int[] heights = testCases[i];
            System.out.println("\n" + "=".repeat(60));
            System.out.println("Test Case " + (i + 1) + ": " + Arrays.toString(heights));
            System.out.println("=".repeat(60));
            
            if (heights.length >= 3) {
                System.out.println("\nVisualization:");
                System.out.println(visualize(heights));
            }
            
            int result1 = trapSnowBruteForce(heights);
            int result2 = trapSnowDP(heights);
            int result3 = trapSnowTwoPointers(heights);
            int result4 = trapSnowStack(heights);
            
            System.out.println("\nResults:");
            System.out.println("Brute Force:   " + result1);
            System.out.println("DP:            " + result2);
            System.out.println("Two Pointers:  " + result3);
            System.out.println("Stack:         " + result4);
            
            SnowPackResult detailed = trapSnowWithDetails(heights);
            System.out.println("\nDetailed: " + detailed);
            
            boolean allMatch = result1 == result2 && result2 == result3 && result3 == result4;
            System.out.println("✓ All match: " + allMatch);
        }
    }
}
