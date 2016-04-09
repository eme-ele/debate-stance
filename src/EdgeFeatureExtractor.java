import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

/**
 * @author pachecog@purdue.edu
 * @version 3/27/2016
 */

public class EdgeFeatureExtractor extends FeatureExtractor {

    public int numTrees;
    public int numAuthorLevelEdgeFeatures;
    public int numOpinionLevelEdgeFeatures;

    public EdgeFeatureExtractor(StanfordCoreNLP pipeline) {
        super(pipeline);
    }

    public void selectFeatures() {
        super.selectFeatures();
        numAuthorLevelEdgeFeatures = numFeatures * 8 + 10;
        numOpinionLevelEdgeFeatures = numFeatures * 2 + 10;
    }

    public HashMap<Integer, Float> getStructureVector(Tree tree, Instance head,
                                                     Instance tail, int offset) {
        HashMap<Integer, Float> structureFeats = new HashMap<>();
        structureFeats.put(offset, (float)tree.getNumChildren(head));
        structureFeats.put(1 + offset, (float)tree.getNumChildren(tail));
        structureFeats.put(2 + offset, (float)tree.getNodeDepth(head));
        structureFeats.put(3 + offset, (float)tree.getNodeDepth(tail));
        structureFeats.put(4 + offset, (float)tree.getNumSibilings(head));
        structureFeats.put(5 + offset, (float)tree.getNumSibilings(tail));

        // these last four things are not working
        structureFeats.put(6 + offset, (float)tree.getNumLeaves(head));
        structureFeats.put(7 + offset, (float)tree.getNumLeaves(tail));
        structureFeats.put(8 + offset, (float)tree.getAvgDistanceLeaves(head));
        structureFeats.put(9 + offset, (float)tree.getAvgDistanceLeaves(head));

        /*for (Integer key: structureFeats.keySet()) {
            System.out.printf(structureFeats.get(key) + " ");
        }
        System.out.println();*/

        return structureFeats;
    }

    public HashMap<Integer, Float> getAuthorLevelEdgeFeatures(Tree tree,
                                                              Instance head,
                                                              Instance tail,
                                                              List<Instance> headOpinions,
                                                              List<Instance> tailOpinions)
    {
        HashMap<Integer, Float> headFeats = getAuthorFeatures(headOpinions, 0);
        HashMap<Integer, Float> tailFeats = getAuthorFeatures(tailOpinions, numFeatures * 4);
        HashMap<Integer, Float> structureFeats = getStructureVector(tree, head, tail, numFeatures * 8);
        HashMap<Integer, Float> edgeFeats = new HashMap<>();
        edgeFeats.putAll(headFeats);
        edgeFeats.putAll(tailFeats);
        edgeFeats.putAll(structureFeats);
        return edgeFeats;
    }


    public HashMap<Integer, Float> getOpinionLevelEdgeFeatures(Tree tree, Instance head, Instance tail)
    {
        HashMap<Integer, Float> headFeats = getFeatureVector(head, 0);
        HashMap<Integer, Float> tailFeats = getFeatureVector(tail, numFeatures);
        HashMap<Integer, Float> structureFeats = getStructureVector(tree, head, tail, numFeatures * 2);
        HashMap<Integer, Float> edgeFeats = new HashMap<>();
        edgeFeats.putAll(headFeats);
        edgeFeats.putAll(tailFeats);
        edgeFeats.putAll(structureFeats);
        return edgeFeats;
    }

    public HashMap<Integer, Float> getAuthorFeatures(List<Instance> opinions, int offset) {
        HashMap<Integer, Float> feats = new HashMap<>();
        HashMap<Integer, Float> abortion = new HashMap<>();
        HashMap<Integer, Float> gayRights = new HashMap<>();
        HashMap<Integer, Float> marijuana = new HashMap<>();
        HashMap<Integer, Float> obama = new HashMap<>();
        for (Instance opinion: opinions) {
            String topic = ((Opinion)opinion).topic;
            switch (topic) {
                case "abortion":
                    abortion.putAll(getFeatureVector(opinion, offset + 0));
                    break;
                case "gayRights":
                    gayRights.putAll(getFeatureVector(opinion, offset + numFeatures));
                    break;
                case "marijuana":
                    marijuana.putAll(getFeatureVector(opinion, offset + numFeatures * 2));
                    break;
                case "obama":
                    obama.putAll(getFeatureVector(opinion, offset + numFeatures * 3));
                    break;
            }
        }
        feats.putAll(abortion);
        feats.putAll(gayRights);
        feats.putAll(marijuana);
        feats.putAll(obama);
        return feats;
    }

    /*public List<HashMap<Integer, Float> > getFeatureMatrix(Tree tree) {
        List<HashMap<Integer, Float> > feats = new ArrayList<>();
        for (String head: tree.adjacencyList.keySet()) {
            Instance headOpinion = tree.nodes.get(head);
            for (String tail: tree.adjacencyList.get(head)) {
                Instance tailOpinion = tree.nodes.get(tail);
                HashMap<Integer, Float> edgeFeats = getEdgeFeatures(tree,
                                                                    headOpinion,
                                                                    tailOpinion);

                feats.add(edgeFeats);
                System.exit(1);
            }
        }
        return feats;
    }*/


}
