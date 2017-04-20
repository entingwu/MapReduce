import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class BetaValueKMeansMapper extends
        Mapper<LongWritable, Text, IntWritable, LongWritable> {

    private static final double betaValueLowerBound = 0.1;
    private static final double betaValueUpperBound = 0.85;
    private static final String NA = "NA";
    private static final String CG = "cg";
    private static final String CHR = "chr";
    private static final String X = "X";
    private static final String Y = "Y";

    private static Map<Integer, Long> centers = new HashMap<Integer, Long>();
    private IntWritable classCenter;
    private int clusterNum = 10;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        Configuration conf = context.getConfiguration();
        String basePath = conf.get("CENPATH");
        Path centroids = new Path(basePath);
        FileSystem fs = centroids.getFileSystem(conf);
        if (!fs.exists(centroids)) {
            // Set up the initial cluster centers if file does not exist
            System.out.println("No center information. Generate automatically");
            for (int i = 0; i < 30; i++) {// 23
                for (int j = 0; j < clusterNum; j++) {
                    // It uses a straight forward way, but later it will be changed.
                    centers.put(new Integer(i * 10 + j), new Long(j * 20000));
                }
            }
        } else {
            RemoteIterator<LocatedFileStatus> status = fs.listFiles(centroids, false);
            while (status.hasNext()) {
                LocatedFileStatus stats = status.next();
                Path p = stats.getPath();
                if (p.getName().contains("part-r")) {
                    System.out.print("Sub path is: " + p.toString() + "\n");
                    BufferedReader br=new BufferedReader(new InputStreamReader(fs.open(p)));
                    try {
                        String line;
                        line=br.readLine();
                        while (line != null) {
                            String [] keyValue = line.split("\t");
                            System.out.println(line);
                            centers.put(new Integer(keyValue[0].trim()), new Long(keyValue[1].trim()));
                            line = br.readLine();
                        }
                    } finally {
                        // you should close out the BufferedReader
                        br.close();
                    }
                }
            }
        }
        super.setup(context);
    }

    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
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
        if (tokens[3].equals(NA) || tokens[4].equals(NA)) {
            return;
        }
        long genePosition = Long.parseLong(tokens[4]);

        // Chromosome
        int chromosomeNum = 0;
        String chromosome = tokens[2];
        if (chromosome.startsWith(CHR)) {
            String realChromosome = chromosome.substring(3);
            if (realChromosome.equals(X) || realChromosome.equals(Y)) {
                chromosomeNum = 23;
            } else {
                chromosomeNum = Integer.parseInt(realChromosome);
            }
        } else {
            // invalid input
            return;
        }

        int maxClusters = clusterNum;
        int base = chromosomeNum * maxClusters;
        int minIndex = 0;
        long minDistance = 1000000000;
        for (int i = 0; i < clusterNum; i++) {
            if (centers.containsKey(base + i)) {
                if (Math.abs(centers.get(base + i) - genePosition) < minDistance) {
                    minDistance = Math.abs(centers.get(base + i) - genePosition);
                    minIndex = i;
                }
            }
        }
        int classNumEncoded = base + minIndex;
        this.classCenter = new IntWritable(classNumEncoded);
        context.write(this.classCenter, new LongWritable(genePosition));
    }
}