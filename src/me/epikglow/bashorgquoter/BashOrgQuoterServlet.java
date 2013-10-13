package me.epikglow.bashorgquoter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.SmsFactory;
import com.twilio.sdk.verbs.Sms;
import com.twilio.sdk.verbs.TwiMLException;
import com.twilio.sdk.verbs.TwiMLResponse;

@SuppressWarnings("serial")
public class BashOrgQuoterServlet extends HttpServlet {
	// Twilio Account SID and Auth Token
	public final String ACCOUNT_SID = "ACc143ae4e70cae8b120be94698b3c69e7";
	public final String AUTH_TOKEN = "131cd097b5bd1886953decd381205729";
	public final String FROM_PHONE = "+17326497078";
	public final TwilioRestClient client = new TwilioRestClient(ACCOUNT_SID,
			AUTH_TOKEN);

	// Takes the quote info and quote text and formats it correctly for sending
	// through Twilio
	public String formatQuote(Element quoteInfoElement, Element quoteTextElement) {
		String quoteNumber = "Quote " + quoteInfoElement.text().split(" ")[0];
		String quoteScore = quoteInfoElement.text().split(" ")[1];
		quoteScore = "Score: "
				+ quoteScore.substring(2, quoteScore.length() - 2);

		String quoteText = quoteTextElement.text();
		String quote = "BashOrgQuoter\n" + quoteNumber + " " + quoteScore
				+ "\n\n" + quoteText;

		System.out.println(quote);

		return quote;
	}

	// Gets the top quote that is at rank place in terms of score and returns it
	// preformatted for sending through Twilio
	public String getTopQuote(int rank) {
		// JSoup Stuff
		Document page = null;
		try {
			if (rank <= 100)
				page = Jsoup.connect("http://bash.org/?top.htm").get();
			else if (rank <= 200)
				page = Jsoup.connect("http://bash.org/?top2.htm").get();
			else
				return "BashOrgQuoter\nYou can only view the top 200 Bash.org quotes.\nPlease choose a rank between 1 and 200, inclusive.";
		} catch (IOException e) {
			e.printStackTrace();
		}

		Element quoteInfoElement = null;
		Element quoteTextElement = null;

		while (rank > 1) {
			quoteInfoElement = page.select("p[class]").first();
			quoteInfoElement.remove();
			quoteTextElement = page.select("p[class]").first();
			quoteTextElement.remove();
			rank--;
		}
		quoteInfoElement = page.select("p[class]").first();
		quoteInfoElement.remove();
		quoteTextElement = page.select("p[class]").first();
		quoteTextElement.remove();

		return formatQuote(quoteInfoElement, quoteTextElement);
	}

	// Gets the first random quote and returns it preformatted for sending
	// through Twilio
	public String getRandomQuote() {
		// JSoup Stuff
		Document page = null;
		try {
			page = Jsoup.connect("http://bash.org/?random1.htm").get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Element quoteInfoElement = page.select("p[class]").first();
		quoteInfoElement.remove();
		Element quoteTextElement = page.select("p[class]").first();
		quoteTextElement.remove();

		return formatQuote(quoteInfoElement, quoteTextElement);
	}

	// Sends an error message--used when the user made an invalid choice
	public String getErrorMessage() {
		return "BashOrgQuoter\nSorry, I couldn't understand your input.\nValid choices:\ntop <rank> (where rank is 1 to 200 inclusive)\nrandom";
	}

	/*
	// DOES: Sends multiple SMS messages if over 160 character limit
	// REASON: Used to handle 160 character limit for SMS messages using Twilio
	// Necessary because MessageFactor and Message classes don't seem to exist
	// in the API(?!)
	public void sendMessages(String fullQuote, String destination) {
		String quote = fullQuote;

		// Twilio Stuff
		Map<String, String> params = new HashMap<String, String>();

		int messageCounter = 1;

		while (quote.length() > 160) {
			params.put("Body", quote.substring(0, 160));
			System.out.println(quote.substring(0, 160));
			quote = quote.substring(160, quote.length()); // Cut off first 160
															// characters in
															// preparation for
															// next message
															// portion
			params.put("To", destination);
			params.put("From", FROM_PHONE);

			SmsFactory smsFactory = client.getAccount().getSmsFactory();

			try {
				smsFactory.create(params);
				System.out.println("Sent SMS message " + messageCounter
						+ " to " + destination + "!");

				// Trying to compensate for tendency for SMS messages to arrive
				// out of order (kinda like UDP, heh)
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (TwilioRestException e1) {
				System.out.println(quote);
				System.out.println(quote.length());
				e1.printStackTrace();
			}

			params.clear();
			messageCounter++;
		}

		// Last segment (length < 160, so it's not necessary to cut off the
		// first 160 characters anymore)
		params.put("Body", quote);
		System.out.println(quote);
		params.put("To", destination);
		params.put("From", "+17326497078");

		SmsFactory smsFactory = client.getAccount().getSmsFactory();
		try {
			smsFactory.create(params);
		} catch (TwilioRestException e) {
			System.out.println(quote);
			System.out.println(quote.length());
			e.printStackTrace();
		}
		System.out.println("Sent SMS message " + messageCounter + " to "
				+ destination + "!");
	}*/

	public void service(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		TwiMLResponse twiml = new TwiMLResponse();
		String receivedMessage = request.getParameter("Body");
		String[] parsedReceivedMessage = receivedMessage.split(" ");
		Sms responseMessage = null;

		if (parsedReceivedMessage.length == 2) {
			if (parsedReceivedMessage[0].equalsIgnoreCase("top")) {
				int choice = Integer.parseInt(parsedReceivedMessage[1]);
				responseMessage = new Sms(getTopQuote(choice));
			} else {
				responseMessage = new Sms(getErrorMessage());
			}
		} else if (parsedReceivedMessage.length == 1) {
			if (parsedReceivedMessage[0].equalsIgnoreCase("random")) {
				responseMessage = new Sms(getRandomQuote());
			} else {
				responseMessage = new Sms(getErrorMessage());
			}
		} else {
			responseMessage = new Sms(getErrorMessage());
		}

		try {
			twiml.append(responseMessage);
		} catch (TwiMLException e) {
			e.printStackTrace();
		}
		response.setContentType("application/xml");
		response.getWriter().print(twiml.toXML());
	}
	
	public static void main(String[] args) {
		Server server = new Server(Integer.valueOf(System.getenv("PORT")));
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new BashOrgQuoterServlet()),"/*");
        try {
			server.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
        try {
			server.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}