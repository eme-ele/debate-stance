import java.io.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
                        switch(data[0]) {
                case "ID":
                    opinion.id = Integer.parseInt(data[1]);
                case "PID":
                    opinion.pid = Integer.parseInt(data[1]);
                case "Stance":
                    opinion.stance = Integer.parseInt(data[1]);
                case "rebuttal":
                    opinion.rebuttal = data[1];
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

}
