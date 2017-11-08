/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package patientlinkage.Util;

import patientlinkage.GarbledCircuit.PatientLinkageGadget;
import com.opencsv.CSVReader;
import flexsc.CompEnv;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import static java.lang.Integer.max;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang.StringUtils;
import patientlinkage.DataType.Helper;
import patientlinkage.DataType.PatientLinkage;
import static java.util.Arrays.copyOf;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import patientlinkage.DataType.BatchDataHelper;
import patientlinkage.DataType.BlkDataHelper;
import patientlinkage.DataType.BlocksHelper;
import patientlinkage.DataType.PatientLinkageResults;
import patientlinkage.DataType.PatientLinkageWssBFOutput;
import static patientlinkage.Util.Main.DEBUG;

/**
 *
 * @author cf
 */
public class Util {

    public static final int BYTE_BITS = 8;
    public static final int DATA_LEN = 32;// 16;

    public static boolean[][][] generateDummyArray(boolean[][][] src) {
        boolean[][][] retArr = new boolean[src.length][][];

        for (int i = 0; i < src.length; i++) {
            retArr[i] = new boolean[src[i].length][];
            for (int j = 0; j < src[i].length; j++) {
                retArr[i][j] = new boolean[src[i][j].length];
            }
        }
        return retArr;
    }

    public static boolean[][][] generateDummyArray(boolean[][][] src, int len) {
        boolean[][][] retArr = new boolean[len][][];
        int width = src[0].length;

        for (int i = 0; i < retArr.length; i++) {
            retArr[i] = new boolean[width][];
            for (int j = 0; j < width; j++) {
                retArr[i][j] = new boolean[src[0][j].length];
            }
        }
        return retArr;
    }

    public static int[][] linspace(int pt0, int pt1, int num_of_intervals) {
        assert num_of_intervals > 0 : "math1.linspace: num of intervals > 0";
        int[] ret = new int[num_of_intervals + 1];

        int[][] ret1 = new int[num_of_intervals][2];

        ret[0] = pt0;
        ret[num_of_intervals] = pt1;

        ret1[0][0] = pt0;
        ret1[num_of_intervals - 1][1] = pt1;

        int int_len = (pt1 - pt0) / num_of_intervals;

        for (int i = 1; i < num_of_intervals; i++) {
            ret[i] = ret[i - 1] + int_len;
            ret1[i - 1][1] = ret[i];
            ret1[i][0] = ret[i];
        }

        return ret1;
    }

    public static int getPtLnkCnts(int[][] ranges1, int opp_num) {
        int ptLnkCnts = 0;

        for (int[] ranges11 : ranges1) {
            ptLnkCnts += (ranges11[1] - ranges11[0]) * opp_num;
        }

        return ptLnkCnts;
    }

    public static <T> T[][] unifyArray(Object[] input, CompEnv<T> eva, int len) {
        T[][] ret = eva.newTArray(len, 0);
        int index = 0;

        for (int i = 0; i < input.length; i++) {
            T[][] tmp = ((T[][]) input[i]);
            for (int j = 0; j < tmp.length; j++) {
                ret[index++] = tmp[j];
            }
        }
        return ret;
    }

    public static <T> Object VunifyObjArray(Object[] input, CompEnv<T> eva, int len_b) {

        int rows = ((PatientLinkageWssBFOutput) input[0]).getA().length;

        int cols = len_b;
        T[][] Ha = eva.newTArray(rows, cols);
        T[][][] Hb = eva.newTArray(rows, cols, 0);
        //Object Hobj=new PatientLinkageWssBFOutput( eva,  rows,  cols);
        int hn = 0;
        for (int i = 0; i < input.length; i++) {
            T[][] a = (T[][]) ((PatientLinkageWssBFOutput) input[i]).getA();
            T[][][] b = (T[][][]) ((PatientLinkageWssBFOutput) input[i]).getB();
            int cols0 = a[0].length;

            for (int k = 0; k < rows; k++) {
                for (int n = 0; n < cols0; n++) {
                    Ha[k][n + hn] = a[k][n];
                    Hb[k][n + hn] = b[k][n];
                }

            }
            hn = hn + cols0;
        }

        return (new PatientLinkageWssBFOutput(Ha, Hb));
    }

    public static <T> T[][][] unifyArray1(Object[] input, CompEnv<T> eva, int len) {

        T[][][] ret = eva.newTArray(len, 0, 0);
        int index = 0;

        for (Object input1 : input) {
            T[][][] tmp = (T[][][]) input1;
            for (T[][] tmp1 : tmp) {
                ret[index++] = tmp1;
            }
        }
        return ret;
    }

    public static <T> T[] unifyArrayWithF(Object[] input, CompEnv<T> eva, int len) {
        T[] ret = eva.newTArray(len);
        int index = 0;

        for (Object input1 : input) {
            T[] tmp = (T[]) input1;
            for (T tmp1 : tmp) {
                ret[index++] = tmp1;
            }
        }

        return ret;
    }

    public static boolean[][][] extractArray(boolean[][][] arr1, ArrayList<PatientLinkage> ptl_arr, String role) {
        boolean[][][] res = new boolean[ptl_arr.size()][][];
        switch (role) {
            case "generator":
                int ind;
                for (int n = 0; n < ptl_arr.size(); n++) {
                    ind = ptl_arr.get(n).getI();
                    res[n] = arr1[ind];
                }
                break;
            case "evaluator":
                for (int n = 0; n < ptl_arr.size(); n++) {
                    ind = ptl_arr.get(n).getJ();
                    res[n] = arr1[ind];
                }
                break;
        }

        return res;
    }

    public static boolean[][][] encodeCobinationAsJAMIA4Criteria(String[][] data1, int[][] properties_bytes) {
        //12, 11, 9, 8
        assert data1[0].length == properties_bytes[0].length;
        boolean[][][] ret = new boolean[data1.length][properties_bytes.length][];

        for (int i = 0; i < data1.length; i++) {
            for (int j = 0; j < properties_bytes.length; j++) {
                String temp = "";
                for (int k = 0; k < properties_bytes[j].length; k++) {
                    temp += resizeString(data1[i][k], properties_bytes[j][k]);
                }
                ret[i][j] = bytes2boolean(temp.getBytes(StandardCharsets.US_ASCII));
            }
        }

        return ret;
    }

    public static String resizeString(String str, int len) {
        if (str.length() < len) {
            return StringUtils.rightPad(str, len);
        } else if (str.length() > len) {
            return StringUtils.substring(str, 0, len);
        } else {
            return str;
        }
    }

    public static boolean[] bytes2boolean(byte[] vals) {
        boolean[] ret = new boolean[BYTE_BITS * vals.length];

        for (int i = 0; i < vals.length; i++) {
            System.arraycopy(fromByte(vals[i]), 0, ret, i * BYTE_BITS, BYTE_BITS);
        }

        return ret;
    }

    public static boolean[] fromByte(byte value) {

        boolean[] res = new boolean[BYTE_BITS];
        for (int i = 0; i < BYTE_BITS; i++) {
            res[i] = (((value >> i) & 1) != 0);
        }
        return res;
    }

    public static boolean[] fromInt(int value, int width) {
        boolean[] res = new boolean[width];
        for (int i = 0; i < width; i++) {
            res[i] = (((value >> i) & 1) != 0);
        }

        return res;
    }

    public static int toInt(boolean[] value) {
        int res = 0;
        for (int i = 0; i < value.length; i++) {
            res = (value[i]) ? (res | (1 << i)) : res;
        }

        return res;
    }

    public static String[][] readAndProcessCSV(String FileName, int records_num) {
        String[][] data1 = null;
        int properties_num = 6;
        Soundex sdx = new Soundex();

        try (CSVReader reader = new CSVReader(new FileReader(FileName))) {
            String[] nextLine;
            data1 = new String[records_num][properties_num];
            reader.readNext();
            int ind = 0;
            while ((nextLine = reader.readNext()) != null && ind < records_num) {
                data1[ind][0] = nextLine[1].toLowerCase();
                data1[ind][1] = nextLine[2].toLowerCase();
                data1[ind][2] = sdx.encode(nextLine[1]).toLowerCase();
                data1[ind][3] = sdx.encode(nextLine[2]).toLowerCase();
                data1[ind][4] = nextLine[6].replaceAll("-", "");
                data1[ind][5] = nextLine[11].replaceAll("-", "");

                ind++;

            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        }

        return data1;
    }

    public static boolean[][][] readAndEncode(String FileName, int[][] lens) {
        ArrayList<boolean[][]> retArrList = new ArrayList<>();
        int properties_num = lens[0].length;
        Soundex sdx = new Soundex();

        try (CSVReader reader = new CSVReader(new FileReader(FileName))) {
            String[] strs;
            reader.readNext();
            while ((strs = reader.readNext()) != null) {

                String[] coms_strs = new String[lens.length];
                Arrays.fill(coms_strs, "");
                for (int i = 0; i < properties_num; i++) {
                    String temp = strs[i].replace("-", "").toLowerCase();
                    for (int j = 0; j < coms_strs.length; j++) {
                        if (lens[j][i] > 65536) {
                            coms_strs[j] += sdx.soundex(temp);
                        } else {
                            coms_strs[j] += resizeString(temp, lens[j][i]);
                        }
                    }
                }
                boolean[][] bool_arr = new boolean[coms_strs.length][];
                for (int j = 0; j < coms_strs.length; j++) {
                    bool_arr[j] = bytes2boolean(coms_strs[j].getBytes(StandardCharsets.US_ASCII));
                }
                retArrList.add(bool_arr);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        }

        boolean[][][] bool_ret = new boolean[retArrList.size()][][];
        for (int i = 0; i < bool_ret.length; i++) {
            bool_ret[i] = retArrList.get(i);
        }

        return bool_ret;
    }

    public static boolean[][][] readAndEncode(String FileName, int[][] lens, int hash_len) {
        ArrayList<boolean[][]> retArrList = new ArrayList<>();
        int properties_num = lens[0].length;
        Soundex sdx = new Soundex();

        try (CSVReader reader = new CSVReader(new FileReader(FileName))) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            String[] strs;
            reader.readNext();
            while ((strs = reader.readNext()) != null) {

                String[] coms_strs = new String[lens.length];
                Arrays.fill(coms_strs, "");
                for (int i = 0; i < properties_num; i++) {
                    String temp = strs[i].replace("-", "").toLowerCase();
                    for (int j = 0; j < coms_strs.length; j++) {
                        if (lens[j][i] > 65536) {
                            coms_strs[j] += sdx.soundex(temp);
                        } else {
                            coms_strs[j] += resizeString(temp, lens[j][i]);
                        }
                    }
                }
                boolean[][] bool_arr = new boolean[coms_strs.length][];
                for (int j = 0; j < coms_strs.length; j++) {
                    //                   bool_arr[j] = bytes2boolean(coms_strs[j].getBytes(StandardCharsets.US_ASCII));
                    bool_arr[j] = bytes2boolean(copyOf(digest.digest(coms_strs[j].getBytes(StandardCharsets.UTF_8)), hash_len));
                }
                retArrList.add(bool_arr);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        }

        boolean[][][] bool_ret = new boolean[retArrList.size()][][];
        for (int i = 0; i < bool_ret.length; i++) {
            bool_ret[i] = retArrList.get(i);
        }

        return bool_ret;
    }

    public static Helper readAndEncodeWithProps(String FileName, int[][] lens) {
        Helper ret = new Helper();
        ArrayList<boolean[][]> retArrList = new ArrayList<>();
        int properties_num = lens[0].length;
        Soundex sdx = new Soundex();

        try (CSVReader reader = new CSVReader(new FileReader(FileName))) {
            String[] strs;
            ret.pros = reader.readNext();
            ret.updatingrules(lens);
            while ((strs = reader.readNext()) != null) {
                ret.IDs.add(strs[0]);
                String[] coms_strs = new String[lens.length];
                Arrays.fill(coms_strs, "");
                for (int i = 0; i < properties_num; i++) {
                    String temp = strs[i].replace("-", "").toLowerCase();
                    for (int j = 0; j < coms_strs.length; j++) {
                        if (lens[j][i] > (Integer.MAX_VALUE / 2)) {
                            coms_strs[j] += sdx.soundex(temp) + resizeString(temp, Integer.MAX_VALUE - lens[j][i]);
                        } else {
                            coms_strs[j] += resizeString(temp, lens[j][i]);
                        }
                    }
                }
                boolean[][] bool_arr = new boolean[coms_strs.length][];
                for (int j = 0; j < coms_strs.length; j++) {
                    bool_arr[j] = bytes2boolean(coms_strs[j].getBytes(StandardCharsets.US_ASCII));
                }
                retArrList.add(bool_arr);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        }

        ret.data_bin = new boolean[retArrList.size()][][];
        for (int i = 0; i < ret.data_bin.length; i++) {
            ret.data_bin[i] = retArrList.get(i);
        }

        return ret;
    }
//-----------------------------------

    public static boolean[] BFstr2boolean(char[] vals) {
        boolean[] ret = new boolean[vals.length];

        for (int i = 0; i < vals.length; i++) {
            ret[i] = (vals[i] == '1');
            //System.arraycopy(fromByte(vals[i]), 0, ret, i * byte_bits, byte_bits);
        }

        return ret;
    }
//-----------------------------------

    public static int[] BFstrNumOfOnesInBFparts(char[] vals, int nOfParts) {

        int partSize = vals.length / nOfParts;
        int partStart = 0, partEnd = partSize - 1;
        int[] Rets = new int[nOfParts];
        //assert(partEnd< vals.length && partStart< vals.length);
        for (int k = 0; k < nOfParts; k++) {
            Rets[k] = 0;
            if (k == (nOfParts - 1)) {
                partEnd = vals.length - 1;
            }
            for (int i = partStart; i <= partEnd; i++) {
                Rets[k] += (vals[i] == '1') ? 1 : 0;
                //System.arraycopy(fromByte(vals[i]), 0, ret, i * byte_bits, byte_bits);
            }
            partStart = partEnd + 1;
            partEnd = partEnd + partSize - 1;

        }

        return Rets;
    }
    //-----------------------------------    

    public static boolean[] BFstrNumOfOnes(char[] vals) {
        int ret = 0;
        boolean[] retBool;
        for (int i = 0; i < vals.length; i++) {
            ret += (vals[i] == '1') ? 1 : 0;
            //System.arraycopy(fromByte(vals[i]), 0, ret, i * byte_bits, byte_bits);
        }
        retBool = fromInt(ret, DATA_LEN);
        //retBool=fromInt(ret, 16 );
        return retBool;
    }
//-----------------------------------    

    public static int BFstrIntNumOfOnes(char[] vals) {
        int ret = 0;

        for (int i = 0; i < vals.length; i++) {
            ret += (vals[i] == '1') ? 1 : 0;
            //System.arraycopy(fromByte(vals[i]), 0, ret, i * byte_bits, byte_bits);
        }

        return ret;
    }

    //-------------------------------------
    public static boolean[][] copyOfRange2d(boolean[][] chkDCi, int[] hRange, int[] vRange) {
        boolean[][] retArr = new boolean[hRange[1] - hRange[0]][vRange[1] - vRange[0]];
        int i0 = 0, j0 = 0;
        if(DEBUG)
            System.out.println("\n In CopyOfRange2d>> chkdci dim(" + chkDCi.length + "," + chkDCi[0].length + "), hRange=" + hRange[0] + "-" + hRange[1] + ", vRange= " + vRange[0] + "-" + vRange[1]);
        for (int i = hRange[0]; i < hRange[1]; i++) {
            j0 = 0;
            for (int j = vRange[0]; j < vRange[1]; j++, j0++) {
                retArr[i0][j0] = chkDCi[i][j];
            }
            i0++;
        }
        return retArr;
    }

    
   public static ArrayList<int[]> getBlkRngesOfAllBatches(ArrayList<int[]> ABblocksCnt, int maxRecsInBatch){
     ArrayList<int[]> batchesRange = new ArrayList();  // <batch,fromBlk,toBlk>
     int fromBlk,toBlk,blk=0,blksCnt=ABblocksCnt.size(),t=0,batch=0;
     while(blk<blksCnt){
         fromBlk=blk;
         t=0;
         do{
             int[] ABblk= ABblocksCnt.get(blk);
             if(ABblk[1]>0 && ABblk[2]>0){
                 t+=Math.max(ABblk[1] , ABblk[2]);
                 
             }
             blk++;
             
                 
         }while (t<maxRecsInBatch && blk<blksCnt);
         toBlk=blk-1;
         int[] e={batch,fromBlk,toBlk};
        batchesRange.add(e);
        batch++;
     }     
   
   return batchesRange;
   } 
    
    public static ArrayList<int[]> getABBlocks4Range(ArrayList<int[]> ABblocksCnt,int fromIdx,int batchSize){
        ArrayList<int[]> batchABblocksCnt = new ArrayList();
        int lastIdx=((fromIdx+batchSize)>ABblocksCnt.size())? ABblocksCnt.size(): fromIdx+batchSize;
        
        for(int i=fromIdx;i<lastIdx;i++){
            int[] e=ABblocksCnt.get(i);
            batchABblocksCnt.add(e);
        }
        return batchABblocksCnt;
    }
    
    
    //-----------------------------------
    public static Helper readBFsWithProps(String FileName, int[][] lens) {
        Helper ret = new Helper();
        ArrayList<boolean[][]> retArrList = new ArrayList<>();
        ArrayList<boolean[][]> retNumOfOnesArrList = new ArrayList<>();
        ArrayList<int[]> retIntNumOfOnesArrList = new ArrayList<>();
        int BFs_num = lens[0].length;
        //Soundex sdx = new Soundex();

        try (CSVReader reader = new CSVReader(new FileReader(FileName))) {
            String[] strs;
            ret.pros = reader.readNext();
            ret.updatingrules(lens);
            while ((strs = reader.readNext()) != null) {
                ret.IDs.add(strs[0]);
                String[] coms_strs = new String[lens[0].length];
                Arrays.fill(coms_strs, "");
                int j = 0;
                for (int i = 0; i < BFs_num; i++) {
                    String temp = strs[i + 1].replace("-", "").toLowerCase();

                    if (lens[0][i] > 0) {
                        coms_strs[j] = temp;
                        j++;
                    }
                }
                boolean[][] bool_arr = new boolean[j][];

                int[] nOf1sInparts;
                boolean[][] boolNumOfOnes = new boolean[j][];
                int[] intNumOfOnes = new int[j];
                for (int k = 0; k < j; k++) {
                    bool_arr[k] = BFstr2boolean(coms_strs[k].toCharArray());
                    intNumOfOnes[k] = BFstrIntNumOfOnes(coms_strs[k].toCharArray());
                    boolNumOfOnes[k] = fromInt(intNumOfOnes[k], DATA_LEN);
                    //boolNumOfOnes[k]=BFstrNumOfOnes(coms_strs[k].toCharArray());
                    /*nOf1sInparts=BFstrNumOfOnesInBFparts( coms_strs[k].toCharArray(),3);
                    System.out.print("#1s in each part=");
                    for(int hh=0;hh<3;hh++)
                        System.out.print("P"+hh+":"+nOf1sInparts[hh]+" , ");
                    System.out.println();*/
                }
                retArrList.add(bool_arr);
                retNumOfOnesArrList.add(boolNumOfOnes);
                retIntNumOfOnesArrList.add(intNumOfOnes);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        }

        ret.data_bin = new boolean[retArrList.size()][][];
        ret.numOfOnesInBFs = new boolean[retNumOfOnesArrList.size()][][];
        ret.intNumOfOnesInBFs = new int[retIntNumOfOnesArrList.size()][];
        for (int i = 0; i < ret.data_bin.length; i++) {
            ret.data_bin[i] = retArrList.get(i);
            ret.numOfOnesInBFs[i] = retNumOfOnesArrList.get(i);
            ret.intNumOfOnesInBFs[i] = retIntNumOfOnesArrList.get(i);
        }

        return ret;
    }

//-----------------------------------
    public static Helper readBFsAndBlocksWithProps(String FileName, int[][] lens) {
        Helper ret = new Helper();
        ArrayList<boolean[][]> retArrList = new ArrayList<>();
        ArrayList<boolean[][]> retNumOfOnesArrList = new ArrayList<>();
        ArrayList<int[]> retIntNumOfOnesArrList = new ArrayList<>();

        int BFs_num = lens[0].length;
        //Soundex sdx = new Soundex();

        try (CSVReader reader = new CSVReader(new FileReader(FileName))) {
            String[] strs;
            ret.pros = reader.readNext();
            ret.updatingrules(lens);
            while ((strs = reader.readNext()) != null) {
                ret.IDs.add(strs[0]);
                int[] blks = new int[3];
                for (int b = 0; b < 3; b++) {
                    blks[b] = Integer.parseInt(strs[b + 1].trim());
                }
                ret.Blocks.add(blks);

                String[] coms_strs = new String[lens[0].length];
                Arrays.fill(coms_strs, "");
                int j = 0;
                for (int i = 0; i < BFs_num; i++) {
                    String temp = strs[i + 4].replace("-", "").toLowerCase();

                    if (lens[0][i] > 0) {
                        coms_strs[j] = temp;
                        j++;
                    }
                }
                boolean[][] bool_arr = new boolean[j][];

                int[] nOf1sInparts;
                boolean[][] boolNumOfOnes = new boolean[j][];
                int[] intNumOfOnes = new int[j];
                for (int k = 0; k < j; k++) {
                    bool_arr[k] = BFstr2boolean(coms_strs[k].toCharArray());
                    intNumOfOnes[k] = BFstrIntNumOfOnes(coms_strs[k].toCharArray());
                    boolNumOfOnes[k] = fromInt(intNumOfOnes[k], DATA_LEN);
                    //boolNumOfOnes[k]=BFstrNumOfOnes(coms_strs[k].toCharArray());
                    /*nOf1sInparts=BFstrNumOfOnesInBFparts( coms_strs[k].toCharArray(),3);
                    System.out.print("#1s in each part=");
                    for(int hh=0;hh<3;hh++)
                        System.out.print("P"+hh+":"+nOf1sInparts[hh]+" , ");
                    System.out.println();*/
                }
                retArrList.add(bool_arr);
                retNumOfOnesArrList.add(boolNumOfOnes);
                retIntNumOfOnesArrList.add(intNumOfOnes);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        }

        ret.data_bin = new boolean[retArrList.size()][][];
        ret.Matched = new boolean[retArrList.size()];
        ret.numOfOnesInBFs = new boolean[retNumOfOnesArrList.size()][][];
        ret.intNumOfOnesInBFs = new int[retIntNumOfOnesArrList.size()][];
        for (int i = 0; i < ret.data_bin.length; i++) {
            ret.data_bin[i] = retArrList.get(i);
            ret.Matched[i] = false;
            ret.numOfOnesInBFs[i] = retNumOfOnesArrList.get(i);
            ret.intNumOfOnesInBFs[i] = retIntNumOfOnesArrList.get(i);
        }

        return ret;
    }

  //-----------------------------------
    // Read BF data for specific Block_Id of Blk_Var and remove matched if BlkData-Helper RemoveMatched set to True
    public static void readBatchBFsDataForBlocksRange(String FileName,BatchDataHelper batchHelper,  ArrayList<int[]> batchABblocksCnt,
                                                     BlocksHelper AllBlocksHelper,int IdLoc,int[] BlksLoc,int[] BFDataLoc) {
       
        
        int numbOfBlkingSchemes=0;
       if(BlksLoc!=null)
           numbOfBlkingSchemes=BlksLoc.length;
        int numbOfUsedBFs=BFDataLoc.length; 
        
        ArrayList<boolean[][]> retArrList = new ArrayList<>();
        ArrayList<boolean[][]> retNumOfOnesArrList = new ArrayList<>();
        ArrayList<int[]> retIntNumOfOnesArrList = new ArrayList<>();
        ArrayList<Integer> BlkIds = new ArrayList<>();
        boolean useElim=batchHelper.removeMatched;
        int BlkVarIdx=batchHelper.BlockingVar;
         for (int i=0;i<batchABblocksCnt.size();i++){
            int[] ABblk=batchABblocksCnt.get(i);
            int BlkId;
            BlkId=ABblk[0];
            BlkIds.add(BlkId);
        }
        //boolean rm=batchHelper.removeMatched;
        //int BFs_num=4;
        
        try (CSVReader reader = new CSVReader(new FileReader(FileName))) {
            String[] strs;
            String[] pros = reader.readNext();
            
            while ((strs = reader.readNext()) != null) {
                
                if(useElim && AllBlocksHelper.IsThisIDMatched(strs[IdLoc-1]))
                    continue;
                else
                {
                int[] blks = new int[numbOfBlkingSchemes];
                for (int b = 0; b < numbOfBlkingSchemes; b++) {
                    blks[b] = Integer.parseInt(strs[BlksLoc[b]- 1].trim());
                }
                
                 if(BlkIds.contains(blks[BlkVarIdx])){
                   batchHelper.IDs.add(strs[IdLoc-1]); 
                   batchHelper.BlockIDs.add(blks[BlkVarIdx]);
                   String[] coms_strs = new String[ numbOfUsedBFs];
                   Arrays.fill(coms_strs, "");
                    int j = 0;
                    for (int i = 0; i < numbOfUsedBFs; i++) {
                        String temp = strs[BFDataLoc[i]-1].replace("-", "").toLowerCase();

                        coms_strs[j] = temp;
                        j++;
                    }
                
                    boolean[][] bool_arr = new boolean[j][];

                    boolean[][] boolNumOfOnes = new boolean[j][];
                    int[] intNumOfOnes = new int[j];
                    for (int k = 0; k < j; k++) {
                        bool_arr[k] = BFstr2boolean(coms_strs[k].toCharArray());
                        intNumOfOnes[k] = BFstrIntNumOfOnes(coms_strs[k].toCharArray());
                        boolNumOfOnes[k] = fromInt(intNumOfOnes[k], DATA_LEN);
                    
                    }
                    retArrList.add(bool_arr);
                    retNumOfOnesArrList.add(boolNumOfOnes);
                    retIntNumOfOnesArrList.add(intNumOfOnes);
                }
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        }

        batchHelper.Blkdata_bin = new boolean[retArrList.size()][][];
        
        batchHelper.BlknumOfOnesInBFs = new boolean[retNumOfOnesArrList.size()][][];
        batchHelper.BlkintNumOfOnesInBFs = new int[retIntNumOfOnesArrList.size()][];
        for (int i = 0; i < retArrList.size(); i++) {
            batchHelper.Blkdata_bin[i] = retArrList.get(i);
            
            batchHelper.BlknumOfOnesInBFs[i] = retNumOfOnesArrList.get(i);
            batchHelper.BlkintNumOfOnesInBFs[i] = retIntNumOfOnesArrList.get(i);
        }

        
    }
  
    
 //readBlockBFsDataFromBatchHelper(batchHelper, blkhelp, h_AllBlocks);   
    
 //-----------------------------------
    // Read BF data for specific Block_Id of Blk_Var and remove matched if BlkData-Helper RemoveMatched set to True
    public static void readBlockBFsDataForBlockingVar(String FileName, BlkDataHelper h1, BlocksHelper allBlks) {
       
        ArrayList<boolean[][]> retArrList = new ArrayList<>();
        ArrayList<boolean[][]> retNumOfOnesArrList = new ArrayList<>();
        ArrayList<int[]> retIntNumOfOnesArrList = new ArrayList<>();
        int BlkVarIdx=h1.BlockingVar;
        int BlkId=h1.BlockID;
        boolean rm=h1.removeMatched;
        int BFs_num=4;
        
        try (CSVReader reader = new CSVReader(new FileReader(FileName))) {
            String[] strs;
            String[] pros = reader.readNext();
            
            while ((strs = reader.readNext()) != null) {
                
                
                int[] blks = new int[3];
                for (int b = 0; b < 3; b++) {
                    blks[b] = Integer.parseInt(strs[b + 1].trim());
                }
                if(!rm || (rm && !allBlks.IsThisIDMatched(strs[0])) ){
                 if(blks[BlkVarIdx]==BlkId){
                   h1.IDs.add(strs[0]); 
                   String[] coms_strs = new String[ BFs_num];
                   Arrays.fill(coms_strs, "");
                    int j = 0;
                    for (int i = 0; i < BFs_num; i++) {
                        String temp = strs[i + 4].replace("-", "").toLowerCase();

                        coms_strs[j] = temp;
                        j++;
                    }
                
                    boolean[][] bool_arr = new boolean[j][];

                    boolean[][] boolNumOfOnes = new boolean[j][];
                    int[] intNumOfOnes = new int[j];
                    for (int k = 0; k < j; k++) {
                        bool_arr[k] = BFstr2boolean(coms_strs[k].toCharArray());
                        intNumOfOnes[k] = BFstrIntNumOfOnes(coms_strs[k].toCharArray());
                        boolNumOfOnes[k] = fromInt(intNumOfOnes[k], DATA_LEN);
                    
                    }
                    retArrList.add(bool_arr);
                    retNumOfOnesArrList.add(boolNumOfOnes);
                    retIntNumOfOnesArrList.add(intNumOfOnes);
                }
            }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        }

        h1.Blkdata_bin = new boolean[retArrList.size()][][];
        
        h1.BlknumOfOnesInBFs = new boolean[retNumOfOnesArrList.size()][][];
        h1.BlkintNumOfOnesInBFs = new int[retIntNumOfOnesArrList.size()][];
        for (int i = 0; i < retArrList.size(); i++) {
            h1.Blkdata_bin[i] = retArrList.get(i);
            
            h1.BlknumOfOnesInBFs[i] = retNumOfOnesArrList.get(i);
            h1.BlkintNumOfOnesInBFs[i] = retIntNumOfOnesArrList.get(i);
        }

        
    }

//-----------------------------------   
 // To read IDs, and Block ids of all large size data file   
//-----------------------------------
    public static BlocksHelper readBlocksAndIDsOfAllBlockingVars(String FileName, int IdLoc, int[] BlksLoc, int[] BFDataLoc  ) {
       BlocksHelper allBlks=new BlocksHelper() ;
        int numbOfBlkingSchemes=0;
       if(BlksLoc!=null)
           numbOfBlkingSchemes=BlksLoc.length;
        int numbOfUsedBFs=BFDataLoc.length;
        
        try (CSVReader reader = new CSVReader(new FileReader(FileName))) {
            String[] strs;
            String[] pros = reader.readNext();
            
            while ((strs = reader.readNext()) != null) {
                
                
                int[] blks = new int[numbOfBlkingSchemes];
                for (int b = 0; b < numbOfBlkingSchemes; b++) {
                    blks[b] = Integer.parseInt(strs[BlksLoc[b] - 1].trim());
                }
                
                   allBlks.IDs.add(strs[IdLoc-1]);  //assuming the  loc of 1st attribute =1
                   allBlks.Blocks.add(blks);
                   
             
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        }

        
        allBlks.Matched = new boolean[allBlks.IDs.size()];
        
        for (int i = 0; i < allBlks.IDs.size(); i++) {
          allBlks.Matched[i] = false;  
        }

     return allBlks;   
    }

//-----------------------------------   
        
//-----------------------------------
    
    
    public static ArrayList<int[]> readPartyBlocksCounts(String FileName) {

        ArrayList<int[]> retArrList = new ArrayList<>(); //BlkID,ArecsCount,BrecsCount

        try (CSVReader reader = new CSVReader(new FileReader(FileName))) {
            String[] strs;
            String[] header = reader.readNext();

            while ((strs = reader.readNext()) != null) {

                int[] blks = new int[2];
                for (int b = 0; b < 2; b++) {
                    blks[b] = Integer.parseInt(strs[b].trim());
                }

                retArrList.add(blks);

            }
            //I think we need to consider recs that not in the common blocks at both sides as an extra gruop!

        } catch (FileNotFoundException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        }

        return retArrList;
    }

    //--------------------------------------- 
    //get the rec count in the blkcount list 
    public static int getOtherPartyBlkCnt4BlkID(int BlkId, ArrayList<int[]> blocksCnt) {
        for (int[] item : blocksCnt) {
            if (item[0] == BlkId) {
                return (item[1]);
            }
        }
        return (0);
    }
//-----------------------------------

    public static ArrayList<int[]> readABblocksCounts(String FileName) {

        ArrayList<int[]> retArrList = new ArrayList<>(); //BlkID,ArecsCount,BrecsCount

        try (CSVReader reader = new CSVReader(new FileReader(FileName))) {
            String[] strs;
            String[] header = reader.readNext();

            while ((strs = reader.readNext()) != null) {

                int[] blks = new int[3];
                for (int b = 0; b < 3; b++) {
                    blks[b] = Integer.parseInt(strs[b].trim());
                }

                retArrList.add(blks);

            }
            //I think we need to consider recs that not in the common blocks at both sides as an extra gruop!

        } catch (FileNotFoundException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PatientLinkageGadget.class.getName()).log(Level.SEVERE, null, ex);
        }

        return retArrList;
    }

    //---------------------------------------
    public static int CountFP0(ArrayList<PatientLinkageResults> BlockVarRes) {
        int c = 0;
        ArrayList<PatientLinkageResults> UniqueFPs = new ArrayList<>();
        for (int i = 0; i < BlockVarRes.size(); i++) {
            PatientLinkageResults r = BlockVarRes.get(i);

            if (!r.AID.equals(r.BID)) {
                r.score = 0;
                if (!UniqueFPs.contains(r)) {
                    UniqueFPs.add(r);
                    c++;
                }
            }
        }

        return c;
    }

    
     //---------------------------------------
     public static int CountFP(ArrayList<PatientLinkageResults> BlockVarRes){
         int c=0;
         boolean verpose=true;
          if (verpose)
            {
              System.out.println("=================== Unique FPs :=======================\n"); 
            }
         ArrayList<PatientLinkageResults> UniqueFPs= new ArrayList<>();
         for(int i=0;i<BlockVarRes.size();i++){
            PatientLinkageResults r= BlockVarRes.get(i);
            
            if (!r.AID.equals(r.BID)){
             PatientLinkageResults temp = new PatientLinkageResults(r.AID,r.BID,0);

//r.score=0;
             if(!UniqueFPs.contains(temp)){
                     UniqueFPs.add(temp);
                    c++;
                     if (verpose)
                        {
                            System.out.println(temp.AID+"<->"+temp.BID); 
                        }
             }
            }
         }
        if (verpose)
            {
              System.out.println("=================== total FPs ( "+c+" )=======================\n"); 
            }
      return c;   
     }
         
    //---------------------------------------
    // count matches without douplicates
    public static int CountTotUniqueMatches0(ArrayList<PatientLinkageResults>[] AllBlockVarsRes) {
        int c = 0;
        ArrayList<PatientLinkageResults> UniqueUnifiedRes = new ArrayList<>();
        for (int k = 0; k < AllBlockVarsRes.length; k++) {
            for (int j = 0; j < AllBlockVarsRes[k].size(); j++) {
                PatientLinkageResults l = AllBlockVarsRes[k].get(j);
                l.score = 0; // to remeove different scores : match only AID and BID
                if (l.AID.equals(l.BID)) {
                    if (!UniqueUnifiedRes.contains(l)) {
                        UniqueUnifiedRes.add(l);
                        c++;
                    }
                }
            }
        }

        return c;
    }

    
     //---------------------------------------
      // count matches without douplicates
    public static int CountTotUniqueMatches(ArrayList<PatientLinkageResults>[] AllBlockVarsRes) {
      int c=0;
      ArrayList<PatientLinkageResults> UniqueUnifiedRes= new ArrayList<>();
      for(int k=0;k<AllBlockVarsRes.length;k++){
          for(int j=0;j<AllBlockVarsRes[k].size();j++){
             PatientLinkageResults l= AllBlockVarsRes[k].get(j);
             //l.score=0; // to remeove different scores : match only AID and BID
             PatientLinkageResults temp = new PatientLinkageResults(l.AID,l.BID,0);
             if(l.AID.equals(l.BID)){
                 if(!UniqueUnifiedRes.contains(temp)){
                     UniqueUnifiedRes.add(temp);
                     c++;
                 }
             }
          }
      }
      
         
      return c;      
    }
      
     //---------------------------------------   
     
      
     //---------------------------------------   
           
    //---------------------------------------   
    public static int CountTotUniqueFP0(ArrayList<PatientLinkageResults>[] AllBlockVarsRes) {
        int c = 0;

        ArrayList<PatientLinkageResults> UniqueUnifiedRes = new ArrayList<>();
        for (int k = 0; k < AllBlockVarsRes.length; k++) {
            for (int j = 0; j < AllBlockVarsRes[k].size(); j++) {
                PatientLinkageResults l = AllBlockVarsRes[k].get(j);
                l.score = 0; // to remeove different scores : match only AID and BID
                if (!l.AID.equals(l.BID)) {
                    if (!UniqueUnifiedRes.contains(l)) {
                        UniqueUnifiedRes.add(l);
                        c++;
                    }
                }
            }
        }
        return c;
    }
   //----------------------------------
     public static int CountTotUniqueFP(ArrayList<PatientLinkageResults>[] AllBlockVarsRes){
      int c=0;
      
      ArrayList<PatientLinkageResults> UniqueUnifiedRes= new ArrayList<>();
      for(int k=0;k<AllBlockVarsRes.length;k++){
          for(int j=0;j<AllBlockVarsRes[k].size();j++){
             PatientLinkageResults l= AllBlockVarsRes[k].get(j);
             PatientLinkageResults temp = new PatientLinkageResults(l.AID,l.BID,0);
             //l.score=0; // to remeove different scores : match only AID and BID
             if(!l.AID.equals(l.BID))
                 if(!UniqueUnifiedRes.contains(temp)){
                     UniqueUnifiedRes.add(temp);
                     c++;
                 }
          }
      }
      return c;       
     }
    //-----------------------------------------
    public static ArrayList<int[]> UpdateBlocksRecCountsOfThisParty(int BlkVar, Helper h1, ArrayList<int[]> BlocksCnt) {
        assert(BlocksCnt.size()>0);
        for (int i = 0; i < BlocksCnt.size(); i++) {
            int[] blk = BlocksCnt.get(i);
            blk[1] = 0;

            BlocksCnt.set(i, blk);
        }
        
        for (int j = 0; j < h1.Blocks.size(); j++) {

            if (!h1.Matched[j]) {
                int[] PartyBlks = h1.Blocks.get(j);
                for (int i = 0; i < BlocksCnt.size(); i++) {
                    int[] blk = BlocksCnt.get(i);
                    if (blk[0] == PartyBlks[BlkVar]) {
                        blk[1]++;
                        BlocksCnt.set(i, blk);
                    }
                }
            }
        }

        return BlocksCnt;
    }

    //---------------------------------------
    public static ArrayList<int[]> getEachBlockCount(int BlkVar, Helper h1) {

        ArrayList<int[]> BlocksCnt = new ArrayList();
        HashMap<Integer, Integer> BlocksCntHm = new HashMap(); //<Integer, Integer>
        for (int i = 0; i < h1.Blocks.size(); i++) {
            int[] PartyBlks = h1.Blocks.get(i);

            int blkid = PartyBlks[BlkVar];

            if (!h1.Matched[i]) {

                if (!BlocksCntHm.containsKey(blkid)) {
                    BlocksCntHm.put(blkid, 1);
                } else {
                    int oldCnt = BlocksCntHm.get(blkid);
                    BlocksCntHm.put(blkid, oldCnt + 1);

                }
            } // if !h1.Matched[i]
        }

        Iterator it = BlocksCntHm.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            int[] el = {(int) pairs.getKey(), (int) pairs.getValue()};
            BlocksCnt.add(el);
        }

        return BlocksCnt;
    }

    
    
    
    //----------------------------------
    
    public static ArrayList<int[]> getEachBlockCount0(int BlkVar, Helper h1) {

        ArrayList<int[]> BlocksCnt = new ArrayList();
        for (int i = 0; i < h1.Blocks.size(); i++) {
            int[] PartyBlks = h1.Blocks.get(i);
            int blkid = PartyBlks[BlkVar];
            int[] el = {blkid, 0};
            if (!BlocksCnt.contains(el)) {
                BlocksCnt.add(el);
            }

        }

        for (int j = 0; j < h1.Blocks.size(); j++) {

            if (!h1.Matched[j]) {
                int[] PartyBlks = h1.Blocks.get(j);

                for (int i = 0; i < BlocksCnt.size(); i++) {
                    int[] blk = BlocksCnt.get(i);
                    if (blk[0] == PartyBlks[BlkVar]) {
                        blk[1]++;
                        BlocksCnt.set(i, blk);
                    }
                }
            }
        }

        return BlocksCnt;
    }

    
    
    //---------------------------------------
    public static int CountUniqueMatches(ArrayList<PatientLinkageResults> BlockVarRes) {
        int c = 0;
        ArrayList<PatientLinkageResults> UniqueM = new ArrayList<>();
        for (int i = 0; i < BlockVarRes.size(); i++) {
            PatientLinkageResults r = BlockVarRes.get(i);

            if (r.AID.equals(r.BID)) {
                PatientLinkageResults temp = new PatientLinkageResults(r.AID, r.BID, 0);

                if (!UniqueM.contains(temp)) {
                    UniqueM.add(temp);
                    c++;
                }
            }
        }

        return c;
    }

    
    
    //--------------------------------------- 
    public static void usagemain() {
        String help_str
                = ""
                + String.format("     -config     <path>      : input configure file path\n")
                + String.format("     -data       <path>      : input data file path\n")
                + String.format("     -help                   : show help");
        System.out.println(help_str);
    }

}
