package d2d.testing.net.threads.workers;

import java.nio.channels.SelectionKey;

import d2d.testing.net.packets.DataReceived;
import d2d.testing.net.threads.selectors.AbstractSelector;
import d2d.testing.net.threads.selectors.ChangeRequest;

public class EchoWorker extends AbstractWorker {

    public EchoWorker(AbstractSelector selector){
        super(selector);
    }

    @Override
    protected void onWorkerRelease() {}

    @Override
    protected void parsePackets(DataReceived dataReceived) {
        dataReceived.getSelector().send(dataReceived.getData());
    }


}