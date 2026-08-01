/**
 * Ptolemy3D - a Java-based 3D Viewer for GeoWeb applications.
 * Copyright (C) 2008 Mark W. Korver
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ptolemy3d.demcutter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * <p>Given an input directory with .hdr and .dat files it generates .bdm files
 * ready for ptolemy3d viewer.</p>
 * <p>Parameters are: <br/>
 * DemCutter [input dir] [startx] [starty] [tile width] [cell width] [area] [output dir] [mult factor] [hdr extension] [dat extension] [isBinary] [outputType] [inside sig] [wall sig]
 * </p>
 *
 * <ul>
 * <li>input dir: directory where DEM and header files resides.</li>
 * <li>startx: minimum X in DD units (degree + 1000000).
 * See wiki page about pTolemy Tile System. startx/starty defines de left-top point of the selection.</li>
 * <li>starty: maximum Y in DD units (degree + 1000000).
 * See wiki page about pTolemy Tile System. startx/starty defines de left-top point of the selection.</li>
 * <li>tile width: size specified in DD units (degree + 1000000). Usually it will corresponds to a tile level.
 * See wiki page about pTolemy Tile System.</li>
 * <li>cell width: size specified in DD units (degree + 1000000).  Usually it will corresponds to a tile level below the 'tile width' level.
 * See wiki page about pTolemy Tile System.</li>
 * <li>area: 2 char file prefix, ex. use japangsi zone (01 - 19) use 00 for DD tiles.</li>
 * <li>output dir: the output direcotry where to write the generated files.</li>
 * <li>mult factor: usually this factor will be always 1000000.</li>
 * <li>hdr extension: a string containing the extension for header files that contains ascii header GRIDFLOAT information.</li>
 * <li>dat extension: a string containing the extension for data files with the ESRI GRIDFLOAT Binary Grid information. Used named as *.dat.</li>
 * <li>isBinary: 1-means binary, 0-means not binary. Specifies if the input files are in binay format. </li>
 * <li>outputType: 1-means TIN, 0-means BDM. Specifies the output file type.</li>
 * <li>inside sig: optional and only applied for TIN output files. Meters value to trim data on inside of dem.</li>
 * <li>wall sig: optional and only applied for TIN output files. Meters value to trim data on edges of dem.</li>
 * </ul>
 *
 * <p>Example usage:<br/>
 * <tt> -> DemCutter ./indata/ -102000000 41000000 102400 12800 00 ./outdata/ 1000000 .hdr .dat 1 0</tt>
 * </p>
 *
 * <p>It is impotant to note an execution of DemCutter only generates one file based on:<br/>
 * <em>filename = area + "x" + minX + "y" + maxY + outputType</em>.</p>
 *
 * <p>The first time DemCutter is executed using an input directory, it scans for all
 * header files and created an index header file named "indexheaders.idx". Next executions
 * looks for that index file and if found it is used to get header information.</p>
 *
 * <p><b>If you add/remove files from the input directory be sure to remove, if exists,
 * the index header file so DemCutter created a new fresh one.</b></p>
 *
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 */
public class DemCutter {

    // Define constants
    private static final Logger logger = Logger.getLogger(DemCutter.class.getName());
    private static final String INDEX_FILENAME = "indexheaders.idx";
    private static final int BDM_OUTPUT = 0;
    private static final int TIN_OUTPUT = 1;
    private static final String BINARY_FILE = "1";
    private static final String DEFAULT_HEADER_EXTENSION = ".hdr";
    private static final String DEFAULT_DATA_EXTENSION = ".asc";
    private static final String TIN_EXTENSION = ".tin";
    private static final String BDM_EXTENSION = ".bdm";
    // Attributes
    private String headerFile = DEFAULT_HEADER_EXTENSION;
    private String dataFile = DEFAULT_DATA_EXTENSION;
    private String demDir = "";
    private Vector<DemHeader> demHeaders = new Vector<DemHeader>(10, 10);
    private int mult = 1;
    private int outputType = BDM_OUTPUT;
    private boolean isBinary = false;
    private int minX;
    private int maxY;
    private int tileWidth;
    private int cellWidth;
    private String area;
    private String outputDir;
    private double insideSig = 0.3;
    private double wallSig = 0.3;

    /**
     * Parse arguments and create DemCutter instance.
     *
     * @param args
     */
    public static void main(String args[]) {

// * <li>input dir: directory where DEM and header files resides.</li>
// * <li>output dir: the output direcotry where to write the generated files.</li>
// * <li>startx: minimum X in DD units (degree + 1000000).
// * See wiki page about pTolemy Tile System. startx/starty defines de left-top point of the selection.</li>
// * <li>starty: maximum Y in DD units (degree + 1000000).
// * See wiki page about pTolemy Tile System. startx/starty defines de left-top point of the selection.</li>
// * <li>tile width: size specified in DD units (degree + 1000000). Usually it will corresponds to a tile level.
// * See wiki page about pTolemy Tile System.</li>
// * <li>cell width: size specified in DD units (degree + 1000000).  Usually it will corresponds to a tile level below the 'tile width' level.
// * See wiki page about pTolemy Tile System.</li>
// * <li>mult factor: usually this factor will be always 1000000.</li>
// * <li>hdr extension: a string containing the extension for header files that contains ascii header GRIDFLOAT information.</li>
// * <li>dat extension: a string containing the extension for data files with the ESRI GRIDFLOAT Binary Grid information. Used named as *.dat.</li>

// * <li>area: 2 char file prefix, ex. use japangsi zone (01 - 19) use 00 for DD tiles.</li>
// * <li>isBinary: 1-means binary, 0-means not binary. Specifies if the input files are in binay format. </li>
// * <li>outputType: 1-means TIN, 0-means BDM. Specifies the output file type.</li>
// * <li>inside sig: optional and only applied for TIN output files. Meters value to trim data on inside of dem.</li>
// * <li>wall sig: optional and only applied for TIN output files. Meters value to trim data on edges of dem.</li>

        Option tileWidthOp = new Option("tw", "tileWidth", true, "size specified in DD units (degree + 1000000). Usually it will corresponds to a tile level. See wiki page about pTolemy Tile System.");
        tileWidthOp.setRequired(true);

        Option cellWidthOp = new Option("cw", "cellWidth", true, "size specified in DD units (degree + 1000000).  Usually it will corresponds to a tile level below the 'tile width' level. See wiki page about pTolemy Tile System.");
        cellWidthOp.setRequired(true);

        Option multFactorOp = new Option("m", "multFactor", true, "(optional) usually this factor will be always 1000000.");
        multFactorOp.setRequired(false);

        Option headerExtensionOp = new Option("h", "header extension", true, "(optional) Usually *.hdr. A string containing the extension for header files that contains ascii header GRIDFLOAT information.");
        headerExtensionOp.setRequired(false);

        Option datExtensionOp = new Option("d", "header extension", true, "(optional) Usually *.dat. A string containing the extension for data files with the ESRI GRIDFLOAT Binary Grid information.");
        datExtensionOp.setRequired(false);

        // Create options to parse
        Options options = new Options();
        Option inputDirectoryOp = new Option("i", "inputDir", true, "Input directory where DEM and header files resides.");
        inputDirectoryOp.setRequired(true);

        Option outputDirectoryOp = new Option("o", "outputDir", true, "Output direcotry where to write the generated files.");
        outputDirectoryOp.setRequired(true);

        Option startxOp = new Option("sx", "startX", true, "minimum X in DD units (degree + 1000000). 'startx' and 'starty' defines de left-top point of the selection. See wiki page about pTolemy Tile System.");
        startxOp.setRequired(true);

        Option startyOp = new Option("sy", "startY", true, "maximum Y in DD units (degree + 1000000). 'startx' and 'starty' defines de left-top point of the selection. See wiki page about pTolemy Tile System.");
        startyOp.setRequired(true);

        Option helpOp = new Option("h", "help", false, "Print this message.");

        options.addOption(inputDirectoryOp);
        options.addOption(helpOp);

        // Options parser bypassed to allow positional arguments directly







        /////////////////////////////

        if (args.length < 12) {
            System.out.println("usage : DemCutter [dem file directory] [startx] [starty] [tile w] [cell width] [area] [outdir] [mult] [hdr] [dat] [isBinary] [outputType] [inside sig] [wall sig]");
            System.exit(1);
        }

        System.out.println("---------------------------------------------");
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            System.out.println("Argument " + i + ": " + arg);
        }
        System.out.println("---------------------------------------------");

        new DemCutter(args);
    }

    /**
     * 
     * @param args
     */
    public DemCutter(String args[]) {

        demDir = args[0];
        if (demDir.endsWith("/")) {
            demDir += "/";
        }

        minX = Integer.parseInt(args[1]);
        maxY = Integer.parseInt(args[2]);
        tileWidth = Integer.parseInt(args[3]);
        cellWidth = Integer.parseInt(args[4]);
        area = args[5];
        outputDir = args[6];
        if (outputDir.endsWith("/")) {
            outputDir += "/";
        }
        mult = Integer.parseInt(args[7]);

        headerFile = args[8];
        dataFile = args[9];
        isBinary = args[10].equals(BINARY_FILE) ? true : false;
        outputType = Integer.parseInt(args[11]);

        if ((outputType == TIN_OUTPUT) && (args.length > 12)) {
            insideSig = Double.parseDouble(args[12]);
        }
        if ((outputType == TIN_OUTPUT) && (args.length > 13)) {
            wallSig = Double.parseDouble(args[13]);
        }

        String fsuffix = (outputType == BDM_OUTPUT) ? BDM_EXTENSION : TIN_EXTENSION;

        int nCellsX = tileWidth / cellWidth + 1;
        int nCellsY = tileWidth / cellWidth + 1;

        System.out.println("Num cells: TileW/CellW = " + tileWidth + "/" + cellWidth + "=" + nCellsX);
        float[] dem = new float[nCellsY * nCellsX];

        Arrays.fill(dem, 0);

        System.out.println("---------------------------------------------");
        System.out.println("Compiling header files information...");

        // Read header files in the input directory
        readHeaderFiles();

        System.out.println("---------------------------------------------");

        int numHeaders = demHeaders.size();
        int yval, xval;
        for (int i = 0; i < nCellsY; i++) {
            yval = maxY - (i * cellWidth);
            for (int j = 0; j < nCellsX; j++) {
                xval = minX + (j * cellWidth);
                // Go through dem headers and get best dem resolution
                float dv = getBestDemValue(xval, yval, numHeaders);

                // System.out.println("Elev (" + xval + "/" + yval + "): " + dv);

                if (((i == 0) && (j == 0))
                        || ((i == 0) && (j == (nCellsX - 1)))
                        || ((i == (nCellsY - 1)) && (j == 0))
                        || ((i == (nCellsY - 1)) && (j == (nCellsX - 1)))) {
                    dem[i * nCellsX + j] = (float) Math.floor(dv); // to fit the above layer
                } else {
                    dem[i * nCellsX + j] = dv;
                }
            }
        }

        DemHeader header;
        for (int k = 0; k < numHeaders; k++) {
            header = (DemHeader) demHeaders.elementAt(k);
            header.closeFile();
        }
        // Write the data to a file
        DataOutputStream fout = null;
        try {
            String filename = area + "x" + minX + "y" + maxY + fsuffix;
            fout = new DataOutputStream(new FileOutputStream(outputDir + filename));

            switch (outputType) {
                case BDM_OUTPUT:
                    for (int i = 0; i < dem.length; i++) {
                        fout.writeShort((short) dem[i]);
                    }
                    break;
                case TIN_OUTPUT:
                    DemConvertor demconv = new DemConvertor(dem, tileWidth, minX, maxY);
                    demconv.verbose = true;
                    demconv.runInsidePass(insideSig);
                    demconv.runWallPass(wallSig);
                    demconv.triangulate();
                    demconv.stripeTris();
                    demconv.createJetstreamFile();
                    fout.write(demconv.bout.toByteArray());
                    break;
            }

            fout.close();
        } catch (FileNotFoundException e) {
            logger.severe(e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            logger.severe(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private float getBestDemValue(double xval, double yval, int numHeaders) {
        float val = 0;
        DemHeader dh;
        int xpos, ypos;
        double bestRes = Double.MAX_VALUE;

        for (int k = 0; k < numHeaders; k++) {
            dh = (DemHeader) demHeaders.elementAt(k);

            // allow on bottom and left borders, but not on top and right.
            // this is consistent with the way we determine the array position because we round to the nearest value.
            boolean c1 = (yval >= dh.getMinY());
            boolean c2 = (yval < dh.getMaxY());
            boolean c3 = (xval >= dh.getMinX());
            boolean c4 = (xval < dh.getMaxX());

            if (c1 && c2 && c3 && c4) {
                if (dh.getCellsize() < bestRes) {

                    if (yval == dh.getMinY()) // because we allow values on the left and bottom border
                    {
                        ypos = 0;
                    } else {
                        ypos = (int) Math.round((double) (dh.getPmaxY() - yval) / dh.getCellsize());
                    }

                    if (xval == dh.getMinX()) // because we allow values on the left and bottom border
                    {
                        xpos = 0;
                    } else {
                        xpos = (int) Math.round((double) (xval - dh.getPminX()) / dh.getCellsize());
                    }

                    float tmpval = dh.read(xpos, ypos);

                    if ((tmpval != dh.getNODATA_value()) && (tmpval >= 0)) {
                        bestRes = dh.getCellsize();
                        val = tmpval;
                    }
                }
            }
        }

        return val;
    }

    /**
     * Reads the header files found in the input directory.
     * Because the number of the header files can be very big, this method
     * creates a file with a compilation of all header info, so next calls
     * don't need to read the header files again, simply read the index file.
     *
     * NOTE: You must remove the index file if you add new header files to the
     * directory to re-create a fresh index file.
     */
    private void readHeaderFiles() {

        // Create a file filter that only accepts files finished with header extension.
        FilenameFilter filter = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(headerFile);
            }
        };

        try {
            File ddir = new File(demDir);

            // If index file doesn't exist then builds it.
            File index = new File(ddir, INDEX_FILENAME);
            if (!index.exists()) {

                System.out.println("Index file doesn't exists. Generating...");

                // Read header files and write to index file
                String[] record = ddir.list(filter);
                BufferedWriter out = new BufferedWriter(new FileWriter(index));
                String temp = "";
                for (int i = 0; i < record.length; i++) {
                    if (record[i].indexOf(headerFile) != -1) {
                        System.out.println("Found header file: " + demDir + record[i]);
                        DemHeader dh = new DemHeader(demDir + record[i], mult, headerFile, dataFile, isBinary);

                        temp = demDir + record[i] + ","
                                + mult + ","
                                + headerFile + ","
                                + dataFile + ","
                                + isBinary + ","
                                + dh.getNcols() + ","
                                + dh.getNrows() + ","
                                + dh.getXllcorner() + ","
                                + dh.getYllcorner() + ","
                                + dh.getCellsize() + ","
                                + dh.getNODATA_value() + ","
                                + dh.getByteorder() + "\n";
                        out.write(temp);
                    }
                }
                out.close();
            }

            // Read the index file
            BufferedReader in = new BufferedReader(new FileReader(index));
            String str;
            System.out.println("Reading index file... " + index.getAbsolutePath());
            while ((str = in.readLine()) != null) {
                String[] fields = str.split(",");

                String filename = fields[0];
                double mul = Double.valueOf(fields[1]);
                String header = fields[2];
                String datFile = fields[3];
                boolean binary = Boolean.valueOf(fields[4]);
                int ncols = Integer.valueOf(fields[5]);
                int nrows = Integer.valueOf(fields[6]);
                double xllcorner = Double.valueOf(fields[7]);
                double yllcorner = Double.valueOf(fields[8]);
                double cellsize = Double.valueOf(fields[9]);
                double NODATA_value = Double.valueOf(fields[10]);
                String byteorder = fields[11];

                DemHeader dheader = new DemHeader(
                        filename, mul, header, datFile, binary, ncols, nrows,
                        xllcorner, yllcorner, cellsize, NODATA_value, byteorder);
                demHeaders.add(dheader);
            }
            in.close();
        } catch (IOException e) {
            logger.severe(e.getMessage());
            e.printStackTrace();
        }
    }
}
