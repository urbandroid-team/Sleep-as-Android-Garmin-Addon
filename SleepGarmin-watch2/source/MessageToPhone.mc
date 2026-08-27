class MessageToPhone {
    var command;
    var data;
    
    // Message can be string or array
    function initialize(message) {
        // Parse string message to command and data
        if (message instanceof Toybox.Lang.String) {
            var index = message.find(";");
            if (index == null) {
                self.command = message;
                self.data = "";
            } else {
                self.command = message.substring(0, index);
                self.data = message.substring(index + 1, message.length());
            }
        } else if (message instanceof Toybox.Lang.Array) {
            self.command = message[0];
            var val = (message.size() > 1 && message[1] != null) ? message[1] : "";
            if (val instanceof Toybox.Lang.Array) {
                self.data = val.toString();
            } else {
                self.data = val;
            }
        }

        if (self.data == null) {
            self.data = "";
        }
    }

    function toRequest() {
        return {command => data};
    }
}