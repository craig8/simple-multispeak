package com.ecec.rweber.multispeak;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

/**
 * Non-visible class for encapsulating the HTTP send/receive functions of the library. This is called in the background by all classes extending the MultiSpeakService class. Any additional envelope information needed to complete the MultiSpeak call is done here as well.  
 * 
 * @author rweber
 *  
 */
class MultiSpeakClient {
	private MultiSpeakEndpoint m_service = null;
	private String m_error = null;
	
	public MultiSpeakClient(MultiSpeakEndpoint service){
		m_service = service;
	}
	
	private boolean hasError(){
		return m_error != null;
	}
	
	private String createEnvelope(String method, List<Element> params){
		Document result = new Document();
		
		//create the root node
		Element root = new Element("Envelope",MultiSpeak.SOAP_NAMESPACE);
		
		//add the header
		root.addContent(m_service.createHeader());
		
		//create the body and the method call
		Element body = new Element("Body",MultiSpeak.SOAP_NAMESPACE);
		
		Element xmlMethod = new Element(method);
		
		if(params != null)
		{
			xmlMethod.addContent(params);
		}
		
		body.addContent(xmlMethod);
		root.addContent(body);
		
		result.setRootElement(root);
		
		return new XMLOutputter().outputString(result);
	}
	
	private Document call(String request, String soapAction) throws MalformedURLException, IOException, JDOMException {
		m_error = null;
		Document result = null;
		
		//create the http connection
		URLConnection connection = m_service.getURL().openConnection();
		HttpURLConnection httpConn = (HttpURLConnection)connection;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
	
		//turn the envelope into a byte array
		byte[] buffer = new byte[request.length()];
		buffer = request.getBytes();
		bout.write(buffer);
		byte[] b = bout.toByteArray();
	
		// Set the appropriate HTTP parameters.
		httpConn.setRequestProperty("Content-Length",
		String.valueOf(b.length));
		httpConn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
		httpConn.setRequestProperty("SOAPAction", soapAction);
		httpConn.setRequestMethod("POST");
		httpConn.setDoOutput(true);
		httpConn.setDoInput(true);
		OutputStream out = httpConn.getOutputStream();
		
		//Write the content of the request to the outputstream of the HTTP Connection
		out.write(b);
		out.close();	
		
		if(httpConn.getResponseCode() == HttpURLConnection.HTTP_OK)
		{
			//Read the response
			InputStreamReader isr = new InputStreamReader(httpConn.getInputStream());
			BufferedReader in = new BufferedReader(isr);
			String outputString = "";
			String aString = "";
			
			//Write the SOAP message response to a String.
			while ((aString = in.readLine()) != null) {
				outputString = outputString + aString;
			}
	
			//parse this into a document
			SAXBuilder builder = new SAXBuilder();
			
			//need a string reader to create an input stream
			result = builder.build(new StringReader(outputString));
		}
		else
		{
			//get the error message
			InputStreamReader errorStream = new InputStreamReader(httpConn.getErrorStream());
			BufferedReader in = new BufferedReader(errorStream);
			m_error = "";
			String aString = "";
			
			while ((aString = in.readLine()) != null) {
				m_error = m_error + aString;
			}
		}
		
		return result;
	}
	
	/**
	 * @param method Multispeak Method to send to the endpoint
	 * @return the xml result of the call (could be null)
	 * @throws MultiSpeakException
	 */
	public Document sendRequest(String method) throws MultiSpeakException {
		return this.sendRequest(method, null);
	}
	
	/**
	 * @param method Multispeak Method to send to the endpoint
	 * @param params the element that contain the parameters for this method
	 * @return the xml result of the call (could be null)
	 * @throws MultiSpeakException
	 */
	public Document sendRequest(String method, List<Element> params) throws MultiSpeakException {
		Document response = null;
		String request = this.createEnvelope(method, params);
		
		//try and send the request to the endpoint
		try{
			response = this.call(request, m_service.getURL() + "/" + method);
			
			if(this.hasError())
			{
				throw new MultiSpeakException(m_error);
			}
		}
		catch(MalformedURLException me)
		{
			//don't do anything here as we'll have an error message
		}
		catch(IOException io)
		{
			
		}
		catch(JDOMException jd)
		{
			
		}
		
		return response;
	}
}
