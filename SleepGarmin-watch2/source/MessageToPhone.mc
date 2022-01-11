class MessageToPhone {
    var command;
    var data;
    
    // Message can be string or array
    function initialize(message) {
        // Parse string message to command and data
        if (message instanceof Toybox.Lang.String) {
            if (message.find(";") == null) { 
                self.command = message;
                self.data = null; 
            } else {
                self.command = message.substring(0, (message.find(";"))-1);
                self.data = message.substring((message.find(";"))+1, message.length());
            }
        } else if (message instanceof Toybox.Lang.Array) {
            self.command = message[0];
            self.data = message[1]; 
        }

        if (self.data == null) {
            self.data = "";
        }

    }
}