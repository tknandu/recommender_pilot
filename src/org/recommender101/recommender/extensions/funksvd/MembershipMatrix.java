package org.recommender101.recommender.extensions.funksvd;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * @author Ahmad
 */
public class MembershipMatrix {

        private int[][] matrix;
                  
        /**
         * creates a MembershipMatrix and randomly initialize memberships
         * 
         * @param n number of patterns/observations
         * @param k number of clusters/groups
         */
        public MembershipMatrix(int n, int k) {
                matrix = new int[n][k];
                
                Random random = new Random(Calendar.getInstance().getTimeInMillis());
                
                // for the first k patterns, assign them to k cluster
                // this way, we guarantee that every cluster has at least one pattern
                for (int i = 0; i < k; i++) {
                        matrix[i][i] = 1;
                }
                
                // for each pattern, randomly choose a cluster for it
                for (int i = k; i < n; i++) {
                        int clusterIndex = random.nextInt(k);
                        matrix[i][clusterIndex] = 1;
                }
        }
        
        /**
         * creates a MembershipMatrix using the specified matrix
         */
        public MembershipMatrix(int[][] matrix) {
                this.matrix = matrix ;
        }
        
        public void movePattern(int patternIndex, int clusterIndex) {
                for (int j = 0; j < matrix[patternIndex].length; j++) {
                        if (j == clusterIndex)
                                matrix[patternIndex][j] = 1;
                        else
                                matrix[patternIndex][j] = 0;
                }
        }
        
        public void moveAllPatterns(int[] clusterIndexes) {
                int n = matrix.length;
                int k = matrix[0].length;
                
                for (int i = 0; i < n; i++) {
                        for (int j = 0; j < k; j++) {
                                if (j == clusterIndexes[i])
                                        matrix[i][j] = 1;
                                else
                                        matrix[i][j] = 0;
                        }
                }
        }
        
        /**
         * returns an arrays of length n, where the ith element represents
         * the clusters index of the ith pattern.
         * 
         * note that this method returns a list so that we can then compare 
         * two clusters configurations (arrays don't implements equals method)
         */
        public List<Integer> getClusters() {
                int n = matrix.length;
                int k = matrix[0].length;
                List<Integer> clusters = new ArrayList<Integer>();
                for (int i = 0; i < n; i++) {
                        for (int j = 0; j < k; j++) {
                                if (matrix[i][j] == 1) {
                                        clusters.add(j);
                                        break;
                                }
                        }
                }
                
                return clusters;
        }
        
        public int[] getPatterns(int cluster) {
                int n = matrix.length;
                List<Integer> patterns = new ArrayList<Integer>();
                for (int i = 0; i < n; i++) {
                        if (matrix[i][cluster] == 1) {
                                patterns.add(i);
                        }
                }
                
                int[] _patterns = new int[patterns.size()];
                for (int i = 0; i < _patterns.length; i++) {
                        _patterns[i] = patterns.get(i);
                }
                
                return _patterns;
        }
        
        public static void main(String[] args) { 
                MembershipMatrix mm = new MembershipMatrix(5, 3);
                List<Integer> clusters = mm.getClusters();
                System.out.println("");
                
                int[] patterns0 = mm.getPatterns(0);
                int[] patterns1 = mm.getPatterns(1);
                int[] patterns2 = mm.getPatterns(2);
                
                System.out.println();
        }
}