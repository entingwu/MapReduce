import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import com.opencsv.CSVParser;

public class ArrDelayMinutes {
    private static final CSVParser parser = new CSVParser(',', '"');
    private static final String FROM_ORD = "FROM_ORD";
    private static final String TO_JFK = "TO-JFK";
    private static final String ORD = "ord";
    private static final String JFK = "jfk";
    private static final String DELIMITER = ",";

    public static class ArrDelayMinutesMapper extends
            Mapper<Object, Text, Text, Text> {

        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            String[] entry = parseCSV(value.toString());

            if (isValidRow(entry)) {
                Text newKey = new Text();
                Text newValue = new Text();

                // The value sent to reducer would be a tuple of (flag,delay,depart,arrival)
                StringBuilder sentValueSb = new StringBuilder();

                // flag indicates it is a flight from ORD or toward JFK.
                String flag = entry[11].toLowerCase().equals(ORD) ? FROM_ORD : TO_JFK;

                // Construct the tuple
                sentValueSb.append(flag).append(DELIMITER);
                String delay = entry[37];
                sentValueSb.append(delay).append(DELIMITER);
                String depart = entry[24];
                sentValueSb.append(depart).append(DELIMITER);
                String arrival = entry[35];
                sentValueSb.append(arrival);
                newValue.set(sentValueSb.toString());

                // The key sent to reducer would join same flight and same flight transfer
                // So it can be constructed as (flight,flightTransfer)
                String flightDate = entry[5];
                String flightTransfer = (entry[11].toLowerCase().equals(ORD) ? entry[17] : entry[11]).toLowerCase();
                newKey.set(flightDate + DELIMITER + flightTransfer);
                context.write(newKey, newValue);
            }
        }

        private boolean isValidRow(String[] row) {

            if (row == null || row.length == 0) {
                return false;
            }

            // Skip if any important column is missing.
            if (row[0].isEmpty() || row[2].isEmpty()
                    || row[5].isEmpty() || row[11].isEmpty() || row[17].isEmpty()
                    || row[24].isEmpty() || row[35].isEmpty() || row[37].isEmpty()
                    || row[41].isEmpty() || row[43].isEmpty()) {
                return false;
            }

            // Time filter, exclude invalid flight times
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
            Date flightDate;
            try {
                flightDate = dateFormatter.parse(row[5]);
                if (flightDate.before(dateFormatter.parse("2007-05-31")) ||
                        flightDate.after(dateFormatter.parse("2008-06-01"))) {
                    return false;
                }
            } catch (ParseException ex) {
                return false;
            }

            // Exclude direct flight
            if (row[11].toLowerCase().equals(ORD) && row[17]
                    .toLowerCase().equals(JFK)) {
                return false;
            }

            // Exclude the flight that are neither from ord nor to jfk
            if (!row[11].toLowerCase().equals(ORD)
                    && !row[17].toLowerCase().equals(JFK)) {
                return false;
            }

            boolean isCancelled = row[41].equals("1");
            boolean isDiverted = row[43].equals("1");

            // Exclude deverted or cancelled flights
            if (isCancelled || isDiverted) {
                return false;
            }

            return true;
        }
    }

    public static String[] parseCSV(String csv) throws IOException {
        return parser.parseLine(csv);
    }

    public static class ArrDelayMinutesReducer extends
            Reducer<Text, Text, Text, Text> {

        public enum Counter {
            TotalDelay,
            TotalFlightsCount
        }

        private float totalDelay;
        private int totalNumber;

        protected void setup(Context context) {
            totalDelay = 0;
            totalNumber = 0;
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            context.getCounter(Counter.TotalDelay).increment((long) totalDelay);
            context.getCounter(Counter.TotalFlightsCount).increment(totalNumber);
            context.write(new Text("total delay in minutes: " + context.getCounter(Counter.TotalDelay).getValue()),
                    new Text("total number of flights: " + context.getCounter(Counter.TotalFlightsCount).getValue()));
        }

        public void reduce(Text key, Iterable<Text> values,
                           Context context) throws IOException, InterruptedException {
            // List from ORD and to JFK
            List<String> fromORD = new ArrayList<>();
            List<String> toJFK = new ArrayList<>();

            for (Text value : values) {
                String record = value.toString();
                if (record.contains(FROM_ORD)) {
                    fromORD.add(record);
                } else {
                    toJFK.add(record);
                }
            }

            // Join operation
            for (String originatedAtORD : fromORD) {
                for (String arrivedAtJFK : toJFK) {
                    String[] extractedFromORD = parseCSV(originatedAtORD);
                    String[] extractedToJFK = parseCSV(arrivedAtJFK);
                    // If the arrival time of transfer flight airport is early than the departure time,
                    // It would be a valid two legged flight.
                    if (isValidTwoLeggedFlight(extractedFromORD, extractedToJFK)) {
                        float delay = Float.parseFloat(extractedFromORD[1]) + Float.parseFloat(extractedToJFK[1]);
                        totalDelay += delay;
                        totalNumber++;
                    }
                }
            }
        }

        private static boolean isValidTwoLeggedFlight(String[] fromORD, String[] toJFK) {
            String arriveTransferAirport = fromORD[3];
            String departTransferAirport = toJFK[2];
            if (Integer.parseInt(arriveTransferAirport) < Integer.parseInt(departTransferAirport)) {
                return true;
            }
            return false;
        }
    }

    public static void main(String[] args) throws Exception {

        Configuration conf = new Configuration();
        Job job = new Job(conf, "2 legged flights computation");
        job.setJarByClass(ArrDelayMinutes.class);
        job.setMapperClass(ArrDelayMinutesMapper.class);
        job.setReducerClass(ArrDelayMinutesReducer.class);
        job.setNumReduceTasks(10);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        String[] argList = new GenericOptionsParser(conf, args).getRemainingArgs();
        FileInputFormat.addInputPath(job, new Path(argList[0]));
        FileOutputFormat.setOutputPath(job, new Path(argList[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
