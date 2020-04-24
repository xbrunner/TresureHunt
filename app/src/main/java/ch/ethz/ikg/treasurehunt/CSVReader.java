package ch.ethz.ikg.treasurehunt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * The CSVReader class helps reading values from a CSV file, represented as an input stream.
 */
public class CSVReader {
    /**
     * Reads from an inputstream, and returns a list of rows.
     *
     * @param inputStream The inputstream to read from.
     * @param separator   The separator with which values are separated. Usually ";" or ",".
     * @param hasHeader   Indicates if this CSV contains a header line or not.
     * @return A list of string array, where each string array corresponds to one row in the CSV.
     * @throws IOException In case the inputstream does not allow reading.
     */
    public static List<String[]> readFile(InputStream inputStream, String separator, boolean hasHeader)
            throws IOException {
        List<String[]> resultList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String csvLine;
            // Skip the first line in case it was indicated that it's a header.
            if (hasHeader) {
                reader.readLine();
            }
            while ((csvLine = reader.readLine()) != null) {
                String[] row = csvLine.split(separator);
                resultList.add(row);
            }
        } finally {
            inputStream.close();
        }
        return resultList;
    }
}
