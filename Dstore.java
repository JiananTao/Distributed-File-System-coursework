import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Dstore {
  private static int port;
  private static int cport;
  private static int timeout;
  private static File file_folder;
  private static Socket controllerSocket;
  private static ServerSocket dServer;

  public Dstore(int port, int cport, int timeout, File file_folder) {
    this.port = port;
    this.cport = cport;
    this.timeout = timeout;
    this.file_folder = file_folder;
  }
  public static void main(String[] args) {
    //读取4个初始参数，并启动服务器
    try {
      port = Integer.parseInt(args[0]);
      cport = Integer.parseInt(args[1]);
      timeout = Integer.parseInt(args[2]);
      file_folder = new File(args[3]);
    }catch (Exception e){
      System.out.println("DStore输入格式错误，请确保3个int1个path");
      System.exit(-1);
    }

    delFile(file_folder);
    if (!file_folder.exists())
      if (!file_folder.mkdir())
        throw new RuntimeException("Cannot create download folder (folder absolute path: " + file_folder.getAbsolutePath() + ")");

    Dstore dstore = new Dstore(port, cport, timeout, file_folder);
    try{
      dServer = new ServerSocket(port);
      controllerSocket = new Socket(InetAddress.getLoopbackAddress(), cport);
      handleClient();
      handleController();
    }catch (IOException e) {
      System.err.println("Can not create dServer: " + e.getMessage());
      System.exit(-1);
    }
  }
  private static void handleClient() {
    new Thread(() -> {
      while (true) {
        try {
          Socket client = dServer.accept();
          client.setSoTimeout(timeout);
          Thread t = new Thread(() -> handle(client));
          t.start();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }).start();
  }
  private static void handleController(){
    new Thread(() -> {
      try {
        PrintWriter pw = new PrintWriter(controllerSocket.getOutputStream(), true);
        BufferedReader br = new BufferedReader(new InputStreamReader(controllerSocket.getInputStream()));
        pw.println(Protocol.JOIN_TOKEN + " " + port);
        String line;
        while ((line = br.readLine()) != null) {
        /*
        if (line.contains(Protocol.STORE_TOKEN)) {
          pw.println(Protocol.ACK_TOKEN);
        } else {
          pw.println("未知协议");
        }

         */
        }
      } catch (Exception e) {
        System.out.println("Dstore服务端" + controllerSocket.getRemoteSocketAddress() + "下线了。");
      }
    }).start();
  }

  public void send(Socket socket, String message) throws IOException {
    PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
    pw.println(message);
  }
  public static void handle(Socket client){
    try {
      OutputStream osC = controllerSocket.getOutputStream();
      PrintWriter pwController = new PrintWriter(osC,true);

      OutputStream os = client.getOutputStream();//os.write
      PrintWriter pwClient = new PrintWriter(os, true);
      InputStream is = client.getInputStream();
      BufferedReader brClient = new BufferedReader(new InputStreamReader(is));
      String line;
      while ((line = brClient.readLine()) != null) {
        String[] line1 = line.split("\\s+");
        if (line.contains(Protocol.STORE_TOKEN)){
          pwClient.println(Protocol.ACK_TOKEN);
          System.out.println(client.getRemoteSocketAddress() + "开始文件传输");
          byte[] bytes = is.readNBytes(Integer.parseInt(line1[2]));
          File file = new File(file_folder, line1[1]);
          try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            fileOutputStream.write(bytes);
          } catch (FileNotFoundException e) {
            System.out.println("error write file: " + e.getMessage());
            e.printStackTrace();
            return;
          }
          System.out.println(client.getRemoteSocketAddress() + "完成文件传输");
          pwController.println(Protocol.STORE_ACK_TOKEN + " " + line1[1]);
        }
      }
    } catch (Exception e) {
      System.out.println("客户端" + client.getRemoteSocketAddress() + "下线了。");
    }
  }
  static boolean delFile(File file) {
    if (!file.exists()) {
      return false;
    }

    if (file.isDirectory()) {
      File[] files = file.listFiles();
      for (File f : files) {
        delFile(f);
      }
    }
    return file.delete();
  }
}
