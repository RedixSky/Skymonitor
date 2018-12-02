package com.skymonitor;

public class NodeInfo {
       private String mNodePublicKey = null;
       private int mStatus = OFFLINE;
       private static int OFFLINE = 0;
       private static int ONLINE = 1;
       private static int INVALID = 2;       
       
       public void setNodePublicKey(String key){
    	   mNodePublicKey = key;
       }
       
       public String getNodePublicKey(){
    	   return mNodePublicKey;
       }
       
       public void setNodeStatus(int status){
    	   mStatus = status;
       }
       
       public int getNodeStatus(){
    	   return mStatus;
       }
       
       public NodeInfo(String key, int status){
         mNodePublicKey = key;
         mStatus = status;
       }
}
