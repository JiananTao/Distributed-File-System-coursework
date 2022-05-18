import java.io.File;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Settings {
  private static Settings instance;
  private List<FileIndex> index = new ArrayList<>();
  private String indexState = new String();
  private List<Integer> dstoreJoined = new ArrayList<Integer>();
  private HashMap<Integer,Socket> clientDstores = new HashMap<Integer,Socket>();
  private HashMap<Integer,Boolean> clientReload = new HashMap<Integer,Boolean>();
  //Dstore ServerPorts, Dstore ClientPorts
  private HashMap<Integer,Integer> pap = new HashMap<Integer,Integer>();

  private Settings() {}


  /**
   *
   * @return
   */
  //Static method for getting the settings instance
  public static Settings getInstance() {
    return instance == null ? instance = new Settings() : instance;
  }

  public void setIndexState(String s){ this.indexState = s;}
  public void safeSetIndexState(String old,String now){
    if (this.indexState.equals(old)){
      this.indexState = now;
    }else{
      System.out.println("current index error\n" + "Excepted: " + old + "\nActual: "+indexState);
    }
  }
  public String getIndexState(){return this.indexState;}
  /**
   *
   * @return
   */
  public List<FileIndex> getIndex() {return index;}

  public void newIndex(String state,int count,String name,List<Integer> ports,int size) {
    FileIndex fileIndex = new FileIndex(count,name,ports,size);
    this.indexState = state;
    index.add(fileIndex);
  }

  public void rmIndex(String name){
    for (int i = 0; i < index.size();i++){
      if (index.get(i).name.equals(name)){
        index.remove(i);
      }
    }
  }

  public void countIndex(String name, int j){
    int c = 0;
    for (int i = 0; i < index.size();i++){
      if (index.get(i).name.equals(name)){
        c = i;
      }
    }
    FileIndex fileIndex = new FileIndex(index.get(c).count + j,name,index.get(c).ports,index.get(c).size);
    index.add(fileIndex);
    index.remove(c);
  }
  public void rmPortIndex(String name, int port){
    int c = 0;
    for (int i = 0; i < index.size();i++){
      if (index.get(i).name.equals(name)){
        c = i;
      }
    }
    int k = 0;
    List<Integer> l = index.get(c).ports;
    for (int i = 0; i < l.size();i++){
      if (l.get(i) == port){
        k = i;
      }
    }
    l.remove(k);
    FileIndex fileIndex = new FileIndex(index.get(c).count,name,l,index.get(c).size);
    index.add(fileIndex);
  }
  public FileIndex findIndex(String name) {
    for (int i = 0; i < index.size(); i++) {
      if (index.get(i).name.equals(name)) {
        return index.get(i);
      }
    }
    return null;
  }

  //最多为1
  public List<FileIndex> checkIndexs(String name){
    List<FileIndex> fs = new ArrayList<FileIndex>();
    for (int i = 0; i < index.size();i++){
      if (index.get(i).name.equals(name)){
        fs.add(index.get(i));
      }
    }
    if (fs.size() > 1){
      System.out.println("index存在重复文件");
    }
    return fs;
  }
/*
  public void stateIndex(String old,String now,String name){
    for (int i = 0; i < index.size();i++){
      if (index.get(i).name.equals(name) && indexState.equals(old)){
        FileIndex fileIndex = new FileIndex(index.get(i).count,name,index.get(i).ports,index.get(i).size);
        index.add(fileIndex);
        index.remove(i);
      }
    }
  }

 */
  /**
   * Dstore
   * Server ports and client ports
   * @return
   */
  public int getPAP(int s){
    return pap.get(s);
  }
  public void addPAP(int s,int c){
    pap.put(s,c);
  }

  /**
   * Server ports store
   * @return
   */
  public List<Integer> getDstoreJoined() {return dstoreJoined;}
  public void addDstoreJoined(int i) {dstoreJoined.add(i);}

  /**
   * Server ports and client store
   * @return
   */
  public void addClientDstores(int port,Socket client){
    this.clientDstores.remove(port);
    this.clientDstores.put(port,client);
  }
  public Socket getClientDstores(int port){
    return clientDstores.get(port);
  }
  public int countClientDstores(){
    return clientDstores.size();
  }

  /**
   *
   * @return
   */
  public void setClientReload(int port,boolean f){
    this.clientReload.remove(port);
    this.clientReload.put(port,f);
  }
  public Boolean getClientReload(int port){return clientReload.get(port);}


}
