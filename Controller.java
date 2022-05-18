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
        System.out.println("Received: " + line);
        String[] line1 = line.split("\\s+");
        if (line1[0].contains(Protocol.JOIN_TOKEN)) {
          Settings.getInstance().addClientDstores(client.getPort(), client);
          Settings.getInstance().addDstoreJoined(Integer.parseInt(line1[1]));
          Settings.getInstance().addPAP( Integer.parseInt(line1[1]),client.getPort());
          System.out.println("new Dstore added: " + client.getLocalPort() + Integer.parseInt(line1[1]));
        } else if (Settings.getInstance().countClientDstores() < r) {
          pw.println(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
        } else {
          boolean flag = ((Settings.getInstance().getIndexState().equals("store in progress")) || (Settings.getInstance().getIndexState().equals("remove in progress")));

          if (line1[0].contains(Protocol.LIST_TOKEN)) {
            if (flag){
              send(client,Protocol.LIST_TOKEN);
            }else {
              list(client, line1);
            }
          } else if (line1[0].contains(Protocol.REMOVE_ACK_TOKEN)){
              Settings.getInstance().countIndex(line1[1],1);
            if (Settings.getInstance().findIndex(line1[1]).count == Settings.getInstance().findIndex(line1[1]).ports.size()) {
              System.out.println(Settings.getInstance().getIndexState());
              if (Settings.getInstance().getIndexState().equals("remove in progress")){
                Settings.getInstance().safeSetIndexState("remove in progress", "remove complete");
                Settings.getInstance().rmIndex(line1[1]);
              }else {
                System.out.println("index错误");
              }
            }
          }else if (line1[0].contains(Protocol.REMOVE_TOKEN)){
            if (flag){
              send(client,Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN);
            }else {
              remove(client, line1);
            }
          }else if (line1[0].contains(Protocol.RELOAD_TOKEN)){
            if (flag){
              send(client,Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN);
            }else {
              reLoad(client, line1);
            }
          }else if (line1[0].contains(Protocol.LOAD_TOKEN)){
            if (flag){
              send(client,Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN);
            }else {
              load(client, line1);
            }
          } else if (line1[0].contains(Protocol.STORE_ACK_TOKEN)) {
            Settings.getInstance().countIndex(line1[1],-1);
            if (Settings.getInstance().findIndex(line1[1]).count == 0) {
              Settings.getInstance().safeSetIndexState("store in progress", "store complete");
            }
          } else if (line1[0].contains(Protocol.STORE_TOKEN)) {
            if (flag){
              send(client,Protocol.ERROR_FILE_ALREADY_EXISTS_TOKEN);
            }else {
              store(client, line1);
            }
          } else {
            send(client,"Unknown Message: " + line);
          }
        }
      }
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }


  private static void list(Socket client,String[] line1) throws IOException {
    String file_list = "";
    for (int i = 0;i <Settings.getInstance().getIndex().size();i++){
      file_list += " " + Settings.getInstance().getIndex().get(i).name;
    }
    send(client,Protocol.LIST_TOKEN + file_list);
  }
  private static void remove(Socket client,String[] line1) throws IOException, InterruptedException {
    if (Settings.getInstance().findIndex(line1[1]) == null) {
      send(client, Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN);
    }else{
      //send(client,Protocol.REMOVE_TOKEN + " " + line1[1]);
      Settings.getInstance().setIndexState("remove in progress");
      FileIndex f = Settings.getInstance().findIndex(line1[1]);
      for (int p : f.ports){
        System.out.println(Settings.getInstance().getClientDstores(Settings.getInstance().getPAP(p)));
        send(Settings.getInstance().getClientDstores(Settings.getInstance().getPAP(p)), Protocol.REMOVE_TOKEN + " " + line1[1]);
      }
      while(!Settings.getInstance().getIndexState().equals("remove complete")) {
        Thread.sleep(50);
      }
      send(client, Protocol.REMOVE_COMPLETE_TOKEN);
    }
  }
  //TODO:未测试
  private static void reLoad(Socket client,String[] line1) throws IOException {
    //LOAD filename
    if (Settings.getInstance().findIndex(line1[1]).equals(null)){
      send(client,Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN);
    }else{
      FileIndex f = Settings.getInstance().findIndex(line1[1]);
      Settings.getInstance().setClientReload(client.getPort(),false);
      for (int p : f.ports){
        send(client, Protocol.LOAD_FROM_TOKEN + " " + p + " " + f.size);
      }
      if (!Settings.getInstance().getClientReload(client.getPort())){
        send(client,Protocol.ERROR_LOAD_TOKEN);
      }
    }
  }

  private static void load(Socket client,String[] line1) throws IOException {
    //LOAD filename
    if (Settings.getInstance().findIndex(line1[1]).equals(null)){
      send(client,Protocol.ERROR_FILE_DOES_NOT_EXIST_TOKEN);
    }else{
      FileIndex f = Settings.getInstance().findIndex(line1[1]);
      send(client,Protocol.LOAD_FROM_TOKEN + " " + f.ports.get(0) + " " + f.size);
    }
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
      System.out.println(l);
      FileIndex f = new FileIndex(r, line1[1], l, Integer.valueOf(line1[2]));
      Settings.getInstance().getIndex().add(f);
      Settings.getInstance().setIndexState("store in progress");
      String allPorts = " ";
      for (int i = 0; i < l.size(); i++) {
        allPorts += l.get(i) + " ";
      }
      send(client,Protocol.STORE_TO_TOKEN + allPorts);
      while(!Settings.getInstance().getIndexState().equals("store complete")) {
        Thread.sleep(50);
      }
      send(client, Protocol.STORE_COMPLETE_TOKEN);
      System.out.println(Settings.getInstance().findIndex(line1[1]).name);
    }



  }

  public static void send(Socket socket, String message) throws IOException {
    PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
    pw.println(message);
  }


}
