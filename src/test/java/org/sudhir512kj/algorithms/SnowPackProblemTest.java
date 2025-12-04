package org.sudhir512kj.algorithms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SnowPackProblemTest {

    @ParameterizedTest
    @MethodSource("provideTestCases")
    void testAllSolutions(int[] heights, int expected, String description) {
        assertEquals(expected, SnowPackProblem.trapSnowBruteForce(heights), "Brute Force: " + description);
        assertEquals(expected, SnowPackProblem.trapSnowDP(heights), "DP: " + description);
        assertEquals(expected, SnowPackProblem.trapSnowTwoPointers(heights), "Two Pointers: " + description);
        assertEquals(expected, SnowPackProblem.trapSnowStack(heights), "Stack: " + description);
    }

    static Stream<Arguments> provideTestCases() {
        return Stream.of(
            Arguments.of(new int[]{3, 0, 2, 0, 4}, 7, "Basic valley"),
            Arguments.of(new int[]{0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1}, 6, "Complex terrain"),
            Arguments.of(new int[]{4, 2, 0, 3, 2, 5}, 9, "Multiple peaks"),
            Arguments.of(new int[]{5, 4, 3, 2, 1}, 0, "Descending slope"),
            Arguments.of(new int[]{1, 2, 3, 4, 5}, 0, "Ascending slope"),
            Arguments.of(new int[]{3, 3, 3, 3}, 0, "Flat terrain"),
            Arguments.of(new int[]{}, 0, "Empty array"),
            Arguments.of(new int[]{5}, 0, "Single element"),
            Arguments.of(new int[]{5, 5}, 0, "Two elements"),
            Arguments.of(new int[]{5, 2, 1, 2, 1, 5}, 14, "Perfect valley"),
            Arguments.of(new int[]{3, 0, 0, 2, 0, 4}, 10, "Multiple zeros"),
            Arguments.of(new int[]{2, 0, 2}, 2, "Simple valley"),
            Arguments.of(new int[]{100, 0, 100}, 100, "Large heights"),
            Arguments.of(new int[]{0, 0, 0, 0}, 0, "All zeros"),
            Arguments.of(new int[]{3, 1, 3, 1, 3}, 4, "Multiple valleys"),
            Arguments.of(new int[]{5, 1, 5, 1, 5, 1, 5}, 16, "Many valleys"),
            Arguments.of(new int[]{10, 5, 8, 3, 9, 2, 7}, 13, "Realistic mountain"),
            Arguments.of(new int[]{1, 0, 1, 0, 1, 0, 1, 0, 1}, 4, "Alternating pattern"),
            Arguments.of(new int[]{10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, 81, "V-shape")
        );
    }

    @Test
    void testNullInput() {
        assertEquals(0, SnowPackProblem.trapSnowBruteForce(null));
        assertEquals(0, SnowPackProblem.trapSnowDP(null));
        assertEquals(0, SnowPackProblem.trapSnowTwoPointers(null));
        assertEquals(0, SnowPackProblem.trapSnowStack(null));
    }

    @Test
    void testDetailedResult() {
        int[] heights = {3, 0, 2, 0, 4};
        SnowPackProblem.SnowPackResult result = SnowPackProblem.trapSnowWithDetails(heights);
        
        assertEquals(7, result.totalSnow);
        assertEquals(3, result.segments.size());
    }

    @Test
    void testVisualization() {
        int[] heights = {3, 0, 2, 0, 4};
        String visualization = SnowPackProblem.visualize(heights);
        
        assertNotNull(visualization);
        assertTrue(visualization.contains("█"));
        assertTrue(visualization.contains("░"));
    }

    @Test
    void testMaxIntegerValues() {
        int[] heights = {Integer.MAX_VALUE / 2, 0, Integer.MAX_VALUE / 2};
        assertEquals(Integer.MAX_VALUE / 2, SnowPackProblem.trapSnowTwoPointers(heights));
    }

    @Test
    void testZigzagPattern() {
        int[] heights = {5, 1, 5, 1, 5, 1, 5};
        assertEquals(16, SnowPackProblem.trapSnowTwoPointers(heights));
    }
}
