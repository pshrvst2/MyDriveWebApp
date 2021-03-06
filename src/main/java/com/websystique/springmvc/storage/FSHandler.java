/**
 * 
 */
package com.websystique.springmvc.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.websystique.springmvc.compression.CompressionFactory;
import com.websystique.springmvc.model.MyDriveFile;
import com.websystique.springmvc.model.MyDriveFileInfo;
import com.websystique.springmvc.storage.filesystem.MyDriveFSManager;

/**
 * @author Piyush
 *
 */
public class FSHandler {
	
	private String user;
	private String UPLOAD_LOCATION;
	
	public FSHandler(String user, String loc){
		this.setUser(user);
		this.UPLOAD_LOCATION = loc;
	}
	
	public void setUser(String user) {
		this.user = user;
	}

	public void pushFile(MyDriveFile file, String fileType, String username)
	{
		MyDriveFSManager manager = MyDriveFSManager.getInstance(user);
		manager.putFile(file.getFileName(), username);
	}
	
	public void compressAndPush(File file, String fileType, String userName)
	{
		System.out.println("File compressed is "+file.getAbsolutePath()+".zip");
		CompressionFactory cFactory = new CompressionFactory();
		cFactory.compressUsingGzip(file.getAbsolutePath());
		File compressedfile = new File(file.getAbsoluteFile()+".zip");
		MyDriveFile mdFile = new MyDriveFile(compressedfile, file.getName(),
											fileType, 
											file.length(),
											compressedfile.length());
		pushFile(mdFile, fileType, userName);
		writeFileDts(userName, file, fileType);
	}
	
	public void writeFileDts(String user, File file, String fileType)
	{
		StringBuffer fileDtl = new StringBuffer("OK::");
		fileDtl.append(file.getName()).append("::").append(file.length()).append("::").append(fileType);
		List<String> lines = Arrays.asList(fileDtl.toString());
		Path pathfile = Paths.get(user+".txt");
		try 
		{
			if(pathfile.toFile().exists())
				Files.write(pathfile.getFileName(), lines, StandardOpenOption.APPEND);
			else
				Files.write(pathfile, lines, Charset.forName("UTF-8"));
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void deleteFile(String userName, String fileName) 
	{
		File file = new File(UPLOAD_LOCATION+fileName);
		if(file!=null && file.exists())
			file.delete();
		file = new File(UPLOAD_LOCATION+fileName+".zip");
		if(file!=null && file.exists())
			file.delete();
		
		editUserFileDtlFiles(userName, fileName);
	}
	
	public void editUserFileDtlFiles(String userName, String fileName)
	{
	     String oldFileName = userName+".txt";
	     String tmpFileName = userName+"_tmp.txt";

	      BufferedReader br = null;
	      BufferedWriter bw = null;
	      try {
	         br = new BufferedReader(new FileReader(oldFileName));
	         bw = new BufferedWriter(new FileWriter(tmpFileName));
	         String line;
	         while ((line = br.readLine()) != null) {
	            if (!line.startsWith("OK::"+fileName))
	            	bw.write(line+"\n");
	            
	         }
	      } catch (Exception e) {
	         return;
	      } finally {
	         try {
	            if(br != null)
	               br.close();
	         } catch (IOException e) {
	            //
	         }
	         try {
	            if(bw != null)
	               bw.close();
	         } catch (IOException e) {
	            //
	         }
	      }
	      // Once everything is complete, delete old file..
	      File oldFile = new File(oldFileName);
	      oldFile.delete();

	      // And rename tmp file's name to old file name
	      File newFile = new File(tmpFileName);
	      newFile.renameTo(oldFile);
	}

	public ArrayList<MyDriveFileInfo> getAllFiles(String userName) {
		ArrayList<MyDriveFileInfo> list = new ArrayList<MyDriveFileInfo>();
		try {
			for (String line : Files.readAllLines(Paths.get(userName+".txt"))) 
			{
				
			    String[] texts = line.split("::");
			    if(texts[0].equals("OK"))
			    {
			    	MyDriveFileInfo mdInfo = new MyDriveFileInfo();
			    	try{ mdInfo.setFileName(texts[1]);} catch(NullPointerException nle){}
			    	try{ mdInfo.setFileType(texts[3]);} catch(NullPointerException nle){}
			    	try{ mdInfo.setFileSize(Integer.valueOf(texts[2]));} catch(NullPointerException nle){}
			    	//try{ mdInfo.setCompressedFileSize(obj.getLong("compressedSize"));} catch(NullPointerException nle){}
		            //try{ mdInfo.setCreatedDate(obj.getDate("createdDate"));} catch(NullPointerException nle){}

			    	list.add(mdInfo);
			    }
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return list;
		}
		return list;
		// TODO Auto-generated method stub
		
	}

	public String getUPLOAD_LOCATION() {
		return UPLOAD_LOCATION;
	}

	public void setUPLOAD_LOCATION(String uPLOAD_LOCATION) {
		UPLOAD_LOCATION = uPLOAD_LOCATION;
	}

}
