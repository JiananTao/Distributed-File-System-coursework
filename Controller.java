import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class Controller {
  private static int cport;
  private static int r;
  private static int timeout;
  private static int rebalance_period;



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
      try (ServerSocket server = new ServerSocket(cport)) {
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

  private static void handleRequest(ServerSocket server) throws IOException {
    while (true) {
      //3.开始等待接受客户端的Socket管道连接
      Socket client = server.accept();
      new ServerReaderThread(client).start();
    }
  }
  static class ServerReaderThread extends Thread{
    private Socket client;


    public ServerReaderThread(Socket client) throws IOException {
      this.client = client;
    }

    @Override
    public void run() {
      try {
        PrintWriter pw = new PrintWriter(client.getOutputStream(),true);
        BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));

        String line;
        while ((line = br.readLine())!= null){
          String [] line1 = line.split("\\s+");
          if (line.contains(Protocol.JOIN_TOKEN)) {
            Settings.getInstance().addDstoreJoined(Integer.parseInt(line1[1]));
            test();
          }else if (Settings.getInstance().getDstoreJoined().size() < r){
            pw.println(Protocol.ERROR_NOT_ENOUGH_DSTORES_TOKEN);
          }else{
            if (line.contains(Protocol.LIST_TOKEN)) {
              list(pw, br);
            } else if (line.contains(Protocol.STORE_TOKEN)) {
              //STORE filename filesize
              Boolean exist = false;
              int files = Settings.getInstance().getIndex().size();
              for (int i = 0; i < files;i++){
                if (Settings.getInstance().getIndex().get(i).name.equals(line1[1])){
                  exist = true;
                }
              }
              if (exist){
                pw.println(Protocol.ERROR_FILE_ALREADY_EXISTS_TOKEN);
              }else {
                List<Integer> l = Settings.getInstance().getDstoreJoined();
                FileIndex newFile = new FileIndex("store in progress",2,line1[1],l,Integer.valueOf(line1[2]));
                Settings.getInstance().getIndex().add(newFile);
                String allPorts = " ";
                for (int i = 0; i < l.size();i++){
                  allPorts += l.get(i) + " ";
                }
                pw.println(Protocol.STORE_TO_TOKEN + allPorts);
              }
            } else if (line.contains(Protocol.STORE_ACK_TOKEN)){
              System.out.println("收到STORE_ACK");
              Settings.getInstance().countIndex(line1[1]);
              if (Settings.getInstance().findIndex(line1[1]).count == 0){
                Settings.getInstance().stateIndex("store in progress","store complete",line1[1]);
              }
              pw.println(Protocol.STORE_COMPLETE_TOKEN);
            }else {
              pw.println("未知协议");
            }
          }
        }
      }catch (Exception e){
        System.out.println("客户端"+client.getRemoteSocketAddress()+"下线了。");
      }
    }

    private void list(PrintWriter pw, BufferedReader br) {
      pw.println(Protocol.LIST_TOKEN);
    }

    private void test(){
      for (int i = 0; i < Settings.getInstance().getDstoreJoined().size();i++){
        System.out.println("已添加的DStore Port:" + Settings.getInstance().getDstoreJoined().get(i));
      }
    }
  }


}
