package util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class LogGenerator
{
    public static final String APP_TEMPLATE = "{\"id\":\"%s\", \"state\":\"%s\", \"type\":\"APPLICATION_LOG\", \"host\":\"12345\", \"timestamp\":%d}";
    public static final String NON_APP_TEMPLATE = "{\"id\":\"%s\", \"state\":\"%s\", \"timestamp\":%d}";

    private long timestamp = System.currentTimeMillis();
    private Random randomGenerator = new Random(78945326);
    private Map<Integer, Integer> usedIds = new HashMap<>();

    public static void main(String[] args) {
        try {
           if (args.length != 1) {
                System.out.println("Usage: gradle <filepath> runLogGenerator");
            }
            String filename = args[0];
            LogGenerator logGenerator = new LogGenerator();
            logGenerator.generate(filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void generate(String filename) throws IOException {
        System.out.println("Generating log entries in: " + filename);
        PrintWriter writer = new PrintWriter(new FileWriter(filename));

        for (int i = 0; i < 100; i++) {
            System.out.println("Generating batch: " + i);
            List<String> batch = generateBatchOfLogEntries(1000);
            batch.forEach(writer::println);
            writer.flush();
        }

        // NB: FileUtils.close(writer) would be better
        writer.close();
    }

    private List<String> generateBatchOfLogEntries(int batchSize) {
        List<String> batch = new ArrayList<>(batchSize);
        for (int i = 0; i < batchSize; i++) {
            // Use APP TEMPLATE 1/4 of the time
            String template = NON_APP_TEMPLATE;
            if (i % 4 == 0) {
                template = APP_TEMPLATE;
            }

            int randomId = getUnusedId();
            String id = String.valueOf(randomId);
            List<String> recordPair = generateLogRecordPair(template, id);
            batch.addAll(recordPair);
        }
        return batch;
    }

    private int getUnusedId() {
        int randomId;
        do {
            randomId = randomGenerator.nextInt(40000000);
        } while (usedIds.containsKey(randomId));
        usedIds.put(randomId, randomId);
        return randomId;
    }

    public List<String> generateLogRecordPair(String template, String id) {
        List<String> recordPairAsStrings = new ArrayList<>(2);
        long eventTimeStamp = timestamp + randomGenerator.nextInt(2);
        String startLogEntry = String.format(template, id, "STARTED", eventTimeStamp);
        recordPairAsStrings.add(startLogEntry);

        eventTimeStamp = timestamp + randomGenerator.nextInt(7);
        String finishLogEntry = String.format(template, id, "FINISHED", eventTimeStamp);
        recordPairAsStrings.add(finishLogEntry);

        // Advance the clock slowly
        timestamp += randomGenerator.nextInt(11) / 10;
        return recordPairAsStrings;
    }
}
