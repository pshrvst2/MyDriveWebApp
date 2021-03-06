package com.websystique.springmvc.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import com.websystique.springmvc.model.FileBucket;
import com.websystique.springmvc.model.MultiFileBucket;
import com.websystique.springmvc.storage.DBHandler;
import com.websystique.springmvc.storage.FSHandler;
import com.websystique.springmvc.storage.StorageManager;
import com.websystique.springmvc.util.FileValidator;
import com.websystique.springmvc.util.MultiFileValidator;

@Controller
public class FileUploadController {
	private static boolean dbON = true;
	public FileUploadController(){
		loadSystemProperties();
	}
	DBHandler dbHandler = null;
	FSHandler fsHandler = null;
	private static String UPLOAD_LOCATION="C:/Users/Piyush/Desktop/temp/app/";
	//private static String UPLOAD_LOCATION="/Users/kevinchen/Desktop/testdownload/";
	/****************************************************************************
	 * Project routing section
	 ****************************************************************************/
	
	@Autowired
	FileValidator fileValidator;

	@Autowired
	MultiFileValidator multiFileValidator;

	@InitBinder("fileBucket")
	protected void initBinderFileBucket(WebDataBinder binder) {
		binder.setValidator(fileValidator);
	}

	@InitBinder("multiFileBucket")
	protected void initBinderMultiFileBucket(WebDataBinder binder) {
		binder.setValidator(multiFileValidator);
	}

	@RequestMapping(value = { "/", "/welcome" }, method = RequestMethod.GET)
	public String getHomePage(ModelMap model) {
		System.out.println("reaching the endpoint");
		//return "welcome";
		return "login";
	}
	
	@RequestMapping(value =  "/login", method = RequestMethod.GET)
	public String getLogin(ModelMap model) {
		return "login";
	}
	@RequestMapping(value =  "/signup", method = RequestMethod.GET)
	public String getSignup(ModelMap model) {
		return "signup";
	}
	@RequestMapping(value =  "/upload", method = RequestMethod.GET)
	public String getUpload(ModelMap model) {
		return "upload";
	}
	@RequestMapping(value = "/singleUpload/{userName}", method = RequestMethod.GET)
	public String getSingleUploadPage(ModelMap model) {
		FileBucket fileModel = new FileBucket();
		model.addAttribute("fileBucket", fileModel);
		return "singleFileUploader";
	}
	@RequestMapping(value = "/multiUpload", method = RequestMethod.GET)
	public String getMultiUploadPage(ModelMap model) {
		MultiFileBucket filesModel = new MultiFileBucket();
		model.addAttribute("multiFileBucket", filesModel);
		return "multiFileUploader";
	}

	@RequestMapping(value = "/multiUpload", method = RequestMethod.POST)
	public String multiFileUpload(@Valid MultiFileBucket multiFileBucket,
			BindingResult result, ModelMap model) throws IOException {

		if (result.hasErrors()) {
			System.out.println("validation errors in multi upload");
			return "multiFileUploader";
		} else {
			System.out.println("Fetching files");
			List<String> fileNames = new ArrayList<String>();
			// Now do something with file...
			for (FileBucket bucket : multiFileBucket.getFiles()) {
				FileCopyUtils.copy(bucket.getFile().getBytes(), new File(UPLOAD_LOCATION + bucket.getFile().getOriginalFilename()));
				fileNames.add(bucket.getFile().getOriginalFilename());
			}

			model.addAttribute("fileNames", fileNames);
			return "multiSuccess";
		}
	}

	@RequestMapping(value = "/singleUpload/{userName}/", method = RequestMethod.POST, headers="Accept=application/json")
	public String singleFileUpload(@Valid FileBucket fileBucket,
			BindingResult result, ModelMap model, @PathVariable String userName) throws IOException {
		System.out.println("reach the singleUpload api");
		if (result.hasErrors()) {
			System.out.println("validation errors");
			return "singleFileUploader";
		} else {
			System.out.println("Fetching file");
			MultipartFile multipartFile = fileBucket.getFile();
			// Now do something with file...
			File file = new File(UPLOAD_LOCATION+fileBucket.getFile().getOriginalFilename());
			multipartFile.transferTo(file);
			compressAndPush(file, multipartFile.getContentType(), userName);
			//FileCopyUtils.copy(fileBucket.getFile().getBytes(), new File( UPLOAD_LOCATION + fileBucket.getFile().getOriginalFilename()));
			System.out.println("the file name: " + fileBucket.getFile().getOriginalFilename() +" for member email "+ userName);
			System.out.println("the size: "+multipartFile.getSize());
			String fileName = multipartFile.getOriginalFilename();
			model.addAttribute("fileName", fileName);
			return "success";
		}
	}
	
	@RequestMapping(value = "/singleUpload", method = RequestMethod.POST)
    public String singleFileUpload(@Valid FileBucket fileBucket,
            BindingResult result, ModelMap model) throws IOException {
 
        if (result.hasErrors()) {
            System.out.println("validation errors");
            return "singleFileUploader";
        } else {
            System.out.println("Fetching file");
            MultipartFile multipartFile = fileBucket.getFile();
 
            // Now do something with file...
            FileCopyUtils.copy(fileBucket.getFile().getBytes(), new File( UPLOAD_LOCATION + fileBucket.getFile().getOriginalFilename()));
            String fileName = multipartFile.getOriginalFilename();
            model.addAttribute("fileName", fileName);
            return "success";
        }
    }
	
	public void compressAndPush(File file, String fileType, String userName)
	{
		System.out.println("File compressed is "+file.getAbsolutePath()+".zip");
		StorageManager sm = new StorageManager(dbON, userName, UPLOAD_LOCATION);

		if(dbON)
		{
			dbHandler = sm.getDBHandler();
			dbHandler.compressAndPush(file, fileType, userName);
		}
		else
		{
			fsHandler = sm.getFSHandler();
			fsHandler.compressAndPush(file, fileType, userName);
		}
	}
	
	

    @RequestMapping(value="/download/{fileName}/", method = RequestMethod.GET)
    public void downloadFile(HttpServletResponse response, @PathVariable String fileName) throws IOException {
    	System.out.println("reaching the download api");
        File file = null;
        file = new File(UPLOAD_LOCATION+fileName);
        System.out.println("the file is :"+UPLOAD_LOCATION+fileName);
        if(!file.exists()){
            String errorMessage = "Sorry. The file you are looking for does not exist";
            System.out.println(errorMessage);
            OutputStream outputStream = response.getOutputStream();
            outputStream.write(errorMessage.getBytes(Charset.forName("UTF-8")));
            outputStream.close();
            return;
        }
         
        String mimeType= URLConnection.guessContentTypeFromName(file.getName());
        if(mimeType==null){
            System.out.println("mimetype is not detectable, will take default");
            mimeType = "application/octet-stream";
        }
         
        System.out.println("mimetype : "+mimeType);
         
        response.setContentType(mimeType);
         
        /* "Content-Disposition : inline" will show viewable types [like images/text/pdf/anything viewable by browser] right on browser 
            while others(zip e.g) will be directly downloaded [may provide save as popup, based on your browser setting.]*/
        //response.setHeader("Content-Disposition", String.format("inline; filename=\"" + file.getName() +"\""));
 
         
        /* "Content-Disposition : attachment" will be directly download, may provide save as popup, based on your browser setting*/
        response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", file.getName()));
         
        response.setContentLength((int)file.length());
 
        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
 
        //Copy bytes from source to destination(outputstream in this example), closes both streams.
        FileCopyUtils.copy(inputStream, response.getOutputStream());
    }
    
    private boolean loadSystemProperties() 
	{
		boolean flag = false;
		try 
		{
			File file = new File("properties/system.properties");
			System.out.println(file.getAbsolutePath());
			FileInputStream fis = new FileInputStream(file);
			Properties properties = new Properties();
			properties.load(fis);
			fis.close();
			Enumeration<Object> keys = properties.keys();
			while(keys.hasMoreElements())
			{
				String key = (String) keys.nextElement();
				String val = properties.getProperty(key);
				if(key.equals("localFilePath"))
					UPLOAD_LOCATION = val;
				else if(key.equals("db"))
				{
					if(val.equalsIgnoreCase("ON"))
						dbON = true;
				}
			}
			if(UPLOAD_LOCATION==null || UPLOAD_LOCATION.equals(""))
			{
				System.out.println("System properties missing! Check configurations!!");
				//_logger.error("System properties missing! Check configurations!!");
				flag = false;
			}
			else
			{
				//checkAndCreateDirectory();
				flag = true;
			}
		} 
		catch (FileNotFoundException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		return flag;
		
	}

}
