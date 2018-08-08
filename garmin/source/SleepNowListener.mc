using Toybox.System as Sys;
using Toybox.Communications as Comm;

class SleepNowListener extends Comm.ConnectionListener
{
    var message;

    function initialize(msg) {
       Comm.ConnectionListener.initialize();
       message = msg;
    }

    function onComplete()
    {
        log(message + " sent OK");
    }

    function onError()
    {
        log(message + " failed to send");
    }
}