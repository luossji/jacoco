package com.kugou.sqlite;
import java.util.HashMap;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProjectRecordDb extends SqliteHelper {
    private String mProjectClassRecordTable = "ProjectClassRecord";
    private String mCoverageResultTable = "CoverageResultRecord";

    public ProjectRecordDb(String dbFilePath) throws ClassNotFoundException, SQLException {
        super(dbFilePath);
    }

    public void resetProjectClassRecord() throws ClassNotFoundException, SQLException{
        System.out.println("[INFO]resetProjectClassRecord");
        executeUpdate(String.format("drop table if exists %s;", mProjectClassRecordTable));
        executeUpdate("CREATE TABLE ProjectClassRecord ( \n" +
                "    classid   VARCHAR,\n" +
                "    className VARCHAR,\n" +
                "    filePath  VARCHAR \n" +
                ");\n");
        executeUpdate("CREATE INDEX classid_index ON ProjectClassRecord(classid);");
    }

    public void resetCoverageResultTable() throws ClassNotFoundException, SQLException{
        System.out.println("[INFO]resetCoverageResultTable");
        executeUpdate(String.format("drop table if exists %s;", mCoverageResultTable));
        executeUpdate("CREATE TABLE CoverageResultRecord ( \n" +
                "    classid VARCHAR,\n" +
                "    srcFile VARCHAR,\n" +
                "    method    VARCHAR,\n" +
                "    startLine INT,\n" +
                "    endLine   INT, \n" +
                "    executed_lines VARCHAR, \n" +
                "    executed_branch VARCHAR\n" +
                ");\n");
    }

    public void appendCoverageResultRecord(String values){
        String sql = String.format("insert into %s values%s", mCoverageResultTable, values);
        System.out.println("[SQL] insert sql");
        try{
            executeUpdate(sql);
        } catch (Exception e){
            System.out.println(String.format("[WARN]exectue sql: %s fail.", sql));
        }
    }
    public String getAppendCoverageResultRecordValues(long classid, String srcFile, String methodName, int startLine, int endLine, String covLines, String executed_branch){
        return String.format("('%s','%s' ,'%s', %s, %s, '%s', '%s')",  classid, srcFile, methodName, startLine, endLine, covLines, executed_branch);
        // return String.format("insert into %s values('%s','%s' ,'%s', %s, %s, '%s')", mCoverageResultTable, classid, srcFile, methodName, startLine, endLine, covLines);
    }
    public String getAppendClassRecordValues(long classid, String className, String filePath){
        return String.format("('%s', '%s', '%s')", classid, className, filePath);
    }

    public void appendClassRecord(String values){
        String sql = String.format("insert into %s values%s", mProjectClassRecordTable, values);
        System.out.println("[SQL] insert sql");
        try{
            executeUpdate(sql);
        } catch (Exception e){
            System.out.println(String.format("[WARN]exectue sql: %s fail.", sql));
        }
    }

    public HashMap getAllClassPath(){
        HashMap<String, String> hm = new HashMap();
        String sql = String.format("select classid, filePath from %s", mProjectClassRecordTable);
        try{
            ResultSet rs = getStatement().executeQuery(sql);
            while(rs.next()){
                hm.put(rs.getString("classid"), rs.getString("filePath"));
            }
        } catch (Exception e){
            System.out.println(String.format("[WARN]exectue sql: %s fail.", sql));
        }
        return hm;
    }
}
