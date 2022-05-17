import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;

public class Controller {
  private static int cport;
  private static int r;
  private static int timeout;
  private static int rebalance_period;
  private static ServerSocket server;
  //private static HashMap<Socket,Integer> clients;


  public Controller(int cport, int r, int timeout, int rebalance_period) {
    this.cport = cport;
    this.r = r;
    this.timeout = timeout;
    this.rebalance_period = rebalance_period;
  }
  public static void main(String[] args) {
    //读取4个初始参数，并启动服务器
    try {
      cport = Integer.parseInt(args[0]);
      r = Integer.parseInt(args[1]);
      timeout = Integer.parseInt(args[2]);
      rebalance_period = Integer.parseInt(args[3]);
    }catch (Exception e){
      System.out.println("输入格式错误，请确保4个int");
      System.exit(-1);
    }
    Controller controller = null;
    try{
      controller = new Controller(cport,r,timeout,rebalance_period);
      try {
        server = new ServerSocket(cport);
        handleRequest(server);
      } catch (IOException e) {
        System.out.println("server" + e.getMessage());
        e.printStackTrace();
      }
    }catch (Exception e){
      System.out.println("controller错误");
      System.exit(-2);
    }
  }
  private static void handleRequest(ServerSocket server) {
    new Thread(() -> {
      while (true) {
        try {
          Socket client = server.accept();
          //client.setSoTimeout(timeout);不能加！！！
          //clients.put(client,client.getLocalPort());
          Thread t = new Thread(() -> handle(client));
          t.start();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }).start();
  }
  public static void handle(Socket client) {
    try {
      PrintWriter pw = new PrintWriter(client.getOutputStream(), true);
      BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));

      String line;
      while ((line = br.readLine()) != null) {
        System.out.println("Received" + line);
        String[] line1 = line.split("\\s+");
        if (line.contains(Protocol.JOIN_TOKEN)) {
          Settings.getInstance().addDstoreJoined(Integer.parseInt(line1[1]));
        } else if (Settings.getInstance().getDstoreJoined().size() < r) {
          pw.println(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
        } else {


          if (line.contains(Protocol.LIST_TOKEN)) {
            list(pw, br);
          } else if (line.contains(Protocol.STORE_ACK_TOKEN)) {
            Settings.getInstance().countIndex(line1[1]);
            if (Settings.getInstance().findIndex(line1[1]).count == 0) {
              Settings.getInstance().stateIndex("store in progress", "store complete", line1[1]);
            }
          } else if (line.contains(Protocol.STORE_TOKEN)) {
            store(client,line1);
          } else {
            pw.println("未知协议");
          }
        }
      }
    } catch (Exception e) {
      System.out.println("客户端" + client.getRemoteSocketAddress() + "下线了。");
    }
  }


  private static void list(PrintWriter pw, BufferedReader br) {
    pw.println(Protocol.LIST_TOKEN);
  }

  private static void store(Socket client,String[] line1) throws IOException, InterruptedException {
    //STORE filename filesize
    Boolean exist = false;
    int files = Settings.getInstance().getIndex().size();
    for (int i = 0; i < files; i++) {
      if (Settings.getInstance().getIndex().get(i).name.equals(line1[1])) {
        exist = true;
      }
    }
    if (exist) {
      send(client,Protocol.ERROR_FILE_ALREADY_EXISTS_TOKEN);
    } else {
      List<Integer> l = Settings.getInstance().getDstoreJoined();
      FileIndex newFile = new FileIndex("store in progress", 2, line1[1], l, Integer.valueOf(line1[2]));
      Settings.getInstance().getIndex().add(newFile);
      String allPorts = " ";
      for (int i = 0; i < l.size(); i++) {
        allPorts += l.get(i) + " ";
      }
      send(client,Protocol.STORE_TO_TOKEN + allPorts);
      while(!Settings.getInstance().findIndex(line1[1]).state.equals("store complete")) {

      }
      System.out.println(Settings.getInstance().findIndex(line1[1]).name);
      send(client, Protocol.STORE_COMPLETE_TOKEN);
    }



  }

  public static void send(Socket socket, String message) throws IOException {
    PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
    pw.println(message);
  }


}
