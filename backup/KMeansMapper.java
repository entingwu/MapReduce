import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;


public class KMeansMapper extends
        Mapper<LongWritable, Text, IntWritable, LongWritable> {

    private static Map<Integer, Long> centers = new HashMap<Integer, Long>();
    private static double betaValueThreshod = 0.5;
    private IntWritable classCenter;
    private int clusterNum = 10;

    @Override
    protected void setup(Context context) throws IOException,
            InterruptedException {
        Configuration conf = context.getConfiguration();
        Path centroids = new Path(conf.get("CENPATH"));

        FileSystem fs = FileSystem.get(conf);

        if (!fs.exists(centroids)) {
            //Initialize the initial cluster centers If file not exists
            System.out.println("No center informations. Generate automatically");
            for (int i = 0; i < 30; i++) {
                for (int j = 0; j < clusterNum; j++) {
                    //It uses a straight forward way, but later it will be changed.
                    centers.put(new Integer(i * 10 + j), new Long(j * 20000));
                }
            }
        } else {

            Reader reader = new Reader(fs, centroids,
                    conf);

            IntWritable key = new IntWritable();
            LongWritable value = new LongWritable();
            String result = "center result is: ";
            while (reader.next(key, value)) {
                if (key != null && value != null) {
                    centers.put(new Integer(key.get()), new Long(value.get()));
                    result += (key.get() + "; " + value.get() + "\n");
                }
            }
            System.out.print(result);
            reader.close();

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
        if (!tokens[0].subSequence(0, 2).equals("cg")) return;
        if (tokens[1].equals("NA") || Double.parseDouble((tokens[1])) < betaValueThreshod)
            return; // We only cluster gene with betaValue larger than threshold.
        if (tokens[3].equals("NA")) return;
        if (tokens[4].equals("NA")) return;
        // gene position
        long genePosition = Long.parseLong(tokens[4]);
        int chromosomeNum = 0;
        int maxClusters = clusterNum;
        // chromosome
        String chromosome = tokens[2];
        if (chromosome.startsWith("chr")) {
            String realChromosome = chromosome.substring(3);
            if (realChromosome.equals("X") || realChromosome.equals("Y")) {
                chromosomeNum = 23;
            } else {
                chromosomeNum = Integer.parseInt(realChromosome);
            }
        } else {
            // invalid input
            return;
        }
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

