package com.brettonw.bag.service;

import com.brettonw.bag.*;
import com.brettonw.servlet.ServletTester;
import org.junit.Test;

import java.io.IOException;

import static com.brettonw.bag.service.Keys.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class Base_Test extends Base {
    ServletTester servletTester;

    public Base_Test () {
        servletTester = new ServletTester (this);
    }

    public void handleEventHello (Event event) {
        event.ok (BagObject.open  ("testing", "123"));
    }

    public void handleEventGoodbye (Event event) {
        assertTrue (event.getQuery () != null);
        assertTrue (event.getQuery ().has (POST_DATA));
        assertTrue (event.getRequest () != null);
        if (event.getQuery ().has ("param3") && (event.getQuery ().getInteger ("param3") == 2)) {
            // deliberately invoke a failure to test the failure handling
            assertTrue (false);
        }
        event.ok (BagObject.open ("testing", "456"));
    }

    public void handleEventDashName (Event event) {
        event.ok ();
    }

    private void assertGet (BagObject bagObject, BagObject query) {
        assertTrue (bagObject.getString (STATUS).equals (OK));
        bagObject = bagObject.getBagObject (QUERY).select (new SelectKey (SelectType.EXCLUDE, POST_DATA));
        assertTrue (bagObject.equals (query));
    }

    @Test
    public void testAttribute () {
        assertTrue (getContext () != null);
        assertTrue (getAttribute (SERVLET) == this);
    }

    @Test
    public void testBadInstall () {
        String event = "JUNK";
        assertFalse (install ("JUNK"));
    }

    @Test
    public void testUnknownEvent () throws IOException {
        BagObject query = BagObject.open (EVENT, "nohandler");
        assertTrue (servletTester.bagObjectFromGet (query).getString (STATUS).equals (ERROR));
    }

    @Test
    public void testMissingHandler () throws IOException {
        BagObject query = BagObject.open (EVENT, "no-handler");
        assertTrue (servletTester.bagObjectFromGet (query).getString (STATUS).equals (ERROR));
    }

    @Test
    public void testGet () throws IOException {
        BagObject query = BagObject
                .open (EVENT, "hello")
                .put ("param1", 1)
                .put ("param2", 2);
        assertGet (servletTester.bagObjectFromGet (query), query);

        query.put ("param3", 3);
        assertGet (servletTester.bagObjectFromGet (query), query);

        query.put ("param4", 4);
        assertTrue (servletTester.bagObjectFromGet (query).getString (STATUS).equals (ERROR));
    }

    @Test
    public void testGetOk () throws IOException {
        BagObject query = BagObject.open (EVENT, OK);
        assertGet (servletTester.bagObjectFromGet (query), query);

        query.put ("param4", 4);
        assertGet (servletTester.bagObjectFromGet (query), query);
    }

    @Test
    public void testPost () throws IOException {
        BagObject query = BagObject
                .open (EVENT, "goodbye")
                .put ("param1", 1)
                .put ("param2", 2);
        BagObject postData = BagObjectFrom.resource (getClass (), "/testPost.json");
        BagObject response = servletTester.bagObjectFromPost (query, postData);
        assertGet (response, query);
        assertTrue (response.getBagObject (QUERY).has (POST_DATA));
        assertTrue (response.getBagObject (QUERY).getBagObject (POST_DATA).equals (postData));

        query.put ("param3", 3);
        response = servletTester.bagObjectFromPost (query, postData);
        assertGet (response, query);
        assertTrue (response.getBagObject (QUERY).has (POST_DATA));
        assertTrue (response.getBagObject (QUERY).getBagObject (POST_DATA).equals (postData));

        query.put ("param4", 4);
        assertTrue (servletTester.bagObjectFromPost (query, postData).getString (STATUS).equals (ERROR));
        query.remove ("param4");

        query.put ("param3", 2);
        assertTrue (servletTester.bagObjectFromPost (query, postData).getString (STATUS).equals (ERROR));
    }

    @Test
    public void testEmptyRequest () throws IOException {
        BagObject response = servletTester.bagObjectFromGet ("");
        assertTrue (response.getString (STATUS).equals (ERROR));
        assertTrue (response.getString (Key.cat (ERROR, 0)).equals ("Missing '" + EVENT + "'"));
    }

    @Test
    public void testHelp () throws IOException {
        BagObject query = BagObject.open (EVENT, HELP);
        BagObject response = servletTester.bagObjectFromGet (query);
        assertTrue (response.getString (STATUS).equals (OK));

        // make a dummy object that should match the response, and verify it does
        BagObject verify = BagObjectFrom.resource (Base_Test.class, "/api.json");
        String help = Key.cat (EVENTS, HELP);
        verify.put (help, api.getObject (help));
        verify.put (NAME, api.getObject (NAME));
        assertTrue (response.getBagObject (RESPONSE).equals (verify));
    }

    @Test
    public void testBadGet () throws IOException {
        BagObject query = BagObject
                .open (EVENT, "halp")
                .put ("param1", 1)
                .put ("param2", 2);
        assertTrue (servletTester.bagObjectFromGet (query).getString (STATUS).equals (ERROR));
    }

    @Test
    public void testBadParameters () throws IOException {
        BagObject query = BagObject
                .open (EVENT, "hello")
                .put ("param1", 1)
                .put ("param3", 3);
        assertTrue (servletTester.bagObjectFromGet (query).getString (STATUS).equals (ERROR));
    }

    @Test
    public void testVersion () throws IOException {
        BagObject query = BagObject.open (EVENT, VERSION);
        BagObject response = servletTester.bagObjectFromGet (query);
        //assertTrue (response.getString (Key.cat (RESPONSE, VERSION)).equals (UNKNOWN));
        //assertTrue (response.getString (Key.cat (RESPONSE, NAME)).equals ("ServletTester"));
        assertTrue (response.getString (STATUS).equals (OK));
    }

    @Test
    public void testMultiple () throws IOException {
        BagObject query = BagObject.open (EVENT, MULTIPLE);
        BagArray postData = BagArray.open (BagObject.open (EVENT, VERSION)).add (BagObject.open (EVENT, "help"));
        assertTrue (servletTester.bagObjectFromPost (query, query).getString (STATUS).equals (ERROR));
        assertTrue (servletTester.bagObjectFromPost (query, postData).getString (STATUS).equals (OK));
    }

    @Test
    public void testDashName () throws IOException {
        BagObject query = BagObject.open (EVENT, "-dash-name");
        assertGet (servletTester.bagObjectFromGet (query), query);
    }
}
