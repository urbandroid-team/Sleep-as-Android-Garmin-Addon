package com.urbandroid.sleep.garmin;

import android.content.Context;

import com.urbandroid.common.logging.Logger;

import org.json.JSONException;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class HttpServer extends NanoHTTPD {

    private static final String TAG = "HttpServer#";
    Context context;

    public HttpServer(int port, Context context) {
        super(port);
        this.context = context;
    }

    private final QueueToWatch queueToWatch = QueueToWatch.getInstance();

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> params = session.getParms();

        for (Map.Entry<String,String> msg : params.entrySet()) {
            // Get all messages from watch and send them to sleep
            MessageHandler.getInstance().handleMessageFromWatchUsingHTTP(msg.getKey(), msg.getValue(), context);
        }

        // Serve all messages enqueued to watch
        return newFixedLengthResponse(serveQueue());
    }

    private String serveQueue() {
        String jsonQueue = null;
        try {
            jsonQueue = queueToWatch.getQueueAsJsonArray();
            queueToWatch.emptyQueue();
        } catch (JSONException e) {
            Logger.logSevere(TAG + "serveQueue", e);
        }
        Logger.logDebug(TAG + "serveQueue: " + jsonQueue);
        return jsonQueue;
    }
}
