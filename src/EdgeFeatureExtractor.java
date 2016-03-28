import java.util.List;
import java.util.HashMap;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

/**
 * @author pachecog@purdue.edu
 * @version 3/27/2016
 */

public class EdgeFeatureExtractor extends FeatureExtractor {

    public EdgeFeatureExtractor(StanfordCoreNLP pipeline) {
        super(pipeline);
    }

    /**
     * Gets a sparse representation of the feature vector
     * @param dataset comprised of a list of instances
     * @return a list of indexed features
     * */
    public List<HashMap<Integer, Float> > getFeatureVector(List<Instance> dataset) {
        HashMap<String, HashMap<String, Tree> > trees = DataParser.parseTrees(dataset);
        return null;
    }


}
