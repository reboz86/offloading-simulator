package ditl.sim.pnt;

import ditl.Bus;
import ditl.sim.*;

public class PntRouterFactory implements RouterFactory {

	private final AdHocRadio adhoc_radio;
	private final PervasiveInfraRadio infra_radio;
	private final Bus<BufferEvent> _bus;
	private final Router infra_router;
	private final int buffer_size;
	private final Integer initialCopies;
	private long ackSize;
	private String strategy;
	
	public PntRouterFactory(Router infraRouter, PervasiveInfraRadio infraRadio, 
			AdHocRadio adhocRadio, int bufferSize, Bus<BufferEvent> bus, Integer initialCopies,long ackSize, String strategy){
		adhoc_radio = adhocRadio;
		infra_router = infraRouter;
		infra_radio = infraRadio;
		buffer_size = bufferSize;
		_bus = bus;
		this.ackSize=ackSize;
		this.initialCopies=initialCopies;
		this.strategy=strategy;
		
	}
	
	@Override
	public Router getNew(Integer id) {
		
		if ( id.equals(infra_router.id()) ){
			return infra_router;
		}
		
		if(initialCopies==null)
			return new PntRouter(adhoc_radio, infra_radio, id, buffer_size, _bus,ackSize, strategy);
		return new PntSprayRouter(adhoc_radio, infra_radio, id, buffer_size, _bus, initialCopies,ackSize);
		
	}

}
