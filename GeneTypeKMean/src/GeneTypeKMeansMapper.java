import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeneTypeKMeansMapper extends Mapper<LongWritable, Text, Text, LongWritable> {

    private static final double betaValueLowerBound = 0.1;
    private static final double betaValueUpperBound = 0.85;
    private static final String NA = "NA";
    private static final String CG = "cg";
    private static final String CHR = "chr";
    private static final String X = "X";
    private static final String Y = "Y";

    private static Map<String, Long> centers = new HashMap<String, Long>();
    private Text classCenter;
    private int clusterNum = 10;
    private List<String> topGeneTypeList;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        Configuration conf = context.getConfiguration();
        String basePath = conf.get("CENPATH");
        Path centroids = new Path(basePath);

        String topGeneTypes = conf.get("TOP_TEN_GENE_TYPES");
        System.out.print("Gene types " + topGeneTypes);
        String[] tmp = topGeneTypes.split(";");
        topGeneTypeList = Arrays.asList(tmp);

        FileSystem fs = centroids.getFileSystem(conf);

        if (!fs.exists(centroids)) {
            // Set up the initial cluster centers if file does not exist
            System.out.println("No center information. Generate automatically");
            for (int k = 0 ; k < topGeneTypeList.size(); ++k) {
                for (int i = 0; i < 30; i++) {
                    for (int j = 0; j < clusterNum; j++) {
                        // It uses a straight forward way, but later it will be changed.
                        centers.put(getKey(topGeneTypeList.get(k), i, j), new Long(j * 20000));
                        centers.put(getKey("Other", i, j), new Long(j * 20000));
                    }
                }
            }
        } else {
            RemoteIterator<LocatedFileStatus> status = fs.listFiles(centroids, false);
            while (status.hasNext()) {
                LocatedFileStatus lfs = status.next();
                Path path = lfs.getPath();
                if (path.getName().contains("part-r")) {
                    System.out.print("Sub part is: " + path.toString() + "\n");
                    BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(path)));

                    try {
                        String line = br.readLine();
                        while (line != null) {
                            String[] keyValue = line.split("\t");
                            System.out.println(line);
                            centers.put(keyValue[0].trim(), new Long(keyValue[1].trim()));
                            line = br.readLine();
                        }
                    } finally {
                        br.close();
                    }
                }
            }
        }
        super.setup(context);
    }

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {
        String line = value.toString();
        String[] tokens = line.split("\t");
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = tokens[i].trim();
        }
        if (!tokens[0].subSequence(0, 2).equals(CG)) return;
        // Beta Value invalid
        if (tokens[1].equals(NA) || (Double.parseDouble(tokens[1]) > betaValueLowerBound &&
                Double.parseDouble(tokens[1]) < betaValueUpperBound)) {
            return; // We only cluster gene with betaValue meets the threshold.
        }
        // Position
        if (tokens[3].equals(NA) || tokens[4].equals(NA)) return;
        long genePosition = Long.parseLong(tokens[4]);

        // Chromosome
        int chromosomeNum = 0;
        String chromosome = tokens[2];
        String geneSymbol = tokens[5];
        if (chromosome.startsWith(CHR)) {
            String realChromosome = chromosome.substring(3);
            if (realChromosome.equals(X) || realChromosome.equals(Y)) {
                chromosomeNum = 23;
            } else {
                chromosomeNum = Integer.parseInt(realChromosome);
            }
        } else {// invalid input
            return;
        }

        // Gene Symbol
        String realGenSymbol = "Other";
        if (!geneSymbol.isEmpty()) {// top gene symbol
            realGenSymbol = geneSymbol.split(";")[0];
            if (!topGeneTypeList.contains(realGenSymbol)) {
                realGenSymbol = "Other";
            }
        }

        int minIndex = 0;
        long minDistance = 1000000000;
        for (int i = 0; i < clusterNum; i++) {
            String keyID = getKey(realGenSymbol, chromosomeNum, i);
            if (centers.containsKey(getKey(realGenSymbol, chromosomeNum, i))) {
                if (Math.abs(centers.get(keyID) - genePosition) < minDistance) {
                    minDistance = Math.abs(centers.get(keyID) - genePosition);
                    minIndex = i;
                }
            }
        }
        this.classCenter = new Text(getKey(realGenSymbol, chromosomeNum, minIndex));
        context.write(this.classCenter, new LongWritable(genePosition));
    }

    private String getKey(String geneType, int chromosome, int center) {
        return geneType + "-" + String.valueOf(chromosome) + "-" + String.valueOf(center);
    }
}