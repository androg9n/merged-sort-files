package org.example.merged_sort_files;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.cli.*;

public class MergeSortFiles {
    private static Comparator<String> comparator;
    private static String last;

    public static void main(String[] args) {

        OptionGroup sortMode = new OptionGroup();
        Option ascendingSort = new Option("a", false, "ascending sort mode");
        Option descendingSort = new Option("d", false, "descending sort mode");
        sortMode.addOption(ascendingSort);
        sortMode.addOption(descendingSort);
        sortMode.setRequired(false);

        OptionGroup dataType = new OptionGroup();
        Option stringType = new Option("s", false, "string data type");
        Option integerType = new Option("i", false, "integer data type");
        dataType.addOption(stringType);
        dataType.addOption(integerType);
        dataType.setRequired(true);

        Options options = new Options();
        options.addOptionGroup(sortMode);
        options.addOptionGroup(dataType);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLine cmd;
        String[] cmdArgs = new String[0];

        try {
            cmd = parser.parse(options, args);
            cmdArgs = cmd.getArgs();

            if (cmd.hasOption(descendingSort.getOpt())) {
                if (cmd.hasOption(stringType.getOpt())) {
                    comparator = Comparator.reverseOrder();
                } else {
                    comparator = parseIntComparator().reversed();
                }
            } else {
                if (cmd.hasOption(stringType.getOpt())) {
                    comparator = String::compareTo;
                } else {
                    comparator = parseIntComparator();
                }
            }
        } catch (ParseException e) {
            System.out.println("Execution impossible. " + e.getMessage());
            helpFormatter.printHelp("mergesort", options);
            System.exit(1);
        }

        if (cmdArgs.length < 2) {
            System.out.println("Execution impossible. Not enough arguments");
            System.exit(1);
        }

        String outFileName = cmdArgs[0];
        List<File> inFiles = Arrays.stream(cmdArgs)
                .skip(1)
                .map(File::new)
                .collect(Collectors.toList());

        while (inFiles.size() > 1) {
            List<File> newInFileNames = new ArrayList<>();
            for (int i = 0; i < inFiles.size(); i += 2) {
                if (i == inFiles.size() - 1) { newInFileNames.add(inFiles.get(i)); } else {
                    try {
                        File outFile = File.createTempFile("sort", "tmp");
                        outFile.deleteOnExit();
                        merge(inFiles.get(i), inFiles.get(i + 1), outFile );
                        newInFileNames.add(outFile);
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
            inFiles = newInFileNames;
        }
        try {
            Files.copy(inFiles.get(0).toPath(), Paths.get(outFileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.out.println("Error copy to out file: " + e.getMessage());
        }
    }

    private static Comparator<String> parseIntComparator() {
        return (o1, o2) -> {
            int i1 = Integer.parseInt(o1);
            int i2 = Integer.parseInt(o2);
            return i1 - i2;
        };
    }

    private static void merge(File mergedFile1, File mergedFile2, File outFile) {
        last = null;
        try (   BufferedReader br1 = new BufferedReader(checkedFileReader(mergedFile1));
                BufferedReader br2 = new BufferedReader(checkedFileReader(mergedFile2));
                BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))
                ) {
            String s1 = null;
            String s2 = null;
            while (true) {
                if (s1 == null) s1 = checkedReadLine(br1);
                if (s2 == null) s2 = checkedReadLine(br2);
                if (s1 == null) {
                    if (s2 != null) {
                        writeString(s2, bw);
                        writeTail(br2, bw);
                    }
                    bw.flush();
                    break;
                }
                if (s2 == null) {
                    writeString(s1, bw);
                    writeTail(br1, bw);
                    bw.flush();
                    break;
                }
                if (comparator.compare(s1, s2) <= 0) {
                    writeString(s1, bw); s1 = null; }
                        else { writeString(s2, bw); s2 = null; }
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void writeString(String s, BufferedWriter bw) throws IOException {
        if (last == null) { bw.write(s + "\n"); last = s; }
            else {
                if (comparator.compare(last, s) <= 0) {
                    bw.write(s + "\n");
                    last = s;
                } else {
                    System.out.println("Error file presorting. Lost line: " + s);
                }
        }
    }

    private static void writeTail(BufferedReader br, BufferedWriter bw) {
        String s;
        try {
            while ((s = checkedReadLine(br)) != null) { writeString(s, bw); }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static FileReader checkedFileReader(File file) {
        FileReader fr = null;
        try {
            fr = new FileReader(file);
        } catch (FileNotFoundException e) {
            System.out.println("Error file not found: " + file.getAbsolutePath());
            try {
                File tempFile = File.createTempFile("sort", "nul");
                tempFile.deleteOnExit();
                fr = new FileReader(tempFile);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return fr;
    }

    private static String checkedReadLine(BufferedReader br) {
        String s = null;
        try {
            while (s == null) {
                s = br.readLine();
                if (s == null) return null;
                try { //noinspection ResultOfMethodCallIgnored
                    comparator.compare(s, "0");
                } catch (NumberFormatException ex) {
                    System.out.println("Error parsing int. Lost line: " + s);
                    s = null;
                }
                if (s != null && s.contains(" ")) {
                    System.out.println("Error of contains space(s). Lost line: " + s);
                    s = null;
                }
            }
        } catch (IOException e) { System.out.println(e.getMessage()); }
        return s;
    }
}
