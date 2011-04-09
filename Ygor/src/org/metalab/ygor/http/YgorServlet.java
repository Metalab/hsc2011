package org.metalab.ygor.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.BCodec;
import org.metalab.ygor.db.YgorRequest;

public abstract class YgorServlet extends HttpServlet {
	private String contentType;
  private final static BCodec base64 = new BCodec(); 

	public YgorServlet(String contentType)	{
		this.contentType = contentType;
	}
		
	protected abstract void process(YgorRequest query, OutputStream out);

	protected void doGet(HttpServletRequest request,
		HttpServletResponse response) throws ServletException, IOException {
		response.setContentType(contentType);
		response.setStatus(HttpServletResponse.SC_OK);
		String message;
		try {
			YgorRequest queryRequest = new YgorRequest(request);
			message = "Successful";
			response.setHeader("ygor-err", "0");
			response.setHeader("ygor-msg", encodeMessage("not implemented"));

			OutputStream out = response.getOutputStream();
			process(queryRequest, out);
			out.flush();
			
		} catch (Throwable t) {
			ByteArrayOutputStream stackTrace = new ByteArrayOutputStream();
			t.printStackTrace(new PrintStream(stackTrace));
			message = new String(stackTrace.toByteArray());

			response.setHeader("ygor-err", "1");
			response.setHeader("ygor-msg", encodeMessage(message));
		} 
		log(message);
	}

	private static String encodeMessage(String message) {
		try {
      return base64.encode(message);
    } catch (EncoderException e) {
      e.printStackTrace();
      return "OUCH! Message encoding failed";
    }
	}
}
