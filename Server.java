import java.net.*; 
import java.io.*;
import java.util.Scanner;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Arrays;

public class Server implements Runnable{ 
    private static String page404 = "<html>\nNot Found\n</html>";
    private Socket clientSocket;
    public Server(Socket s){
        clientSocket = s;
    }
    public static void main(String[] args) throws IOException {
        int port;
        if(args.length >= 1){
            port = Integer.parseInt(args[0]);
        }
        else{
            port = 10007;
        }
        System.out.println(port);
        ServerSocket serverSocket = new ServerSocket(10007);
        while(true){
            //Code for spawning new threads to deal with concurrent clients
            Server server = new Server(serverSocket.accept());
            Thread t = new Thread(server);
            t.start();
            
        }
    }
    @Override //Code that the threads run
    public void run(){
        try{
            BufferedReader in = new BufferedReader(new 
                InputStreamReader( clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true); 
            BufferedOutputStream dOut = new BufferedOutputStream(clientSocket.getOutputStream());
            String inputLine = in.readLine();
            String inputLine2 = "test";
            String range = "";
            int i = 0;
            //Used to find the range line
            while(inputLine2 != null && inputLine2 != "" && i < 8){
                i++;
                if(inputLine2.contains("Range")){
                    range = inputLine2;
                    break;
                }
                else if(inputLine2.contains("Cache-Control")){
                    break;
                }
                inputLine2 = in.readLine();
            }
            
            System.out.println(inputLine);
            String[] words = inputLine.split(" ");
            String type = words[0];
            String fName = words[1].substring(1,words[1].length());
            SimpleDateFormat sdf = new SimpleDateFormat("EEEEE, dd MMM yyyy HH:mm:ss z");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            if(!type.equals("GET")){ //If not a get request
                System.out.println("Not implemented");
                print(page404.length(), "text/html", 
                    page404.getBytes(), "HTTP/1.1 404 Not Found",
                    in, out, dOut, sdf.format(new Date()));
            }
            else if(fName.contains("favicon")){

            }
            else if(range.contains("Range")){ //Used if range is implemented
                File f = new File(fName);
                String[] bounds = range.split("=")[1].split("-");
                int start = Integer.parseInt(bounds[0]);
                String content = content(fName);
                int end;
                if(bounds.length > 1){
                    end = Integer.parseInt(bounds[1]);
                }
                else{
                    end = (int)f.length();
                }
                int length = end - start;
                FileInputStream fIS = new FileInputStream(f);
                byte[] data = new byte[fIS.available()];
                fIS.read(data);
                if(fIS != null){
                    fIS.close();
                }
                data = Arrays.copyOfRange(data, start, end);
                print(length, content, data, "HTTP/1.1 206 Partial Content", 
                    in, out, dOut, sdf.format(f.lastModified()));
            }
            else{
                try{//Used if file exists and no range
                    File f = new File(fName);
                    System.out.println("file found!");
                    int length = (int)f.length();
                    String content = content(fName);
                    byte[] data = null;
                    data = read(f, length);
                    print(length, content, data, "HTTP/1.1 200 OK", 
                        in, out, dOut, sdf.format(f.lastModified()));
                    in.close();
                    out.close();
                    dOut.close();
                    clientSocket.close();
                }
                catch(FileNotFoundException e){ //if file does not exist
                    System.out.println("File not found");
                    print(page404.length(), "text/html", 
                        page404.getBytes(), "HTTP/1.1 404 Not Found",
                        in, out, dOut, sdf.format(new Date()));
                    in.close();
                    out.close();
                    dOut.close();
                    clientSocket.close();
                }
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
        catch(NullPointerException e){
            e.printStackTrace();
        }
    }
    private static void print(int length, String content, byte[] data, String status, 
        BufferedReader in, PrintWriter out, BufferedOutputStream dOut, 
        String date) throws IOException{ //Function used to send correct information
        SimpleDateFormat sdf = new SimpleDateFormat("EEEEE, dd MMM yyyy HH:mm:ss z");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        out.println(status);
        out.println("Connection: Keep-Alive");
        out.println("Date: " + sdf.format(new Date()));
        out.println("Content-Length: " + length);
        out.println("Content-Type: " + content);
        out.println("Accept-Ranges: bytes");
        out.println("Last-Modified" +  date);//sdf.format(f.lastModified()));
        out.println();
        out.flush();
        dOut.write(data, 0, length);
        dOut.flush();
    }

    private static String content(String name){ //Returns correct content type
        if(name.endsWith(".txt")){
            return "text/plain";
        }
        else if(name.endsWith(".css")){
            return "text/css";
        }
        else if(name.endsWith(".html") || name.endsWith(".htm")){
            return "text/html";
        }
        else if(name.endsWith(".gif")){
            return "image/gif";
        }
        else if(name.endsWith("jpg") || name.endsWith("jpeg")){
            return "image/jpeg";
        }
        else if(name.endsWith(".png")){
            return "image/png";
        }
        else if(name.endsWith(".js")){
            return "application/javascript";
        }
        else if(name.endsWith(".mp4")){
            return "video/mp4";
        }
        else if(name.endsWith(".webm") || name.endsWith(".ogg")){
            return "video/webm";
        }
        else{
            return "application/octet-stream";
        }
    }

    private static byte[] read(File f, int length) throws IOException{ //Used to read file data
        FileInputStream fIS = new FileInputStream(f);
        byte[] data = new byte[length];
        fIS.read(data);
        if(fIS != null){
            fIS.close();
        }
        return data;
    }
} 
