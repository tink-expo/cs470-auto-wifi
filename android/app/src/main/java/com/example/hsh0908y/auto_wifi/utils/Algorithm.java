package com.example.hsh0908y.auto_wifi.utils;

import java.util.ArrayList;
import java.util.List;

public class Algorithm {
    public static List<List<Integer>> getIndexGraphComponentList(List<List<Integer>> graph) {
        List<List<Integer>> graphComponentList = new ArrayList<>();
        boolean[] visited = new boolean[graph.size()];
        for (int i = 0; i < graph.size(); ++i) {
            if (!visited[i]) {
                graphComponentList.add(new ArrayList<Integer>());
                Dfs(i, graph, visited, graphComponentList.get(graphComponentList.size() - 1));
            }
        }
        return graphComponentList;
    }

    public static int getLengthOfLongestCommonSubsequence(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; ++i) {
            for (int j = 0; j <= n; ++j) {
                if (i == 0 || j == 0) {
                    dp[i][j] = 0;
                } else if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = 1 + dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp[m][n];
    }

    public static List<Integer> getLengthAndEndOfLongestCommonSubstring(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();

        int[][] dp = new int[m + 1][n + 1];

        int maxLength = 0;
        int maxEndI = -1;
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0 || j == 0) {
                    dp[i][j] = 0;
                } else if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    if (dp[i][j] > maxLength) {
                        maxLength = dp[i][j];
                        maxEndI = i;
                    }
                }
            }
        }

        List<Integer> lengthAndStart = new ArrayList<>();
        lengthAndStart.add(maxLength);
        lengthAndStart.add(maxEndI);
        return lengthAndStart;
    }

    public static int getLengthOfLongestCommonSubstring(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();

        int[][] dp = new int[m + 1][n + 1];

        int maximum = 0;
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0 || j == 0) {
                    dp[i][j] = 0;
                } else if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    maximum = Math.max(maximum, dp[i][j]);
                }
            }
        }

        return maximum;
    }

    public static int getCustomizedEditDistance(String s1, String s2) {
        int l1 = s1.length();
        int l2 = s2.length();
        int[][] dp = new int[l1 + 1][l2 + 1];
        for (int i = 1; i <= l1; ++i) {
            dp[i][0] = i;
        }
        for (int j = 1; j <= l2; ++j) {
            dp[0][j] = j;
        }

        for (int j = 1; j <= l2; ++j) {
            for (int i = 1; i <= l1; ++i) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(
                            2 + dp[i - 1][j - 1],
                            1 + Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        return dp[l1 - 1][l2 - 1];
    }

    public static boolean isSubsequenceOf(String sub, String target) {
        if(sub.isEmpty()) {
            return true;
        }

        int i = 0;
        int j = 0;
        while(i < sub.length() && j < target.length()){
            if(sub.charAt(i) == target.charAt(j)){
                ++i;
            }
            ++j;

            if(i == sub.length()) {
                return true;
            }
        }

        return false;
    }

    private static void Dfs(int curIndex, List<List<Integer>> graph, boolean[] visited, List<Integer> component) {
        visited[curIndex] = true;
        component.add(curIndex);
        for (int nextIndex : graph.get(curIndex)) {
            if (!visited[nextIndex]) {
                Dfs(nextIndex, graph, visited, component);
            }
        }
    }
}
