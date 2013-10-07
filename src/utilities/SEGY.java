/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.GLBuffers;
import java.io.File;
import java.io.FileInputStream;
import java.nio.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import seismic.FESVo;
import seismic.GeoTag;
import seismic.SeismicMetadata;
import seismic.Trace;

/**
 *
 * @author Aqeel
 */
public final class SEGY extends SeismicFile{
    
    // ::: Size of Headers in SEG-Y file :::
    public static final int SEGY_TEXTUAL_HEADER_SIZE = 3200;
    public static final int SEGY_BINARY_HEADER_SIZE = 400;
    public static final int SEGY_TRACE_HEADER_SIZE = 240;
    
    // ::: Byte no. of some interesting values from SEGY Binary Header :::
    // NOTE: address = byte no. - 1
    public static final int SEGY_SAMPLE_INTERVAL_BYTE_NUM  = 3217; // 2 bytes
    public static final int SEGY_SAMPLES_PER_DATA_TRACE_BYTE_NUM = 3221; // 2 bytes
    public static final int SEGY_DATA_SAMPLE_FORMAT_BYTE_NUM = 3225; // 2 bytes
    public static final int SEGY_FIXED_LENGTH_TRACE_FLAG_BYTE_NUM = 3225; // 2 bytes
    public static final int SEGY_NUM_OF_EXTENDED_TEXTUAL_FILE_HEADER_BYTE_NUM = 3505; // 2 bytes
    
    // ::: Byte no. of some interesting values from SEGY Trace Header :::
    // NOTE: address = address of current trace header + byte no. - 1
    public static final int SEGY_TRACE_SEQUENCE_NUM_WITHIN_LINE_BYTE_NUM = 1; // 4 bytes
    public static final int SEGY_TRACE_X_COORDINATE_BYTE_NUM = 73; // 4 bytes
    public static final int SEGY_TRACE_Y_COORDINATE_BYTE_NUM = 77; // 4 bytes
    
    // Data Samples (Trace) Format Code (as per SEGY rev1):
    public static final int SEGY_4BYTE_IBM_FLOATING_POINT_FORMAT = 1;
    public static final int SEGY_4BYTE_INTEGER_FORMAT = 2;
    public static final int SEGY_2BYTE_INTEGER_FORMAT = 3;
    public static final int SEGY_4BYTE_IEEE_FLOATING_POINT_FORMAT = 5;

    // ========================================================================
    
    // important meta data:
    private int samplesPerTrace;
    private int sampleDataFormat;
    private int sampleSize; 
    private int sampleInterval;
    private int timeMin;
    private int timeMax;
    private final String textualFileHeader;
    
    // ------------------------------------------------------------------------
    
    // constructor
    public SEGY(String fileName)
    {
        super(fileName);
        
        // retrieve important meta data of this segy file:
        
        textualFileHeader = readTextualFileHeader();
        
        samplesPerTrace = readShort(SEGY_SAMPLES_PER_DATA_TRACE_BYTE_NUM);
        sampleDataFormat = readShort(SEGY_DATA_SAMPLE_FORMAT_BYTE_NUM);
        sampleInterval = (readShort(SEGY_SAMPLE_INTERVAL_BYTE_NUM) / 1000); // convert it to milliseconds
        
        int [] temp = readTimeMinMax();
        timeMin = temp[0];
        timeMax = temp[1];
        
        
        try {
            if (sampleDataFormat == SEGY_4BYTE_IEEE_FLOATING_POINT_FORMAT)
                sampleSize = 4; //bytes
            else
                // throw an exception as no other format is supported for now:
                throw new Exception("Unsuported sample data format!");
        } 
        catch (Exception ex) {
            System.err.println(ex);
        }
        
    }//SEGY
    
    // ------------------------------------------------------------------------
    // :::::::::::: General Read int/short values given a location ::::::::::::
    // ------------------------------------------------------------------------
    
    // read an integer (4 bytes) at the given location:
    public int readInt(long location)
    {
        return read(location, 4);
    }// readInt
    
    // read a short (2 bytes) at the given location:
    public short readShort(long location)
    {
        return (short)read(location, 2);
    }//readShort
    
    // ------------------------------------------------------------------------
        
    public String getTextualFileHeader()
    {
        return this.textualFileHeader;
        
    }//getTextualFileHeader
    
    // ------------------------------------------------------------------------
    
    // return first inline number from the textual file header
    // if not found, return -1
    public int readFirstInLine()
    {
        return this.readInfoFromTextualHeader("First inline");
    }//getFirstInLine
    
    // ------------------------------------------------------------------------
    
    // return last inline number from the textual file header
    // if not found, return -1
    public int readLastInLine()
    {
        return this.readInfoFromTextualHeader("Last inline:");
    }//getLastInLine
    
    // ------------------------------------------------------------------------
    
    // return first xline number from the textual file header
    // if not found, return -1
    public int readFirstXLine()
    {
        return this.readInfoFromTextualHeader("First xline");
    }//getFirstXLine
    
    // ------------------------------------------------------------------------
    
    // return last xline number from the textual file header
    // if not found, return -1
    public int readLastXLine()
    {
        return this.readInfoFromTextualHeader("Last xline");
    }//getLastXLine
    
    // ------------------------------------------------------------------------
    
    // return dimension of this segy file as a 3-int array
    // if one value is not available, return null
    public int [] getDimension()
    {
        // first, check that none of the returned values is -1:
        int firstInLine = readFirstInLine();
        int lastInLine = readLastInLine();
        int firstXLine = readFirstXLine();
        int lastXLine = readLastXLine();
        
        if (    firstInLine == -1
             || lastInLine == -1
             || firstXLine == -1
             || lastXLine == -1)
        {
            return null;
        }//if
        
        int x = lastInLine - firstInLine + 1;
        int y = lastXLine - firstXLine + 1;
        int z = samplesPerTrace;
        
        return new int[]{x, y, z};
    }//getLastXLine
    
    
    // ------------------------------------------------------------------------
    
    // reads the entire volume relying on getDimension() to determine size of buffer
    public ByteBuffer readVolume()
    {
        // get volume size:
        long fileSize = file.length();
        
        int sizeOfExtendedHeaders = 
                readShort(SEGY.SEGY_NUM_OF_EXTENDED_TEXTUAL_FILE_HEADER_BYTE_NUM)
                * SEGY.SEGY_TEXTUAL_HEADER_SIZE;
        
        int sizeOfAllHeaders = SEGY.SEGY_TEXTUAL_HEADER_SIZE
                             + SEGY.SEGY_BINARY_HEADER_SIZE
                             + sizeOfExtendedHeaders;
        
        long sizeOfAllTraces = fileSize
                             - sizeOfAllHeaders;
        
        int traceDataSize = this.samplesPerTrace * this.sampleSize;
        
        // trace header and trace data
        int traceFullSize = SEGY.SEGY_TRACE_HEADER_SIZE + traceDataSize; 
        
        long numOfTraces = sizeOfAllTraces / traceFullSize;
        
        long volumeSize = numOfTraces * traceDataSize;
        
        // allocate a ByteBuffer:
        ByteBuffer buffer = ByteBuffer.allocateDirect((int)volumeSize);
        
        
        // Now, reading trace data from file:
        FileInputStream inStream = null;
        try
        {
            byte [] b = new byte[traceDataSize];
            
            inStream = new FileInputStream(file);

            // skip over all headers:
            inStream.skip(sizeOfAllHeaders);
            
            // go through each trace and put into the buffer:
            for (int i=0; i < numOfTraces; i++)
            {
                // skip through trace header:
                inStream.skip(SEGY.SEGY_TRACE_HEADER_SIZE);
                
                // read trace data bytes:
                int numOfBytesReturned = inStream.read(b);
                
                // throw exception if returned value is not the required size:
                if (numOfBytesReturned != traceDataSize)
                    throw new Exception("Error in reading trace no. " + i+1);
                
                
                // Byte order in SEGY is "big-endian", 
                // but OpenGL requires "little-endian" 
                // and cann't use Java order() method
                // so now swap byte order: (only supporting 4-byte data)
                for (int j=0; j < b.length; j+= 4)
                {
                    // swap bytes 0 and 3:
                    byte temp = b[j+3];
                    b[3+j] = b[j];
                    b[j] = temp;

                    // swap bytes 1 and 2
                    temp = b[2+j];
                    b[2+j] = b[1+j];
                    b[1+j] = temp;
                }//for
                
                
                // put byte array into the buffer:
                buffer.put(b);
                
            }//for
                        
            // closing:
            inStream.close();
            
        }//try

        catch(Exception ex){
            System.err.println(ex);
        }
        
        // rewind buffer:
        buffer.rewind();        
        
        return buffer;
    }

    // ------------------------------------------------------------------------
    
    public int getSampleDataFormat() {
        return sampleDataFormat;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public int getSamplesPerTrace() {
        return samplesPerTrace;
    }

    // sample iterval in millisecond
    public int getSampleInterval() {
        return sampleInterval;
    }

    // max TWT in millisecond
    public int getTimeMax() {
        return timeMax;
    }

    // min TWT in millisecond
    public int getTimeMin() {
        return timeMin;
    }

    // manually set time max if constructor cound not read it from the textual file header
    public void setTimeMax(int timeMax) {
        this.timeMax = timeMax;
    }

    // manually set time min if constructor cound not read it from the textual file header
    public void setTimeMin(int timeMin) {
        this.timeMin = timeMin;
    }
    
    
    // ------------------------------------------------------------------------
    
    public void loadTracesToFESVo(int sourceID)
    {
        int sizeOfExtendedHeaders = 
                readShort(SEGY.SEGY_NUM_OF_EXTENDED_TEXTUAL_FILE_HEADER_BYTE_NUM)
                * SEGY.SEGY_TEXTUAL_HEADER_SIZE;
        
        int sizeOfAllHeaders = SEGY.SEGY_TEXTUAL_HEADER_SIZE
                             + SEGY.SEGY_BINARY_HEADER_SIZE
                             + sizeOfExtendedHeaders;
        
        int traceDataSize = this.samplesPerTrace * this.sampleSize;          
        
        FileInputStream inStream = null;
        
        try
        {
            inStream = new FileInputStream(this.file);

            // skip over all headers:
            inStream.skip(sizeOfAllHeaders);
            
            // a byte array to hold current trace data:
//            byte [] traceB = new byte[traceDataSize];
            
            // a ByteBuffer to obtain an integer:
            ByteBuffer buffer;
            int numOfBytesReturned;
            
            // number of bytes storing X and Y Coordinates in SEGY file
            int length = 4;
            
            // byte arrays to hold X and Y coordinate bytes from file:
            byte [] bX = new byte[length];
            byte [] bY = new byte[length];
            
            // X and Y Coordinate of current trace:
            int coordX, coordY;
            
            // an index to indicate current trace number within SEGY
            int i=0; 
            
            // Trace object for current trace:
//            Trace trace;
            
            
            // read traces till end of segy file:
            while (true)
            {
                // read x, y location of this trace:
                // Note: read directly using this method's inStream 
                // instead of readInt() method as this will re-open the file:
                
                // skip over a number of bytes based on required location:
                inStream.skip(SEGY.SEGY_TRACE_X_COORDINATE_BYTE_NUM-1); // address = location - 1

                // read the required bytes storing X coordinates:
                numOfBytesReturned = inStream.read(bX);

                // exit while loop if it's end of thie file:
                if (numOfBytesReturned == -1)
                    break;
                
                // update trace index:
                i++;
                
                // throw exception if returned value is not the required size:
                if (numOfBytesReturned != length)
                    throw new Exception("Error in reading header of trace no. " + i);
                
                // read the required bytes storing Y coordinates:
                // no need to skip as Y coordinates bytes should come immidiatly after X coordinates bytes
                numOfBytesReturned = inStream.read(bY);

                // throw exception if returned value is not the required size:
                if (numOfBytesReturned != length)
                    throw new Exception("Error in reading header of trace no. " + i);
                
                // get X coordinate this trace:
                buffer = ByteBuffer.wrap(bX);
                coordX = buffer.getInt();
                
                // get Y coordinate this trace:
                buffer = ByteBuffer.wrap(bY);
                coordY = buffer.getInt();
                
                
                // skip through trace header taking into account current location:
                inStream.skip(SEGY.SEGY_TRACE_HEADER_SIZE - SEGY.SEGY_TRACE_X_COORDINATE_BYTE_NUM - (length*2) +1);
                
                // a byte array to hold current trace data:
                byte [] traceB = new byte[traceDataSize];
            
            
                // read trace data bytes:
                numOfBytesReturned = inStream.read(traceB);
                
                // throw exception if returned value is not the required size:
                if (numOfBytesReturned != traceDataSize)
                    throw new Exception("Error in reading trace no. " + i);
                
                
                // Byte order in SEGY is "big-endian", 
                // but OpenGL requires "little-endian" 
                // and cann't use Java order() method
                // so now swap byte order: (only supporting 4-byte data)
                for (int j=0; j < traceB.length; j+= 4)
                {
                    // swap bytes 0 and 3:
                    byte temp = traceB[j+3];
                    traceB[3+j] = traceB[j];
                    traceB[j] = temp;

                    // swap bytes 1 and 2
                    temp = traceB[2+j];
                    traceB[2+j] = traceB[1+j];
                    traceB[1+j] = temp;
                }//for
                
                
                // create a Trace object for this trace:
                Trace trace = new Trace(coordX, coordY, sourceID);
                trace.setSamples(traceB);
                
                // put trace into FESVo data map:
                FESVo.putTrace(trace);
                
            }//while
                        
            // closing:
            inStream.close();
            
            // update FESVo with meta-data of this SEGY:
            FESVo.setMetadata(
                    sourceID, new SeismicMetadata(  getSamplesPerTrace(),
                                                    getSampleDataFormat(),
                                                    getSampleInterval(),
                                                    getTimeMin(),
                                                    getTimeMax()));
            
            // [TESTING]
            System.out.println(i + " traces were loaded into FESVo.");
            
        }//try

        catch(Exception ex){
            System.err.println(ex);
        }
    }
    
    // ========================================================================
    // ::::::::::::::::::::::: Private (Helper) Methods :::::::::::::::::::::::
    // ========================================================================

    public String readTextualFileHeader()
    {
        FileInputStream inStream = null;
        StringBuffer headerSB = new StringBuffer(); // the returned header as StringBuffer
        byte [] headerB = new byte[SEGY_TEXTUAL_HEADER_SIZE];

        try
        {
            inStream = new FileInputStream(file);
            
            // read the required bytes:
            int numOfBytesReturned = inStream.read(headerB);

            // throw exception if returned value is not the required size:
            if (numOfBytesReturned != SEGY_TEXTUAL_HEADER_SIZE)
                throw new Exception("Error in reading file header!");
            
            // construct a String by decoding this array of bytes
            // using a "charset" specified for EBCDIC = Cp1047
            // ref: http://docs.oracle.com/javase/6/docs/technotes/guides/intl/encoding.doc.html
            String headerS = new String(headerB, "Cp1047");
            
            // as specified by SEGY rev1, the textual file header contains 40 lines,
            // thus, add new line character, '\n', at the end of each 80 bytes:
            headerSB = new StringBuffer(headerS);
            for (int i=0; i < 40; i++)
            {
                headerSB.insert((i+1)*80+i, '\n');
            }
        
            // closing:
            inStream.close();
            
        }//try

        catch(Exception ex){
            System.err.println(ex);
        }
        
        
        return headerSB.toString();
        
    }//readTextualFileHeader
    
    // ------------------------------------------------------------------------
    
    // read a number at the given location,
    // based on required number of bytes (2 or 4)
    private int read(long location, int length)
    {
        int result = 0;
        FileInputStream inStream = null;
        
        try
        {
            if (length != 2 && length != 4)
                throw new Exception("Requested length is not 2 or 4 bytes!");
            
            byte [] b = new byte[length];
            
            inStream = new FileInputStream(file);

            // skip over a number of bytes based on required location:
            inStream.skip(location-1); // address = location - 1
            
            // read the required bytes:
            int numOfBytesReturned = inStream.read(b);

            // throw exception if returned value is not the required size:
            if (numOfBytesReturned != length)
                throw new Exception("Error in reading file header!");
            
            // a ByteBuffer to obtain an integer:
            ByteBuffer buffer = ByteBuffer.wrap(b);
            if (length ==2)
                result = buffer.getShort();
            else
                result = buffer.getInt();
            
            // closing:
            inStream.close();
            
        }//try

        catch(Exception ex){
            System.err.println(ex);
        }
        
        return result;
    }
    
    // ------------------------------------------------------------------------
    
    // return -1 if value is not found
    private int readInfoFromTextualHeader(String info)
    {
        // returned value, default is -1 if no value found:
        int value = -1;
        
        String header = this.getTextualFileHeader();
        
        //int index = header.lastIndexOf(info);
        int index = header.indexOf(info) + info.length();
        
        // get a substring from this index up to (e.g.) 40 more characters:
        // this should be enough to capture the required value
        String subHeader = header.substring(index, index+40);
        
        // split the obtained substring around white spaces:
        String [] splitSubHeader = subHeader.split(" ");
                
        for (String i : splitSubHeader)
        {
            try{
                value = Math.round(Float.parseFloat(i));
                break;
            }
            catch (NumberFormatException ex){
                ; // do nothing
            }
            
        }//for
        
        return value;
    }//readInfoFromTextualHeader
    
    // ------------------------------------------------------------------------
    
    // reads time min/max from textual file header;
    // stores -1 if value is not found:
    private int [] readTimeMinMax() {
        
        // returned value, default is -1 if no value found:
        int [] results = new int[] {-1, -1};
        
        String header = this.getTextualFileHeader();
        
        int index1;
        if ((index1 = header.indexOf("Time")) == -1)
            if ((index1 = header.indexOf("time")) == -1)
                return results;
        
        // get a substring from this index up to 80 more characters:
        // this should cover the whole line of where Time exists
        String subHeader = header.substring(index1, index1+80);
        
        // split the obtained substring around white spaces:
        String [] splitSubHeader = subHeader.split(" ");
              
        int index2 = 0;
        
        // look for "min":
        for (; index2 < splitSubHeader.length; index2++)
        {
            if (splitSubHeader[index2].indexOf("min") != -1 ||
                splitSubHeader[index2].indexOf("Min") != -1)
                break;
        }
        
        index2++;
        
        // get min value:
        for (; index2 < splitSubHeader.length; index2++)
        {
            try{
                results[0] = Math.round(Float.parseFloat(splitSubHeader[index2]));
                break;
            }
            catch (NumberFormatException ex){
                ; // do nothing
            }
        }
        
        index2++;
        
        // look for max value:
        for (; index2 < splitSubHeader.length; index2++)
        {
            if (splitSubHeader[index2].indexOf("max") != -1 ||
                splitSubHeader[index2].indexOf("Max") != -1)
                break;
        }
        
        index2++;
        
        // get max value:
        for (; index2 < splitSubHeader.length; index2++)
        {
            try{
                results[1] = Math.round(Float.parseFloat(splitSubHeader[index2]));
                break;
            }
            catch (NumberFormatException ex){
                ; // do nothing
            }
        }
        
        return results;
    }
    
}//SEGY class
