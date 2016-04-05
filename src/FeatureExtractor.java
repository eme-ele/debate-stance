import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;

/**
 * @author pachecog@purdue.edu
 * @version 3/27/2016
 */

public class FeatureExtractor {

    public HashMap<String, Integer> unigramFrequency;
    public HashMap<String, Integer> unigramIndex;

    public int numUnigrams;
    public int numFeatures;

    // needed for preprocessing
    private StanfordCoreNLP pipeline;

    public FeatureExtractor(StanfordCoreNLP pipeline) {
        // containers
        unigramFrequency = new HashMap<>();
        unigramIndex = new HashMap<>();
        // libraries needed for preprocessing
        this.pipeline = pipeline;
    }

       public List<CoreMap> preprocessInstance(Instance sample) {
        Opinion o = (Opinion) sample;
        Annotation ann = new Annotation(o.text);
        pipeline.annotate(ann);

        List<CoreMap> sentences = ann.get(SentencesAnnotation.class);
        return sentences;

    }

    public void extractFeatures(List<Instance> dataset) {
        for (Instance sample: dataset) {
            List<CoreMap> sentences = preprocessInstance(sample);
            extractUnigrams(sentences);
        }

        //System.out.println("Extracted " + unigramFrequency.size() + " distinct unigrams");
    }

    public void extractUnigrams(List<CoreMap> sentences) {
        for (CoreMap sentence: sentences) {
            for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
                String word = token.get(TextAnnotation.class);
                if (unigramFrequency.containsKey(word)) {
                    unigramFrequency.put(word, unigramFrequency.get(word) + 1);
                } else {
                    unigramFrequency.put(word, 1);
                }
            }
        }
    }

     public void selectFeatures() {
        selectUnigrams();
        //System.out.println("Selected " + numUnigrams  + " unigrams");
        numFeatures = numUnigrams;

        //System.out.println("Selected " + numFeatures  + " features");
    }

    /**
     * Selects unigramFrequency based on their frequency, consider only
     * words with a frequency >= 5 in the training set.
     * And indexes them
     * * */
    public void selectUnigrams() {
        int index = 0;
        HashMap<String, Integer> selectedUnigrams = new HashMap<>();
        for (Map.Entry<String, Integer> entry: unigramFrequency.entrySet()) {
            if (entry.getValue() >= 5) {
                unigramIndex.put(entry.getKey(), index);
                index++;
                selectedUnigrams.put(entry.getKey(), entry.getValue());
            }

        }
        unigramFrequency = selectedUnigrams;
        numUnigrams = unigramFrequency.size();
    }

    public HashMap<Integer, Float> getFeatureVector(Instance sample, int offset) {
        List<CoreMap> sentences = preprocessInstance(sample);
        HashMap<Integer, Float> unigramVector = getUnigramVector(sentences, offset);
        return unigramVector;
    }

    /**
     * Gets a sparse representation of the feature vector
     * @param dataset comprised of a list of instances
     * @return a list of indexed features
     * */
    public List<HashMap<Integer, Float> > getFeatureMatrix(List<Instance> dataset) {
        ArrayList<HashMap<Integer, Float> > featureMatrix = new ArrayList<>();
        for (Instance sample: dataset) {
            HashMap<Integer, Float> featureVector = getFeatureVector(sample, 0);
            featureMatrix.add(featureVector);
        }
        return featureMatrix;
    }

     /**
     * Gets a sparse representation of the unigram vector
     * @param dataset comprised of a list of instances
     * @return a list of indexed unigrams
     * */
    public HashMap<Integer, Float> getUnigramVector(List<CoreMap> sentences, int offset) {
        HashMap<Integer, Float> unigramVector = new HashMap<>();
        for (CoreMap sentence: sentences) {
            for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
                String word = token.get(TextAnnotation.class);
                if (unigramIndex.containsKey(word)) {
                    unigramVector.put(unigramIndex.get(word) + offset, (float)unigramFrequency.get(word));
                }
            }

        }
        return unigramVector;
    }

}
