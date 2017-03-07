import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by rittick on 3/7/17.
 */
public class ModifyFile {
    private String fileToModify;

    public ModifyFile(String fileToModify) {
        this.fileToModify = fileToModify;
    }

    public boolean modifyFile(){
        boolean isModified = false;

        BufferedWriter bufferedWriter = null;
        FileWriter fileWriter = null;
        try {

            File file = new File(fileToModify);
            fileWriter = new FileWriter(file.getAbsoluteFile(), true);
            bufferedWriter = new BufferedWriter(fileWriter);

            bufferedWriter.write("THIS FILE IS MODIFIED.");
            System.out.println(file.getName()+" MODIFIED.");

            isModified = true;

        }catch (IOException e){
            e.printStackTrace();
        }finally {
            try {
                if(bufferedWriter != null){
                    bufferedWriter.close();
                }

                if(fileWriter != null){
                    fileWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    return isModified;
    }


/*    public static void main(String[] args) {
        ModifyFile modifyFile = new ModifyFile("Node1/Myfiles/file1.txt");
        boolean isFileModified = modifyFile.modifyFile();
        System.out.println("File is modified: "+isFileModified);
    }*/

}
