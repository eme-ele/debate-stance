import java.io.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Collection;

/**
 * @author pachecog@purdue.edu
 * @version 3/27/2016
 */

public class DataParser {

    public static String parseTextFile(String textFilename) throws IOException {
        BufferedReader textFile = new BufferedReader(new FileReader(textFilename));
        String textDataRow = textFile.readLine();
        String fullText = "";

        while(textDataRow != null) {
            fullText += textDataRow;
            textDataRow = textFile.readLine();
        }
        textFile.close();
        return fullText;
    }

    public static Opinion parseMetaFile(String metaFilename) throws IOException {
        Opinion opinion = new Opinion();
        BufferedReader metaFile = new BufferedReader(new FileReader(metaFilename));
        String metaDataRow = metaFile.readLine();

        while (metaDataRow != null) {
            String[] data = metaDataRow.split("=");

            if (data.length == 2) {
                switch(data[0]) {
                    case "ID":
                        opinion.id = Integer.parseInt(data[1]);
                        break;
                    case "PID":
                        opinion.pid = Integer.parseInt(data[1]);
                        break;
                    case "Stance":
                        opinion.stance = Integer.parseInt(data[1]);
                        break;
                    case "rebuttal":
                        opinion.rebuttal = data[1];
                        break;
                }
            }
            metaDataRow = metaFile.readLine();
        }

        metaFile.close();
        return opinion;
    }

    public static void parseAuthorFile(List<HashMap<String, Instance> > folds) throws IOException {
        for (String topic: Config.TOPICS) {
            System.out.println("Authors for " + topic);

            File dir = new File(Config.AUTHOR_ROOT + topic);
            File[] directoryListing = dir.listFiles();
            for (File file: directoryListing) {
                BufferedReader authorFile = new BufferedReader(new FileReader(file));
                String authorDataRow = authorFile.readLine();
                while(authorDataRow != null) {
                    String[] data = authorDataRow.split(" ");

                    for (int i = 0; i < Config.NFOLDS; i++) {
                        if (folds.get(i).containsKey(data[0])) {
                            ((Opinion)folds.get(i).get(data[0])).author = data[1];
                            break;
                        }
                    }
                    authorDataRow = authorFile.readLine();
                }
                authorFile.close();
            }

        }
    }

    public static List<HashMap<String, Instance> > parseFolds() throws IOException {
        ArrayList<HashMap<String,Instance> > folds = new ArrayList<>(4);
        Pattern pattern = Pattern.compile("([A-Z]+)\\d+");

        for (int i = 1; i <= Config.NFOLDS; i++) {
            HashMap<String, Instance> foldInstances = new HashMap<>();

            for (String topic: Config.TOPICS) {
                int countOpinions = 1;
                String foldFilename = Config.FOLDS_ROOT + topic + "_folds/Fold-" + i;
                System.out.println(foldFilename);
                BufferedReader foldFile = new BufferedReader(new FileReader(foldFilename));
                String foldDataRow = foldFile.readLine();

                while (foldDataRow != null) {
                    // get opinion text in .data file
                    String textFilename = Config.DATA_ROOT + topic + "/" + foldDataRow + ".data";
                    String fullText = parseTextFile(textFilename);

                    // opinion meta data in .meta file
                    String metaFilename = Config.DATA_ROOT + topic + "/" + foldDataRow + ".meta";
                    Opinion opinion = parseMetaFile(metaFilename);

                    // other opinion data
                    opinion.text = fullText;
                    opinion.topic = topic;
                    Matcher m = pattern.matcher(foldDataRow);
                    m.find();
                    opinion.debate = m.group(1);

                    foldInstances.put(opinion.debate + opinion.id, opinion);
                    countOpinions += 1;
                    foldDataRow = foldFile.readLine();

                }
                foldFile.close();
            }
            folds.add(foldInstances);
        }

        parseAuthorFile(folds);
        return folds;
    }

    public static List<Integer> parseLabels(Collection<Instance> dataset) {
        ArrayList<Integer> labels = new ArrayList<>();
        for (Instance sample: dataset) {
            Opinion o = (Opinion) sample;
            labels.add(o.stance);
        }
        return labels;
    }

    public static HashMap<String, HashMap<String, Tree> > parseForests(Collection<Instance> dataset) {
        HashMap<String, HashMap<String, Tree> > forests = new HashMap<>();
        for (Instance sample: dataset) {
            Opinion o = (Opinion) sample;
            // add topic if it doesn't exist
            if (!forests.containsKey(o.topic)) {
                //System.out.println("Adding forests for " + o.topic);
                HashMap<String, Tree> debateForest = new HashMap<>();
                forests.put(o.topic, debateForest);
            }
            // add debate in topic if doesn't exist
            if (!forests.get(o.topic).containsKey(o.debate)) {
                //System.out.println("Adding tree for " + o.debate + " in " + o.topic);
                Tree t = new Tree(o.topic, o.debate);
                forests.get(o.topic).put(o.debate, t);
            }

            forests.get(o.topic).get(o.debate).addNode(o);
        }
        return forests;
    }

    public static List<Tree> getAllTrees(Collection<Instance> dataset) {
        HashMap<String, HashMap<String, Tree> > allForests = parseForests(dataset);
        List<Tree> allTrees = new ArrayList<>();

        for (String topic: allForests.keySet()) {
            HashMap<String, Tree> debateForests = allForests.get(topic);
            //System.out.println("Number of debate forests for " + topic + ": " + debateForests.size());
            List<Tree> forest = new ArrayList<>();

            for (String debate: debateForests.keySet()) {
                Tree t = debateForests.get(debate);
                //System.out.println(t);
                //System.exit(1);
                forest.addAll(t.getSubtrees());
            }

            //System.out.println("All trees for " + topic + ": " + forest.size());
            allTrees.addAll(forest);
        }

        return allTrees;
    }

    public static HashMap<String, List<Instance>> getByAuthor(Collection<Instance> dataset) {
        HashMap<String, List<Instance>> map = new HashMap<>();
        for (Instance opinion: dataset) {
            Opinion o = (Opinion) opinion;
            if (!map.containsKey(o.author)) {
                map.put(o.author, new ArrayList<>());
            }
            map.get(o.author).add(opinion);
        }
        return map;
    }

}
