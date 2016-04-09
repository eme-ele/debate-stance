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
    public HashMap<String, String> parents;

    public Tree(String topic, String debate) {
        this.topic = topic;
        this.debate = debate;
        roots = new HashMap<>();
        nodes = new HashMap<>();
        adjacencyList = new HashMap<>();
        parents = new HashMap<>();
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
            List<String> adj = new ArrayList<>();
            adj.add(tailId);
            adjacencyList.put(headId, adj);
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

        // create node if it doesn't exist
        if (!nodes.containsKey(sampleId)) {
            nodes.put(sampleId, sample);
        }

        // add it to list of roots if its root
        if (o.pid == -1 && !roots.containsKey(sampleId)) {
            roots.put(sampleId, sample);
        } else {
            // if not, add edge to the parent
            String parentId = o.debate + o.pid;
            addEdge(parentId, sampleId);
            parents.put(sampleId, parentId);
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

            ArrayDeque<String> queue = new ArrayDeque<>();
            queue.add(root);

            while (queue.size() > 0) {
                String currentNode = queue.poll();
                // add to subtree
                st.addNode(nodes.get(currentNode));

                List<String> neighbours = adjacencyList.get(currentNode);
                // if it is not a leaf, check its children
                if (neighbours != null) {
                    for (String child: neighbours) {
                        queue.add(child);
                    }
                } else {
                    // add empty adjacency list for leaves
                    st.adjacencyList.put(currentNode, new ArrayList<>());
                }
            }

            subTrees.add(st);
        }

        return subTrees;
    }

    @Override
    public String toString() {
        String str = "roots: ";
        for (String rootId: roots.keySet()) {
            str += rootId + " ";
        }
        str = str.trim() + "\n";
        str += "adjacencies:\n";
        for (String node: adjacencyList.keySet()) {
            str += node + ": <";
            for (String adj: adjacencyList.get(node)) {
                str += adj + ", ";
            }
            str = str.trim() + ">\n";
        }
        return str;
    }

    public int getNumChildren(Instance node) {
        Opinion o = (Opinion) node;
        String sampleId = o.debate + o.id;
        return adjacencyList.get(sampleId).size();
    }

    public int getNodeDepth(Instance node) {
        int count = 0;
        Opinion o = (Opinion) node;
        String sampleId = o.debate + o.id;
        while (!roots.containsKey(sampleId)) {
            sampleId = parents.get(sampleId);
            count += 1;
        }
        return count;
    }

    public int getNumSibilings(Instance node) {
        Opinion o = (Opinion) node;
        String sampleId = o.debate + o.id;
        if (parents.containsKey(sampleId)) {
            int parentChildren =
                adjacencyList.get(parents.get(sampleId)).size();
            return parentChildren - 1;
        } else {
            return 0;
        }
    }

    public int getNumLeaves(Instance node) {
        int numLeaves = 0;
        Opinion o = (Opinion) node;
        String sampleId = o.debate + o.id;
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(sampleId);

        while (queue.size() > 0) {
            String currentNode = queue.poll();

            List<String> neighbours = adjacencyList.get(currentNode);
            if (neighbours != null) {
                for (String child: neighbours) {
                    queue.add(child);
                }

            } else {
                numLeaves++;
            }

        }
        return numLeaves;

    }

    public double getAvgDistanceLeaves(Instance node) {
        int numLeaves = 0;
        Opinion o = (Opinion) node;
        String sampleId = o.debate + o.id;
        ArrayDeque<String> queue = new ArrayDeque<>();
        HashMap<String, Integer> distances = new HashMap<>();
        queue.add(sampleId);
        distances.put(sampleId, 0);
        List<Integer> finalDistances = new ArrayList<>();

        while (queue.size() > 0) {
            String currentNode = queue.poll();

            List<String> neighbours = adjacencyList.get(currentNode);
            if (neighbours != null) {
                for (String child: neighbours) {
                    queue.add(child);
                    distances.put(child, distances.get(currentNode) + 1);
                }

            } else {
                finalDistances.add(distances.get(currentNode));
                numLeaves++;
            }

        }

        double avg = 0.0;
        for (Integer leaf: finalDistances) {
            avg += leaf;
        }

        if (numLeaves > 0)
            return avg/numLeaves;
        else
            return 0;

    }


}
