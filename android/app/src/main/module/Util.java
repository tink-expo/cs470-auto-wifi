package autowifi.experimental;

import java.util.ArrayList;
import java.util.List;

public class Util {
    public static List<List<Integer>> getIndexGraphComponentList(List<List<Integer>> graph) {
        List<List<Integer>> graphComponentList = new ArrayList<>();
        boolean[] visited = new boolean[graph.size()];
        for (int i = 0; i < graph.size(); ++i) {
            if (!visited[i]) {
                graphComponentList.add(new ArrayList<>());
                Dfs(i, graph, visited, graphComponentList.get(graphComponentList.size() - 1));
            }
        }
        return graphComponentList;
    }

    public static int getLengthOfLongestCommonSubsequence(String s1, String s2) {
        // System.out.printf("%s %s , %d %d\n", s1, s2, s1.length(), s2.length());
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
