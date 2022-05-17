import java.util.List;

public class FileIndex {
  String state;
  int count;
  String name;
  List<Integer> ports;
  int size;

  public FileIndex(String state,int count,String name, List<Integer> ports, int size) {
    this.state = state;
    this.count = count;
    this.name = name;
    this.ports = ports;
    this.size = size;
  }



}
