package com.urv.zion.runtime.function;


import com.ibm.storlet.sbus.SBusDatagram;
import com.urv.zion.runtime.api.Api;
import com.urv.zion.runtime.context.Context;

import redis.clients.jedis.Jedis;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;


import com.google.common.cache.Cache;


public class FunctionExecutionTask implements Runnable {
	private Logger logger_;
	private Properties prop_;
	private Function function_;
	private SBusDatagram dtg_;
	private FileOutputStream functionLog_;
	private Jedis redis_;
	
	private Context ctx;
	private Api api;
	
	private Map<String, String> object_metadata = null;
	private Map<String, String> request_headers = null;
	private Map<String, String> functionParameters = null;
	
	private FileDescriptor inputStreamFd = null;
	private FileDescriptor outputStreamFd = null;
	private FileDescriptor commandFd = null;
	
	private Cache<String, byte[]> cache;
	
	
	

	/*------------------------------------------------------------------------
	 * CTOR
	 * */
	public FunctionExecutionTask(SBusDatagram dtg, Properties prop, Jedis redis, Function function, FileOutputStream functionLog, Logger logger, Cache<String, byte[]> cachemc) {
		this.dtg_ = dtg;
		this.prop_ = prop;
		this.function_ = function;
		this.logger_ = logger;
		this.functionLog_ = functionLog;
		this.redis_ = redis;
		
		 this.cache = cachemc;   
		
		logger_.trace("Function execution task created");	
	}
	
	
	/*------------------------------------------------------------------------
	 * processDatagram
	 * 
	 * Process input datagram
	 * */
	@SuppressWarnings("unchecked")
	private void processDatagram(){
		HashMap<String, String>[] data = this.dtg_.getFilesMetadata();
		JSONObject metadata;
		
		outputStreamFd = this.dtg_.getFiles()[0];
		logger_.trace("Got object output stream");

		commandFd = this.dtg_.getFiles()[1];
		logger_.trace("Got Function command stream");
			
		inputStreamFd = this.dtg_.getFiles()[2];
		try {
			metadata = (JSONObject)new JSONParser().parse(data[2].get("data"));
			object_metadata = (Map<String, String>) metadata.get("object_metadata");
			request_headers = (Map<String, String>) metadata.get("request_headers");
			functionParameters = (Map<String, String>) metadata.get("parameters");
		} catch (ParseException e) {
			logger_.trace("Error parsing object headers, request metadata and parameters");
		}
		
		
		byte[] policy_binary=null;
    	
        try {

  // TODO :   existe esto del Referer en las functions ??
        	
        	
        String policyObject = req_md.get("Referer").split("/",6)[5]+ ".pol";
        String myObject = req_md.get("Referer").split("/",6)[5];

      
       	if ( ( policy_binary = cache.getIfPresent(policyObject)) == null ) {
       		InputStream in = swift.get(policyObject).getInputStream();
    		cache.put(policyObject,in.readAllBytes());
    		policy_binary = cache.getIfPresent(policyObject);
       		
//       		InputStream inaux = swift.get(myObject).getInputStream();
//       		byte[] len = inaux.readAllBytes();
//       		System.out.println("llegit  : "+len.length);
        	}
        } catch (IOException  e) {
        	System.out.println("Error en lectura del fitxer de dades directament");
        	e.printStackTrace();
        } 
		
		
		
		
		
		
		metadata = null;
		logger_.trace("Got object input stream, request headers, object metadata and function parameters");
		
		this.api = new Api(redis_, prop_, request_headers, logger_);
		this.ctx = new Context(inputStreamFd, outputStreamFd, functionParameters, functionLog_, commandFd, 
						  	   object_metadata, request_headers, logger_, api.swift, policy_binary);
	}

	
	/*------------------------------------------------------------------------
	 * run
	 * 
	 * Actual function invocation
	 * */
	public void run() {
		
		processDatagram();
		
		IFunction function = this.function_.getFunction();
		String functionName = this.function_.getName();
		
		logger_.trace("START: Going to execute '"+functionName+"' function");
		function.invoke(this.ctx, this.api);
		ctx.close();
		api.close();
		logger_.trace("END: Function '"+functionName+"' executed");

	}
}