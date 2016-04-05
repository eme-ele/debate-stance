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

    public EdgeFeatureExtractor(StanfordCoreNLP pipeline) {
        super(pipeline);
    }

    public HashMap<Integer, Float> getEdgeFeatures(Instance head, Instance tail) {
        HashMap<Integer, Float> headFeats = getFeatureVector(head, 0);
        HashMap<Integer, Float> tailFeats = getFeatureVector(tail, numFeatures);
        HashMap<Integer, Float> edgeFeats = new HashMap<>();
        edgeFeats.putAll(headFeats);
        edgeFeats.putAll(tailFeats);
        return edgeFeats;
    }

    public List<HashMap<Integer, Float> > getFeatureMatrix(Tree tree) {
        List<HashMap<Integer, Float> > feats = new ArrayList<>();
        for (String head: tree.adjacencyList.keySet()) {
            Instance headOpinion = tree.nodes.get(head);
            for (String tail: tree.adjacencyList.get(head)) {
                Instance tailOpinion = tree.nodes.get(tail);
                HashMap<Integer, Float> edgeFeats = getEdgeFeatures(headOpinion,
                                                                    tailOpinion);

                feats.add(edgeFeats);
                System.exit(1);
            }
        }
        return feats;
    }


}
