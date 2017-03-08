import java.io.File;
import java.util.ArrayList;

/**
 * Created by rittick on 3/7/17.
 */
public class OriginFileInventory {
    //Fields
    private String path;
    private Integer originServerID;
    private ArrayList<FileInfo> myFiles = new ArrayList<>();


    public OriginFileInventory(String path, Integer originServerID) {
        this.path = path;
        this.originServerID = originServerID;
    }

    //Methods
    public ArrayList<FileInfo> prepareOriginFileInventory(){
        File directory = new File(this.path);
        String[] fileNames = directory.list();

        for(int i=0; i<fileNames.length; i++){
            File nextFile = new File(path+"/"+fileNames[i]);
            FileInfo fileInfo = new FileInfo(fileNames[i], 1, originServerID, nextFile);
            fileInfo.setConsistencyState("VALID");
            myFiles.add(fileInfo);
        }

        return myFiles;
    }

    public void displayFileInfo(FileInfo obj){
        System.out.println(obj.getFileName());
        System.out.println(obj.getVersionNumber());
        System.out.println(obj.getOriginServerID());
        System.out.println(obj.getFileObject().lastModified());
        System.out.println(obj.getConsistencyState());
    }

    /*public static void main(String[] args) {

        OriginFileInventory originFileInventory = new OriginFileInventory("Node1/Myfiles/", 1);
        ArrayList<FileInfo> fileInfos = originFileInventory.prepareOriginFileInventory();

        for(int j=0; j<fileInfos.size(); j++){
            originFileInventory.displayFileInfo(fileInfos.get(j));
        }
    }*/




}
