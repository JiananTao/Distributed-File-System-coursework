import java.util.List;

public class FileIndex {
  int count;
  String name;
  List<Integer> ports;
  int size;

  public FileIndex(int count,String name, List<Integer> ports, int size) {
    this.count = count;
    this.name = name;
    this.ports = ports;
    this.size = size;
  }

}
