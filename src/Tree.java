import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.ArrayDeque;

/**
 * @author pachecog@purdue.edu
 * @version 3/27/2016
 */

public class Tree {

    public String topic;
    public String debate;
    public HashMap<String, Instance> roots;
    public HashMap<String, Instance> nodes;
    public HashMap<String, List<String> > adjacencyList;

    public Tree(String topic, String debate) {
        this.topic = topic;
        this.debate = debate;
        nodes = new HashMap<>();
        adjacencyList = new HashMap<>();
    }

     /**
     * Adds an edge to the adjacencyList
     * @param headId id of the head
     * @param tailId id of the tail
     * * */
    public void addEdge(String headId, String tailId) {
        if (adjacencyList.containsKey(headId)) {
            adjacencyList.get(headId).add(tailId);
        } else {
            adjacencyList.put(headId, new ArrayList<>());
        }
    }

    /**
     * Add a node to the tree.
     * It calls addEdge to add the edge to its parent
     * @param sample instance to be added
     * */
    public void addNode(Instance sample) {
        Opinion o = (Opinion) sample;
        String sampleId = o.debate + o.id;
        if (!nodes.containsKey(sampleId)) {
            nodes.put(sampleId, sample);
        }

        if (o.pid == -1 && !roots.containsKey(sampleId)) {
            roots.put(sampleId, sample);
        } else {
            String parentId = o.debate + o.pid;
            addEdge(parentId, sampleId);
        }
    }

    /**
     * Runs BFS to get the list of connected subtrees
     * @return a list of connected subtrees
     * */
    public List<Tree> getSubtrees() {
        ArrayList<Tree> subTrees = new ArrayList<>();
        for (String root: roots.keySet()) {
            Tree st = new Tree(this.topic, this.debate);

            HashSet<String> visited = new HashSet<>();
            ArrayDeque<String> queue = new ArrayDeque<>();
            queue.add(root);

            while (queue.size() > 0) {
                String currentNode = queue.poll();
                // add to subtree
                st.addNode(nodes.get(currentNode));
                for (String child: adjacencyList.get(currentNode)) {
                    if (!visited.contains(child)) {
                        queue.add(child);
                    }
                }
            }

            subTrees.add(st);
        }

        return subTrees;
    }


}
