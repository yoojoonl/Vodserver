import java.net.*;
import java.util.*;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

public class Server{
  private static HashMap<String, String> names = new HashMap<String, String>();
  private static HashMap<String, String[]> neighbors = new HashMap<String, String[]>();
  private static HashMap<String, Boolean> refreshed = new HashMap<String, Boolean>();
  private static HashMap<String, Boolean> deactive = new HashMap<String, Boolean>();

  public static void main( String args[]) throws Exception {
  }
  public static void start(int port) throws Exception{
    DatagramSocket dsock;
    try{
      dsock = new DatagramSocket(port);
    }
    catch(SocketException e){
      return;
    }
    byte arr1[] = new byte[250];                                
    DatagramPacket dpack = new DatagramPacket(arr1, arr1.length );

    Timer t = new Timer();
    TimerTask tt = new TimerTask(){
      @Override
      public void run(){
        for(String key : refreshed.keySet()){
          Boolean value = refreshed.get(key);
          if(value == false){
            if(!deactive.containsKey(key)){
              deactive.put(key, true);
            }
          }
          else{
            if(deactive.containsKey(key)){
              deactive.remove(key);
            }
            refreshed.replace(key, false);
          }
        }
      };
    };
    t.scheduleAtFixedRate(tt,30000,30000);

    while(true){
      arr1 = new byte[250];
      dpack = new DatagramPacket(arr1, arr1.length ); 
      dsock.receive(dpack);
      byte arr2[] = dpack.getData();
      String temp_msg = new String(arr2);
      int packSize = dpack.getLength();
      String s2 = new String(arr2, 0, packSize);
      String[] params = s2.split(":");
      String command = params[0];
      if(command.equals("new")){
        String name = params[1];
        String uuid = params[2];
        int peer_count = Integer.parseInt(params[3]);
        String[] peers = new String[peer_count];
        for(int i = 0;i < peer_count; i++){
          String[] parts = params[i + 4].split(",");
          if(!refreshed.containsKey(parts[0])){
            refreshed.put(parts[0], false);
          }
          if(!deactive.containsKey(parts[0])){
            deactive.put(parts[0], true);
          }
          String part = parts[0] + ":" + parts[3];
          peers[i] = part;
        }
        names.put(uuid, name);
        neighbors.put(uuid, peers);
        refreshed.put(uuid,false);
        dsock.send(dpack);
      }
      else if(command.equals("add")){
        if(neighbors.containsKey(params[1])){
          String[] temp = neighbors.get(params[1]);
          String[] new_temp = new String[temp.length+1];
          for(int i = 0; i < temp.length; i++){
            new_temp[i] = temp[i];
          }
          new_temp[temp.length] = params[2] + ":" + params[3];
          neighbors.replace(params[1], new_temp);
          if(!refreshed.containsKey(params[2])){
            refreshed.put(params[2], false);
          }
          if(!deactive.containsKey(params[2])){
            deactive.put(params[2], true);
          }
        }
        dsock.send(dpack);
      }
      else if(command.equals("neighbors")){
        if(neighbors.containsKey(params[1])){
          String[] temp = neighbors.get(params[1]);
          String new_temp = "";
          for(int i = 0; i < temp.length; i++){
            String[] temp_params = temp[i].split(":");
            if(deactive.containsKey(temp_params[0])){
              continue;
            }
            if(names.containsKey(temp_params[0])){
              temp[i] = names.get(temp_params[0]) + ":" + temp_params[1];
            }
            new_temp += temp[i] + ",";
          }
          new_temp = new_temp.substring(0,new_temp.length()-1);
          byte[] arr = new_temp.getBytes();
          dpack.setData(arr);
          dsock.send(dpack);
        }
      }
      else if(command.equals("active")){
        if(refreshed.containsKey(params[1])){
          refreshed.replace(params[1], true);
        }
        else{
          refreshed.put(params[1], true);
        }
        if(deactive.containsKey(params[1])){
          deactive.remove(params[1]);
        }
        dsock.send(dpack);
      }
      else if(command.equals("map")){
        String add = "";
        for(String key : neighbors.keySet()){
          if(!deactive.containsKey(key)){
            String name = key;
            if(names.containsKey(key)){
              name = names.get(key);
            }
            add += name + ":{";
            String[] neighbor = neighbors.get(key);
            String ret = "";
            for(int i = 0; i < neighbor.length; i++){
              String[] temp_params = neighbor[i].split(":");
              //String[] parts = temp_params.split(":");
              String temp_name = temp_params[0];
              if(deactive.containsKey(temp_name)){
                continue;
              }
              if(names.containsKey(temp_params[0])){
                temp_name = names.get(temp_params[0]);
              }
              ret = temp_name + ":" + temp_params[1] + ",";
              add += ret;
            }
            if(add.charAt(add.length() - 1) == ','){
              add = add.substring(0,add.length()-1);
            }
            add += "},\n";
          }
        }
        if(add.charAt(add.length() - 1) == ','){
          add = add.substring(0,add.length()-1);
        }
        byte[] arr = add.getBytes();
        dpack.setData(arr);
        dsock.send(dpack);
      }
      else if(command.equals("kill")){
        System.exit(-1);
      }
    }
  }
}