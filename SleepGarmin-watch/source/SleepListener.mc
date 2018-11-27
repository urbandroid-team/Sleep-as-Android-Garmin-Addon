using Toybox.System as Sys;
using Toybox.Communications as Comm;

class SleepListener extends Comm.ConnectionListener
{
    var message;

    function initialize(msg) {
       Comm.ConnectionListener.initialize();
       message = msg;
    }

    function onComplete()
    {
        messageQueue.remove(message);
        deliveryInProgress = false;
        log("msg sent ok, q size " + messageQueue.size());
        betalog(message + "sent OK");
    }

    function onError()
    {
        deliveryInProgress = false;
        deliveryErrorCount++;
        log(message + "failed to send");
        betalog(message + "failed to send");
    }
}
