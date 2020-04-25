import java.net.*;
import java.util.*;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;



public class Contentserver{
  private static String config = "node.conf";
  private static String uuid_string;
  private static UUID uuid;
  private static String name;
  private static int backend_port;
  private static int peer_count;
  private static String[] peers;
  private static Server server;
  private static ExecutorService executor = Executors.newFixedThreadPool(10);

  public static void main(String args[]) throws Exception{
    if(args.length == 2){
      if(args[0].equals("-c")){
        config = args[1];
      }
    }
    File f = new File(config);
    Scanner scanner = new Scanner(f);
    int count = 0;
    int connections = 0;
    while(scanner.hasNextLine()){
      String line = scanner.nextLine();
      String[] values = line.split("=");
      values[1] = values[1].substring(1,values[1].length());
      if(count == 0){
        if(values[0].equals("uuid ")){
          uuid_string = values[1];
          uuid = UUID.fromString(values[1]);
        }
        else{
          uuid = UUID.randomUUID();
          uuid_string = uuid.toString();
          count++;
          RandomAccessFile rf = new RandomAccessFile(f, "rw");
          byte[] content = new byte[(int)rf.length()];
          rf.read(content);
          String add = "uuid = " + uuid_string + "\n";
          byte[] add_content = add.getBytes();
          byte[] new_content = new byte[(int)rf.length() + add_content.length];
          System.arraycopy(add_content, 0, new_content, 0, add_content.length);
          System.arraycopy(content, 0, new_content, add_content.length, content.length);
          String write = new String(new_content);
          rf.setLength(0);
          rf.write(new_content);
        }

      }
      if(count == 1){
        name = values[1];
      }
      else if(count == 2){
        backend_port = Integer.parseInt(values[1]);
      }
      else if(count == 3){
        peer_count = Integer.parseInt(values[1]);
        peers = new String[peer_count];
        connections = peer_count;
      }
      else if(count > 3){
        if(count > (3 + peer_count)){
          break;
        }
        peers[count - 4] = values[1];
      }
      count++;
    }
    /* Console print for reading config file inputs
    System.out.println("Config file is:" + config);
    System.out.println("uiud is:" + uuid_string);
    System.out.println("Name is:" + name);
    System.out.println("backend_port is:" + backend_port);
    System.out.println("peer_count is:" + peer_count);
    */

    server = new Server();
    executor.submit(() -> {
      try{
        server.start(18346);
      }
      catch(Exception e){}
      }
    );

    InetAddress add = InetAddress.getByName("localhost");                                     
    DatagramSocket dsock = new DatagramSocket( );
    String message1 = "new:" + name + ":" + uuid_string + ":" + peer_count;  //new:name:uuid:peer_count:peeruuid,{distance
    for(int i = 0; i < peer_count; i++){
      message1 += ":" + peers[i];
    }
    byte arr[] = message1.getBytes( );  
    DatagramPacket dpack = new DatagramPacket(arr, arr.length, add, 18346);
    dsock.send(dpack);                                   // send the packet                         // note the time of sending the message
    dsock.receive(dpack);                                // receive the packet
    String message2 = new String(dpack.getData( ));
    Timer t = new Timer();
    TimerTask tt = new TimerTask(){
      @Override
      public void run(){
        try{
          InetAddress add = InetAddress.getByName("localhost");                                     
          DatagramSocket dsock = new DatagramSocket( );
          String message1 = "active:" + uuid_string;
          byte arr[] = message1.getBytes( );  
          DatagramPacket dpack = new DatagramPacket(arr, arr.length, add, 18346);
          dsock.send(dpack);                    
          dsock.receive(dpack);
        }
        catch(IOException e){
          e.printStackTrace();
        }
      };
    };
    t.scheduleAtFixedRate(tt,0,10000);
    while(true){
      Scanner input_scanner = new Scanner(System.in);
      String inputString = input_scanner.nextLine();
      if(inputString.equals("uuid")){
        System.out.println("uuid:" + uuid_string);
      }
      else if(inputString.contains("addneighbor")){
        String[] parts = inputString.split("=");
        String temp_uuid = parts[1].substring(0,parts[1].length()-4);
        String temp_distance = parts[4];
        message1 = "add:" + uuid_string + ":" + temp_uuid + ":" + temp_distance;
        arr = message1.getBytes();
        dpack.setData(arr);
        dsock.send(dpack);
        dsock.receive(dpack);
      }
      else if(inputString.contains("neighbors")){
        message1 = "neighbors:" + uuid_string;
        arr = message1.getBytes();
        dpack.setData(arr);
        dsock.send(dpack);
        byte[] arr1 = new byte[250];
        DatagramPacket recieve_dpack = new DatagramPacket(arr1, arr1.length ); 
        dsock.receive(recieve_dpack);
        message2 = new String(recieve_dpack.getData());
        String[] params = message2.split(":");
        System.out.println("neighbors content server:" + message2);
      }
      else if(inputString.contains("map")){
        System.out.println("map");
        message1 = "map";
        arr = message1.getBytes();
        dpack.setData(arr);
        dsock.send(dpack);
        System.out.println("waiting");
        byte[] arr1 = new byte[150];
        DatagramPacket recieve_dpack = new DatagramPacket(arr1, arr1.length ); 
        dsock.receive(recieve_dpack);
        message2 = new String(recieve_dpack.getData());
        System.out.println("server map:" + message2);
      }
      else if(inputString.contains("kill")){
        message1 = "kill";
        arr = message1.getBytes();
        dpack.setData(arr);
        dsock.send(dpack);
        dsock.receive(dpack);
        System.exit(-1);
      }
    }
  }
}