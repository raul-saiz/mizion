package com.urv.zion.runtime.context;

import java.io.FileDescriptor;
import java.io.FileOutputStream;

import org.slf4j.Logger;

import com.urv.zion.runtime.api.Swift;

import java.util.Map;


public class Context {
	
	private Logger logger_;	
	public Log log;
	public Response response;
	public Request request;
	public Object object;
	public Function function;
	
	
	public byte [] policy;


	public Context(FileDescriptor inputStreamFd, FileDescriptor outputStreamFd, Map<String, String> functionParameters, 
				   FileOutputStream functionLog, FileDescriptor commandFd, Map<String, String> objectMd, 
				   Map<String, String> reqMd, Logger localLog, Swift swift,  byte [] myPolicy) 
	{	
		String currentObject = reqMd.get("X-Container")+"/"+reqMd.get("X-Object");
		
		logger_ = localLog;
		log = new Log(functionLog, logger_);
		function = new Function(functionParameters, logger_);
		response = new Response(logger_);
		request = new Request(commandFd, reqMd, response, logger_);
		object = new Object(inputStreamFd, outputStreamFd, commandFd, objectMd, currentObject, request, response, swift, logger_);
		
		
		policy = myPolicy;
		
		request.setObjectCtx(object);

		logger_.trace("Full Context created");
	}

	public void close(){
		request.forward();
		object.stream.close();
		object.metadata.flush();
	}
}