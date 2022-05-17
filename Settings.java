import java.io.File;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Settings {
  private static Settings instance;
  private List<FileIndex> index = new ArrayList<>();
  private List<Integer> dstoreJoined = new ArrayList<Integer>();
  private HashMap<Integer,Socket> clientNow = new HashMap<Integer,Socket>();

  private Settings() {



  }


  /**
   *
   * @return
   */
  //Static method for getting the settings instance
  public static Settings getInstance() {
    return instance == null ? instance = new Settings() : instance;
  }

  /**
   *
   * @return
   */
  public List<FileIndex> getIndex() {return index;}

  public void addIndex(String state,int count,String name,List<Integer> ports,int size) {
    FileIndex fileIndex = new FileIndex(state,count,name,ports,size);
    index.add(fileIndex);
  }

  public void countIndex(String name){
    for (int i = 0; i < index.size();i++){
      if (index.get(i).name.equals(name)){
        FileIndex fileIndex = new FileIndex(index.get(i).state,index.get(i).count - 1,name,index.get(i).ports,index.get(i).size);
        index.add(fileIndex);
        index.remove(i);
      }
    }
  }

  public FileIndex findIndex(String name){
    for (int i = 0; i < index.size();i++){
      if (index.get(i).name.equals(name)){
        return index.get(i);
      }
    }
    return null;
  }

  public void stateIndex(String old,String now,String name){
    for (int i = 0; i < index.size();i++){
      if (index.get(i).name.equals(name) && index.get(i).state.equals(old)){
        FileIndex fileIndex = new FileIndex(now,index.get(i).count,name,index.get(i).ports,index.get(i).size);
        index.add(fileIndex);
        index.remove(i);
      }
    }
  }

  /**
   *
   * @return
   */
  public List<Integer> getDstoreJoined() {
    return dstoreJoined;
  }

  public void addDstoreJoined(int i) {
    dstoreJoined.add(i);
  }

  public void setClientNow(Socket client,int port){
    this.clientNow.put(port,client);
  }
  public Socket getClientNow(int port){
    return clientNow.get(port);
  }
}
