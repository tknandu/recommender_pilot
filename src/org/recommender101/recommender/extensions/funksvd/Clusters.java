package org.recommender101.recommender.extensions.funksvd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Clusters {
    static ArrayList<Set<Integer>> cluster;// = new ArrayList<Set<Integer>>();
    static ArrayList<Integer> ItemClusterMap;// = new ArrayList<Integer>();
    
    public void addItem(int itemId,String clusterId)
    {
        int clusId = Integer.parseInt(clusterId);
        if(cluster.get(clusId)==null)
        {
            Set<Integer> s= new HashSet<Integer>();
            cluster.add(clusId,s);
        }
        cluster.get(clusId).add(itemId);
        ItemClusterMap.add(itemId,clusId);
    }
    
    public Clusters(int size,int numItems)
    {
        cluster = new ArrayList<Set<Integer>>(size);
        ItemClusterMap = new ArrayList<Integer>(numItems);
        for (int j=0;j<numItems;j++) {
            int a= -1;
            ItemClusterMap.add(a);
        }
        for(int i =0;i<size;i++)
        {
            Set<Integer> s= new HashSet<Integer>();
            cluster.add(s);
        }
    }
    
    public static void printClusters() {
        for(int i =0;i<cluster.size();i++)
        {
            System.out.println("Cluster "+ i+":");
            for(int j : cluster.get(i))
            {
                System.out.print(j+", ");
            }
            System.out.println();
        }
    }

}