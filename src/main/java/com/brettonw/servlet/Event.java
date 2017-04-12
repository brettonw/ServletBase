package com.brettonw.servlet;

import com.brettonw.bag.Bag;
import com.brettonw.bag.BagArray;
import com.brettonw.bag.BagObject;
import com.brettonw.bag.Key;
import com.brettonw.bag.formats.MimeType;
import lombok.NonNull;
import lombok.Value;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import static com.brettonw.servlet.Keys.*;

@Value
public class Event {
    private static final Logger log = LogManager.getLogger (Event.class);

    @NonNull private final BagObject query;
    @NonNull private final HttpServletRequest request;
    @NonNull private final HttpServletResponse response;

    public String getEventName () {
        return query.getString (EVENT);
    }

    public Event respond (String mimeType, String responseText) throws IOException {
        // set the response types
        String UTF_8 = StandardCharsets.UTF_8.name ();
        response.setContentType (mimeType + "; charset=" + UTF_8);
        response.setCharacterEncoding (UTF_8);

        // write out the response
        PrintWriter out = response.getWriter ();
        out.println (responseText);
        //out.flush ();
        out.close ();

        return this;
    }

    public Event respondJson (Bag bag) throws IOException {
        return respond (MimeType.JSON, bag.toString (MimeType.JSON));
    }

    public Event respondHtml (String html) throws IOException {
        return respond ("text/html", html);
    }

    public Event ok (Bag bag) throws IOException {
        return respondJson (BagObject
                .open (QUERY, query)
                .put (STATUS, OK)
                .put (RESPONSE, bag));
    }

    public Event ok () throws IOException {
        return ok (null);
    }

    public Event error (BagArray errors) throws IOException {
        // log the errors
        for (int i = 0, end = errors.getCount (); i < end; ++i) {
            log.error (errors.getString (i));
        }

        // and respond to the end user...
        return respondJson (BagObject
                .open (QUERY, query)
                .put (STATUS, ERROR)
                .put (ERROR, errors));
    }

    public Event error (String error) throws IOException {
        return error (BagArray.open (error));
    }

    public boolean hasValidParameters (BagObject parameterSpecification) throws IOException {
        BagArray errors = new BagArray ();

        // loop over the query parameters to be sure they are all valid
        String[] queryParameters = query.keys ();
        for (int i = 0; i < queryParameters.length; ++i) {
            String queryParameter = queryParameters[i];
            if (!queryParameter.equals (EVENT)) {
                if ((parameterSpecification == null) || (! parameterSpecification.has (queryParameter))) {
                    errors.add ("Unknown parameter supplied: '" + queryParameter + "'");
                }
            }
        }

        // loop over the parameter specification to be sure all of the required ones are present
        if (parameterSpecification != null) {
            String[] expectedParameters = parameterSpecification.keys ();
            for (int i = 0; i < expectedParameters.length; ++i) {
                String expectedParameter = expectedParameters[i];
                if (parameterSpecification.getBoolean (Key.cat (expectedParameter, REQUIRED), () -> false)) {
                    if (!query.has (expectedParameter)) {
                        errors.add ("Missing parameter: '" + expectedParameter + "'");
                    }
                }
            }
        }

        // check to see if there are problems
        if (errors.getCount () > 0) {
            error (errors);
            return false;
        }

        return true;
    }
}
