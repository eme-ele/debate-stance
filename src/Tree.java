import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * @author pachecog@purdue.edu
 * @version 3/27/2016
 */

public class Tree {

    public String topic;
    public String debate;
    public HashMap<String, Instance> nodes;
    public HashMap<String, List<String> > adjacencyList;

    public Tree(String topic, String debate) {
        this.topic = topic;
        this.debate = debate;
        nodes = new HashMap<>();
        adjacencyList = new HashMap<>();
    }

    public void addEdge(String headId, String tailId) {
        if (adjacencyList.containsKey(headId)) {
            adjacencyList.get(headId).add(tailId);
        } else {
            adjacencyList.put(headId, new ArrayList<>());
        }
    }

    public void addNode(Instance sample) {
        Opinion o = (Opinion) sample;
        String sampleId = o.debate + o.id;
        if (!nodes.containsKey(sampleId)) {
            this.nodes.put(sampleId, sample);
        }
        String parentId = o.debate + o.pid;
        addEdge(parentId, sampleId);
    }

}
